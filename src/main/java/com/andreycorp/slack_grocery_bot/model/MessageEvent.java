package com.andreycorp.slack_grocery_bot.model;


/** MessageEvent carries a text payload (what the user typed).

 * A simple POJO (record) representing a Slack "app_mention" message event
 * that we store for later summarization.
 */
public record MessageEvent(
        String user, // Slack user ID
        String channel,
        String text,
        String ts
) {}
