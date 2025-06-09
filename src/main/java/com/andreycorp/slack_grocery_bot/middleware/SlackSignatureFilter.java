package com.andreycorp.slack_grocery_bot.middleware;

import com.slack.api.app_backend.SlackSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter to verify Slack request signatures for incoming events and commands.
 * It checks the "X-Slack-Signature" and "X-Slack-Request-Timestamp" headers
 * against the signing secret configured in application properties.
 */

// Marks the filter as very high priority
// Ensures signature-checking happens before everything else.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SlackSignatureFilter extends OncePerRequestFilter {

    private final SlackSignature.Verifier verifier;

    public SlackSignatureFilter(@Value("${slack.signing.secret}") String signingSecret) {
        // build verifier from your signing secret
        this.verifier = new SlackSignature.Verifier(new SlackSignature.Generator(signingSecret));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // only POSTs to /slack/** need verifying
        String uri = request.getRequestURI();
        return !request.getMethod().equalsIgnoreCase("POST")
                || !uri.startsWith("/slack/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // buffers the incoming body so we can read it here without consuming it for  controllers.
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        // Extract headers
        String timestamp = wrapped.getHeader("X-Slack-Request-Timestamp");
        String signature = wrapped.getHeader("X-Slack-Signature");
        if (timestamp == null || signature == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing Slack signature headers");
            return;
        }

        // read the raw bytes exactly as Slack sent them
        byte[] body = StreamUtils.copyToByteArray(wrapped.getInputStream());
        String rawBody = new String(body, StandardCharsets.UTF_8);
        // Make it available downstream
        // Controllers can retrieve the JSON string via request.getAttribute("rawBody")
        wrapped.setAttribute("rawBody", rawBody);

        // Verify signature
        if (!verifier.isValid(timestamp, rawBody, signature)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid Slack signature");
            return;
        }

        // If everything ok, the request handed off to the next filter or controller.
        filterChain.doFilter(wrapped, response);
    }
}
