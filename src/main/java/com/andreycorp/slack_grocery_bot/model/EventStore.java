package com.andreycorp.slack_grocery_bot.model;

import java.util.List;

/**
 * Contract for storing and fetching Slack events (messages & reactions).
 *<p>
 * Includes both legacy, timestamp-based methods and new per-tenant overloads.
 */
public interface EventStore {

    /**
     * Persist a new message event.
     * @param event the MessageEvent to save
     */
    void saveMessage(MessageEvent event);

    /**
     * Persist a new reaction event.
     * @param event the ReactionEvent to save
     */
    void saveReaction(ReactionEvent event);

    /**
     * Retrieve all message events recorded since (and including) the given timestamp.
     * @param fromTs Slack timestamp (string) to start from
     * @return list of MessageEvent
     */
    List<MessageEvent> fetchMessagesSince(String fromTs);

    /**
     * Retrieve all reaction events recorded since (and including) the given timestamp.
     * @param fromTs Slack timestamp (string) to start from
     * @return list of ReactionEvent
     */
    List<ReactionEvent> fetchReactionsSince(String fromTs);

    /**
     * Remove events older than a given cutoff to free memory.
     * @param beforeTs Slack timestamp; all events strictly before this will be deleted
     */
    void pruneEventsBefore(String beforeTs);

    // --- Per-tenant overloads ---

    /**
     * Retrieve all message events for the given workspace (tenant).
     * @param teamId Slack workspace ID
     * @return list of MessageEvent
     */
    List<MessageEvent> fetchMessagesForTeam(String teamId);

    /**
     * Retrieve all reaction events for the given workspace (tenant).
     * @param teamId Slack workspace ID
     * @return list of ReactionEvent
     */
    List<ReactionEvent> fetchReactionsForTeam(String teamId);

    /**
     * Remove events older than a given cutoff for the specified workspace.
     * @param teamId Slack workspace ID
     * @param beforeTs Slack timestamp; all events strictly before this will be deleted
     */
    void pruneEventsBeforeForTeam(String teamId, String beforeTs);
}
