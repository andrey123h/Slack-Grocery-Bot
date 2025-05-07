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
    private final EventStore eventStore;

    public SlackEventsController(SlackMessageService slackMessageService, EventStore eventStore) {
        this.slackMessageService = slackMessageService;
        this.eventStore = eventStore;
    }

    @Value("${slack.signing.secret}")
    private String signingSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receive(
            @RequestHeader("X-Slack-Signature") String signature,
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestBody String rawBody
    ) throws Exception {
        // Verify the Slack signature.HMAC-SHA256 check ensures that the request really came from Slack

        String base = "v0:" + timestamp + ":" + rawBody;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String computed = "v0=" + HexFormat.of().formatHex(mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        if (!computed.equals(signature)) {
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // Parse the JSON payload
        JsonNode payload = objectMapper.readTree(rawBody);
        String type = payload.get("type").asText();

        //  URL verification handshake
        if ("url_verification".equals(type)) {
            return ResponseEntity.ok(payload.get("challenge").asText());
        }

        //  Event callbacks
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

        //  Acknowledge
        return ResponseEntity.ok("");
    }

    private void handleMessageEvent(JsonNode event) {
        // pulling fields out of the JSON
        String user = event.get("user").asText();
        String channel = event.get("channel").asText();
        String text = event.get("text").asText();
        String ts = event.get("ts").asText();

        MessageEvent me = new MessageEvent(user, channel, text, ts);
        eventStore.saveMessage(me);
        System.out.printf("Recorded message: %s%n", me);
    }

    private void handleReactionAdded(JsonNode event) {
        // pulling fields out of the JSON
        String reaction = event.get("reaction").asText();
        String user = event.get("user").asText();
        JsonNode item = event.get("item");
        String channel = item.get("channel").asText();
        String messageTs = item.get("ts").asText();

        ReactionEvent re = new ReactionEvent(user, reaction, channel, messageTs);
        eventStore.saveReaction(re);
        System.out.printf("Recorded reaction: %s%n", re);
    }

    @GetMapping("/messages")
    public List<MessageEvent> getMessages() {
        return eventStore.fetchMessagesSince("0"); // “0” means “from the beginning”
    }

    @GetMapping("/reactions")
    public List<ReactionEvent> getReactions() {
        return eventStore.fetchReactionsSince("0");
    }


}
