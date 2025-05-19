package com.andreycorp.slack_grocery_bot;

import com.slack.api.app_backend.SlackSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/slack/commands")
public class SlackCommandController {

    private final SlackSignature.Verifier sigVerifier;
    private final WeeklyOrderScheduler scheduler;
    private final EventStore eventStore;
    private final SummaryService summaryService;
    private final SlackMessageService slackMessageService;

    @Value("${slack.order.channel}")
    private String orderChannel;

    public SlackCommandController(
            @Value("${slack.signing-secret}") String signingSecret,
            WeeklyOrderScheduler scheduler,
            EventStore eventStore,
            SummaryService summaryService,
            SlackMessageService slackMessageService
    ) {
        SlackSignature.Generator gen = new SlackSignature.Generator(signingSecret);
        this.sigVerifier = new SlackSignature.Verifier(gen);
        this.scheduler = scheduler;
        this.eventStore = eventStore;
        this.summaryService = summaryService;
        this.slackMessageService = slackMessageService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> handle(
            @RequestHeader("X-Slack-Signature") String slackSig,
            @RequestHeader("X-Slack-Request-Timestamp") String tsHeader,
            @RequestBody byte[] rawBodyBytes,
            @RequestParam("command") String command,
            @RequestParam("user_id") String userId
    ) {
        String rawBody = new String(rawBodyBytes, StandardCharsets.UTF_8);

        //  Verify signature
        if (!sigVerifier.isValid(tsHeader, rawBody, slackSig)) {
            return ResponseEntity.status(401).body("Invalid signature");
        }

        //  Confirm correct command
        if (!"/grocery-summary-admin".equals(command)) {
            return ResponseEntity.badRequest().body("Unknown command: " + command);
        }

        //  Ack immediately via ack (not using slackMessageService) so Slack won't see a timeout within 3 seconds
        ResponseEntity<String> ack = ResponseEntity.ok(
                "📨 Got it! Generating your summary..."
        );

        //  Run the heavy work asynchronously:
        //  Kick off work on another thread ,frees the HTTP thread to return the 200 OK (“ack”) immediately
        // runAsync: The work inside the lambda happens fully in the background.
        CompletableFuture.runAsync(() -> {
            try {
                // Admin-only check
                if (!slackMessageService.isWorkspaceAdmin(userId)) {
                    // If not admin → DM “not authorized”
                    String dm = slackMessageService.openImChannel(userId);
                    slackMessageService.sendMessage(dm,
                            " Only workspace admins can run this command.");
                    return;
                }

                // Locate the open thread TS
                String threadTs = scheduler.getCurrentThreadTs();
                if (threadTs == null) {
                    String dm = slackMessageService.openImChannel(userId);
                    slackMessageService.sendMessage(dm,
                            "No active grocery thread to summarize.");
                    return;
                }

                // Fetch events since that TS, filtered to the order channel
                List<MessageEvent> events = eventStore.fetchMessagesSince(threadTs).stream()
                        .filter(m -> orderChannel.equals(m.channel()))
                        .collect(Collectors.toList());

                // Open a DM and post the summary
                String adminDm = slackMessageService.openImChannel(userId);
                summaryService.summarizeThread(orderChannel, threadTs, events, adminDm);

            } catch (Exception e) {
                // Log the error
                e.printStackTrace();
                // Notify the user in DM
                try {
                    String dm = slackMessageService.openImChannel(userId);
                    slackMessageService.sendMessage(dm,
                            "something went wrong generating your summary.");
                } catch (IOException ignored) {}
            }
        });

        return ack;
    }
}
