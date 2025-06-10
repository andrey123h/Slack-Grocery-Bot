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

### Core Funcullality: 
- Opens a new thread (Monday morning) in a designated “orders” channel with simple ordering instructions.
- Collects user orders via free form messages (no strict form is require)
- Allows “+1” reactions on others’ orders to indicate support and popularity of items.
- Closes the thread later in the week (Thursday afternoon) and posts a formatted summary showing per-user totals and reaction counts.
- Provides admin tools including a slash command to trigger on-demand summaries.
- Interactive Block Kit UI for managing default item lists.

### Admin Features
- Interactive UI: Block Kit interface for managing default grocery items
- On-Demand Summaries: Slash command to generate summaries anytime, DM and thread support.


### User Experience
- Simple Ordering: Users just mention the bot and list items (e.g - '@GrocFriend 10 apple, 2.5 kg sugar, Milk') 
- Format Flexibility: Supports varied input formats 
- Default Handling: Assumes quantity of 1 when not specified

### Secure Verification:
- HTTP middleware uses Slack’s signing secret to verify each HTTP request is genuinely from Slack before any business logic runs.

### Integration Points
- Event Subscription: Processes Slack events including messages, reactions, and app-clickes
- Interactive Components: Handles button clicks and menu selections the interface
- Command Handling: Responds to slash commands for administrative functions
- View Publishing: Dynamically builds and updates Home tab views

## Using GrocFriend: Screenshots & Flows

- Orders
![orders](https://github.com/user-attachments/assets/bef64b67-ff10-44f7-baa7-c94ada9dd71e)

- Admin summary invoke with more orders and "+1"
![+1](https://github.com/user-attachments/assets/d1396560-71f4-497f-bb69-e716a078835d)

- DM summary admin invoke
![DM](https://github.com/user-attachments/assets/81aca1d8-d012-4426-bdb2-1253bf047a27)

- UI
![UI3](https://github.com/user-attachments/assets/744ad127-2b24-4705-b5f2-7c50701461c6)
![UI2](https://github.com/user-attachments/assets/75a892d4-f7b4-4fe1-87f4-575d2983abfc)
![UI1](https://github.com/user-attachments/assets/a3e69f7c-0bcf-41cb-9269-ec26d2175e13)








   
