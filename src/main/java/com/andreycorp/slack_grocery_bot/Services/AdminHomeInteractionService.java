package com.andreycorp.slack_grocery_bot.Services;

import com.andreycorp.slack_grocery_bot.UI.HomeViewBuilder;
import com.andreycorp.slack_grocery_bot.UI.ViewPayloads;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Service handling all Home-tab Block Kit interactions:
 *  • Schedule pickers for open/close day & time
 *  • “Save schedule” button
 *  • Default-item buttons & overflow menu
 */
@Service
public class AdminHomeInteractionService {

    private final SlackMessageService      slackMessageService;
    private final DefaultsStoreService     defaultGroceryService;
    private final HomeViewBuilder          homeViewBuilder;
    private final ScheduleSettingsService  scheduleSettingsService;

    public AdminHomeInteractionService(
            SlackMessageService      slackMessageService,
            DefaultsStoreService     defaultGroceryService,
            HomeViewBuilder          homeViewBuilder,
            ScheduleSettingsService  scheduleSettingsService
    ) {
        this.slackMessageService     = slackMessageService;
        this.defaultGroceryService   = defaultGroceryService;
        this.homeViewBuilder         = homeViewBuilder;
        this.scheduleSettingsService = scheduleSettingsService;
    }

    /**
     * Handle all block_actions from the Admin Home:
     *  - open_day_picker, open_time_picker,
     *  - close_day_picker, close_time_picker,
     *  - save_schedule,
     *  - add_default, default_item_actions.
     */
    public void handleBlockActions(JsonNode payload) throws IOException {
        JsonNode action  = payload.get("actions").get(0);
        String actionId  = action.get("action_id").asText();
        String userId    = payload.get("user").get("id").asText();

        switch (actionId) {
            // --- Schedule pickers ---
            case "open_day_picker":
                scheduleSettingsService.updateOpenDay(
                        action.get("selected_option").get("value").asText()
                );
                break;

            case "open_time_picker":
                scheduleSettingsService.updateOpenTime(
                        action.get("selected_time").asText()
                );
                break;

            case "close_day_picker":
                scheduleSettingsService.updateCloseDay(
                        action.get("selected_option").get("value").asText()
                );
                break;

            case "close_time_picker":
                scheduleSettingsService.updateCloseTime(
                        action.get("selected_time").asText()
                );
                break;

            case "save_schedule":
                //  persist & re-render the Home tab
                String full = homeViewBuilder.buildAdminHomeJson(
                        defaultGroceryService.listAll()
                );
                slackMessageService.publishHomeView(userId, full);

                //  now actually re-schedule the open/close jobs
                scheduleSettingsService.apply();
                return;


            // --- Default-item actions ---
            case "add_default":
                String triggerId = payload.get("trigger_id").asText();
                slackMessageService.openModal(triggerId, ViewPayloads.MODAL_JSON);
                return;

            case "default_item_actions":
                String[] parts = action.get("selected_option").get("value").asText().split("\\|", 2);
                String mode     = parts[0];
                String itemName = parts[1];
                if ("DELETE".equals(mode)) {
                    defaultGroceryService.deleteDefault(itemName);
                } else {
                    int qty = defaultGroceryService.listAll().getOrDefault(itemName, 1);
                    String prefilled = buildPrefilledModal(itemName, qty);
                    String trigger = payload.get("trigger_id").asText();
                    slackMessageService.openModal(trigger, prefilled);
                    return;
                }
                break;

            default:
                // ignore any other actions
        }

        // After any schedule‐change or default deletion, re-publish the Admin Home
        Map<String,Integer> defaults = defaultGroceryService.listAll();
        String homeJson = homeViewBuilder.buildAdminHomeJson(defaults);
        slackMessageService.publishHomeView(userId, homeJson);
    }

    /**
     * Handle modal submissions for adding or editing default items.
     */
    public void handleViewSubmission(JsonNode payload) throws IOException {
        JsonNode view      = payload.get("view");
        String privateMeta = view.get("private_metadata").asText();   // "ADD|" or "EDIT|OldName"
        String[] meta      = privateMeta.split("\\|", 2);
        String mode        = meta[0];
        String original    = meta.length>1 ? meta[1] : null;

        JsonNode values    = view.get("state").get("values");
        String newName     = values.get("item_name_block").get("item_name").get("value").asText().trim();
        String qtyText     = values.get("quantity_block").get("quantity").get("value").asText().trim();
        int newQty;
        try { newQty = Integer.parseInt(qtyText); }
        catch (NumberFormatException e) { newQty = 1; }

        if ("ADD".equals(mode)) {
            defaultGroceryService.upsertDefault(newName, newQty);
        } else {
            if (original != null && !original.equals(newName)) {
                defaultGroceryService.deleteDefault(original);
            }
            defaultGroceryService.upsertDefault(newName, newQty);
        }

        String userId = payload.get("user").get("id").asText();
        String homeJson = homeViewBuilder.buildAdminHomeJson(defaultGroceryService.listAll());
        slackMessageService.publishHomeView(userId, homeJson);
    }

    /**
     * Build a JSON string for the Edit modal, with fields pre-filled.
     */
    public String buildPrefilledModal(String originalItem, int existingQty) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
                .append("  \"type\":\"modal\",\n")
                .append("  \"callback_id\":\"add_edit_default_modal\",\n")
                .append("  \"private_metadata\":\"EDIT|").append(originalItem).append("\",\n")
                .append("  \"title\":{ \"type\":\"plain_text\",\"text\":\"Edit Default\",\"emoji\":true },\n")
                .append("  \"submit\":{ \"type\":\"plain_text\",\"text\":\"Save\",\"emoji\":true },\n")
                .append("  \"close\":{ \"type\":\"plain_text\",\"text\":\"Cancel\",\"emoji\":true },\n")
                .append("  \"blocks\":[\n")
                .append("    { \"type\":\"input\",\"block_id\":\"item_name_block\",\n")
                .append("      \"label\":{ \"type\":\"plain_text\",\"text\":\"Item Name\",\"emoji\":true },\n")
                .append("      \"element\":{ \"type\":\"plain_text_input\",\"action_id\":\"item_name\",\"initial_value\":\"")
                .append(originalItem).append("\",\"placeholder\":{ \"type\":\"plain_text\",\"text\":\"e.g. Apple\" } }\n")
                .append("    },\n")
                .append("    { \"type\":\"input\",\"block_id\":\"quantity_block\",\n")
                .append("      \"label\":{ \"type\":\"plain_text\",\"text\":\"Quantity\",\"emoji\":true },\n")
                .append("      \"element\":{ \"type\":\"plain_text_input\",\"action_id\":\"quantity\",\"initial_value\":\"")
                .append(existingQty).append("\",\"placeholder\":{ \"type\":\"plain_text\",\"text\":\"e.g. 2\" } }\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}");
        return sb.toString();
    }
}
