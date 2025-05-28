package com.andreycorp.slack_grocery_bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock SlackMessageService slackMessageService;
    @Mock OrderParser       orderParser;
    @Mock EventStore        eventStore;

    @InjectMocks
    SummaryService summaryService;

    private final String orderChannel = "C123";
    private final String threadTs     = "1618033.999";
    private final String adminChannel = "DADMIN";

    @BeforeEach
    void setUp() {
        // @InjectMocks handles initialization
    }

    @Test
    void emptyEvents_postsNoOrdersMessage() throws IOException {
        summaryService.summarizeThread(orderChannel, threadTs, List.of(), adminChannel);

        verify(slackMessageService)
                .sendMessage(orderChannel, "No orders were placed this week.", threadTs);
        verifyNoMoreInteractions(slackMessageService);
    }

    @Test
    void singleIntegerOrder_noReactions_postsSummary() throws IOException {
        MessageEvent m = new MessageEvent("U1", orderChannel, "ignored text", "ts1");
        when(orderParser.parseAll(m.text()))
                .thenReturn(List.of(new OrderParser.ParsedOrder(2.0, "apples")));
        when(eventStore.fetchReactionsSince(threadTs))
                .thenReturn(List.of());

        summaryService.summarizeThread(orderChannel, threadTs, List.of(m), null);

        String expected = "*Weekly Grocery Summary:*\n"
                + "• <@U1>: 2× apples\n";
        verify(slackMessageService)
                .sendMessage(orderChannel, expected, threadTs);
        verifyNoMoreInteractions(slackMessageService);
    }

    @Test
    void singleFractionalOrder_noReactions_postsFractionalQty() throws IOException {
        MessageEvent m = new MessageEvent("U2", orderChannel, "ignored", "ts2");
        when(orderParser.parseAll(m.text()))
                .thenReturn(List.of(new OrderParser.ParsedOrder(1.5, "sugar")));
        when(eventStore.fetchReactionsSince(threadTs))
                .thenReturn(List.of());

        summaryService.summarizeThread(orderChannel, threadTs, List.of(m), null);

        String expected = "*Weekly Grocery Summary:*\n"
                + "• <@U2>: 1.5× sugar\n";
        verify(slackMessageService)
                .sendMessage(orderChannel, expected, threadTs);
    }

    @Test
    void singleOrder_withPlusOneReactions_includesSuffix() throws IOException {
        MessageEvent m = new MessageEvent("U3", orderChannel, "ignored", "ts3");
        when(orderParser.parseAll(m.text()))
                .thenReturn(List.of(new OrderParser.ParsedOrder(3.0, "bananas")));
        ReactionEvent r = new ReactionEvent("Ux", "+1", orderChannel, "ts3");
        when(eventStore.fetchReactionsSince(threadTs))
                .thenReturn(List.of(r));

        summaryService.summarizeThread(orderChannel, threadTs, List.of(m), null);

        String expected = "*Weekly Grocery Summary:*\n"
                + "• <@U3>: 3× bananas (1× 👍)\n";
        verify(slackMessageService)
                .sendMessage(orderChannel, expected, threadTs);
    }

    @Test
    void multipleUsersAndItems_mixedReactions_postsCorrectAggregates_looseOrder() throws IOException {
        // Arrange
        MessageEvent m1a = new MessageEvent("U4", orderChannel, "order1", "t1");
        MessageEvent m1b = new MessageEvent("U4", orderChannel, "order2", "t2");
        MessageEvent m2  = new MessageEvent("U5", orderChannel, "order3", "t3");

        when(orderParser.parseAll("order1"))
                .thenReturn(List.of(new OrderParser.ParsedOrder(1.0, "apple")));
        when(orderParser.parseAll("order2"))
                .thenReturn(List.of(new OrderParser.ParsedOrder(2.0, "orange")));
        when(orderParser.parseAll("order3"))
                .thenReturn(List.of(new OrderParser.ParsedOrder(5.0, "pear")));

        ReactionEvent r1 = new ReactionEvent("X", "+1", orderChannel, "t1");
        ReactionEvent r2 = new ReactionEvent("Y", "+1", orderChannel, "t1");
        ReactionEvent r3 = new ReactionEvent("Z", "+1", orderChannel, "t3");
        when(eventStore.fetchReactionsSince(threadTs))
                .thenReturn(List.of(r1, r2, r3));

        // Act
        summaryService.summarizeThread(orderChannel, threadTs,
                List.of(m1a, m1b, m2), null);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(slackMessageService)
                .sendMessage(eq(orderChannel), captor.capture(), eq(threadTs));
        String summary = captor.getValue();

        assertTrue(summary.startsWith("*Weekly Grocery Summary:*"));
        assertTrue(summary.contains("• <@U4>:"),        "Must include U4 header");
        assertTrue(summary.contains("1× apple (2× 👍)"),  "Apple should show 2 reacts");
        assertTrue(summary.contains("2× orange"),         "Orange should appear even with no reactions");
        assertTrue(summary.contains("• <@U5>:"),        "Must include U5 header");
        assertTrue(summary.contains("5× pear (1× 👍)"), "Pear should show 1 react");
    }

    @Test
    void withAdminChannel_sendsDmAfterPostingSummary() throws IOException {
        MessageEvent m = new MessageEvent("U6", orderChannel, "ignored", "tz");
        when(orderParser.parseAll(m.text()))
                .thenReturn(List.of(new OrderParser.ParsedOrder(4.0, "milk")));
        when(eventStore.fetchReactionsSince(threadTs))
                .thenReturn(List.of());

        summaryService.summarizeThread(orderChannel, threadTs,
                List.of(m), adminChannel);

        verify(slackMessageService)
                .sendMessage(eq(orderChannel), anyString(), eq(threadTs));
        verify(slackMessageService)
                .sendMessage(eq(adminChannel), contains("Summary:"));
    }
}
