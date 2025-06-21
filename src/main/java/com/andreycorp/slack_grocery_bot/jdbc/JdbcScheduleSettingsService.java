package com.andreycorp.slack_grocery_bot.jdbc;

import com.andreycorp.slack_grocery_bot.context.TenantContext;
import com.andreycorp.slack_grocery_bot.model.ScheduleSettings;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for persisting and retrieving per-tenant schedule settings.
 * Responsible for all reads and writes of the schedule_settings table
 */

@Repository
public class JdbcScheduleSettingsService {
    private final DataSource ds;
    private final TenantContext tenantContext;

    public JdbcScheduleSettingsService(DataSource ds, TenantContext tenantContext) {
        this.ds = ds;
        this.tenantContext = tenantContext; // holds the current workspace ID
    }

    //––– EXISTING REQUEST-SCOPED METHODS –––//
    // Inside an HTTP request. Whenever Slack sends  a web-hook. workspace ID is already set in TenantContext.

    /**
     * Fetch the schedule settings for the current workspace (from TenantContext).
     * @return ScheduleSettings or null if none exist
     */
    public ScheduleSettings findByTeamId() {
        return findByTeamId(tenantContext.getTeamId());
    }

    /**
     * Upsert the schedule settings for the current workspace (from TenantContext).
     */
    public void upsert(String openDay, LocalTime openTime,
                       String closeDay, LocalTime closeTime) {
        upsert(tenantContext.getTeamId(), openDay, openTime, closeDay, closeTime);
    }

    //––– NEW EXPLICIT-TENANT ID METHODS –––//
    // Outside any HTTP request. On application startup and  Cron callback threads, there isn’t a live web request
    // so TenantContext isn’t populated. must pass the teamId explicitly.

    /**
     * Fetch the schedule settings for the given workspace.
     * Safe to call at startup without a request scope.
     * @param teamId Slack workspace ID
     * @return ScheduleSettings or null if none exist
     */
    public ScheduleSettings findByTeamId(String teamId) {
        String sql = """
            SELECT open_day, open_time, close_day, close_time
              FROM schedule_settings
             WHERE team_id = ?
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String openDay  = rs.getString("open_day");
                    Time   ot       = rs.getTime("open_time");
                    String closeDay = rs.getString("close_day");
                    Time   ct       = rs.getTime("close_time");
                    return new ScheduleSettings(
                            openDay,
                            ot.toLocalTime().toString(),
                            closeDay,
                            ct.toLocalTime().toString()
                    );
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Failed to fetch schedule_settings for team_id=" + teamId, ex
            );
        }
        return null;
    }

    /**
     * Upsert the schedule settings for the given workspace.
     * Safe to call at startup without a request scope.
     *
     * @param teamId    Slack workspace ID
     * @param openDay   e.g. "MON"
     * @param openTime  e.g. LocalTime.of(9,0)
     * @param closeDay  e.g. "THU"
     * @param closeTime e.g. LocalTime.of(17,0)
     */
    public void upsert(String teamId,
                       String openDay, LocalTime openTime,
                       String closeDay, LocalTime closeTime) {
        String sql = """
            INSERT INTO schedule_settings(team_id, open_day, open_time, close_day, close_time)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (team_id) DO UPDATE SET
              open_day   = EXCLUDED.open_day,
              open_time  = EXCLUDED.open_time,
              close_day  = EXCLUDED.close_day,
              close_time = EXCLUDED.close_time,
              updated_at = now()
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, teamId);
            ps.setString(2, openDay);
            ps.setTime(3, Time.valueOf(openTime));
            ps.setString(4, closeDay);
            ps.setTime(5, Time.valueOf(closeTime));
            ps.executeUpdate();

        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Failed to upsert schedule_settings for team_id=" + teamId, ex
            );
        }
    }

    /**
     * Return the list of all workspace IDs.
     * Used at startup to bootstrap each tenant's scheduler.
     */
    public List<String> findAllTeamIds() {
        String sql = "SELECT team_id FROM workspace";
        List<String> ids = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getString("team_id"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to list all workspaces", ex);
        }
        return ids;
    }
}
