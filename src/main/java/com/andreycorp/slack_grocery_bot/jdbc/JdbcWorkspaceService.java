package com.andreycorp.slack_grocery_bot.jdbc;

import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO for persisting and retrieving workspace (tenant) credentials.
 */

@Repository
public class JdbcWorkspaceService {
    private final DataSource ds;

    public JdbcWorkspaceService(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Inserts a new workspace or updates its bot token and signing secret on conflict.
     */

    public void upsertWorkspace(String teamId, String botToken, String signingSecret) {
        String sql = "INSERT INTO workspace(team_id, bot_token, signing_secret, created_at) " +
                "VALUES(?, ?, ?, now()) " +
                "ON CONFLICT (team_id) DO UPDATE " +
                "  SET bot_token = EXCLUDED.bot_token, " +
                "      signing_secret = EXCLUDED.signing_secret";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, botToken);
            ps.setString(3, signingSecret);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to upsert workspace", ex);
        }
    }

    /**
     * Retrieves the bot token for the given workspace.
     * @param teamId Slack workspace ID
     * @return stored bot_token
     */

    public String getBotToken(String teamId) {
        String sql = "SELECT bot_token FROM workspace WHERE team_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("bot_token");
                } else {
                    throw new RuntimeException("No workspace found for team_id " + teamId);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to retrieve bot token", ex);
        }
    }
}
