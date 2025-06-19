package com.andreycorp.slack_grocery_bot.Services;

import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.andreycorp.slack_grocery_bot.model.ReactionEvent;
import com.andreycorp.slack_grocery_bot.UI.HomeViewBuilder;
import com.andreycorp.slack_grocery_bot.context.TenantContext;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Map;

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

    public SlackEventHandlers(
            SlackMessageService slackMessageService,
            DefaultsStoreService defaultGroceryService,
            HomeViewBuilder homeViewBuilder,
            EventStore eventStore, TenantContext tenantContext
    ) {
        this.slackMessageService = slackMessageService;
        this.defaultGroceryService = defaultGroceryService;
        this.homeViewBuilder = homeViewBuilder;
        this.eventStore = eventStore;
        this.tenantContext = tenantContext;
    }

    /**
     * Builds & publishes the Home tab view when a user opens the App Home.
     * Scopes defaults and admin check to the current workspace (tenant).
     */
    public void handleAppHomeOpened(JsonNode event) throws IOException {

       // String teamId = tenantContext.getTeamId();

        //  Who opened the Home
        String userId = event.get("user").asText();

        // Check admin status in current workspace
        boolean isAdmin = slackMessageService.isWorkspaceAdmin(userId);

        if (isAdmin) {
            //  Load this workspace's default grocery items
            Map<String, Integer> defaults = defaultGroceryService.listAll();
            //  Build and publish admin view
            String adminHomeJson = homeViewBuilder.buildAdminHomeJson(defaults);
            slackMessageService.publishHomeView(userId, adminHomeJson);
        } else {
            //  Build and publish user welcome view
            String userHomeJson = homeViewBuilder.buildUserWelcomeHomeJson();
            slackMessageService.publishHomeView(userId, userHomeJson);
        }
    }

    /**
     * Processes messages that mention the bot:
     * records them in the event store and adds a "Completed" reaction for user  acknowledgment.
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
