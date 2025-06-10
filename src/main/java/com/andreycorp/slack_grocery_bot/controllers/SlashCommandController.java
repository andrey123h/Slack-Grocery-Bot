package com.andreycorp.slack_grocery_bot.controllers;

import com.andreycorp.slack_grocery_bot.*;
import com.andreycorp.slack_grocery_bot.Services.SlackMessageService;
import com.andreycorp.slack_grocery_bot.Services.SummaryService;
import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.andreycorp.slack_grocery_bot.parsers.SlackRequestParser;
import com.andreycorp.slack_grocery_bot.scheduler.WeeklyOrderScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Handles incoming Slack slash commands for grocery order summaries.
 *  Validates the command and user ID.
 *  Immediately acknowledge response to Slack.
 *  Runs the summary generation asynchronously. Admin check.
 */

@RestController
@RequestMapping("/slack/commands")
public class SlashCommandController {

    @Value("${slack.order.channel}")
    private String orderChannel;

    private final WeeklyOrderScheduler scheduler;
    private final EventStore eventStore;
    private final SummaryService summaryService;
    private final SlackMessageService slackMessageService;

    public SlashCommandController(
            WeeklyOrderScheduler scheduler,
            EventStore eventStore,
            SummaryService summaryService,
            SlackMessageService slackMessageService
    ) {
        this.scheduler = scheduler;
        this.eventStore = eventStore;
        this.summaryService = summaryService;
        this.slackMessageService = slackMessageService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, // expects URL-encoded form data
            produces = MediaType.TEXT_PLAIN_VALUE // reply with plain text
    )
    public ResponseEntity<String> handle(HttpServletRequest request) throws IOException {
        // extracting and parsing JSON body that filter cached
        String rawBody = SlackRequestParser.extractRawBody(request);
        Map<String,String> params = SlackRequestParser.parseFormUrlEncoded(rawBody);

        String command = params.get("command");
        String userId  = params.get("user_id");

        // Check for required parameters and command validity
        ResponseEntity<String> error = validateSlashCommand(command, userId);
        if (error != null) {
            return error;
        }

        // 5) Immediate ack
        ResponseEntity<String> ack = ResponseEntity.ok(
                "📨 Got it! Generating your summary.."
        );

        // 6) Heavy work off the HTTP thread
        runSummaryTask(userId);

        return ack;
    }

    /**
     * Ensures the slash payload has both command & user_id,
     * and that the command matches  expected value.
     * @param command the slash command string (e.g. "/grocery-summary-admin")
     * @param userId The Slack user ID of the user who issued the command
     * @return a 400 ResponseEntity on error, or null if OK
     */
    private ResponseEntity<String> validateSlashCommand(String command, String userId) {
        if (command == null || userId == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Required parameters 'command' and 'user_id' are missing");
        }
        if (!"/grocery-summary-admin".equals(command)) {
            return ResponseEntity
                    .badRequest()
                    .body("Unknown command: " + command);
        }
        return null;
    }

    /**
     * Kicks off the summary generation asynchronously.
     * Checks if the user is a workspace admin and if there is an active grocery thread.
     * @param userId the Slack user ID of the command invoker
     */
    private void runSummaryTask(String userId) {
        CompletableFuture.runAsync(() -> {
            try {
                if (!slackMessageService.isWorkspaceAdmin(userId)) {
                    String dm = slackMessageService.openImChannel(userId);
                    slackMessageService.sendMessage(dm, "Only workspace admins can run this command.");
                    return;
                }

                String threadTs = scheduler.getCurrentThreadTs();
                if (threadTs == null) {
                    String dm = slackMessageService.openImChannel(userId);
                    slackMessageService.sendMessage(dm, "No active grocery thread to summarize.");
                    return;
                }

                List<MessageEvent> events = eventStore.fetchMessagesSince(threadTs).stream()
                        .filter(m -> orderChannel.equals(m.channel()))
                        .collect(Collectors.toList());

                String adminDm = slackMessageService.openImChannel(userId);
                summaryService.summarizeThread(orderChannel, threadTs, events, adminDm);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    String dm = slackMessageService.openImChannel(userId);
                    slackMessageService.sendMessage(dm, "Something went wrong generating your summary.");
                } catch (IOException ignored) {}
            }
        });
    }

}
