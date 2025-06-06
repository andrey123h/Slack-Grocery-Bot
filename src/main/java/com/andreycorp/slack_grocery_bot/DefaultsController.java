package com.andreycorp.slack_grocery_bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.app_backend.SlackSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exposes only the interaction endpoint for:
 *   • “➕ Add New Default” button
 *   • Overflow menu (Edit / Delete)
 *   • Modal submissions
 *
 */
@RestController
@RequestMapping("/slack")
public class DefaultsController {

    private final SlackMessageService slackMessageService;
    private final DefaultGroceryService defaultGroceryService;
    private final SlackSignature.Generator sigGenerator;
    private final SlackSignature.Verifier sigVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultsController(
            SlackMessageService slackMessageService,
            DefaultGroceryService defaultGroceryService,
            @Value("${slack.signing.secret}") String signingSecret
    ) {
        this.slackMessageService = slackMessageService;
        this.defaultGroceryService = defaultGroceryService;
        this.sigGenerator = new SlackSignature.Generator(signingSecret);
        this.sigVerifier = new SlackSignature.Verifier(sigGenerator);
    }

    /**
     * Interaction endpoint for:
     *   • block_actions (button clicks, overflow menu)
     *   • view_submission (modal “Save”)
     *
     * Slack will POST with Content-Type = application/x-www-form-urlencoded
     * and a field named “payload” containing the JSON.
     */
    @PostMapping(
            path = "/interact/defaults",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> handleDefaultsInteraction(
            @RequestHeader("X-Slack-Signature") String slackSig,
            @RequestHeader("X-Slack-Request-Timestamp") String tsHeader,
            @RequestParam("payload") String payloadFormField
    ) throws Exception {
        // Bug: 401
        //
        //
        //   if (!sigVerifier.isValid(tsHeader, rawUrlEncodedBodyBytes, slackSig)) {
        //       return ResponseEntity.status(401).body("");
        //   }

        // 1) Parse the JSON payload.
        JsonNode payload = objectMapper.readTree(payloadFormField);
        String type = payload.get("type").asText();

        if ("block_actions".equals(type)) {
            // Admin clicked “➕ Add New Default” or overflow menu in the Home view.
            JsonNode action = payload.get("actions").get(0);
            String actionId = action.get("action_id").asText();

            switch (actionId) {
                case "add_default": {
                    // Open the “Add / Edit Default” modal (no prefilled values).
                    String triggerId = payload.get("trigger_id").asText();
                    slackMessageService.openModal(triggerId, ViewPayloads.MODAL_JSON);
                    break;
                }
                case "default_item_actions": {
                    // Overflow menu: value = “EDIT|ItemName” or “DELETE|ItemName”
                    String selected = action.get("selected_option").get("value").asText();
                    String[] parts = selected.split("\\|", 2);
                    String mode = parts[0];       // "EDIT" or "DELETE"
                    String itemName = parts[1];   // e.g. "Apple"
                    String userId = payload.get("user").get("id").asText();

                    if ("DELETE".equals(mode)) {
                        // Remove from store, then re‐publish Home view
                        defaultGroceryService.deleteDefault(itemName);
                        String homeJson = rebuildAdminHomeJson(defaultGroceryService.listAll());
                        slackMessageService.publishHomeView(userId, homeJson);
                    } else { // mode == "EDIT"
                        // Prefill modal with existing values
                        int existingQty = defaultGroceryService.listAll().getOrDefault(itemName, 1);
                        String prefilledModal = buildPrefilledModal(itemName, existingQty);
                        String trigger = payload.get("trigger_id").asText();
                        slackMessageService.openModal(trigger, prefilledModal);
                    }
                    break;
                }
                // (Ignore any other action_id.)
            }
        }
        else if ("view_submission".equals(type)) {
            // Admin clicked “Save” inside the “Add/Edit Default” modal.
            JsonNode view = payload.get("view");
            String privateMeta = view.get("private_metadata").asText();
            // private_metadata = "ADD|" or "EDIT|OriginalName"
            String[] meta = privateMeta.split("\\|", 2);
            String mode = meta[0];                // "ADD" or "EDIT"
            String originalItem = meta.length > 1 ? meta[1] : null;

            // Extract new values from view.state.values
            JsonNode values = view.get("state").get("values");
            String newItemName = values.get("item_name_block")
                    .get("item_name").get("value").asText().trim();
            String qtyText = values.get("quantity_block")
                    .get("quantity").get("value").asText().trim();
            int newQty;
            try {
                newQty = Integer.parseInt(qtyText);
            } catch (NumberFormatException e) {
                newQty = 1;
            }

            if ("ADD".equals(mode)) {
                defaultGroceryService.upsertDefault(newItemName, newQty);
            } else {
                // EDIT mode: if name changed, delete original key first
                if (originalItem != null && !originalItem.equals(newItemName)) {
                    defaultGroceryService.deleteDefault(originalItem);
                }
                defaultGroceryService.upsertDefault(newItemName, newQty);
            }

            // After saving, re‐publish the Admin Home view so the list refreshes
            String userId = payload.get("user").get("id").asText();
            String homeJson = rebuildAdminHomeJson(defaultGroceryService.listAll());
            slackMessageService.publishHomeView(userId, homeJson);
            // Returning 200 OK auto‐closes the modal.
        }

        // Always respond 200 OK so Slack knows we handled the interaction.
        return ResponseEntity.ok("");
    }

    /**
     * Rebuild the Admin Home JSON by calling exactly the same helper used in SlackEventsController.
     * This helper is essentially the same code, so that the “Defaults” list refreshes after each add/edit/delete.
     */
    private String rebuildAdminHomeJson(Map<String, Integer> defaults) {
        // (A) The static “Welcome” block (same as in the event controller)
        String welcomeBlock =
                "{\n" +
                        "  \"type\": \"header\",\n" +
                        "  \"text\": { \"type\": \"plain_text\", \"text\": \"👋 Welcome to Office Grocery Bot\", \"emoji\": true }\n" +
                        "},\n" +
                        "{\n" +
                        "  \"type\": \"section\",\n" +
                        "  \"text\": { \"type\": \"mrkdwn\", \"text\": \"Hello! If you’re an admin, you can manage default groceries below. If you’re not an admin, please see your instructions above.\" }\n" +
                        "},\n" +
                        "{ \"type\": \"divider\" },\n";

        // (B) The “Current Defaults:” header
        String defaultsHeader =
                "{\n" +
                        "  \"type\": \"section\",\n" +
                        "  \"text\": { \"type\": \"mrkdwn\", \"text\": \"*Current Defaults:*\" }\n" +
                        "},\n";

        // (C) One section+overflow per default item
        String itemBlocks = defaults.entrySet().stream()
                .map(e -> {
                    String name = e.getKey();
                    int qty = e.getValue();
                    return "    { \"type\": \"section\",\n" +
                            "      \"text\": { \"type\": \"mrkdwn\", \"text\": \"• *" + name + "* — " + qty + "\" },\n" +
                            "      \"accessory\": {\n" +
                            "        \"type\": \"overflow\",\n" +
                            "        \"action_id\": \"default_item_actions\",\n" +
                            "        \"options\": [\n" +
                            "          { \"text\": { \"type\": \"plain_text\", \"text\": \"Edit\", \"emoji\": true }, \"value\": \"EDIT|" + name + "\" },\n" +
                            "          { \"text\": { \"type\": \"plain_text\", \"text\": \"Delete\", \"emoji\": true }, \"value\": \"DELETE|" + name + "\" }\n" +
                            "        ]\n" +
                            "      }\n" +
                            "    },";
                })
                .collect(Collectors.joining("\n"));

        String dynamicPart = itemBlocks.isEmpty()
                ? ""
                : itemBlocks + "\n";

        // (D) The final divider + “Add New Default” button
        String footerPart =
                "{ \"type\": \"divider\" },\n" +
                        "{\n" +
                        "  \"type\": \"actions\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"type\": \"button\",\n" +
                        "      \"text\": { \"type\": \"plain_text\", \"text\": \"➕ Add New Default\", \"emoji\": true },\n" +
                        "      \"action_id\": \"add_default\",\n" +
                        "      \"style\": \"primary\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        // (E) Wrap as “home” JSON
        return "{\n" +
                "  \"type\": \"home\",\n" +
                "  \"blocks\": [\n" +
                welcomeBlock +
                defaultsHeader +
                dynamicPart +
                footerPart +
                "  ]\n" +
                "}";
    }

    /**
     * Build a modal JSON that pre‐fills “Item Name” and “Quantity” in the blocks.
     * Used when editing an existing item.
     */
    private String buildPrefilledModal(String originalItem, int existingQty) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"modal\",\n");
        sb.append("  \"callback_id\": \"add_edit_default_modal\",\n");
        sb.append("  \"private_metadata\": \"EDIT|" + originalItem + "\",\n");
        sb.append("  \"title\": { \"type\": \"plain_text\", \"text\": \"Edit Default\", \"emoji\": true },\n");
        sb.append("  \"submit\": { \"type\": \"plain_text\", \"text\": \"Save\", \"emoji\": true },\n");
        sb.append("  \"close\": { \"type\": \"plain_text\", \"text\": \"Cancel\", \"emoji\": true },\n");
        sb.append("  \"blocks\": [\n");
        sb.append("    {\n");
        sb.append("      \"type\": \"input\",\n");
        sb.append("      \"block_id\": \"item_name_block\",\n");
        sb.append("      \"label\": { \"type\": \"plain_text\", \"text\": \"Item Name\", \"emoji\": true },\n");
        sb.append("      \"element\": {\n");
        sb.append("        \"type\": \"plain_text_input\",\n");
        sb.append("        \"action_id\": \"item_name\",\n");
        sb.append("        \"initial_value\": \"" + originalItem + "\",\n");
        sb.append("        \"placeholder\": { \"type\": \"plain_text\", \"text\": \"e.g. Apple\" }\n");
        sb.append("      }\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"type\": \"input\",\n");
        sb.append("      \"block_id\": \"quantity_block\",\n");
        sb.append("      \"label\": { \"type\": \"plain_text\", \"text\": \"Quantity\", \"emoji\": true },\n");
        sb.append("      \"element\": {\n");
        sb.append("        \"type\": \"plain_text_input\",\n");
        sb.append("        \"action_id\": \"quantity\",\n");
        sb.append("        \"initial_value\": \"" + existingQty + "\",\n");
        sb.append("        \"placeholder\": { \"type\": \"plain_text\", \"text\": \"e.g. 2\" }\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
}