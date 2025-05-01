package com.andreycorp.slack_grocery_bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@RestController
@RequestMapping("/slack/events")
public class SlackEventsController {
    private final SlackMessageService slackMessageService;

    public SlackEventsController(SlackMessageService slackMessageService) {
        this.slackMessageService = slackMessageService;
    }

    @Value("${slack.signing.secret}")
    private String signingSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory stores for tests
    private final List<MessageEvent> messages = new ArrayList<>();
    private final List<ReactionEvent> reactions = new ArrayList<>();

    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receive(
            @RequestHeader("X-Slack-Signature") String signature,
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestBody String rawBody
    ) throws Exception {
        // 1) Verify signature
        String base = "v0:" + timestamp + ":" + rawBody;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String computed = "v0=" + HexFormat.of().formatHex(mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        if (!computed.equals(signature)) {
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // 2) Parse JSON
        JsonNode payload = objectMapper.readTree(rawBody);
        String type = payload.get("type").asText();

        // 3) URL verification handshake
        if ("url_verification".equals(type)) {
            return ResponseEntity.ok(payload.get("challenge").asText());
        }

        // 4) Event callbacks
        if ("event_callback".equals(type)) {
            JsonNode event = payload.get("event");
            // Ignore any bot-generated events
            if (event.has("bot_id")) {
                return ResponseEntity.ok("");
            }
            String et = event.get("type").asText();

            if ("app_mention".equals(et)) {
                handleMessageEvent(event);
                String channel = event.get("channel").asText();
                String text = event.get("text").asText();
                slackMessageService.sendMessage(channel, "Got your order: " + text + " ✅");
            } else if ("reaction_added".equals(et)) {
                handleReactionAdded(event);
            }
        }

        // 5) Acknowledge
        return ResponseEntity.ok("");
    }

    private void handleMessageEvent(JsonNode event) {
        String user = event.get("user").asText();
        String channel = event.get("channel").asText();
        String text = event.get("text").asText();
        String ts = event.get("ts").asText();

        messages.add(new MessageEvent(user, channel, text, ts));
        System.out.printf("Recorded message: %s%n", messages.get(messages.size() - 1));
    }

    private void handleReactionAdded(JsonNode event) {
        String reaction = event.get("reaction").asText();
        String user = event.get("user").asText();
        JsonNode item = event.get("item");
        String channel = item.get("channel").asText();
        String messageTs = item.get("ts").asText();

        reactions.add(new ReactionEvent(user, reaction, channel, messageTs));
        System.out.printf("Recorded reaction: %s%n", reactions.get(reactions.size() - 1));
    }

    @GetMapping("/messages")
    public List<MessageEvent> getMessages() {
        return List.copyOf(messages);
    }

    @GetMapping("/reactions")
    public List<ReactionEvent> getReactions() {
        return List.copyOf(reactions);
    }

    public static record MessageEvent(String user, String channel, String text, String ts) {}
    public static record ReactionEvent(String user, String reaction, String channel, String ts) {}
}
