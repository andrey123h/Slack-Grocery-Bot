package com.andreycorp.slack_grocery_bot.UI;

import com.andreycorp.slack_grocery_bot.Services.ScheduleSettingsService;
import com.andreycorp.slack_grocery_bot.model.ScheduleSettings;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Home-view JSON construction logic, including day+time pickers and a Save button.
 */
@Component
public class HomeViewBuilder {

    private final ScheduleSettingsService scheduleSettingsService;

    public HomeViewBuilder(ScheduleSettingsService scheduleSettingsService) {
        this.scheduleSettingsService = scheduleSettingsService;
    }

    /**
     * Build the Home view for workspace admins:
     *  1) Header + intro
     *  2) Day & time pickers for open/close
     *  3)  Save schedule button
     *  4) Current Defaults + Add New Default
     */
    public String buildAdminHomeJson(Map<String, Integer> defaults) {
        ScheduleSettings settings = scheduleSettingsService.get();
        String openDay   = settings.getOpenDay();   // e.g. "MON"
        String openTime  = settings.getOpenTime();  // e.g. "09:00"
        String closeDay  = settings.getCloseDay();  // e.g. "THU"
        String closeTime = settings.getCloseTime(); // e.g. "17:00"

        Map<String,String> dayLabels = Map.of(
                "MON","Monday", "TUE","Tuesday", "WED","Wednesday",
                "THU","Thursday", "FRI","Friday", "SAT","Saturday", "SUN","Sunday"
        );
        String openDayLabel  = dayLabels.getOrDefault(openDay, "Monday");
        String closeDayLabel = dayLabels.getOrDefault(closeDay, "Thursday");

        String welcomeBlock =
                "{\n" +
                        "  \"type\": \"header\",\n" +
                        "  \"text\": {\"type\":\"plain_text\",\"text\":\"👋 Welcome to GrocFriend! Your best grocery friend\",\"emoji\":true}\n" +
                        "},\n" +
                        "{\n" +
                        "  \"type\": \"section\",\n" +
                        "  \"text\": {\"type\":\"mrkdwn\",\"text\":\"Hello admin, Bellow is your dashboard.\"}\n" +
                        "},\n" +
                        "{ \"type\": \"divider\" },\n";

        String openDayBlock =
                "{\n" +
                        "  \"type\": \"input\",\n" +
                        "  \"block_id\": \"open_day_block\",\n" +
                        "  \"dispatch_action\": true,\n" +
                        "  \"label\": {\"type\":\"plain_text\",\"text\":\"Order thread opens on\",\"emoji\":true},\n" +
                        "  \"element\": {\n" +
                        "    \"type\": \"static_select\",\n" +
                        "    \"action_id\": \"open_day_picker\",\n" +
                        "    \"initial_option\": {\"text\":{\"type\":\"plain_text\",\"text\":\"" + openDayLabel + "\"},\"value\":\"" + openDay + "\"},\n" +
                        "    \"options\": [\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Monday\",\"emoji\":true},\"value\":\"MON\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Tuesday\",\"emoji\":true},\"value\":\"TUE\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Wednesday\",\"emoji\":true},\"value\":\"WED\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Thursday\",\"emoji\":true},\"value\":\"THU\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Friday\",\"emoji\":true},\"value\":\"FRI\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Saturday\",\"emoji\":true},\"value\":\"SAT\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Sunday\",\"emoji\":true},\"value\":\"SUN\"}\n" +
                        "    ]\n" +
                        "  }\n" +
                        "},\n";

        String openTimeBlock =
                "{\n" +
                        "  \"type\": \"input\",\n" +
                        "  \"block_id\": \"open_time_block\",\n" +
                        "  \"dispatch_action\": true,\n" +
                        "  \"label\": {\"type\":\"plain_text\",\"text\":\"Time\",\"emoji\":true},\n" +
                        "  \"element\": {\"type\":\"timepicker\",\"action_id\":\"open_time_picker\",\"initial_time\":\"" + openTime + "\"}\n" +
                        "},\n";

        String closeDayBlock =
                "{\n" +
                        "  \"type\": \"input\",\n" +
                        "  \"block_id\": \"close_day_block\",\n" +
                        "  \"dispatch_action\": true,\n" +
                        "  \"label\": {\"type\":\"plain_text\",\"text\":\"Order thread closes on\",\"emoji\":true},\n" +
                        "  \"element\": {\n" +
                        "    \"type\": \"static_select\",\n" +
                        "    \"action_id\": \"close_day_picker\",\n" +
                        "    \"initial_option\": {\"text\":{\"type\":\"plain_text\",\"text\":\"" + closeDayLabel + "\"},\"value\":\"" + closeDay + "\"},\n" +
                        "    \"options\": [\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Monday\",\"emoji\":true},\"value\":\"MON\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Tuesday\",\"emoji\":true},\"value\":\"TUE\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Wednesday\",\"emoji\":true},\"value\":\"WED\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Thursday\",\"emoji\":true},\"value\":\"THU\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Friday\",\"emoji\":true},\"value\":\"FRI\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Saturday\",\"emoji\":true},\"value\":\"SAT\"},\n" +
                        "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Sunday\",\"emoji\":true},\"value\":\"SUN\"}\n" +
                        "    ]\n" +
                        "  }\n" +
                        "},\n";

        String closeTimeBlock =
                "{\n" +
                        "  \"type\": \"input\",\n" +
                        "  \"block_id\": \"close_time_block\",\n" +
                        "  \"dispatch_action\": true,\n" +
                        "  \"label\": {\"type\":\"plain_text\",\"text\":\"Time\",\"emoji\":true},\n" +
                        "  \"element\": {\"type\":\"timepicker\",\"action_id\":\"close_time_picker\",\"initial_time\":\"" + closeTime + "\"}\n" +
                        "},\n";

        String saveButtonBlock =
                "{\n" +
                        "  \"type\": \"actions\",\n" +
                        "  \"elements\": [\n" +
                        "    { \"type\": \"button\",\n" +
                        "      \"text\": {\"type\":\"plain_text\",\"text\":\"Apply Changes\",\"emoji\":true},\n" +
                        "      \"action_id\": \"save_schedule\",\n" +
                        "      \"style\": \"primary\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "},\n";

