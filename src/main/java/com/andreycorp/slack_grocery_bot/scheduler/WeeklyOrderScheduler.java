package com.andreycorp.slack_grocery_bot.scheduler;

import com.andreycorp.slack_grocery_bot.Services.SlackMessageService;
import com.andreycorp.slack_grocery_bot.Services.SummaryService;
import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.andreycorp.slack_grocery_bot.llm.AISummaryService;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class WeeklyOrderScheduler {

    private final SlackMessageService slackMessageService;
    private final EventStore          eventStore;
    private final SummaryService      summaryService;
    private final AISummaryService    aiSummaryService; // optional
    private final String              orderChannel;
    private final String              adminChannel;

    // Keep track of the current thread timestamp per workspace
    private final Map<String,String> currentThreadTs = new ConcurrentHashMap<>();

    public WeeklyOrderScheduler(
            SlackMessageService slackMessageService,
            EventStore eventStore,
            SummaryService summaryService,
            AISummaryService aiSummaryService,
            @Value("${slack.order.channel}") String orderChannel, // #office-grocery same for all tenants
            @Value("${slack.admin.channel:}") String adminChannel //  not in production.
    ) {
        this.slackMessageService = slackMessageService;
        this.eventStore          = eventStore;
        this.summaryService      = summaryService;
        this.aiSummaryService    = aiSummaryService;
        this.orderChannel        = orderChannel;
        this.adminChannel        = adminChannel;
    }

    /**
     * Opens a new order thread for the given workspace (teamId).
     */
    public void openOrderThreadFor(String teamId) throws Exception {
        ChatPostMessageResponse resp =
                slackMessageService.sendMessageForTeam(teamId, orderChannel, NEW_THREAD_MSG);
        if (resp.isOk()) {
            String ts = resp.getTs();
            currentThreadTs.put(teamId, ts);
            slackMessageService.pinMessageForTeam(teamId, orderChannel, ts);
        } else {
            System.err.println("Failed to open thread for " + teamId + ": " + resp.getError());
        }
    }

    /**
     * Closes the current order thread for the given workspace, posts summaries, and prunes history.
     */
    public void closeOrderThreadFor(String teamId) throws Exception {
        String threadTs = currentThreadTs.get(teamId);
        if (threadTs == null) {
            return;
        }

        // Fetch only this team's thread messages
        List<MessageEvent> threadMsgs = eventStore.fetchMessagesForTeam(teamId).stream()
                .filter(m -> orderChannel.equals(m.channel()) && m.ts().compareTo(threadTs) >= 0)
                .collect(Collectors.toList());

        // Manual summary
        summaryService.summarizeThreadForTeam(
                teamId, orderChannel, threadTs, threadMsgs, adminChannel
        );

        // Optional AI summaries
        // aiSummaryService.postDeepseekSummaryForTeam(teamId, threadMsgs, threadTs);
        // aiSummaryService.postChatGptSummaryForTeam(teamId, threadMsgs, threadTs);

        // Prune past events for this workspace
        eventStore.pruneEventsBeforeForTeam(teamId, threadTs);

        // Clear the stored timestamp
        currentThreadTs.remove(teamId);
    }

    /**
     * (Optional) Retrieve the last-opened thread timestamp for a workspace.
     */
    public String getCurrentThreadTsFor(String teamId) {
        return currentThreadTs.get(teamId);
    }

    // Message to open a new grocery-order thread
    private static final String NEW_THREAD_MSG =
            "*🛒 New Grocery Order Thread! Please add your items*.\n\n" +
                    "Mention me, then list your items :\n" +
                    "```@GrocFriend 2 apples, 1.5 kg sugar, banana```\n\n" +
                    "Supported formats:\n" +
                    "  – Integers or decimals (e.g. `2`, `1.5`)\n" +
                    "  – Commas/semicolons/periods to separate items\n" +
                    "  – Multi-word items (e.g. `2 green apples`)\n" +
                    "  – Default quantity of `1` if omitted\n" +
                    "  – Special characters supported (e.g. `crème fraîche`)\n\n" +
                    "React with 👍 to encourage an order.\n" +
                    "Items quantity with the same name will be aggregated per user.\n" +
                    "You can find the real-time grocery list in the *GrocFriend* home tab.\n";
}
