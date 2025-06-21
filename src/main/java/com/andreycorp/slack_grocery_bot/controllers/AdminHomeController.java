package com.andreycorp.slack_grocery_bot.controllers;

import com.andreycorp.slack_grocery_bot.context.TenantContext;
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
 *  Controller for handling Slack Home-tab interactions
 */

@RestController
@RequestMapping("/slack/home-tab")
public class AdminHomeController {

    private final AdminHomeInteractionService defaultsInteractionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TenantContext tenantContext;

    public AdminHomeController(
            AdminHomeInteractionService defaultsInteractionService, TenantContext tenantContext
    ) {

        this.defaultsInteractionService = defaultsInteractionService;
        this.tenantContext = tenantContext;
    }
    /**
     * Handles Slack block_actions & view_submission
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, // expects URL-encoded form data
            produces = MediaType.TEXT_PLAIN_VALUE // reply with plain text
    )
    public ResponseEntity<String> handleDefaultsInteraction(HttpServletRequest request) throws IOException {
        //  Retrieve the raw URL-encoded body that the filter (middleware) cached
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

        /// Extract and set tenant context
        if (payload.has("team") && payload.get("team").has("id")) {
            String teamId = payload.get("team").get("id").asText();
            tenantContext.setTeamId(teamId);
        }

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

        //  ack with an empty 200
        return ResponseEntity.ok("");
    }
}