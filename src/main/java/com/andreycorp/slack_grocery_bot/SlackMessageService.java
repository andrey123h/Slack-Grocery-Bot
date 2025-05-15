package com.andreycorp.slack_grocery_bot;

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SlackMessageService {

    @Value("${slack.bot.token}")
    private String botToken;



    public ChatPostMessageResponse sendMessage(String channelId, String text) throws Exception {
        Slack slack = Slack.getInstance();
        return slack.methods(botToken).chatPostMessage(req ->
                req.channel(channelId)
                        .text(text)
        );
    }

    // New overload to support threading
    public ChatPostMessageResponse sendMessage(String channelId, String text, String threadTs) throws Exception {
        Slack slack = Slack.getInstance();
        return slack.methods(botToken).chatPostMessage(req ->
                req.channel(channelId)
                        .text(text)
                        .threadTs(threadTs)
        );
    }
}