package com.andreycorp.slack_grocery_bot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIClientService {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final URI chatEndpoint = URI.create("https://api.openai.com/v1/chat/completions");

    public OpenAIClientService(
            @Value("${openai.api.key}") String apiKey
    ) {
        this.apiKey = apiKey;
    }

    /**
     * Sends a ChatCompletion request to OpenAI and returns the assistant's reply.
     */

    public String chatCompletion(List<Map<String, String>> messages,
                                 String model,
                                 double temperature)
            throws IOException, InterruptedException
    {

        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.set("messages", mapper.valueToTree(messages));
        body.put("temperature", temperature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(chatEndpoint)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());
        return root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }

    /**
     * Convenience wrapper for a single system + user prompt.
     */

    public String generate(String systemPrompt,
                           String userPrompt)
            throws IOException, InterruptedException
    {
        List<Map<String, String>> msgs = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
        );
        return chatCompletion(msgs, "gpt-3.5-turbo", 0.0);
    }
}
