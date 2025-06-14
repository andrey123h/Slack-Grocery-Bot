package com.andreycorp.slack_grocery_bot.scheduler;

import com.andreycorp.slack_grocery_bot.Services.SlackMessageService;
import com.andreycorp.slack_grocery_bot.Services.SummaryService;
import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Component responsible for the actual work of opening/closing threads.
 * Scheduling is delegated to ScheduleSettingsService.
 */
@Component
public class WeeklyOrderScheduler {

    private final SlackMessageService slackMessageService;
    private final EventStore        eventStore;
    private final SummaryService    summaryService;
    private final String            orderChannel;
    private final String            adminChannel;

    // Tracks the currently open thread timestamp
    private String currentThreadTs;

    public WeeklyOrderScheduler(SlackMessageService slackMessageService,
                                EventStore eventStore,
                                SummaryService summaryService,
                                @Value("${slack.order.channel}") String orderChannel,
                                @Value("${slack.admin.channel:}")  String adminChannel) {
        this.slackMessageService = slackMessageService;
        this.eventStore          = eventStore;
        this.summaryService      = summaryService;
        this.orderChannel        = orderChannel;
        this.adminChannel        = adminChannel;
    }

    /**
     * Opens a new grocery order thread.
     * Invoked by ScheduleSettingsService on Mondays at configured time.
     */
    public void openOrderThread() throws Exception {
        String prompt =
                "*🛒 New Grocery Order Thread!* Please add your items by Thursday EOD.\n\n" +
                        "Mention me, then list your items in one line:\n```@GrocFriend 2 apples, 1.5 kg sugar, banana```\n\n" +
                        "Supported formats:\n" +
                        "  – Integers or decimals (e.g. `2`, `1.5`)\n" +
                        "  – Commas/semicolons/periods to separate items\n" +
                        "  – Multi-word items (e.g. `2 green apples`)\n" +
                        "  – Default quantity of `1` if omitted\n" +
                        "  – Special characters supported (e.g. `crème fraîche`)\n\n" +
                        "React with 👍 to encourage an order!\n";

        ChatPostMessageResponse resp = slackMessageService.sendMessage(orderChannel, prompt);
        if (resp.isOk()) {
            currentThreadTs = resp.getTs();
            slackMessageService.pinMessage(orderChannel, currentThreadTs);
        } else {
            System.err.println("Failed to open thread: " + resp.getError());
        }
    }

    /**
     * Closes the current thread and posts a summary.
     * Invoked by ScheduleSettingsService on Thursdays at configured time.
     */
    public void closeOrderThread() throws Exception {
        if (currentThreadTs == null) {
            System.out.println("No open thread to close.");
            return;
        }
        List<MessageEvent> all = eventStore.fetchMessagesSince("0");
        List<MessageEvent> threadMsgs = all.stream()
                .filter(m -> orderChannel.equals(m.channel()) && m.ts().compareTo(currentThreadTs) >= 0)
                .collect(Collectors.toList());

        summaryService.summarizeThread(orderChannel, currentThreadTs, threadMsgs, adminChannel);
        currentThreadTs = null;
    }

    /** @return the timestamp of the currently open thread */
    public String getCurrentThreadTs() {
        return currentThreadTs;
    }
}