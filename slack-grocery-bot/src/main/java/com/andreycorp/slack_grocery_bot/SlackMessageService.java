package com.andreycorp.slack_grocery_bot;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SlackMessageService {

    @Value("${slack.bot.token}")
    private String botToken;

    /**
     * Sends a message to the given Slack channel.
     *
     * @param channelId the ID of the channel (e.g. "C08PHL1GWH5")
     * @param text      the message text
     * @return the Slack API response
     * @throws Exception if the Slack API call fails
     */
    public ChatPostMessageResponse sendMessage(String channelId, String text) throws Exception {
        Slack slack = Slack.getInstance();
        return slack.methods(botToken).chatPostMessage(req ->
                req.channel(channelId)
                        .text(text)
        );
    }
}
