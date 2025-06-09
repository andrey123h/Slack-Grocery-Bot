package com.andreycorp.slack_grocery_bot;

import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Component responsible for managing weekly grocery order threads in Slack.
 * It opens a new thread every Monday and closes it with a summary every Thursday.
 */


@Component
public class WeeklyOrderScheduler {

    private final SlackMessageService slackMessageService;
    private final EventStore eventStore;
    private final SummaryService summaryService;

    @Value("${slack.order.channel}")
    private String orderChannel;

    @Value("${slack.admin.channel:}")  // Optional admin channel for notifications.
    private String adminChannel;

    // holds the ts of the currently open thread
    private String currentThreadTs;

    public WeeklyOrderScheduler(SlackMessageService slackMessageService,
                                EventStore eventStore, SummaryService summaryService) {
        this.slackMessageService = slackMessageService;
        this.eventStore = eventStore;
        this.summaryService = summaryService;
    }


    /**
     * Opens a new grocery order thread every Monday at 09:00 Jerusalem time,
     * posts the opening prompt with ordering instructions, and pins the message for visibility.
     */
    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Jerusalem")
    public void openOrderThread() throws Exception {
        // Compose prompt with instructions
                String prompt =
                "*🛒 New Grocery Order Thread!* Please add your items by Thursday EOD.\n" +
                        "\n" +
                        "Mention me, then list your items in one line:\n" +
                        "```" +
                        "@GrocFriend 2 apples, 1.5 kg sugar, banana\n" +
                        "```" +
                        "\n" +
                        "Supported formats:\n" +
                        "    – Integers or decimals for quantity (e.g. `2`, `1.5`)\n" +
                        "    – Commas, semicolons or periods before a space to separate items\n" +
                        "    – Multi-word items (e.g. `2 green apples`)\n" +
                        "    – Items without quantities default to `1`\n" +
                        "    – Special characters are supported (e.g. `crème fraîche`)\n" +
                        "\n" +
                        "React with 👍 to encourage an order!\n";


        ChatPostMessageResponse resp = slackMessageService.sendMessage(orderChannel, prompt);
        if (resp.isOk()) {
            currentThreadTs = resp.getTs();
            System.out.println("Opened thread at ts=" + currentThreadTs);
            // Pin the prompt message so it remains visible
            slackMessageService.pinMessage(orderChannel, currentThreadTs);
        } else {
            System.err.println("Failed to open thread: " + resp.getError());
        }
    }



    /**
     * Closes and summarizes the thread every Thursday at 17:00 Jerusalem time
     */

    @Scheduled(cron = "0 0 17 * * THU", zone = "Asia/Jerusalem")
    public void closeOrderThread() throws Exception {
        if (currentThreadTs == null) {
            System.out.println("No open thread to close.");
            return;
        }
        // Fetch all stored messages
        List<MessageEvent> all = eventStore.fetchMessagesSince("0");
        // filters the retrieved messages to include only those that belong to the current thread
        List<MessageEvent> threadMsgs = all.stream()
                .filter(m -> orderChannel.equals(m.channel()) && m.ts().compareTo(currentThreadTs) >= 0)
                .collect(Collectors.toList());

        // summary creation and posting
        summaryService.summarizeThread(orderChannel, currentThreadTs, threadMsgs, adminChannel);

        // currentThreadTs is reset to nul, reset for next cycle
        currentThreadTs = null;
    }

    public String getCurrentThreadTs() {
        return currentThreadTs;
    }
}
