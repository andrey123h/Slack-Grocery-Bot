package com.andreycorp.slack_grocery_bot;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.pins.PinsAddResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service class for interacting with Slack's messaging API.
 * Provides methods to send and pin messages with comprehensive error handling.
 */

@Service
public class SlackMessageService {

    @Value("${slack.bot.token}")
    private String botToken;

    /**
     * Sends a message to a specified Slack channel.
     *
     * @param channelId The ID of the Slack channel.
     * @param text      The text of the message.
     * @return A ChatPostMessageResponse containing the Slack API response.
     * @throws IOException on network errors or Slack API errors.
     */
    public ChatPostMessageResponse sendMessage(String channelId, String text)
            throws IOException {
        try {
            Slack slack = Slack.getInstance();
            ChatPostMessageResponse response = slack.methods(botToken)
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
     *
     * @param channelId The ID of the Slack channel.
     * @param text      The text of the message.
     * @param threadTs  The timestamp of the thread to post into.
     * @return A ChatPostMessageResponse containing the Slack API response.
     * @throws IOException on network errors or Slack API errors.
     */
    public ChatPostMessageResponse sendMessage(String channelId, String text, String threadTs)
            throws IOException {
        try {
            Slack slack = Slack.getInstance();
            ChatPostMessageResponse response = slack.methods(botToken)
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
     *
     * @param channelId The ID of the Slack channel.
     * @param messageTs The timestamp of the message to pin.
     * @throws IOException on network errors or Slack API errors.
     */
    public void pinMessage(String channelId, String messageTs)
            throws IOException {
        try {
            Slack slack = Slack.getInstance();
            PinsAddResponse response = slack.methods(botToken)
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
}
