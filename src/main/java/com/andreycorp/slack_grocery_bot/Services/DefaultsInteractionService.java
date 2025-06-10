package com.andreycorp.slack_grocery_bot.Services;

//import com.andreycorp.slack_grocery_bot.DefaultsStoreService;
//import com.andreycorp.slack_grocery_bot.SlackMessageService;
import com.andreycorp.slack_grocery_bot.UI.HomeViewBuilder;
import com.andreycorp.slack_grocery_bot.UI.ViewPayloads;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/** This service handles user interactions with the Slack Block Kit UI elements in the Home tab.
 * It processes button clicks and menu selections, allowing admins to add, edit, or delete default grocery items.
 * It also manages the display of the Home tab with the latest default items.
 *
 * The service uses SlackMessageService to open modals and publish Home views,
 * and DefaultGroceryService to manage the list of default grocery items.
 */

@Service
public class DefaultsInteractionService {

    private final SlackMessageService slackMessageService;
    private final DefaultsStoreService defaultGroceryService;
    private final HomeViewBuilder homeViewBuilder;

    public DefaultsInteractionService(
            SlackMessageService slackMessageService,
            DefaultsStoreService defaultGroceryService,
            HomeViewBuilder homeViewBuilder
    ) {
        this.slackMessageService = slackMessageService;
        this.defaultGroceryService = defaultGroceryService;
        this.homeViewBuilder = homeViewBuilder;
    }

    /**
     * This method processes user interactions (Admin) with Block Kit UI elements (buttons, menus) in the Slack interface
     * Handles when an admin clicks interactive elements in the Home tab
     * "add_default" - opens an empty "Add"  modal to creating a new deafalut grocery item
     *  "default_item_actions" - When admin selects an option from an item's overflow menu
     *  *       (EDIT|ItemName or DELETE|ItemName)
     *  For "DELETE": Removes the item from storage and refreshes the Home tab
     *  For "EDIT": Opens a modal prefilled with the item's current values
     *  * After deletion or editing, updates the Admin Home view with the latest defaults.
     * @param payload the JSON payload sent by Slack for block_actions
     * @throws IOException if opening the modal or publishing the view fails
     */
    public void handleBlockActions(JsonNode payload) throws IOException {
        // Extracts the first action object from the Slack interaction payload.
        // This represents the specific user action (e.g., button click, menu selection) being processed.
        JsonNode action = payload.get("actions").get(0);

        // Retrieves the action_id string from the action object.
        // The action_id identifies which interactive component triggered the event (e.g., "add_default", "default_item_actions").
        String actionId = action.get("action_id").asText();

        // Extracts the Slack user ID of the user who performed the action from the payload
        // This is used to identify which user's Home tab or modal should be updated.
        String userId   = payload.get("user").get("id").asText();
        // Based on the actionId, perform different operations
        switch (actionId) {
            case "add_default":
                String triggerId = payload.get("trigger_id").asText();
                slackMessageService.openModal(triggerId, ViewPayloads.MODAL_JSON);
                break;

            case "default_item_actions":
                String selected = action.get("selected_option").get("value").asText();
                String[] parts  = selected.split("\\|", 2);
                String mode     = parts[0];       // EDIT or DELETE
                String itemName = parts[1];

                if ("DELETE".equals(mode)) {
                    defaultGroceryService.deleteDefault(itemName);
                    String homeJson = homeViewBuilder.buildAdminHomeJson(defaultGroceryService.listAll());
                    slackMessageService.publishHomeView(userId, homeJson);
                } else { // EDIT
                    int qty = defaultGroceryService.listAll().getOrDefault(itemName, 1);
                    String prefilled = buildPrefilledModal(itemName, qty);
                    String trigger   = payload.get("trigger_id").asText();
                    slackMessageService.openModal(trigger, prefilled);
                }
                break;
        }
    }


    /**
     * This method processes form submissions when an admin submits a modal
     * Handles the data when admin clicks "Save" in the Add/Edit modal
     * Extracts the operation mode ("ADD" or "EDIT") and the original item name from the modal's private metadata
     * Retrieves the new item name and quantity values from the form fields
     * For "ADD" mode: Creates a new default grocery item with the specified name and quantity
     * For "EDIT" mode: Updates the existing item
     * Refreshes the admin's Home tab to display the updated list of default items<
     * If quantity parsing fails, it defaults to 1.
     *
     * @param payload The JSON payload from Slack containing the view submission data, including
     *                form values and user information
     * @throws IOException If there's an error when publishing the updated Home view
     */
    public void handleViewSubmission(JsonNode payload) throws IOException {
        JsonNode view = payload.get("view");
        String privateMeta = view.get("private_metadata").asText();   // e.g. "ADD|" or "EDIT|OldName"
        String[] meta      = privateMeta.split("\\|", 2);
        String mode        = meta[0];                                // ADD or EDIT
        String original    = meta.length > 1 ? meta[1] : null;

        // grab the new values
        JsonNode values = view.get("state").get("values");
        String newName = values.get("item_name_block")
                .get("item_name").get("value").asText().trim();
        String qtyText = values.get("quantity_block")
                .get("quantity").get("value").asText().trim();
        int newQty;
        try { newQty = Integer.parseInt(qtyText); }
        catch (NumberFormatException e) { newQty = 1; }

        // ADD vs EDIT
        if ("ADD".equals(mode)) {
            defaultGroceryService.upsertDefault(newName, newQty);
        } else {
            if (original != null && !original.equals(newName)) {
                defaultGroceryService.deleteDefault(original);
            }
            defaultGroceryService.upsertDefault(newName, newQty);
        }

        // re‐publish the updated Home tab
        String userId   = payload.get("user").get("id").asText();
        Map<String,Integer> defaults = defaultGroceryService.listAll();
        String homeJson = homeViewBuilder.buildAdminHomeJson(defaults);
        slackMessageService.publishHomeView(userId, homeJson);
    }


    /**
     * Build a modal JSON that pre‐fills "Item Name" and "Quantity" in the blocks.
     * Used when editing an existing item.
     */
    public String buildPrefilledModal(String originalItem, int existingQty) {
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