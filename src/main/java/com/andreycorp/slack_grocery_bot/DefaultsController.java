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
    private final HomeViewBuilder homeViewBuilder;

    public DefaultsController(
            SlackMessageService slackMessageService,
            DefaultGroceryService defaultGroceryService,
            @Value("${slack.signing.secret}") String signingSecret, HomeViewBuilder homeViewBuilder
    ) {
        this.slackMessageService = slackMessageService;
        this.defaultGroceryService = defaultGroceryService;
        this.sigGenerator = new SlackSignature.Generator(signingSecret);
        this.homeViewBuilder = homeViewBuilder;
        this.sigVerifier = new SlackSignature.Verifier(sigGenerator);
    }

    /**
     *  endpoint for:
     *   • block_actions (button clicks, overflow menu)
     *   • view_submission (modal “Save”)
     *
     * Slack will POST with Content-Type = application/x-www-form-urlencoded
     * and a field named “payload” containing the JSON.
     */
    @PostMapping(
            path = "/interact/defaults",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, //  Accepts URL-encoded form data
            produces = MediaType.TEXT_PLAIN_VALUE //  Returns plain text responses
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

        // Parse the JSON payload.
        JsonNode payload = objectMapper.readTree(payloadFormField);
        String type = payload.get("type").asText();

        //  Handle different interaction types
        //  "add_default" - open an empty modal
        //  "default_item_actions" → parse "EDIT|item" or "DELETE|item"

        if ("block_actions".equals(type)) {
            // Admin clicked a button or selected an overflow menu item.
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
                        String homeJson = homeViewBuilder.buildAdminHomeJson(defaultGroceryService.listAll());
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
        // Admin clicked “Save” inside the “Add/Edit Default” modal.
        else if ("view_submission".equals(type)) {

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
            } catch (NumberFormatException e) { // If parsing fails, default to 1
                newQty = 1;
            }

            if ("ADD".equals(mode)) {
                defaultGroceryService.upsertDefault(newItemName, newQty);
            } else {
                // EDIT mode: if name changed, delete original key first
                if (originalItem != null && !originalItem.equals(newItemName)) {
                    defaultGroceryService.deleteDefault(originalItem);
                }
                //  upsert the new item (which may be the same name)
                defaultGroceryService.upsertDefault(newItemName, newQty);
            }

            // After saving, re‐publish the Admin Home view so the list refreshes
            String userId = payload.get("user").get("id").asText(); // to  know whose Home tab to update
            // 1) Get the updated defaults map
            Map<String,Integer> defaults = defaultGroceryService.listAll();
            // 2) Build the full Home-tab JSON using your shared builder
            String homeJson = homeViewBuilder.buildAdminHomeJson(defaults);
            // 3) Publish the view
            slackMessageService.publishHomeView(userId, homeJson);
        }
        return ResponseEntity.ok("");
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