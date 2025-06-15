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


/**
 * Service for interacting with the Ollama LLM API.
 * Handles sending prompts and receiving generated text.
 */


@Service
public class OllamaClientService {
    private final HttpClient http = HttpClient.newHttpClient(); // HTTP client for making requests to the Ollama API
    private final ObjectMapper mapper = new ObjectMapper(); // JSON mapper for request/response serialization
    private final URI endpoint; // Ollama API endpoint
    private final String model; // Model name for text generation

    /**
     * Constructs the OllamaClientService with the configured endpoint and model.
     *
     * @param endpoint The Ollama API endpoint URL.
     * @param model    The model name to use for text generation.
     */

    public OllamaClientService(
            @Value("${ollama.endpoint}") String endpoint,
            @Value("${ollama.model}")    String model
    ) {
        this.endpoint = URI.create(endpoint);
        this.model    = model;
    }

    /**
     * Sends the given prompt to Ollama’s /api/generate and returns the generated text response.
     * Constructs a JSON request body with the model and prompt,
     * Sends it to the Ollama API, and processes the response.
     *  @param prompt The input text prompt to send to the model.
     *  @return The generated text response from the model.
     */
    public String generate(String prompt) throws IOException, InterruptedException {
        // Build JSON body
        ObjectNode body = mapper.createObjectNode()
                .put("model", model)
                .put("prompt", prompt)
                .put("stream", false);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        // Send request
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        // System.out.println("OLLAMA → status=" + resp.statusCode() + " body=" + resp.body()); Debug log

        JsonNode root = mapper.readTree(resp.body());

        // Case 1: new API uses 'results' array
        JsonNode results = root.path("results");
        if (results.isArray() && results.size() > 0) {
            return results.get(0).path("text").asText("");
        }

        // Case 2: legacy or error responses may use 'response'
        if (root.has("response")) {
            return root.path("response").asText("");
        }

        // No usable field found
        throw new IllegalStateException(
                "Ollama returned no usable summary field: " + resp.body()
        );
    }
}
