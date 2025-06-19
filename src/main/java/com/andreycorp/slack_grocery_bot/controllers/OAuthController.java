package com.andreycorp.slack_grocery_bot.controllers;

import com.andreycorp.slack_grocery_bot.jdbc.JdbcWorkspaceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 *  Controller to handle Slack OAuth callback for app installation.
 *  1) Exchanges the code for a bot token.
 *  2) Persists the workspace credentials in the database.
 */

@RestController
public class OAuthController {

    @Value("${slack.signing-secret}")
    private String signingSecret; // same for all workspaces - tenants


    private final JdbcWorkspaceService workspaceService;
    private final String clientId;   // identify of the GrocFriend Slack app
    private final String clientSecret; // secret of the GrocFriend Slack app
    private final RestTemplate rest; // ** used to make HTTP requests to Slack API. server-to-Slack requests
                                    // OAuth endpoint isn’t wrapped by the Slack SDK, so need to a basic RestTemplate call. **

    public OAuthController(
            JdbcWorkspaceService workspaceService,
            @Value("${slack.client.id}") String clientId,
            @Value("${slack.client.secret}") String clientSecret
    ) {
        this.workspaceService = workspaceService;
        this.clientId         = clientId;
        this.clientSecret     = clientSecret;
        this.rest             = new RestTemplate();
    }

    @GetMapping("/oauth/callback") // the URL Slack redirects to after user authorizes the app
    public ResponseEntity<String> callback(@RequestParam String code) throws Exception {
        //  Build the token‐exchange URL
        String url = "https://slack.com/api/oauth.v2.access"
                + "?client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&code=" + code;
        String resp = rest.getForObject(url, String.class); /// GET request to Slack's for the OAuth token - bot token
        JsonNode json = new ObjectMapper().readTree(resp); /// parse the response JSON for team_id and access_token
        //  Check if the response is valid (successful)
        if (!json.get("ok").asBoolean()) {
            return ResponseEntity.status(400)
                    .body("OAuth failed: " + json.get("error").asText());
        }

        // Extract team_id and tokens
        String teamId        = json.get("team").get("id").asText(); // team ID of the workspace
        String botToken      = json.get("access_token").asText(); // bot token for the app

        //  Persist (or update) the workspace credentials
        workspaceService.upsertWorkspace(teamId, botToken, signingSecret);

        //  Send confirmation message
        return ResponseEntity.ok("App successfully installed for team " + teamId);
    }
}

