package com.andreycorp.slack_grocery_bot;

/**  ReactionEvent carries a reaction payload (which emoji was added)

 * Represents a Slack "reaction_added" event
 */
public record ReactionEvent(
        String user, // Slack user ID.  who added the reaction.
        String reaction, //  the reaction name ( "+1", "heart", etc.)
        String channel, //  channel ID. in which the reacted‐to message lives
        String ts // The timestamp of the message that received the reaction
) {}
