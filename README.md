# GrocFriend
## Spring Boot application to manage and automate weekly grocery orders through Slack.
![GrocFriend](https://github.com/user-attachments/assets/940dce17-7b59-4ded-b9af-60a8b8661b5f)


Leverages the Slack Web API alongside Spring MVC
REST controllers for order intake, implements custom Slack slash commands,
interactive UI, uses Java’s regex for
parsing free-form message text, and relies on Jackson
for JSON payload handling. Timed operations are
handled by Spring Scheduling. Testing with JUnit & Mockito.
Implementing an HTTP middleware for authentication and validating of Slack signature 
### In progress: DB, adding deployment pipelines for staging and production, LLM API integration.


## In this project, I deliberately chose to build my Slack integration with plain Spring MVC instead of using the Bolt framework, so I could dive deep into the underlying HTTP mechanics, REST controllers, middleware and many other important concepts with which I gained hands-on experience.

### Funcullality: 
- Opens a new thread (Monday morning) in a designated “orders” channel with simple ordering instructions.
- Collects user orders via free form messages (no strict form is require)
- Allows “+1” reactions on others’ orders to indicate support and popularity of items.
- Closes the thread later in the week (Thursday afternoon) and posts a formatted summary showing per-user totals and reaction counts.
- Provides admin tools including a slash command to trigger on-demand summaries and an interactive Block Kit UI for managing default item lists.

### Controllers (Spring MVC):
   - Event controller handles incoming Slack event payloads (messages, reactions, URL verification, block actions).
   - Command controller listens for slash commands (e.g. /grocery-summary-admin).
   - Defaults controller manages admin interactions for editing the default items list via Block Kit.

### Signature Verification:
  - A reusable middleware uses Slack’s signing secret to verify each HTTP request is genuinely from Slack before any business logic runs.

### Storing:
  - An interface (currently backed by an in-memory implementation) records messages and reactions.

### Business Services:
 - OrderParser turns free-form text into structured item–quantity pairs.
 - SummaryService aggregates events, counts reactions, composes the summary text.
 - SlackMessageService wraps Slack’s Java SDK calls (opening channels, sending messages, publishing Home-tab views, inspections for users type).
 - HomeViewBuilder constructs a dynamic Home-tab interface for workspace admins and regular users.

### Scheduling & Async Processing:
  - A WeeklyOrderScheduler component, driven by Spring’s @Scheduled, automatically opens and closes threads on configured time.
  - Long running tasks run in the background, ensuring always acknowledge Slack within their 3 second timeout.


   
