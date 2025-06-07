package com.andreycorp.slack_grocery_bot;

/**
 * Holds the JSON payloads from Block Kit Builder:
 *  1) HOME_JSON_HEADER:    The fixed header portion of the “Manage Grocery Defaults” Home view
 *  2) HOME_JSON_FOOTER:    The fixed footer portion of the Home view
 *  3) MODAL_JSON:          The “Add / Edit Default” Modal view
 *
 * The controller will insert dynamic item blocks between HEADER and FOOTER when publishing.
 */
public class ViewPayloads {
    /**
     * The “header” portion of  *Manage Grocery Defaults* Home view.
     *
     * Contains:
     *   • A top‐level `type: "home"` wrapper
     *   • A header block showing “Manage Grocery Defaults”
     *   • A divider
     *   • A section titled “*Current Defaults:*”
     */
    public static final String HOME_JSON_HEADER =
            "{\n" +
                    "  \"type\": \"home\",\n" +
                    "  \"blocks\": [\n" +
                    "    { \"type\": \"header\", \"text\": { \"type\": \"plain_text\", \"text\": \"Manage Grocery Defaults\", \"emoji\": true } },\n" +
                    "    { \"type\": \"divider\" },\n" +
                    "    { \"type\": \"section\", \"text\": { \"type\": \"mrkdwn\", \"text\": \"*Current Defaults:*\" } },\n";

    /**
     * The “footer” portion of your Home view, always appended after any dynamic item blocks.
     *
     * Contains:
     *   • divider
     *   • an Actions block with a single “➕ Add New Default” button
     */
    public static final String HOME_JSON_FOOTER =
            "    { \"type\": \"divider\" },\n" +
                    "    { \"type\": \"actions\", \"elements\": [\n" +
                    "      { \"type\": \"button\", \"text\": { \"type\": \"plain_text\", \"text\": \"➕ Add New Default\", \"emoji\": true }, \"action_id\": \"add_default\", \"style\": \"primary\" }\n" +
                    "    ] }\n" +
                    "  ]\n" +
                    "}";

    /**
     * The static JSON payload for your “Add / Edit Default” modal.
     *
     * Blocks:
     *   1) Plain‐text input for “Item Name”, block_id = "item_name_block"
     *   2) Plain‐text input for “Quantity”, block_id = "quantity_block"
     */
    public static final String MODAL_JSON =
            "{\n" +
                    "  \"type\": \"modal\",\n" +
                    "  \"callback_id\": \"add_edit_default_modal\",\n" +
                    "  \"title\": { \"type\": \"plain_text\", \"text\": \"Add / Edit Default\", \"emoji\": true },\n" +
                    "  \"submit\": { \"type\": \"plain_text\", \"text\": \"Save\", \"emoji\": true },\n" +
                    "  \"close\": { \"type\": \"plain_text\", \"text\": \"Cancel\", \"emoji\": true },\n" +
                    "  \"blocks\": [\n" +
                    "    { \"type\": \"input\", \"block_id\": \"item_name_block\", \"label\": { \"type\": \"plain_text\", \"text\": \"Item Name\", \"emoji\": true },\n" +
                    "      \"element\": { \"type\": \"plain_text_input\", \"action_id\": \"item_name\", \"placeholder\": { \"type\": \"plain_text\", \"text\": \"e.g. Apple\" } }\n" +
                    "    },\n" +
                    "    { \"type\": \"input\", \"block_id\": \"quantity_block\", \"label\": { \"type\": \"plain_text\", \"text\": \"Quantity\", \"emoji\": true },\n" +
                    "      \"element\": { \"type\": \"plain_text_input\", \"action_id\": \"quantity\", \"placeholder\": { \"type\": \"plain_text\", \"text\": \"e.g. 2\" } }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
}
