package com.andreycorp.slack_grocery_bot.middleware;

import com.slack.api.app_backend.SlackSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SlackSignatureFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${slack.signing.secret}")
    private String signingSecret;

    private SlackSignature.Generator sigGenerator;

    @BeforeEach
    void setUp() {
        // exactly what Slack does on its side when it signs every request
        sigGenerator = new SlackSignature.Generator(signingSecret);
    }

    // --- EVENTS endpoint tests ---


    @Test
    void whenValidSignature_thenControllerRuns() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String body = "{\"type\":\"url_verification\",\"challenge\":\"test_challenge\"}";

        // ← use generate(...) here
        String validSig = sigGenerator.generate(timestamp, body);

        mockMvc.perform(post("/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .header("X-Slack-Signature", validSig)
                        .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("test_challenge"));
    }

    @Test
    void whenInvalidSignature_thenRejected401() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String body = "{\"type\":\"event_callback\",\"event\":{}}";
        String badSig = "v0=0000000000000000000000000000000000000000000000000000000000000000";

        mockMvc.perform(post("/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .header("X-Slack-Signature", badSig)
                        .content(body)
                )
                .andExpect(status().isUnauthorized());
    }

    // --- COMMANDS endpoint tests ---

    @Test
    void whenValidSignatureOnCommands_thenControllerRuns() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String rawBody = "command=/grocery-summary-admin&user_id=U123";
        String validSig = sigGenerator.generate(timestamp, rawBody);

        mockMvc.perform(post("/slack/commands")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .header("X-Slack-Signature", validSig)
                        .content(rawBody))
                .andExpect(status().isOk())
                .andExpect(content().string("📨 Got it! Generating your summary..."));
    }

    @Test
    void whenInvalidSignatureOnCommands_thenRejected401() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String rawBody = "command=/grocery-summary-admin&user_id=U123";
        String badSig = "v0=0000000000000000000000000000000000000000000000000000000000000000";

        mockMvc.perform(post("/slack/commands")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .header("X-Slack-Signature", badSig)
                        .content(rawBody))
                .andExpect(status().isUnauthorized());
    }

    // --- /slack/interact/defaults ---

    @Test
    void whenValidSignatureOnDefaults_thenControllerReturnsMissingPayload() throws Exception {
        String ts      = String.valueOf(Instant.now().getEpochSecond());
        String rawBody = "";  // no "payload=" param => controller returns Bad Request
        String sig     = sigGenerator.generate(ts, rawBody);

        mockMvc.perform(post("/slack/interact/defaults")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-Slack-Request-Timestamp", ts)
                        .header("X-Slack-Signature",       sig)
                        .content(rawBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing payload"));
    }

    @Test
    void whenInvalidSignatureOnDefaults_thenRejected401() throws Exception {
        String ts      = String.valueOf(Instant.now().getEpochSecond());
        String rawBody = "";
        String sig     = "v0=0000000000000000000000000000000000000000000000000000000000000000";

        mockMvc.perform(post("/slack/interact/defaults")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-Slack-Request-Timestamp", ts)
                        .header("X-Slack-Signature",       sig)
                        .content(rawBody))
                .andExpect(status().isUnauthorized());
    }
}
