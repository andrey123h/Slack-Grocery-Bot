package com.andreycorp.slack_grocery_bot.Services;

import com.andreycorp.slack_grocery_bot.context.TenantContext;
import com.andreycorp.slack_grocery_bot.jdbc.JdbcWorkspaceService;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsOpenResponse;
import com.slack.api.methods.response.pins.PinsAddResponse;
import com.slack.api.methods.response.reactions.ReactionsAddResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.User; // isAdmin() isOwner()
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Service for sending messages and interacting with Slack API.
 * Provides methods to send messages, pin messages, open direct message channels,
 * check user roles, publish Home tab views, open modals, and add reactions.
 * Centralizes Slack API client creation
 * all methods are tenant-aware, using the current team ID from TenantContext.
 */


@Service
public class SlackMessageService {


    private final TenantContext tenantContext;
    private final JdbcWorkspaceService jdcbWorkspaceService;

    public SlackMessageService(TenantContext tenantContext, JdbcWorkspaceService jdcbWorkspaceService) {
        this.tenantContext = tenantContext;
        this.jdcbWorkspaceService = jdcbWorkspaceService;
    }

    /**
     *  Returns a MethodsClient instance for making Slack API calls.
     *  MethodsClient is the client use to call Slack’s Web API methods.
     *  Used to send messages, pin messages, open channels, etc.
     *  The bot token is retrieved based on the current tenant context (team ID).
     */

    private MethodsClient client() {
        String teamId = tenantContext.getTeamId();
        String token = jdcbWorkspaceService.getBotToken(teamId);
        return Slack.getInstance().methods(token);
    }

    /**
     * Returns a MethodsClient for an explicit tenant ID.
     */
    private MethodsClient clientForTeam(String teamId) {
        String token = jdcbWorkspaceService.getBotToken(teamId);
        return Slack.getInstance().methods(token);
    }


    /**
     * Sends a message to a specified Slack channel.
     */
    public ChatPostMessageResponse sendMessage(String channelId, String text)
            throws IOException {
        try {
            ChatPostMessageResponse response = client()
                    .chatPostMessage(req -> req
                            .channel(channelId)
                            .text(text)
                    );
            if (!response.isOk()) {
                throw new IOException("Slack API error: " + response.getError());
            }
            return response;
        } catch (SlackApiException e) {
            throw new IOException("Failed to send Slack message", e);
        }
    }

    /**
     * Sends a message to a specified Slack channel as a reply in a thread.
     */
    public ChatPostMessageResponse sendMessage(String channelId, String text, String threadTs)
            throws IOException {
        try {
            ChatPostMessageResponse response = client()
                    .chatPostMessage(req -> req
                            .channel(channelId)
                            .text(text)
                            .threadTs(threadTs)
                    );
            if (!response.isOk()) {
                throw new IOException("Slack API error: " + response.getError());
            }
            return response;
        } catch (SlackApiException e) {
            throw new IOException("Failed to send Slack thread message", e);
        }
    }

    /**
     * Pins a message in the specified Slack channel, identified by its timestamp.
     */
    public void pinMessage(String channelId, String messageTs)
            throws IOException {
        try {
            PinsAddResponse response = client()
                    .pinsAdd(req -> req
                            .channel(channelId)
                            .timestamp(messageTs)
                    );
            if (!response.isOk()) {
                throw new IOException("Slack API pin error: " + response.getError());
            }
        } catch (SlackApiException e) {
            throw new IOException("Failed to pin Slack message", e);
        }
    }

    /**
     * Opens  a 1:1 DM channel with the given user ID.
     */

    public String openImChannel(String userId) throws IOException {
        try {
            ConversationsOpenResponse resp = client()
                    .conversationsOpen(r -> r.users(List.of(userId)));
            if (!resp.isOk()) {
                throw new IOException("conversations.open error: " + resp.getError());
            }
            return resp.getChannel().getId();
        } catch (SlackApiException e) {
            throw new IOException("Failed to open IM channel", e);
        }
    }

    /**
     * Returns true if the user is a workspace admin or owner.
     */

