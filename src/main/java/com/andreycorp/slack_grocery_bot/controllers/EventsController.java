package com.andreycorp.slack_grocery_bot.controllers;

import com.andreycorp.slack_grocery_bot.*;
import com.andreycorp.slack_grocery_bot.Services.SlackEventHandlers;
import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.parsers.SlackRequestParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  Controller to handle incoming Slack events.
 *  1) URL‐verification (challenge handshake).
 *  2) app_home_opened (publish Home-tab view).
 *  3) app_mention (record order + add reaction).
 *  4) reaction_added (record user reactions).
 */

@RestController
@RequestMapping("/slack/events")
public class EventsController {
    private final SlackEventHandlers handlers;
    private final EventStore eventStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventsController(
            SlackEventHandlers handlers,
            EventStore eventStore
    ) {
        this.handlers = handlers;
        this.eventStore = eventStore;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE, // expects JSON body
            produces = MediaType.TEXT_PLAIN_VALUE // reply with plain text
    )
    public ResponseEntity<String> receive(HttpServletRequest request) throws Exception {
        // extracting and parsing JSON body that filter cached
        JsonNode payload = SlackRequestParser.parseJsonBody(request, objectMapper);
        String type = SlackRequestParser.extractJsonType(payload);
        //  URL verification handshake
        if ("url_verification".equals(type)) {
            return ResponseEntity.ok(payload.get("challenge").asText());
        }

        //  Incoming event callbacks
        if ("event_callback".equals(type)) {
            JsonNode event = payload.get("event");
            String eventType = event.get("type").asText();

            switch (eventType) {
                case "app_home_opened":
                    handlers.handleAppHomeOpened(event);
                    break;
                case "app_mention":
                    handlers.handleMessageEvent(event);
                    break;
                case "reaction_added":
                    handlers.handleReactionAdded(event);
                    break;
                default:
                    // ignore other event types
            }
        }

        // reply 200 OK to Slack
        return ResponseEntity.ok("");
    }

    // -- debug endpoints to fetch stored messages and reactions --
    /*
    @GetMapping("/messages")
    public List<MessageEvent> getMessages() {
        return eventStore.fetchMessagesSince("0");
    }

    @GetMapping("/reactions")
    public List<ReactionEvent> getReactions() {
        return eventStore.fetchReactionsSince("0");
    }*/
}
