package com.andreycorp.slack_grocery_bot;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A  component responsible for converting raw Slack order messages into
 * structured quantity–item pairs. Supports processing multiple orders in a single
 * message, separated by commas or periods.
 */

@Component
public class OrderParser {

    /**
     * Regex pattern matching optional leading whitespace, a quantity (one or more digits),
     * optional whitespace, then the item name.
     * Group 1 captures the quantity digits, Group2 ca ptures the item text.
     */
    private static final Pattern ENTRY_PATTERN =
            Pattern.compile("^\\s*(\\d+)\\s*(.+)$");

    /**
     * Processes a raw message string (which may include a bot mention and multiple
     * order entries) and returns a list of parsed orders. Each entry is split on
     * commas or periods, and if no leading quantity is found, defaults to 1.
     *
     * @param rawText the original message text from Slack, possibly including '<@...>' mention
     * @return a list of ParsedOrder objects, one per detected order entry
     */
    public List<ParsedOrder> parseAll(String rawText) {
        // Strip bot mention and trim whitespace
        String text = rawText.trim().replaceAll("^<@[^>]+>\\s*", "");
        // Split on commas or periods
        String[] tokens = text.split("\\s*(?:[,\\.])\\s*");
        List<ParsedOrder> orders = new ArrayList<>();
        for (String token : tokens) {
            if (token.isBlank()) continue;
            orders.add(parseEntry(token));
        }
        return orders;
    }

    /**
     * Parses a single token into a ParsedOrder. If the token matches ENTRY_PATTERN,
     * the extracted quantity and item are used; otherwise defaults to qty=1.
     *
     * @param entry a string representing one order, e.g. "2 apples" or "banana"
     * @return a ParsedOrder containing the quantity and item name
     */
    private ParsedOrder parseEntry(String entry) {
        String trimmed = entry.trim().toLowerCase();
        Matcher matcher = ENTRY_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            int qty = Integer.parseInt(matcher.group(1));
            String item = matcher.group(2).trim();
            return new ParsedOrder(qty, item);
        } else {
            // Default to qty = 1
            return new ParsedOrder(1, trimmed);
        }
    }

    /**
     * Holder for a parsed order entry, capturing the quantity and the item name.
     */
    public static class ParsedOrder {
        /** the number of items requested */
        public final int qty;
        /** the normalized name of the item */
        public final String item;

        /**
         * Constructs a ParsedOrder with the given quantity and item name.
         *
         * @param qty  the parsed quantity
         * @param item the parsed item description
         */
        public ParsedOrder(int qty, String item) {
            this.qty = qty;
            this.item = item;
        }

        @Override
        public String toString() {
            return qty + "× " + item;
        }
    }
}
