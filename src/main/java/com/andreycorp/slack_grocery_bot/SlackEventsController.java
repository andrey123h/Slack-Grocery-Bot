package com.andreycorp.slack_grocery_bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Listens for various Slack events:
 *  1) URL‐verification (for initial handshake - slacks challenge).
 *  2) "app_home_opened" (to publish a Home‐tab view, differing for admins vs. regular users).
 *  2.1 this controller builds & publishes the initial Home-tab view on app_home_opened.
 *  2.2 DefaultsController is the interaction handler for user interactions with the Home view.
 *  3) "app_mention" (users order recording and processing).
 *  4) "reaction_added" (reaction‐recording behavior).
 */
@RestController
@RequestMapping("/slack/events")
public class SlackEventsController {
    private final SlackMessageService slackMessageService;
    private final DefaultGroceryService defaultGroceryService;
    private final EventStore eventStore;
    private final OrderParser orderParser;
    private final HomeViewBuilder homeViewBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SlackEventsController(
            SlackMessageService slackMessageService,
            DefaultGroceryService defaultGroceryService,
            EventStore eventStore,
            HomeViewBuilder homeViewBuilder,
            OrderParser orderParser
    ) {
        this.slackMessageService = slackMessageService;
        this.defaultGroceryService = defaultGroceryService;
        this.eventStore = eventStore;
        this.orderParser = orderParser;
        this.homeViewBuilder = homeViewBuilder;}

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE, //  Accepts JSON payloads
            produces = MediaType.TEXT_PLAIN_VALUE //  Returns plain text responses
    )
    public ResponseEntity<String> receive(
            HttpServletRequest request
    ) throws Exception {

        //Parse json body from the request
        String rawBody = (String) request.getAttribute("rawBody");
        JsonNode payload = objectMapper.readTree(rawBody);
        String type = payload.path("type").asText();

        // URL verification
        if ("url_verification".equals(type)) {
            return ResponseEntity.ok(payload.get("challenge").asText());
        }

        // Event callback.
        if ("event_callback".equals(type)) {
            JsonNode event = payload.get("event");
            String eventType = event.get("type").asText();

            // 3a) Handle "app_home_opened" - Slack sends app_home_opened event whenever any user clicks on app.
            if ("app_home_opened".equals(eventType)) {
                String userId = event.get("user").asText();

                // Check if user is workspace admin. HomeViewBuilder for the correct Home-tab JSON
                boolean isAdmin = slackMessageService.isWorkspaceAdmin(userId);

                if (isAdmin) {
                    // Build and publish the Admin Home view
                    String adminHomeJson = homeViewBuilder.buildAdminHomeJson(defaultGroceryService.listAll());
                    slackMessageService.publishHomeView(userId, adminHomeJson);
                } else {
                    // Build and publish the simple User Welcome Home
                    String userHomeJson = homeViewBuilder.buildUserWelcomeHomeJson();
                    slackMessageService.publishHomeView(userId, userHomeJson);
                }
                return ResponseEntity.ok("");
            }
            // Handle "app_mention" events and response massage
            if ("app_mention".equals(eventType)) {
                handleMessageEvent(event);
                var orders = orderParser.parseAll(event.get("text").asText());
                String threadTs = event.has("thread_ts")
                        ? event.get("thread_ts").asText()
                        : event.get("ts").asText();

                String ackBody = orders.stream()
                        .map(o -> {
                            boolean isWhole = o.qty == Math.floor(o.qty);
                            String qtyStr = isWhole
                                    ? String.format("%d", (long) o.qty)
                                    : String.valueOf(o.qty);
                            return String.format("%sx %s", qtyStr, o.item);
                        })
                        .collect(Collectors.joining(", "));

                slackMessageService.sendMessage(
                        event.get("channel").asText(),
                        String.format("Got your order: %s ✅", ackBody),
                        threadTs
                );
                return ResponseEntity.ok("");
            }


            if ("reaction_added".equals(eventType)) {
                handleReactionAdded(event);
                return ResponseEntity.ok("");
            }
        }

        return ResponseEntity.ok("");
    }

    private void handleMessageEvent(JsonNode event) {
        String user    = event.get("user").asText();
        String channel = event.get("channel").asText();
        String text    = event.get("text").asText();
        String ts      = event.get("ts").asText();

        MessageEvent me = new MessageEvent(user, channel, text, ts);
        eventStore.saveMessage(me);
        System.out.printf("Recorded message: %s%n", me);
    }

    private void handleReactionAdded(JsonNode event) {
        String reaction = event.get("reaction").asText();
        String user     = event.get("user").asText();
        JsonNode item   = event.get("item");
        String channel  = item.get("channel").asText();
        String messageTs= item.get("ts").asText();

        ReactionEvent re = new ReactionEvent(user, reaction, channel, messageTs);
        eventStore.saveReaction(re);
        System.out.printf("Recorded reaction: %s%n", re);
    }

    @GetMapping("/messages")
    public List<MessageEvent> getMessages() {
        return eventStore.fetchMessagesSince("0");
    }

    @GetMapping("/reactions")
    public List<ReactionEvent> getReactions() {
        return eventStore.fetchReactionsSince("0");
    }
}
