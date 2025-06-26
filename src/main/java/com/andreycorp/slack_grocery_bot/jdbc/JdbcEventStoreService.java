package com.andreycorp.slack_grocery_bot.jdbc;

import com.andreycorp.slack_grocery_bot.context.TenantContext;
import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.andreycorp.slack_grocery_bot.model.ReactionEvent;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed implementation of EventStore that persists and loads
 * Slack message and reaction events, with both legacy and per-tenant methods.
 */
@Repository
public class JdbcEventStoreService implements EventStore {
    private final DataSource ds;
    private final TenantContext tenantContext;

    public JdbcEventStoreService(DataSource ds, TenantContext tenantContext) {
        this.ds = ds;
        this.tenantContext = tenantContext;  // for legacy request-scoped methods
    }

    @Override
    public void saveMessage(MessageEvent e) {
        String sql = "INSERT INTO message_event(team_id,channel_id,user_id,text,ts,ts_epoch) VALUES(?,?,?,?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.teamId());
            ps.setString(2, e.channel());
            ps.setString(3, e.user());
            ps.setString(4, e.text());
            ps.setString(5, e.ts());
            ps.setDouble(6, Double.parseDouble(e.ts()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("saveMessage failed", ex);
        }
    }

    @Override
    public void saveReaction(ReactionEvent e) {
        String sql = "INSERT INTO reaction_event(team_id,channel_id,user_id,reaction,ts,ts_epoch) VALUES(?,?,?,?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.teamId());
            ps.setString(2, e.channel());
            ps.setString(3, e.user());
            ps.setString(4, e.reaction());
            ps.setString(5, e.ts());
            ps.setDouble(6, Double.parseDouble(e.ts()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("saveReaction failed", ex);
        }
    }

    @Override
    public List<MessageEvent> fetchMessagesSince(String fromTs) {
        String sql = "SELECT team_id,user_id,channel_id,text,ts " +
                "FROM message_event WHERE team_id = ? AND ts_epoch >= ? " +
                "ORDER BY ts_epoch";
        List<MessageEvent> out = new ArrayList<>();
        String currentTeamId = tenantContext.getTeamId();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, currentTeamId);
            ps.setDouble(2, Double.parseDouble(fromTs));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new MessageEvent(
                            rs.getString("team_id"),
                            rs.getString("user_id"),
                            rs.getString("channel_id"),
                            rs.getString("text"),
                            rs.getString("ts")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("fetchMessagesSince failed", ex);
        }
        return out;
    }

    @Override
    public List<ReactionEvent> fetchReactionsSince(String fromTs) {
        String sql = "SELECT team_id,user_id,reaction,channel_id,ts " +
                "FROM reaction_event WHERE team_id = ? AND ts_epoch >= ? " +
                "ORDER BY ts_epoch";
        List<ReactionEvent> out = new ArrayList<>();
        String currentTeamId = tenantContext.getTeamId();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, currentTeamId);
            ps.setDouble(2, Double.parseDouble(fromTs));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ReactionEvent(
                            rs.getString("team_id"),
                            rs.getString("user_id"),
                            rs.getString("reaction"),
                            rs.getString("channel_id"),
                            rs.getString("ts")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("fetchReactionsSince failed", ex);
        }
        return out;
    }

    @Override
    public void pruneEventsBefore(String beforeTs) {
        String delMsg   = "DELETE FROM message_event WHERE team_id = ? AND ts_epoch < ?";
        String delReact = "DELETE FROM reaction_event WHERE team_id = ? AND ts_epoch < ?";
        String currentTeamId = tenantContext.getTeamId();
        try (Connection c = ds.getConnection();
             PreparedStatement ps1 = c.prepareStatement(delMsg);
             PreparedStatement ps2 = c.prepareStatement(delReact)) {
            ps1.setString(1, currentTeamId);
            ps1.setDouble(2, Double.parseDouble(beforeTs));
            ps1.executeUpdate();
            ps2.setString(1, currentTeamId);
            ps2.setDouble(2, Double.parseDouble(beforeTs));
            ps2.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("pruneEventsBefore failed", ex);
        }
    }

    // --- Per-tenant implementations ---

    @Override
    public List<MessageEvent> fetchMessagesForTeam(String teamId) {
        String sql = "SELECT team_id,user_id,channel_id,text,ts " +
                "FROM message_event WHERE team_id = ? ORDER BY ts_epoch";
        List<MessageEvent> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new MessageEvent(
                            rs.getString("team_id"),
                            rs.getString("user_id"),
                            rs.getString("channel_id"),
                            rs.getString("text"),
                            rs.getString("ts")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("fetchMessagesForTeam failed", ex);
        }
        return out;
    }

    @Override
    public List<ReactionEvent> fetchReactionsForTeam(String teamId) {
        String sql = "SELECT team_id,user_id,reaction,channel_id,ts " +
                "FROM reaction_event WHERE team_id = ? ORDER BY ts_epoch";
        List<ReactionEvent> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ReactionEvent(
                            rs.getString("team_id"),
                            rs.getString("user_id"),
                            rs.getString("reaction"),
                            rs.getString("channel_id"),
                            rs.getString("ts")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("fetchReactionsForTeam failed", ex);
        }
        return out;
    }

    @Override
    public void pruneEventsBeforeForTeam(String teamId, String beforeTs) {
        String delMsg   = "DELETE FROM message_event WHERE team_id = ? AND ts_epoch < ?";
        String delReact = "DELETE FROM reaction_event WHERE team_id = ? AND ts_epoch < ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps1 = c.prepareStatement(delMsg);
             PreparedStatement ps2 = c.prepareStatement(delReact)) {
            ps1.setString(1, teamId);
            ps1.setDouble(2, Double.parseDouble(beforeTs));
            ps1.executeUpdate();
            ps2.setString(1, teamId);
            ps2.setDouble(2, Double.parseDouble(beforeTs));
            ps2.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("pruneEventsBeforeForTeam failed", ex);
        }
    }
}
