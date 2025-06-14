package com.andreycorp.slack_grocery_bot.Services;

import com.andreycorp.slack_grocery_bot.model.EventStore;
import com.andreycorp.slack_grocery_bot.model.MessageEvent;
import com.andreycorp.slack_grocery_bot.model.ReactionEvent;
import com.andreycorp.slack_grocery_bot.UI.HomeViewBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import java.io.IOException;

/**
 * Centralizes handling of Slack event callbacks.
 */

@Service
public class SlackEventHandlers {
    private final SlackMessageService slackMessageService;
    private final DefaultsStoreService defaultGroceryService;
    private final HomeViewBuilder homeViewBuilder;
    private final EventStore eventStore;

    public SlackEventHandlers(
            SlackMessageService slackMessageService,
            DefaultsStoreService defaultGroceryService,
            HomeViewBuilder homeViewBuilder,
            EventStore eventStore
    ) {
        this.slackMessageService = slackMessageService;
        this.defaultGroceryService = defaultGroceryService;
        this.homeViewBuilder = homeViewBuilder;
        this.eventStore = eventStore;
    }

    /**
     * Builds & publishes the Home tab view when a user opens the App Home.
     * If the user is an admin, shows the admin view with a dashboard. If not, shows user's view.
     */

    public void handleAppHomeOpened(JsonNode event) throws IOException {
        String userId = event.get("user").asText();
        boolean isAdmin = slackMessageService.isWorkspaceAdmin(userId);
        if (isAdmin) {
            String adminHomeJson = homeViewBuilder.buildAdminHomeJson(defaultGroceryService.listAll());
            //System.out.println(">>> Home view JSON:\n" + adminHomeJson); //debug only
            slackMessageService.publishHomeView(userId, adminHomeJson);
        } else {
            String userHomeJson = homeViewBuilder.buildUserWelcomeHomeJson();
            slackMessageService.publishHomeView(userId, userHomeJson);
        }
    }

    /**
     * Processes messages that mention the bot:
     * records them in the event store and adds a "Completed" reaction.
     */

    public void handleMessageEvent(JsonNode event) throws IOException {
        String user    = event.get("user").asText();
        String channel = event.get("channel").asText();
        String text    = event.get("text").asText();
        String ts      = event.get("ts").asText();

        MessageEvent me = new MessageEvent(user, channel, text, ts);
        eventStore.saveMessage(me);

        // Acknowledge  user with a checkmark reaction
        slackMessageService.addReaction(channel, ts, "white_check_mark");
        System.out.printf("Recorded message: %s%n", me);
    }

    /**
     * Handles reaction added events, recording user reactions
     * and ignoring the bot's own "Completed" reaction.
     */
    public void handleReactionAdded(JsonNode event) {
        String reaction = event.get("reaction").asText();
        if ("white_check_mark".equals(reaction)) {
            return;
        }

        String user     = event.get("user").asText();
        JsonNode item   = event.get("item");
        String channel  = item.get("channel").asText();
        String messageTs= item.get("ts").asText();

        ReactionEvent re = new ReactionEvent(user, reaction, channel, messageTs);
        eventStore.saveReaction(re);
        System.out.printf("Recorded reaction: %s%n", re);
    }
}
