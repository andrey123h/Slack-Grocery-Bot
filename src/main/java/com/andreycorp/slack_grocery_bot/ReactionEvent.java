package com.andreycorp.slack_grocery_bot;

/**  ReactionEvent carries a reaction payload (which emoji was added)

 * Represents a Slack "reaction_added" event for later tallying.
 */
public record ReactionEvent(
        String user,
        String reaction,
        String channel,
        String ts
) {}
