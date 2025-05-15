package com.andreycorp.slack_grocery_bot;

import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public SummaryService(SlackMessageService slackMessageService) {
        this.slackMessageService = slackMessageService;
    }

    /**
     * Aggregates a list of MessageEvent into a summary text and posts it to Slack
     * @param orderChannel the channel where the thread lives
     * @param threadTs the timestamp of the thread to post into
     * @param events the collected MessageEvent objects in the thread
     * @param adminChannel optional admin channel for DM
     * @throws IOException on Slack API I/O errors
     */
    public void summarizeThread(
            String orderChannel,
            String threadTs,
            List<MessageEvent> events,
            String adminChannel
    ) throws Exception {
        // If no events are present
        if (events.isEmpty()) {
            slackMessageService.sendMessage(orderChannel, "No orders were placed this week.", threadTs);
            return;
        }
        // Aggregate orders by user
        Map<String, Map<String, Integer>> ordersByUser = new HashMap<>();
        for (MessageEvent m : events) {
            String userId = m.user();  // Extract the user ID from the message.
            String rawText = m.text().trim();  // Get the raw text of the message and trim whitespace.
            // Strip leading bot mention, e.g. "<@U12345> "
            String text = rawText.replaceAll("^<@[^>]+>\\s*", "");
            String[] parts = text.split("\\s+", 2);  // Split the text into parts (quantity and item).
            int qty;
            String item;
            // If a numeric quantity is provided, parse it and extract the item name.
            if (parts.length >= 2 && parts[0].matches("\\d+")) {
                qty = Integer.parseInt(parts[0]);
                item = parts[1].trim().toLowerCase();
            } else {
                // No explicit qty, default to 1 and treat full text as item name
                qty = 1;
                item = text.toLowerCase();
            }
            // Update the orders map
            ordersByUser
                    .computeIfAbsent(userId, u -> new HashMap<>())
                    .merge(item, qty, Integer::sum);
        }
        // Build summary message
        StringBuilder summary = new StringBuilder("*Weekly Grocery Summary:*\n");
        ordersByUser.forEach((userId, items) -> {
            // Mention the user in the summary using their Slack user ID.
            String mention = "<@" + userId + ">";
            summary.append("• ").append(mention).append(": ");
            String line = items.entrySet().stream()
                    .map(e -> e.getValue() + "× " + e.getKey())
                    .collect(Collectors.joining(", "));
            summary.append(line).append("\n");
        });
        // Post into thread
        slackMessageService.sendMessage(orderChannel, summary.toString(), threadTs);
        if (adminChannel != null && !adminChannel.isEmpty()) { // If an admin channel is provided
            slackMessageService.sendMessage(adminChannel, "Grocery thread closed. Summary:\n" + summary.toString());
        }
    }
}
