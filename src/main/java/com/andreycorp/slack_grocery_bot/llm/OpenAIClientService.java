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

/**
 * Service for interacting with OpenAI's API to generate text response
 * Uses the gpt-3.5-turbo model by default.
 */


@Service
public class OpenAIClientService {
    private final HttpClient http = HttpClient.newHttpClient(); // HTTP client for making requests to the OpenAI API
    private final ObjectMapper mapper = new ObjectMapper(); // JSON object mapper for request/response serialization
    private final String apiKey; // The API key used for authenticating with OpenAI
    private final URI chatEndpoint = URI.create("https://api.openai.com/v1/chat/completions"); // The endpoint for the chat completion API

    /**
     * Constructor that initializes the OpenAI client service with the API key.
     *
     * @param apiKey The OpenAI API key
     */

    public OpenAIClientService(
            @Value("${openai.api.key}") String apiKey
    ) {
        this.apiKey = apiKey;
    }

    /**
     * Sends a ChatCompletion request to OpenAI and returns the assistant's reply.
     *
     * Constructs a request with the specified messages, model, and temperature
     * Sends the request to the OpenAI API with proper authentication
     *  Parses the JSON response to extract the generated text
     * @param messages A list of message maps, each containing "role" and "content" keys
     * @param model The OpenAI model to use (e.g., "gpt-3.5-turbo")
     * @param temperature Controls randomness: 0 is deterministic, higher values (0-2) increase randomness
     * @return The generated text response from the model
     * @throws IOException If there is an error in the HTTP request/response processing
     * @throws InterruptedException If the HTTP request is interrupted
     *
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
     *  This method constructs a standard message sequence with:
     *  - A system message setting the AI's behavior/context
     *  - A user message containing the actual prompt
     *  Uses GPT-3.5-Turbo with temperature 0 for consistent, deterministic responses.
     * @param systemPrompt Instructions defining the AI's behavior/role
     * @param userPrompt The actual content/question to generate a response for
     * @return The generated text response from the model
     * @throws IOException If there is an error in the HTTP request/response processing
     * @throws InterruptedException If the HTTP request is interrupted
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