        String defaultsHeader =
                "{\n" +
                        "  \"type\": \"section\",\n" +
                        "  \"text\": {\"type\":\"mrkdwn\",\"text\":\"*Current Defaults:*\"}\n" +
                        "},\n";

        String itemBlocks = defaults.entrySet().stream()
                .map(e ->
                        "{ \"type\":\"section\",\n" +
                                "  \"text\":{ \"type\":\"mrkdwn\",\"text\":\"• *" + e.getKey() + "* — " + e.getValue() + "\"},\n" +
                                "  \"accessory\":{\n" +
                                "    \"type\":\"overflow\",\n" +
                                "    \"action_id\":\"default_item_actions\",\n" +
                                "    \"options\":[\n" +
                                "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Edit\",\"emoji\":true},\"value\":\"EDIT|" + e.getKey() + "\"},\n" +
                                "      {\"text\":{\"type\":\"plain_text\",\"text\":\"Delete\",\"emoji\":true},\"value\":\"DELETE|" + e.getKey() + "\"}\n" +
                                "    ]\n" +
                                "  }\n" +
                                "}"
                )
                .collect(Collectors.joining(",\n"));

        String addDefaultFooter =
                "{ \"type\":\"divider\" },\n" +
                        "{\n" +
                        "  \"type\":\"actions\",\n" +
                        "  \"elements\":[\n" +
                        "    { \"type\":\"button\",\n" +
                        "      \"text\": {\"type\":\"plain_text\",\"text\":\"Add New Default\",\"emoji\":true},\n" +
                        "      \"action_id\":\"add_default\",\n" +
                        "      \"style\":\"primary\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        return "{\n" +
                "  \"type\":\"home\",\n" +
                "  \"blocks\":[\n" +
                welcomeBlock +
                openDayBlock +
                openTimeBlock +
                closeDayBlock +
                closeTimeBlock +
                saveButtonBlock +
                defaultsHeader +
                (itemBlocks.isEmpty() ? "" : itemBlocks + ",\n") +
                addDefaultFooter +
                "  ]\n" +
                "}";
    }

    /**
     * Build a simple Home view for regular users (no admin controls).
     */
    public String buildUserWelcomeHomeJson() {
        return "{\n" +
                "  \"type\":\"home\",\n" +
                "  \"blocks\":[\n" +
                "    {\"type\":\"header\",\"text\":{\"type\":\"plain_text\",\"text\":\"👋 Welcome to GrocFriend! Your best grocery friend\",\"emoji\":true}},\n" +
                "    {\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"To place your weekly grocery orders, go to #office-grocery and mention @GrocFriend in the weekly thread; `@GrocFriend 2 apples, 3 bananas`.\"}},\n" +
                "    {\"type\":\"divider\"},\n" +
                "    {\"type\":\"actions\",\"elements\":[{\"type\":\"button\",\"text\":{\"type\":\"plain_text\",\"text\":\"🏠 Go to #office-grocery\",\"emoji\":true},\"url\":\"https://slack.com/app_redirect?channel=<YOUR_OFFICE_GROCERY_CHANNEL_ID>\"}]}\n" +
                "  ]\n" +
                "}";
    }
}
