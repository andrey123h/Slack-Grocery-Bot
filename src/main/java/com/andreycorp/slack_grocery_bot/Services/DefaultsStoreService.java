package com.andreycorp.slack_grocery_bot.Services;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the workspace‐admin’s list of default groceries in memory.
 * Key = item name (String), Value = quantity (Integer).
 */
@Service
public class DefaultsStoreService {
    // Use LinkedHashMap to preserve insertion order
    private final Map<String, Integer> defaults = new LinkedHashMap<>();

    /** Return a copy of the map so callers can render it (e.g. in Home view). */
    public synchronized Map<String, Integer> listAll() {
        return new LinkedHashMap<>(defaults);
    }

    /** Create or update a default item. */
    public synchronized void upsertDefault(String itemName, int qty) {
        defaults.put(itemName, qty);
    }

    /** Remove a default item by name. */
    public synchronized void deleteDefault(String itemName) {
        defaults.remove(itemName);
    }
}