    public boolean isWorkspaceAdmin(String userId) throws IOException {
        try {
            // returns metadata about a Slack user, to determine whether they’re an admin
            UsersInfoResponse resp = client()
                    .usersInfo(r -> r.user(userId));
            //  Checking resp.isOk() and resp.getUser():
            // catches cases where Slack responded with "ok": false
            // guards against an unexpected SDK bug or empty payload
            if (!resp.isOk() || resp.getUser() == null) {
                throw new IOException("users.info error: " + resp.getError());
            }
            User u = resp.getUser();
            return Boolean.TRUE.equals(u.isAdmin()) || Boolean.TRUE.equals(u.isOwner());
        } catch (SlackApiException e) {
            throw new IOException("Failed to call users.info", e);
        }
    }

    /**
     * Publishes a Home‐tab view for a specific user.
     */
    public void publishHomeView(String userId, String viewJson) throws IOException {
        try {
            var response = client().viewsPublish(req -> req
                    .userId(userId)
                    .viewAsString(viewJson)
            );
            if (!response.isOk()) {
                throw new IOException("Slack API error on views.publish: " + response.getError());
            }
        } catch (SlackApiException e) {
            throw new IOException("Failed to call views.publish", e);
        }
    }

    /**
     * Opens a Modal view (raw JSON from Block Kit Builder’s “Modal Preview”).
     */
    public void openModal(String triggerId, String viewJson) throws IOException {
        try {
            var response = client().viewsOpen(req -> req
                    .triggerId(triggerId)
                    .viewAsString(viewJson)
            );
            if (!response.isOk()) {
                throw new IOException("Slack API error on views.open: " + response.getError());
            }
        } catch (SlackApiException e) {
            throw new IOException("Failed to call views.open", e);
        }
    }


    /**
     * Adds a reaction (emoji) to a Slack message. Used for "Completed" reaction
     */
    public void addReaction(String channel, String ts, String emojiName) throws IOException {
        try {
            ReactionsAddResponse response = client().reactionsAdd(req -> req
                    .channel(channel)
                    .timestamp(ts)
                    .name(emojiName)
            );
            if (!response.isOk()) {
                throw new IOException("Slack API error on reactions.add: " + response.getError());
            }
        } catch (SlackApiException e) {
            throw new IOException("Failed to add reaction", e);
        }
    }


    /**
     * Look up a public channel by its name (without the “#”) and return its ID.
     * the channel name must be exactly grocery-channel within the workspace.
     */

    public String getChannelIdByName(String channelName) throws IOException {
        try {
            var resp = client().conversationsList(r -> r
                    .excludeArchived(true)
                    .limit(1000)
            );
            if (!resp.isOk()) {
                throw new IOException("conversations.list error: " + resp.getError());
            }
            return resp.getChannels().stream()
                    .filter(c -> c.getName().equals(channelName))
                    .findFirst()
                    .map(c -> c.getId())
                    .orElseThrow(() ->
                            new IOException("Channel not found: " + channelName));
        } catch (SlackApiException e) {
            throw new IOException("Failed to list conversations", e);
        }
    }

    // ---- Explicit-teamID overloads ----

    public ChatPostMessageResponse sendMessageForTeam(
            String teamId, String channelId, String text) throws IOException {
        try {
            ChatPostMessageResponse resp = clientForTeam(teamId)
                    .chatPostMessage(r -> r.channel(channelId).text(text));
            if (!resp.isOk()) throw new IOException(resp.getError());
            return resp;
        } catch (SlackApiException e) {
            throw new IOException(e);
        }
    }

    public ChatPostMessageResponse sendMessageForTeam(
            String teamId, String channelId, String text, String threadTs) throws IOException {
        try {
            ChatPostMessageResponse resp = clientForTeam(teamId)
                    .chatPostMessage(r -> r.channel(channelId).text(text).threadTs(threadTs));
            if (!resp.isOk()) throw new IOException(resp.getError());
            return resp;
        } catch (SlackApiException e) {
            throw new IOException(e);
        }
    }

    public void pinMessageForTeam(String teamId, String channelId, String messageTs) throws IOException {
        try {
            PinsAddResponse resp = clientForTeam(teamId)
                    .pinsAdd(r -> r.channel(channelId).timestamp(messageTs));
            if (!resp.isOk()) throw new IOException(resp.getError());
        } catch (SlackApiException e) {
            throw new IOException(e);
        }
    }


}
