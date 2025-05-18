package com.andreycorp.slack_grocery_bot;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.app_backend.SlackSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/slack/events")
public class SlackEventsController {
    private final SlackMessageService slackMessageService;
    private final EventStore eventStore;
    private final SlackSignature.Generator sigGenerator;
    private final SlackSignature.Verifier sigVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderParser orderParser;

    public SlackEventsController(
            SlackMessageService slackMessageService,
            EventStore eventStore,
            @Value("${slack.signing.secret}") String signingSecret,
            OrderParser orderParser) {
        this.slackMessageService = slackMessageService;
        this.eventStore = eventStore;
        this.sigGenerator = new SlackSignature.Generator(signingSecret);
        this.orderParser = orderParser;
        this.sigVerifier  = new SlackSignature.Verifier(sigGenerator);
    }


    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> receive(
            @RequestHeader("X-Slack-Signature") String incomingSig,
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestBody byte[] rawBodyBytes
    ) throws Exception {
        String rawBody = new String(rawBodyBytes, StandardCharsets.UTF_8);


        // Slack  Signature verification
        if (!sigVerifier.isValid(timestamp, rawBody, incomingSig)) {
            return ResponseEntity.status(401).body("Invalid signature");
        }
        // parsed the incoming JSON then pulled out type field
        JsonNode payload = objectMapper.readTree(rawBody);
        String type = payload.get("type").asText();

        // Handles Slack's "url_verification" event
        if ("url_verification".equals(type)) {
            return ResponseEntity.ok(payload.get("challenge").asText());
        }

        if ("event_callback".equals(type)) {
            JsonNode event = payload.get("event");
            // ignore bot messages
            if (event.has("bot_id")) {
                return ResponseEntity.ok("");
            }
            String eventType = event.get("type").asText();

            if ("app_mention".equals(eventType)) {
                // record message
                handleMessageEvent(event);

                // parse multiple orders in one line
                List<OrderParser.ParsedOrder> orders = orderParser.parseAll(event.get("text").asText());

                // determine thread timestamp
                String threadTs = event.has("thread_ts")
                        ? event.get("thread_ts").asText()
                        : event.get("ts").asText();

                // build ACK body
                String ackBody = orders.stream()
                        .map(o -> String.format("%d× %s", o.qty, o.item))
                        .collect(Collectors.joining(", "));

                // send acknowledgement in thread
                slackMessageService.sendMessage(
                        event.get("channel").asText(),
                        String.format("Got your order: %s ✅", ackBody),
                        threadTs
                );

            } else if ("reaction_added".equals(eventType)) {
                handleReactionAdded(event);
            }
        }
        return ResponseEntity.ok("");
    }


    private void handleMessageEvent(JsonNode event) {
        String user = event.get("user").asText();
        String channel = event.get("channel").asText();
        String text = event.get("text").asText();
        String ts = event.get("ts").asText();

        MessageEvent me = new MessageEvent(user, channel, text, ts);
        eventStore.saveMessage(me);
        System.out.printf("Recorded message: %s%n", me);
    }

    private void handleReactionAdded(JsonNode event) {
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
        return eventStore.fetchMessagesSince("0");
    }

    @GetMapping("/reactions")
    public List<ReactionEvent> getReactions() {
        return eventStore.fetchReactionsSince("0");
    }
}