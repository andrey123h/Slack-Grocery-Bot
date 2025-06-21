package com.andreycorp.slack_grocery_bot.Services;

import com.andreycorp.slack_grocery_bot.jdbc.JdbcDefaultsStoreService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service façade over the JDBC DAO for default grocery items,
 * note : this was in memory before
 */

@Service
public class DefaultsStoreService {
    private final JdbcDefaultsStoreService dao;

    public DefaultsStoreService(JdbcDefaultsStoreService dao) {
        this.dao = dao;
    }

    /** Fetch all defaults for the current workspace. */
    public Map<String, Integer> listAll() {
        return dao.listAll();
    }

    /** Create or update the default quantity for an item. */
    public void upsertDefault(String itemName, int qty) {
        dao.upsertDefault(itemName, qty);
    }

    /** Remove a default item by name. */
    public void deleteDefault(String itemName) {
        dao.deleteDefault(itemName);
    }
}
