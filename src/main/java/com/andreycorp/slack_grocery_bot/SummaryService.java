package com.andreycorp.slack_grocery_bot;

import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class responsible for summarizing grocery orders from Slack messages
 * and posting the summary back to Slack.
 */
@Service
public class SummaryService {

    private final SlackMessageService slackMessageService;
    private final OrderParser orderParser;

    public SummaryService(SlackMessageService slackMessageService,
                          OrderParser orderParser) {
        this.slackMessageService = slackMessageService;
        this.orderParser = orderParser;
    }

    /**
     * Aggregates a list of MessageEvent into a summary text and posts it to Slack
     * @param orderChannel the channel where the thread lives
     * @param threadTs the timestamp of the thread to post into
     * @param events the collected MessageEvent objects in the thread
     * @param adminChannel optional admin channel for DM
     * @throws Exception on Slack API I/O errors
     */
    public void summarizeThread(
            String orderChannel,
            String threadTs,
            List<MessageEvent> events,
            String adminChannel
    ) throws Exception {
        // If no events are present
        if (events.isEmpty()) {
            slackMessageService.sendMessage(orderChannel,
                    "No orders were placed this week.", threadTs);
            return;
        }

        // Aggregate orders by user using shared OrderParser
        Map<String, Map<String, Integer>> ordersByUser = new HashMap<>();
        for (MessageEvent m : events) {
            // parse multiple entries from each message
            List<OrderParser.ParsedOrder> orders = orderParser.parseAll(m.text());
            for (OrderParser.ParsedOrder po : orders) {
                ordersByUser
                        .computeIfAbsent(m.user(), u -> new HashMap<>())
                        .merge(po.item, po.qty, Integer::sum);
            }
        }

        // Build summary message
        StringBuilder summary = new StringBuilder("*Weekly Grocery Summary:*\n");
        ordersByUser.forEach((userId, items) -> {
            String mention = "<@" + userId + ">";
            summary.append("• ").append(mention).append(": ");
            String line = items.entrySet().stream()
                    .map(e -> e.getValue() + "× " + e.getKey())
                    .collect(Collectors.joining(", "));
            summary.append(line).append("\n");
        });

        // Post into thread
        slackMessageService.sendMessage(orderChannel, summary.toString(), threadTs);
        // Optionally notify admin channel
        if (adminChannel != null && !adminChannel.isEmpty()) {
            slackMessageService.sendMessage(adminChannel,
                    "Grocery thread closed. Summary:\n" + summary.toString());
        }
    }
}