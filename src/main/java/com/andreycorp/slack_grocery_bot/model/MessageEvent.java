package com.andreycorp.slack_grocery_bot.model;


/** MessageEvent carries a text payload (what the user typed).

 * A simple POJO (record) representing a Slack "app_mention" message event
 * that we store for later summarization.
 */
public record MessageEvent(
        String teamId, // Slack team ID of where the message was posted
        String user, // Slack user ID
        String channel, // Channel ID where the message was posted
        String text, // The text of the message
        String ts // Timestamp of the message
) {}
