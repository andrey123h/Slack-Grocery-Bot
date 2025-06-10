package com.andreycorp.slack_grocery_bot;

import com.andreycorp.slack_grocery_bot.parsers.OrderParser;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderParserTest {

    private final OrderParser parser = new OrderParser();

    @Test
    void parseSimpleOrderWithQuantityAndItem() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 apples");

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
    }

    @Test
    void parseMultipleOrdersSeparatedByCommas() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 apples, 3 bananas, 1 milk");

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
        assertEquals(3, result.get(1).qty);
        assertEquals("bananas", result.get(1).item);
        assertEquals(1, result.get(2).qty);
        assertEquals("milk", result.get(2).item);
    }

    @Test
    void parseMultipleOrdersSeparatedByPeriods() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 apples. 3 bananas. 1 milk");

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
        assertEquals(3, result.get(1).qty);
        assertEquals("bananas", result.get(1).item);
        assertEquals(1, result.get(2).qty);
        assertEquals("milk", result.get(2).item);
    }

    @Test
    void stripBotMentionFromOrderText() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("<@BOTID> 2 apples, 3 bananas");

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
    }

    @Test
    void defaultToQuantityOneWhenNotSpecified() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("apples");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
    }

    @Test
    void handleMixOfSpecifiedAndDefaultQuantities() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 apples, bananas, 3 oranges");

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
        assertEquals(1, result.get(1).qty);
        assertEquals("bananas", result.get(1).item);
        assertEquals(3, result.get(2).qty);
        assertEquals("oranges", result.get(2).item);
    }

    @Test
    void handleEmptyMessage() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("");

        assertTrue(result.isEmpty());
    }

    @Test
    void handleWhitespaceOnlyMessage() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void skipEmptyEntriesBetweenDelimiters() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 apples,, 3 bananas");

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
        assertEquals(3, result.get(1).qty);
        assertEquals("bananas", result.get(1).item);
    }

    @Test
    void normalizeItemNamesToLowercase() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 APPLES, 3 BaNaNaS");

        assertEquals(2, result.size());
        assertEquals("apples", result.get(0).item);
        assertEquals("bananas", result.get(1).item);
    }

    @Test
    void handleVariousWhitespacePatterns() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("  2    apples   ,   3     bananas  ");

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
        assertEquals(3, result.get(1).qty);
        assertEquals("bananas", result.get(1).item);
    }


    @Test
    void handleLargeQuantityValues() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("999999 apples");

        assertEquals(1, result.size());
        assertEquals(999999, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
    }

    @Test
    void handleMultiWordItemNames() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 green apples, 3 red onions");

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("green apples", result.get(0).item);
        assertEquals(3, result.get(1).qty);
        assertEquals("red onions", result.get(1).item);
    }

    @Test
    void handleMixedDelimiters() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 apples, 3 bananas. 1 orange; 4 peaches");

        assertEquals(4, result.size());
        assertEquals("apples", result.get(0).item);
        assertEquals("bananas", result.get(1).item);
        assertEquals("orange", result.get(2).item);
        assertEquals("peaches", result.get(3).item);
    }

    @Test
    void handleItemsWithSpecialCharacters() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 hähnchen, 3 crème fraîche");

        assertEquals(2, result.size());
        assertEquals("hähnchen", result.get(0).item);
        assertEquals("crème fraîche", result.get(1).item);
    }

    @Test
    void handleItemsWithNumbersInName() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 7up, 3 coca-cola");

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("7up", result.get(0).item);
        assertEquals(3, result.get(1).qty);
        assertEquals("coca-cola", result.get(1).item);
    }

    @Test
    void handleOrdersWithAdditionalContextInfo() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 apples (organic), 3 bananas from the market");

        assertEquals(2, result.size());
        assertEquals("apples (organic)", result.get(0).item);
        assertEquals("bananas from the market", result.get(1).item);
    }

    @Test
    void handleExtremelyLongOrderMessage() {
        StringBuilder longOrder = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longOrder.append(i).append(" item").append(i).append(", ");
        }

        List<OrderParser.ParsedOrder> result = parser.parseAll(longOrder.toString());

        assertEquals(50, result.size());
        assertEquals(49, result.get(49).qty);
        assertEquals("item49", result.get(49).item);
    }

    @Test
    void handleItemsWithEmoji() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 🍎 apples, 3 🍌 bananas");

        assertEquals(2, result.size());
        assertEquals("🍎 apples", result.get(0).item);
        assertEquals("🍌 bananas", result.get(1).item);
    }

    @Test
    void handleInvalidQuantityFormat() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("two apples, many bananas");

        assertEquals(2, result.size());
        // Assuming parser defaults to 1 when it can't parse the quantity
        assertEquals(1, result.get(0).qty);
        assertEquals("two apples", result.get(0).item);
        assertEquals(1, result.get(1).qty);
        assertEquals("many bananas", result.get(1).item);
    }

    @Test
    void handleMultipleConsecutiveDelimiters() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2 apples,,,,3 bananas....4 oranges");

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).qty);
        assertEquals("apples", result.get(0).item);
        assertEquals(3, result.get(1).qty);
        assertEquals("bananas", result.get(1).item);
        assertEquals(4, result.get(2).qty);
        assertEquals("oranges", result.get(2).item);
    }

    @Test
    void parseFractionalQuantity() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("2.5 apples");

        assertEquals(1, result.size(), "Should parse exactly one order");
        assertEquals(2.5, result.get(0).qty, 1e-6, "Quantity should be parsed as 2.5");
        assertEquals("apples", result.get(0).item, "Item name should be 'apples'");
    }

    @Test
    void delimiterPeriodOnlyBeforeQuantity() {
        List<OrderParser.ParsedOrder> result = parser.parseAll("apples. 2 bananas");

        assertEquals(2, result.size(), "Should split into two orders");
        // first token "apples" defaults to qty = 1
        assertEquals(1.0, result.get(0).qty, 1e-6, "First order defaults qty=1");
        assertEquals("apples", result.get(0).item, "First item should be 'apples'");
        // second token "2 bananas" parses normally
        assertEquals(2.0, result.get(1).qty, 1e-6, "Second order qty=2");
        assertEquals("bananas", result.get(1).item, "Second item should be 'bananas'");
    }

}