package com.andreycorp.slack_grocery_bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/slack/commands")
public class SlackCommandController {

    @Value("${slack.order.channel}")
    private String orderChannel;

    private final WeeklyOrderScheduler scheduler;
    private final EventStore eventStore;
    private final SummaryService summaryService;
    private final SlackMessageService slackMessageService;

    public SlackCommandController(
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
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> handle(HttpServletRequest request) throws IOException {
        // 1) Grab the raw URL-encoded body that our filter saved
        String rawBody = (String) request.getAttribute("rawBody");
        if (rawBody == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Missing request body");
        }

        // 2) Parse it into a Map<String,String>
        Map<String,String> params = Arrays.stream(rawBody.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                ));

        String command = params.get("command");
        String userId  = params.get("user_id");

        // 3) Validate required params
        if (command == null || userId == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Required parameters 'command' and 'user_id' are missing");
        }

        // 4) Confirm correct command
        if (!"/grocery-summary-admin".equals(command)) {
            return ResponseEntity
                    .badRequest()
                    .body("Unknown command: " + command);
        }

        // 5) Immediate ack
        ResponseEntity<String> ack = ResponseEntity.ok(
                "📨 Got it! Generating your summary..."
        );

        // 6) Heavy work off the HTTP thread
        CompletableFuture.runAsync(() -> {
            try {
                if (!slackMessageService.isWorkspaceAdmin(userId)) {
                    String dm = slackMessageService.openImChannel(userId);
                    slackMessageService.sendMessage(dm,
                            "Only workspace admins can run this command.");
                    return;
                }

                String threadTs = scheduler.getCurrentThreadTs();
                if (threadTs == null) {
                    String dm = slackMessageService.openImChannel(userId);
                    slackMessageService.sendMessage(dm,
                            "No active grocery thread to summarize.");
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
                    slackMessageService.sendMessage(dm,
                            "Something went wrong generating your summary.");
                } catch (IOException ignored) {}
            }
        });

        return ack;
    }
}
