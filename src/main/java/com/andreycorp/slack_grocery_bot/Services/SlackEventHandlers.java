package com.andreycorp.slack_grocery_bot.Services;


import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.andreycorp.slack_grocery_bot.model.ReactionEvent;
import com.andreycorp.slack_grocery_bot.UI.HomeViewBuilder;
import com.andreycorp.slack_grocery_bot.context.TenantContext;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralizes handling of Slack event callbacks.
 */

@Service
public class SlackEventHandlers {
    private final SlackMessageService slackMessageService;
    private final DefaultsStoreService defaultGroceryService;
    private final HomeViewBuilder homeViewBuilder;
    private final EventStore eventStore;
    private final TenantContext tenantContext;
    private final SummaryService summaryService;

    public SlackEventHandlers(
            SlackMessageService slackMessageService,
            DefaultsStoreService defaultGroceryService,
            HomeViewBuilder homeViewBuilder,
            EventStore eventStore, TenantContext tenantContext, SummaryService summaryService
    ) {
        this.slackMessageService = slackMessageService;
        this.defaultGroceryService = defaultGroceryService;
        this.homeViewBuilder = homeViewBuilder;
        this.eventStore = eventStore;
        this.tenantContext = tenantContext;
        this.summaryService = summaryService;
    }

    /**
     * Builds & publishes the Home tab view when a user opens the App Home.
     * Scopes defaults and admin check to the current workspace (tenant).
     * with real-time summary
     */


    public void handleAppHomeOpened(JsonNode event) throws IOException {
        String userId = event.get("user").asText();
        boolean isAdmin = slackMessageService.isWorkspaceAdmin(userId);
        // Generate current workspace summary
        String summaryMd = summaryService.generateSummaryMarkdown();

        // Resolve channel ID once per workspace
        String groceryChannelId = slackMessageService.getChannelIdByName("office-grocery");

        if (isAdmin) {
            // Admins get the admin dashboard + summary
            Map<String,Integer> defaults = defaultGroceryService.listAll();
            String adminJson = homeViewBuilder.buildAdminHomeJson(defaults, summaryMd);
            slackMessageService.publishHomeView(userId, adminJson);

        } else {
            // Regular users get the welcome + real-time summary
            String userJson = homeViewBuilder.buildUserWelcomeHomeJson(summaryMd, groceryChannelId);
            slackMessageService.publishHomeView(userId, userJson);
        }
    }

    /**
     * Processes messages that mention the bot:
     * records them in the event store and adds a "Completed" reaction for user  acknowledgment.
     * updates the Home tab
     */

    public void handleMessageEvent(JsonNode event) throws IOException {
        // extract event details
        String user    = event.get("user").asText();
        String channel = event.get("channel").asText();
        String text    = event.get("text").asText();
        String ts      = event.get("ts").asText();

        // tenant ID from context
        String teamId = tenantContext.getTeamId();

        // save the message in the event store
        MessageEvent me = new MessageEvent(teamId, user, channel, text, ts);
        eventStore.saveMessage(me);

        // acknowledge with a checkmark reaction
        slackMessageService.addReaction(channel, ts, "white_check_mark");
        System.out.printf("Recorded message: %s%n", me); // debug log

        // After each new order, rebuild and republish Home tab for this user
        String summaryMd = summaryService.generateSummaryMarkdown();
        // Resolve channel ID  per workspace
        String groceryChannelId = slackMessageService.getChannelIdByName("office-grocery");

        boolean isAdmin  = slackMessageService.isWorkspaceAdmin(user);
        String homeJson;

        if (isAdmin) {
            // Admins get the admin dashboard + summary
            Map<String,Integer> defaults = defaultGroceryService.listAll();
            String adminJson = homeViewBuilder.buildAdminHomeJson(defaults, summaryMd);
            slackMessageService.publishHomeView(user, adminJson);

        } else {
            // Regular users get the welcome + real-time summary
            String userJson = homeViewBuilder.buildUserWelcomeHomeJson(summaryMd, groceryChannelId);
            slackMessageService.publishHomeView(user, userJson);
        }
    }

    /**
     * Handles reaction added events, recording user reactions
     * and ignoring the bot's own "Completed" reaction.
     */

    public void handleReactionAdded(JsonNode event) {

        String reaction = event.get("reaction").asText();
        if ("white_check_mark".equals(reaction)) { // ignore our own acknowledgment reaction
            return;
        }
        // extract event details
        String user      = event.get("user").asText();
        JsonNode item    = event.get("item");
        String channel   = item.get("channel").asText();
        String messageTs = item.get("ts").asText();
        // tenant ID from context
        String teamId    = tenantContext.getTeamId();
        // save the reaction in the event store
        ReactionEvent re = new ReactionEvent(teamId, user, reaction, channel, messageTs);
        eventStore.saveReaction(re);

        System.out.printf("Recorded reaction: %s%n", re); // debug log
    }

}
