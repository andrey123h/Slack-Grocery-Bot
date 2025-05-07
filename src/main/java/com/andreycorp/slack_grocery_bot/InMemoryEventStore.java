package com.andreycorp.slack_grocery_bot;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class InMemoryEventStore implements EventStore {
    private final CopyOnWriteArrayList<MessageEvent> messages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ReactionEvent> reactions = new CopyOnWriteArrayList<>();

    @Override public void saveMessage(MessageEvent event)      { messages.add(event); }
    @Override public void saveReaction(ReactionEvent event)    { reactions.add(event); }

    @Override
    public List<MessageEvent> fetchMessagesSince(String fromTs) {
        double cutoff = Double.parseDouble(fromTs);
        return messages.stream()
                .filter(e -> Double.parseDouble(e.ts()) >= cutoff)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReactionEvent> fetchReactionsSince(String fromTs) {
        double cutoff = Double.parseDouble(fromTs);
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
