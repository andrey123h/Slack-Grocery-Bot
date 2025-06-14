package com.andreycorp.slack_grocery_bot.scheduler;

import com.andreycorp.slack_grocery_bot.llm.OllamaClientService;
import com.andreycorp.slack_grocery_bot.llm.OpenAIClientService;
import com.andreycorp.slack_grocery_bot.Services.SlackMessageService;
import com.andreycorp.slack_grocery_bot.Services.SummaryService;
import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.andreycorp.slack_grocery_bot.model.ReactionEvent;
import com.andreycorp.slack_grocery_bot.parsers.OrderParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class WeeklyOrderScheduler {

    private final SlackMessageService slackMessageService;
    private final EventStore          eventStore;
    private final SummaryService      summaryService;
    private final OrderParser         orderParser;
    private final OllamaClientService ollamaClientService;
    private final OpenAIClientService openAIClientService;
    private final String              orderChannel;
    private final String              adminChannel;

    private String currentThreadTs;

    public WeeklyOrderScheduler(
            SlackMessageService slackMessageService,
            EventStore eventStore,
            SummaryService summaryService,
            OrderParser orderParser,
            OllamaClientService ollamaClientService,
            OpenAIClientService openAIClientService,
            @Value("${slack.order.channel}") String orderChannel,
            @Value("${slack.admin.channel:}") String adminChannel
    ) {
        this.slackMessageService   = slackMessageService;
        this.eventStore            = eventStore;
        this.summaryService        = summaryService;
        this.orderParser           = orderParser;
        this.ollamaClientService   = ollamaClientService;
        this.openAIClientService   = openAIClientService;
        this.orderChannel          = orderChannel;
        this.adminChannel          = adminChannel;
    }

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

    public void closeOrderThread() throws Exception {
        if (currentThreadTs == null) return;

        //  fetch thread messages
        List<MessageEvent> threadMsgs = eventStore.fetchMessagesSince("0").stream()
                .filter(m -> orderChannel.equals(m.channel()) && m.ts().compareTo(currentThreadTs) >= 0)
                .collect(Collectors.toList());

        /// manual summary
        summaryService.summarizeThread(orderChannel, currentThreadTs, threadMsgs, adminChannel);

        ///deepseek summary
        postDeepseekSummary(threadMsgs);

        /// ChatGPT summary
        postChatGptSummary(threadMsgs);

        //  Reset
        currentThreadTs = null;
    }
    // posts a summary of the orders using Deepseek LLM
    private void postDeepseekSummary(List<MessageEvent> threadMsgs) throws Exception {
        List<String> rawOrders = threadMsgs.stream()
                .flatMap(m -> orderParser.parseAll(m.text()).stream())
                .map(po -> po.item)
                .collect(Collectors.toList());
        if (rawOrders.isEmpty()) return;

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode dsPayload = mapper.createObjectNode();
        dsPayload.set("rawOrders", mapper.valueToTree(rawOrders));
        String dsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dsPayload);

        // prompt for Deepseek
        StringBuilder dsPrompt = new StringBuilder()
                .append("You are a grocery-order summarizer.\n")
                .append("Return ONLY a Markdown bullet list of these items, same order, including duplicates.\n\n")
                .append("```json\n").append(dsJson).append("\n```\n")
                .append("### ANSWER:");

        String deepseekResult = ollamaClientService.generate(dsPrompt.toString());
        slackMessageService.sendMessage(
                orderChannel,
                "*Deepseek(Free module) Simple items Summary:*\n" + deepseekResult,
                currentThreadTs
        );
    }
    // posts a summary of the orders using ChatGPT
    private void postChatGptSummary(List<MessageEvent> threadMsgs) throws Exception {
        // Use SummaryService to compute the raw summary text
        String rawSummary = summaryService.buildSummaryText(
                summaryService.processMessageEvents(threadMsgs),
                summaryService.processReactions(currentThreadTs)
        );

        // Prompt ChatGPT to polish it
        String systemPrompt = """
            You are a helpful assistant that improves the tone and formatting of grocery summaries.
            Given an existing grocery summary, improve its tone and formatting
            for end users, without changing any numbers or item names.
            For each item, add a corresponding emoji to make it more engaging.
            Output only the polished summary text.
            """;

        String polished = openAIClientService.generate(systemPrompt, rawSummary);

        slackMessageService.sendMessage(
                orderChannel,
                "*ChatGPT Weekly Summary:*\n" + polished,
                currentThreadTs
        );
    }

    public String getCurrentThreadTs() {
        return currentThreadTs;
    }
}