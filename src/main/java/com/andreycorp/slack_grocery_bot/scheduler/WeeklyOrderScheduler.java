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

import java.util.stream.Collectors;

@Component
public class WeeklyOrderScheduler {

    private final SlackMessageService slackMessageService;
    private final EventStore          eventStore;
    private final SummaryService      summaryService;
    private final AISummaryService    aiSummaryService; // optional, not in deployment
    private final String              orderChannel;
    private final String              adminChannel;

    private String currentThreadTs;

    public WeeklyOrderScheduler(
            SlackMessageService slackMessageService,
            EventStore eventStore,
            SummaryService summaryService,
            AISummaryService aiSummaryService,
            @Value("${slack.order.channel}") String orderChannel, // #office-grocery same for all tenants
            @Value("${slack.admin.channel:}") String adminChannel // optional. not in deployment.
    ) {
        this.slackMessageService   = slackMessageService;
        this.eventStore            = eventStore;
        this.summaryService        = summaryService;
        this.aiSummaryService = aiSummaryService;
        this.orderChannel          = orderChannel;
        this.adminChannel          = adminChannel;
    }


    /** Opens a new order thread in the specified Slack channel.
     * Sends a message with instructions on how to place orders.
     * Pins the message to the channel for visibility.
     * @throws Exception if there is an error sending the message or pinning it
     */
    public void openOrderThread() throws Exception {

        ChatPostMessageResponse resp = slackMessageService.sendMessage(orderChannel, NEW_THREAD_MSG);
        if (resp.isOk()) {
            currentThreadTs = resp.getTs(); // Save the thread timestamp
            slackMessageService.pinMessage(orderChannel, currentThreadTs);
        } else {
            System.err.println("Failed to open thread: " + resp.getError());
        }
    }

    /** Closes the current order thread, and posts the summary
     * Manuel summary + AI summaries (ChatGPT, Deepseek).
     * @throws Exception if there is an error processing messages or posting summaries
     */
    public void closeOrderThread() throws Exception {
        if (currentThreadTs == null) return;

        //  fetch thread messages
        List<MessageEvent> threadMsgs = eventStore.fetchMessagesSince("0").stream()
                .filter(m -> orderChannel.equals(m.channel()) && m.ts().compareTo(currentThreadTs) >= 0)
                .collect(Collectors.toList());

        /** manual summary */
        summaryService.summarizeThread(orderChannel, currentThreadTs, threadMsgs, adminChannel);

        // ---- Deepseek (Ollama) and ChatGPT summaries ----
        ///aiSummaryService.postDeepseekSummary(threadMsgs, currentThreadTs);
        ///aiSummaryService.postChatGptSummary(threadMsgs, currentThreadTs);



        // Prune all past events for this workspace so history is cleared
        eventStore.pruneEventsBefore(currentThreadTs);
        // Reset the current thread timestamp for closing
        currentThreadTs = null;
    }


    public String getCurrentThreadTs() {
        return currentThreadTs;
    }



    // Message to be sent when a new grocery order thread is opened
    private  static final String NEW_THREAD_MSG =
            "*🛒 New Grocery Order Thread! Please add your items*.\n\n" +
                    "Mention me, then list your items :\n```@GrocFriend 2 apples, 1.5 kg sugar, banana```\n\n" +
                    "Supported formats:\n" +
                    "  – Integers or decimals (e.g. `2`, `1.5`)\n" +
                    "  – Commas/semicolons/periods to separate items\n" +
                    "  – Multi-word items (e.g. `2 green apples`)\n" +
                    "  – Default quantity of `1` if omitted\n" +
                    "  – Special characters supported (e.g. `crème fraîche`)\n\n" +
                    "React with 👍 to encourage an order.\n"+
                    "Items quantity with same name will be aggregated per user.\n"+
                    "You can find the real-time grocery list in the *GrocFriend*  home tab.\n";
}