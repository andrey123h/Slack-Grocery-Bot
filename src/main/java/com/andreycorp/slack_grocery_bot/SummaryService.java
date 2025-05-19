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
        // If nobody mentioned the bot in that thread
        if (events.isEmpty()) {
            slackMessageService.sendMessage(orderChannel,
                    "No orders were placed this week.", threadTs);
            return;
        }

        // Outer key = userId
        // Inner map = (itemName , totalQuantity)
        Map<String, Map<String, Integer>> ordersByUser = new HashMap<>();
        //  Outer key = userId
        //Inner key = itemName
        // later look up how many reactions each of those messages got
        Map<String, Map<String, List<String>>> tsByUserItem = new HashMap<>();
        // loop every message, parse its orders, and merge quantities
        for (MessageEvent m : events) {
            String user = m.user();
            // Prepare per-user buckets if this is the first time we've seen them
            ordersByUser.computeIfAbsent(user, u -> new HashMap<>());
            tsByUserItem.computeIfAbsent(user, u -> new HashMap<>());
            // Turn the raw text into (item, qty) pairs
            List<OrderParser.ParsedOrder> parsed = orderParser.parseAll(m.text());
            // For each parsed order
            // Accumulate the quantity for that user+item
            // merge:
            // if the map already has that item, sum the old and new qty. otherwise insert the new qty.
            for (OrderParser.ParsedOrder po : parsed) {
                ordersByUser.get(user)
                        .merge(po.item, po.qty, Integer::sum);

                // record timestamps for each item. for later look up reactions on exactly those messages.
                tsByUserItem.get(user)
                        .computeIfAbsent(po.item, i -> new ArrayList<>())
                        .add(m.ts());
            }
        }

        // Pull in every reaction event from the store that happened at or after the thread opened
        List<ReactionEvent> reactions = eventStore.fetchReactionsSince(threadTs);
        Map<String, Long> plusOneCountByTs = reactions.stream()
                // keep only the +1 (thumbs-up) reactions
                .filter(r -> "+1".equals(r.reaction()))
                // group them by the timestamp of the message they were applied to,
                // and count how many there are in each group
                .collect(Collectors.groupingBy(
                        ReactionEvent::ts,
                        Collectors.counting()
                ));

        //  Build summary with reaction annotations
        StringBuilder summary = new StringBuilder("*Weekly Grocery Summary:*\n");
        ordersByUser.forEach((user, items) -> {
            summary.append("• <@").append(user).append(">: ");
            String line = items.entrySet().stream()
                    .map(e -> {
                        String item = e.getKey();
                        int qty = e.getValue();

                        long totalReacts = tsByUserItem.getOrDefault(user, Collections.emptyMap())
                                .getOrDefault(item, Collections.emptyList())
                                .stream()
                                .mapToLong(ts -> plusOneCountByTs.getOrDefault(ts, 0L))
                                .sum();

                        String suffix = totalReacts > 0
                                ? String.format(" (%dx 👍)", totalReacts)
                                : "";
                        return String.format("%d× %s%s", qty, item, suffix);
                    })
                    .collect(Collectors.joining(", "));
            summary.append(line).append("\n");
        });

        // 4) Post the summary
        slackMessageService.sendMessage(orderChannel, summary.toString(), threadTs);
        if (adminChannel != null && !adminChannel.isEmpty()) {
            slackMessageService.sendMessage(adminChannel,
                    "Grocery thread closed. Summary:\n" + summary.toString());
        }
    }
}
