package com.andreycorp.slack_grocery_bot.llm;

import com.andreycorp.slack_grocery_bot.Services.SlackMessageService;
import com.andreycorp.slack_grocery_bot.Services.SummaryService;
import com.andreycorp.slack_grocery_bot.parsers.OrderParser;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for generating and posting AI-powered summaries
 * using both Deepseek (Ollama) and ChatGPT (OpenAI)
 */
@Service
public class AISummaryService {

    private final SlackMessageService   slackMessageService;
    private final SummaryService        summaryService;
    private final OpenAIClientService   openAIClientService;
    private final OllamaClientService   ollamaClientService;
    private final OrderParser           orderParser;
    private final String                orderChannel;

    public AISummaryService(
            SlackMessageService slackMessageService,
            SummaryService summaryService,
            OpenAIClientService openAIClientService,
            OllamaClientService ollamaClientService,
            OrderParser orderParser,
            @Value("${slack.order.channel}") String orderChannel
    ) {
        this.slackMessageService   = slackMessageService;
        this.summaryService        = summaryService;
        this.openAIClientService   = openAIClientService;
        this.ollamaClientService   = ollamaClientService;
        this.orderParser           = orderParser;
        this.orderChannel          = orderChannel;
    }

    /**
     * Posts a simple item list summary using Deepseek via the Ollama client.
     * @param threadMsgs List of message events in the thread
     * @param threadTs   Slack timestamp to reply in-thread
     * @throws Exception on any error during generation or messaging
     */
    public void postDeepseekSummary(List<MessageEvent> threadMsgs, String threadTs) throws Exception {
        List<String> rawOrders = threadMsgs.stream()
                .flatMap(m -> orderParser.parseAll(m.text()).stream())
                .map(po -> po.item)
                .collect(Collectors.toList());
        if (rawOrders.isEmpty()) return;

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode dsPayload = mapper.createObjectNode();
        dsPayload.set("rawOrders", mapper.valueToTree(rawOrders));
        String dsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dsPayload);

        StringBuilder dsPrompt = new StringBuilder()
                .append("You are a grocery-order summarizer.\n")
                .append("Return ONLY a Markdown bullet list of these items, same order, including duplicates.\n\n")
                .append("```json\n").append(dsJson).append("\n```\n")
                .append("### ANSWER:");

        String deepseekResult = ollamaClientService.generate(dsPrompt.toString());
        slackMessageService.sendMessage(
                orderChannel,
                "*Deepseek Simple items Summary:*\n" + deepseekResult,
                threadTs
        );
    }

    /**
     * Builds a polished weekly summary through ChatGPT and posts it to Slack.
     * @param threadMsgs List of message events in the thread
     * @param threadTs   Slack timestamp to reply in-thread
     * @throws Exception on any error during generation or messaging
     */
    public void postChatGptSummary(List<MessageEvent> threadMsgs, String threadTs) throws Exception {
        String rawSummary = summaryService.buildSummaryText(
                summaryService.processMessageEvents(threadMsgs),
                summaryService.processReactions(threadTs)
        );

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
                threadTs
        );
    }
}
