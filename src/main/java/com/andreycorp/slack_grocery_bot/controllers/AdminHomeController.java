package com.andreycorp.slack_grocery_bot.controllers;

//import com.andreycorp.slack_grocery_bot.DefaultsInteractionService;
import com.andreycorp.slack_grocery_bot.Services.AdminHomeInteractionService;
import com.andreycorp.slack_grocery_bot.parsers.SlackRequestParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Controller to handle Slack interactions related to defaults in the UI.
 */

@RestController
@RequestMapping("/slack")
public class AdminHomeController {

    private final AdminHomeInteractionService defaultsInteractionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminHomeController(
            AdminHomeInteractionService defaultsInteractionService
    ) {

        this.defaultsInteractionService = defaultsInteractionService;
    }

    /**
     * Handles Slack block_actions & view_submission for defaults.
     */
    @PostMapping(
            path = "/interact/defaults",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, // expects URL-encoded form data
            produces = MediaType.TEXT_PLAIN_VALUE // reply with plain text
    )
    public ResponseEntity<String> handleDefaultsInteraction(HttpServletRequest request) throws IOException {
        //  Retrieve the raw URL-encoded body that the filter cached
        String rawBody = SlackRequestParser.extractRawBody(request);

        //  Parse the form-encoded pairs into a Map
        Map<String, String> params = SlackRequestParser.parseFormUrlEncoded(rawBody);

        // Extract  JSON payload
        String jsonPayload = params.get("payload");
        if (jsonPayload == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Missing payload");
        }

        //  Parse the JSON and pull out the interaction type
        JsonNode payload = objectMapper.readTree(jsonPayload);
        String type = SlackRequestParser.extractJsonType(payload);

        //  Handle the interaction based on its type
        switch (type) {
            case "block_actions":
                defaultsInteractionService.handleBlockActions(payload);
                break;
            case "view_submission":
                defaultsInteractionService.handleViewSubmission(payload);
                break;
            default:
                // nothing to do for other interaction types
        }

        //  Always ack with an empty 200
        return ResponseEntity.ok("");
    }
}