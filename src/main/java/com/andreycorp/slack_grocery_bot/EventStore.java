package com.andreycorp.slack_grocery_bot;

import java.util.List;

/**
 * Contract for storing and fetching Slack events (messages & reactions).
 */
public interface EventStore {

    /**
     * Persist a new message event.
     *
     * @param event the MessageEvent to save
     */
    void saveMessage(MessageEvent event);

    /**
     * Persist a new reaction event.
     *
     * @param event the ReactionEvent to save
     */
    void saveReaction(ReactionEvent event);

    /**
     * Retrieve all message events recorded since (and including) the given timestamp.
     *
     * @param fromTs Slack timestamp (string) to start from
     * @return list of MessageEvent
     */
    List<MessageEvent> fetchMessagesSince(String fromTs);

    /**
     * Retrieve all reaction events recorded since (and including) the given timestamp.
     *
     * @param fromTs Slack timestamp (string) to start from
     * @return list of ReactionEvent
     */
    List<ReactionEvent> fetchReactionsSince(String fromTs);

    /**
     * (Optional) Remove events older than a given cutoff to free memory.
     *
     * @param beforeTs Slack timestamp; all events strictly before this will be deleted
     */
    void pruneEventsBefore(String beforeTs);
}
