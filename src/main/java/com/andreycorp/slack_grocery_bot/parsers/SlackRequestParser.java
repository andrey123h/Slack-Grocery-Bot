package com.andreycorp.slack_grocery_bot.parsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * extracting and parsing raw request bodies cached by SlackSignatureFilter.
 */

public class SlackRequestParser {

    /**
     * Retrieves the raw request body stored by the filter.
     * @throws IOException if the body is missing
     */
    public static String extractRawBody(HttpServletRequest request) throws IOException {
        Object raw = request.getAttribute("rawBody");
        if (raw == null) {
            throw new IOException("Missing request body");
        }
        return raw.toString();
    }

    /**
     * Parses an application/x-www-form-urlencoded body into a map of parameters.
     */
    public static Map<String,String> parseFormUrlEncoded(String rawBody) {
        return Arrays.stream(rawBody.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                ));
    }

    /**
     * Extracts and parses JSON body into a JsonNode using the provided ObjectMapper.
     */
    public static JsonNode parseJsonBody(HttpServletRequest request, ObjectMapper mapper) throws IOException {
        String raw = extractRawBody(request);
        return mapper.readTree(raw);
    }

    /**
     * Convenience: Extracts the 'type' field from a JSON payload.
     */
    public static String extractJsonType(JsonNode payload) {
        return payload.path("type").asText();
    }
}
