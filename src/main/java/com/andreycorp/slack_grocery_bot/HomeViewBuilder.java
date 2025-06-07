package com.andreycorp.slack_grocery_bot;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 *  Home‐view JSON construction logic.
 */

@Component
public class HomeViewBuilder {

    /**
     * Build the Home view for workspace admins:
     *  1) A Welcome header/section
     *  2) A "Current Defaults:" section
     *  3) One overflow‐enabled section per default item
     *  4) A final divider + "Add New Default" button
     */
    public String buildAdminHomeJson(Map<String, Integer> defaults) {
        String welcomeBlock =
                "{\n" +
                        "  \"type\": \"header\",\n" +
                        "  \"text\": { \"type\": \"plain_text\", \"text\": \"👋 Welcome to Office Grocery Bot\", \"emoji\": true }\n" +
                        "},\n" +
                        "{\n" +
                        "  \"type\": \"section\",\n" +
                        "  \"text\": { \"type\": \"mrkdwn\", \"text\": \"Hello Admin! Below you can manage default grocery items.\" }\n" +
                        "},\n" +
                        "{ \"type\": \"divider\" },\n";

        String defaultsHeader =
                "{\n" +
                        "  \"type\": \"section\",\n" +
                        "  \"text\": { \"type\": \"mrkdwn\", \"text\": \"*Current Defaults:*\" }\n" +
                        "},\n";

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
     * Build a simple Home view for regular users (no default‐list management).
     */
    public String buildUserWelcomeHomeJson() {
        return "{\n" +
                "  \"type\": \"home\",\n" +
                "  \"blocks\": [\n" +
                "    { \"type\": \"header\", \"text\": { \"type\": \"plain_text\", \"text\": \"👋 Welcome to Office Grocery Bot\", \"emoji\": true } },\n" +
                "    { \"type\": \"section\", \"text\": { \"type\": \"mrkdwn\", \"text\": \"To place your weekly grocery order, go to #office-grocery and mention @Office Grocery Bot in a thread. Example: `@Office Grocery Bot 2 apples, 1 banana`.\" } },\n" +
                "    { \"type\": \"divider\" },\n" +
                "    { \"type\": \"actions\", \"elements\": [\n" +
                "      { \"type\": \"button\", \"text\": { \"type\": \"plain_text\", \"text\": \"🏠 Go to #office-grocery\", \"emoji\": true },\n" +
                "        \"url\": \"https://slack.com/app_redirect?channel=<YOUR_OFFICE_GROCERY_CHANNEL_ID>\" }\n" +
                "    ] }\n" +
                "  ]\n" +
                "}";
    }
}
