package com.andreycorp.slack_grocery_bot.parsers;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A component responsible for converting raw Slack order messages into
 * structured quantity–item pairs. Supports processing multiple orders in a single
 * message, separated by commas, semicolons, or period delimiters only when followed by a space+digit.
 *
 * This parser handles various input formats including:
 * - Single items: "2 apples"
 * - Multiple items: "2 apples, 3 bananas"
 * - Mixed delimiters: "2 apples, 3 bananas. 1 orange; 4 peaches"
 * - Items without quantities (defaults to 1)
 * - Fractional quantities: "1.5 kg sugar"
 * - Items with special characters: "2 hähnchen, 3 crème fraîche"
 * - Multi-word items: "2 green apples"
 */
@Component
public class OrderParser {

    /**
     * Regex pattern matching optional leading whitespace, a quantity (integer or decimal),
     * optional whitespace, then the item name.
     * Group 1 captures the quantity digits (possibly with decimal), Group 2 captures the item text.
     */
    private static final Pattern ENTRY_PATTERN =
            Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*(.+)$");

    /**
     * Processes a raw message string (which include a bot mention and multiple
     * order entries) and returns a list of parsed orders. Each entry is split on
     * commas or semicolons, or on periods that precede a space+digit, and if no leading quantity is found, defaults to 1.
     *
     * @param rawText the original message text from Slack, possibly including '<@...>' mention
     * @return a list of ParsedOrder objects, one per detected order entry
     */
    public List<ParsedOrder> parseAll(String rawText) {
        // Strip bot mention and trim whitespace
        String text = rawText.trim().replaceAll("^<@[^>]+>\\s*", "");
        // Split on commas, semicolons, or periods when followed by space+digit
        String[] tokens = text.split("\\s*(?:[,;]|\\.(?=\\s+\\d))\\s*");
        List<ParsedOrder> orders = new ArrayList<>();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
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
            double qty = Double.parseDouble(matcher.group(1));
            String item = matcher.group(2).trim();
            return new ParsedOrder(qty, item);
        } else {
            // Default to qty = 1
            return new ParsedOrder(1.0, trimmed);
        }
    }

    /**
     * Holder for a parsed order entry, capturing the quantity and the item name.
     */
    public static class ParsedOrder {
        /** the number of items requested, integer or fraction */
        public final double qty;
        /** the normalized name of the item */
        public final String item;

        /**
         * Constructs a ParsedOrder with the given quantity and item name.
         *
         * @param qty  the parsed quantity
         * @param item the parsed item description
         */
        public ParsedOrder(double qty, String item) {
            this.qty = qty;
            this.item = item;
        }

        @Override
        public String toString() {
            return qty + "× " + item;
        }
    }
}
