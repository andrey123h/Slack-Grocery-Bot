package com.andreycorp.slack_grocery_bot;

import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyOrderScheduler {

    private final SlackMessageService slackMessageService;
    private final EventStore eventStore;

    @Value("${slack.order.channel}")
    private String orderChannel;

    // holds the ts of the currently open thread
    private String currentThreadTs;

    public WeeklyOrderScheduler(SlackMessageService slackMessageService,
                                EventStore eventStore) {
        this.slackMessageService = slackMessageService;
        this.eventStore  = eventStore;
    }
    // @Scheduled(cron = "0 * * * * *", zone = "Asia/Jerusalem")
    // @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Jerusalem")
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Jerusalem")
    public void openOrderThread() throws Exception {
        String prompt = "*🛒 New Grocery Order Thread!* Please add your items by Thursday EOD.";
        ChatPostMessageResponse resp = slackMessageService.sendMessage(orderChannel, prompt);
        if (resp.isOk()) {
            currentThreadTs = resp.getTs(); // the timestamp of the newly‐posted “New Grocery Order Thread!” message.
            System.out.println("Opened thread at ts=" + currentThreadTs);
        } else {
            System.err.println("Failed to open thread: " + resp.getError());
        }
    }

    @Scheduled(cron = "0 0 17 * * THU", zone = "Asia/Jerusalem")
    public void closeOrderThread() {
        // summary logic next
        System.out.println("Closing thread ts=" + currentThreadTs);
    }

    public String getCurrentThreadTs() {
        return currentThreadTs;
    }
}
