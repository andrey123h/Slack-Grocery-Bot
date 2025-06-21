package com.andreycorp.slack_grocery_bot.jdbc;

import com.andreycorp.slack_grocery_bot.context.TenantContext;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC-backed implementation of the DefaultsStoreService,
 * persisting default grocery items per workspace.
 */

@Repository
public class JdbcDefaultsStoreService {
    private final DataSource ds;
    private final TenantContext tenantContext;

    public JdbcDefaultsStoreService(DataSource ds, TenantContext tenantContext) {
        this.ds = ds;
        this.tenantContext = tenantContext; // holds the current workspace ID
    }

    /**
     * Return all default items for the current workspace as a LinkedHashMap to preserve insertion order.
     */
    public Map<String, Integer> listAll() {
        String sql = "SELECT item_name, quantity FROM default_item WHERE team_id = ? ORDER BY id";
        Map<String, Integer> defaults = new LinkedHashMap<>();
        String teamId = tenantContext.getTeamId();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    defaults.put(
                            rs.getString("item_name"),
                            rs.getInt("quantity")
                    );
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Failed to list defaults for team_id " + teamId, ex
            );
        }
        return defaults;
    }

    /**
     * Create or update a default item for the current workspace.
     */
    public void upsertDefault(String itemName, int qty) {
        String sql = "INSERT INTO default_item(team_id, item_name, quantity) " +
                "VALUES(?, ?, ?) " +
                "ON CONFLICT (team_id, item_name) DO UPDATE " +
                "SET quantity = EXCLUDED.quantity";
        String teamId = tenantContext.getTeamId();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, itemName);
            ps.setInt(3, qty);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Failed to upsert default item '" + itemName + "' for team_id " + teamId,
                    ex
            );
        }
    }

    /**
     * Remove a default item by name for the current workspace.
     */
    public void deleteDefault(String itemName) {
        String sql = "DELETE FROM default_item WHERE team_id = ? AND item_name = ?";
        String teamId = tenantContext.getTeamId();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, itemName);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Failed to delete default item '" + itemName + "' for team_id " + teamId,
                    ex
            );
        }
    }
}
