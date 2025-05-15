package com.andreycorp.slack_grocery_bot;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class InMemoryEventStore implements EventStore {
    private final CopyOnWriteArrayList<MessageEvent> messages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ReactionEvent> reactions = new CopyOnWriteArrayList<>();

    // Persist a new message event in and reactions to memory. Thread Safety (CopyOnWriteArrayList)
    @Override public void saveMessage(MessageEvent event)      { messages.add(event); }
    @Override public void saveReaction(ReactionEvent event)    { reactions.add(event); }


    /**  Return every MessageEvent whose Slack timestamp is at or after the given fromTs
     * fetchMessagesSince:
     * 1) Parses the provided Slack timestamp string into a numeric cutoff.
     * 2) Streams the in-memory messages list, filtering only events whose ts ≥ cutoff.
     * 3) Collects and returns a new List of matching MessageEvent objects.
     */
    @Override
    public List<MessageEvent> fetchMessagesSince(String fromTs) {
        double cutoff = Double.parseDouble(fromTs); // Converts the Slack timestamp to a number
        return messages.stream()
                .filter(e -> Double.parseDouble(e.ts()) >= cutoff)
                .collect(Collectors.toList());
    }


    @Override
    public List<ReactionEvent> fetchReactionsSince(String fromTs) {
        double cutoff = Double.parseDouble(fromTs); // Parsing the Cutoff
        return reactions.stream()
                .filter(r -> Double.parseDouble(r.ts()) >= cutoff)
                .collect(Collectors.toList());
    }

    @Override
    public void pruneEventsBefore(String beforeTs) {
        double cutoff = Double.parseDouble(beforeTs);
        messages.removeIf(e -> Double.parseDouble(e.ts()) < cutoff);
        reactions.removeIf(r -> Double.parseDouble(r.ts()) < cutoff);
    }
}
