package com.andreycorp.slack_grocery_bot;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class responsible for summarizing grocery orders from Slack messages
 * and posting the summary back to Slack, including +1 reaction counts per item.
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
     * Aggregates a list of MessageEvent into a summary text (including reaction counts)
     * and posts it to Slack.
     *
     * @param orderChannel the channel where the thread lives
     * @param threadTs     the timestamp of the thread to post into
     * @param events       the collected MessageEvent objects in the thread
     * @param adminChannel optional admin channel for DM
     */
    public void summarizeThread(
            String orderChannel,
            String threadTs,
            List<MessageEvent> events,
            String adminChannel
    ) throws IOException {
        if (events.isEmpty()) {
            slackMessageService.sendMessage(orderChannel,
                    "No orders were placed this week.", threadTs);
            return;
        }

        // Use Double for quantities to match updated parser
        Map<String, Map<String, Double>> ordersByUser = new HashMap<>();
        Map<String, Map<String, List<String>>> tsByUserItem = new HashMap<>();

        for (MessageEvent m : events) {
            String user = m.user();
            ordersByUser.computeIfAbsent(user, u -> new HashMap<>());
            tsByUserItem.computeIfAbsent(user, u -> new HashMap<>());

            List<OrderParser.ParsedOrder> parsed = orderParser.parseAll(m.text());
            for (OrderParser.ParsedOrder po : parsed) {
                // merge double quantities
                ordersByUser.get(user)
                        .merge(po.item, po.qty, Double::sum);

                tsByUserItem.get(user)
                        .computeIfAbsent(po.item, i -> new ArrayList<>())
                        .add(m.ts());
            }
        }

        // Fetch +1 reactions since thread open
        List<ReactionEvent> reactions = eventStore.fetchReactionsSince(threadTs);
        Map<String, Long> plusOneCountByTs = reactions.stream()
                .filter(r -> "+1".equals(r.reaction()))
                .collect(Collectors.groupingBy(
                        ReactionEvent::ts,
                        Collectors.counting()
                ));

        StringBuilder summary = new StringBuilder("*Weekly Grocery Summary:*\n");
        ordersByUser.forEach((user, items) -> {
            summary.append("• <@").append(user).append(">: ");
            String line = items.entrySet().stream()
                    .map(e -> {
                        String item = e.getKey();
                        double qty = e.getValue();

                        long totalReacts = tsByUserItem.getOrDefault(user, Collections.emptyMap())
                                .getOrDefault(item, Collections.emptyList())
                                .stream()
                                .mapToLong(ts -> plusOneCountByTs.getOrDefault(ts, 0L))
                                .sum();

                        String suffix = totalReacts > 0
                                ? String.format(" (%dx 👍)", totalReacts)
                                : "";

                        // Format quantity: drop .0 for whole numbers
                        String qtyStr = (qty == (long) qty)
                                ? String.format("%d", (long) qty)
                                : String.valueOf(qty);

                        return String.format("%s× %s%s", qtyStr, item, suffix);
                    })
                    .collect(Collectors.joining(", "));
            summary.append(line).append("\n");
        });

        slackMessageService.sendMessage(orderChannel, summary.toString(), threadTs);
        if (adminChannel != null && !adminChannel.isEmpty()) {
            slackMessageService.sendMessage(adminChannel,
                    " Summary:\n" + summary.toString());
        }
    }
}
