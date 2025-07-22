package com.andreycorp.slack_grocery_bot.controllers;


import com.andreycorp.slack_grocery_bot.Services.SlackEventHandlers;
import com.andreycorp.slack_grocery_bot.context.TenantContext;
import com.andreycorp.slack_grocery_bot.parsers.SlackRequestParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



/**
 *  Controller to handle incoming Slack events.
 *  1) url_verification event (when Slack verifies the endpoint on configuring an Events API Request URL.)
 *  2) app_home_opened (when a user opens the app's Home tab)
 *  3) app_mention (when the bot is mentioned in a message)
 *  4) reaction_added (when a user adds a reaction to a message)
 */

@RestController
@RequestMapping("/slack/events")
public class EventsController {
    private final SlackEventHandlers handlers;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TenantContext tenantContext;

    public EventsController(
            SlackEventHandlers handlers,
            TenantContext tenantContext
    ) {
        this.handlers = handlers;
        this.tenantContext = tenantContext;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE, // expects JSON body
            produces = MediaType.TEXT_PLAIN_VALUE // reply with plain text
    )
    public ResponseEntity<String> receive(HttpServletRequest request) throws Exception {
        // extracting and parsing JSON body that filter (middleware) cached
        JsonNode payload = SlackRequestParser.parseJsonBody(request, objectMapper);
        String type = SlackRequestParser.extractJsonType(payload);


        //  URL verification handshake
        if ("url_verification".equals(type)) {
            String challenge = payload.get("challenge").asText();
            return ResponseEntity.ok(challenge);
        }

        //  Incoming event callbacks
        if ("event_callback".equals(type)) {

            /// Extract and set tenant context for this request
            String teamId = payload.get("team_id").asText(); // extract team ID from payload
            tenantContext.setTeamId(teamId);
            //  Extract the event type from the payload
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

        //  ack  with an empty 200
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
