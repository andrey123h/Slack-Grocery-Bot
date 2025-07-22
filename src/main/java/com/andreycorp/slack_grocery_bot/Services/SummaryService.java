package com.andreycorp.slack_grocery_bot.Services;

import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.andreycorp.slack_grocery_bot.parsers.OrderParser;
import com.andreycorp.slack_grocery_bot.model.ReactionEvent;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Service for summarizing weekly grocery orders from Slack messages.
 * Aggregates orders by user, counts reactions, and formats a summary message.
 */

@Service
public class SummaryService {

    private final SlackMessageService slackMessageService;
    private final OrderParser orderParser;
    private final EventStore eventStore;

    public SummaryService(
            SlackMessageService slackMessageService,
            OrderParser orderParser,
            EventStore eventStore
    ) {
        this.slackMessageService = slackMessageService;
        this.orderParser = orderParser;
        this.eventStore = eventStore;
    }


    /**
     * Pulls every order & +1 reaction in *this* workspace and
     * returns a Markdown summary.
     *
     */
    public String generateSummaryMarkdown() {
        //  fetch all messages for this tenant
        List<MessageEvent> msgs = eventStore.fetchMessagesSince("0");
        // parse orders
        OrderSummaryData data = processMessageEvents(msgs);
        //  fetch +1 reactions
        Map<String, Long> plusOnes = eventStore.fetchReactionsSince("0").stream()
                .filter(r -> "+1".equals(r.reaction()))
                .collect(Collectors.groupingBy(ReactionEvent::ts, Collectors.counting()));
        //  render
        return buildSummaryText(data, plusOnes);
    }



    /**
     * Processes message events to extract order information by user and item.
     */
    public OrderSummaryData processMessageEvents(List<MessageEvent> events) {
        Map<String, Map<String, Double>> ordersByUser = new HashMap<>();
        Map<String, Map<String, List<String>>> tsByUserItem = new HashMap<>();

        for (MessageEvent m : events) {
            String user = m.user();

            ordersByUser.computeIfAbsent(user, u -> new HashMap<>());
            tsByUserItem.computeIfAbsent(user, u -> new HashMap<>());

            List<OrderParser.ParsedOrder> parsed = orderParser.parseAll(m.text());
            for (OrderParser.ParsedOrder po : parsed) {
                ordersByUser.get(user)
                        .merge(po.item, po.qty, Double::sum);

                tsByUserItem.get(user)
                        .computeIfAbsent(po.item, i -> new ArrayList<>())
                        .add(m.ts());
            }
        }

        return new OrderSummaryData(ordersByUser, tsByUserItem);
    }

    /**
     * Processes reaction events to count +1 reactions by message timestamp.
     */
    public Map<String, Long> processReactions(String threadTs) {
        List<ReactionEvent> reactions = eventStore.fetchReactionsSince(threadTs);

        return reactions.stream()
                .filter(r -> "+1".equals(r.reaction()))
                .collect(Collectors.groupingBy(
                        ReactionEvent::ts,
                        Collectors.counting()
                ));
    }

    /**
     * Builds a formatted summary text from order data and reaction counts.
     */
    public String buildSummaryText(OrderSummaryData orderData, Map<String, Long> plusOneCountByTs) {
        StringBuilder summary = new StringBuilder("*Weekly Grocery Summary:*\n");

        orderData.ordersByUser.forEach((user, items) -> {
            summary.append("• <@").append(user).append(">: ");
            String line = items.entrySet().stream()
                    .map(e -> formatItemEntry(e, user, orderData.tsByUserItem, plusOneCountByTs))
                    .collect(Collectors.joining(", "));
            summary.append(line).append("\n");
        });

        return summary.toString();
    }

    /**
     * Formats a single item entry with quantity and reaction counts.
     */
    private String formatItemEntry(
            Map.Entry<String, Double> entry,
            String user,
            Map<String, Map<String, List<String>>> tsByUserItem,
            Map<String, Long> plusOneCountByTs) {

        String item = entry.getKey();
        double qty = entry.getValue();

        long totalReacts = tsByUserItem.getOrDefault(user, Collections.emptyMap())
                .getOrDefault(item, Collections.emptyList())
                .stream()
                .mapToLong(ts -> plusOneCountByTs.getOrDefault(ts, 0L))
                .sum();

        String suffix = totalReacts > 0
                ? String.format(" (%d× 👍)", totalReacts)
                : "";

        // Format quantity: drop .0 for whole numbers
        String qtyStr = (qty == (long) qty)
                ? String.format("%d", (long) qty)
                : String.valueOf(qty);

        return String.format("%s× %s%s", qtyStr, item, suffix);
    }


    /**
     * Data class to hold order summary information.
     */
    private static class OrderSummaryData {
        // user → (item → totalQty)
        final Map<String, Map<String, Double>> ordersByUser;
        // user → (item → list of messageTs where that item appeared)
        final Map<String, Map<String, List<String>>> tsByUserItem;

        OrderSummaryData(
                Map<String, Map<String, Double>> ordersByUser,
                Map<String, Map<String, List<String>>> tsByUserItem) {
            this.ordersByUser = ordersByUser;
            this.tsByUserItem = tsByUserItem;
        }
    }




    // main method to summarize athread for a specific team
    public void summarizeThreadForTeam(
            String teamId,
            String orderChannel,
            String threadTs,
            List<MessageEvent> events,
            String adminChannel
    ) throws IOException {
        if (events.isEmpty()) {
            slackMessageService.sendMessageForTeam(
                    teamId, orderChannel, "No orders were placed this week.", threadTs
            );
            return;
        }
        OrderSummaryData orderData = processMessageEvents(events);
        Map<String, Long> plusOneCountByTs = eventStore.fetchReactionsForTeam(teamId).stream()
                .filter(r -> "+1".equals(r.reaction()) && r.ts().compareTo(threadTs) >= 0)
                .collect(Collectors.groupingBy(ReactionEvent::ts, Collectors.counting()));
        String summary = buildSummaryText(orderData, plusOneCountByTs);
        // post via explicit-team methods
        slackMessageService.sendMessageForTeam(teamId, orderChannel, summary, threadTs);
        if (adminChannel != null && !adminChannel.isEmpty()) {
            slackMessageService.sendMessageForTeam(
                    teamId, adminChannel, "Summary:\n" + summary
            );
        }
    }
}