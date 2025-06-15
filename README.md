# GrocFriend
## Spring Boot application to manage and automate weekly grocery orders through Slack.
![GrocFriend](https://github.com/user-attachments/assets/940dce17-7b59-4ded-b9af-60a8b8661b5f)


Leverages the Slack Web API alongside Spring MVC
integrated two LLM backends, Ollama’s deepseek-r1:1.5b and OpenAI’s ChatGPT API via clear prompt engineering.
REST controllers for order intake, implements custom Slack slash commands,
interactive UI, uses Java’s regex for
parsing free-form message text, and relies on Jackson
for JSON payload handling. Timed operations are
handled by Spring Scheduling. Testing with JUnit & Mockito.
Implementing an HTTP middleware for authentication and validating of Slack signature 
### In progress: multi-tenant database support, adding deployment pipelines for staging and production


## In this project, I deliberately chose to build my Slack integration with plain Spring MVC instead of using the Bolt framework, so I could dive deep into the underlying HTTP mechanics, REST controllers, middleware and many other important concepts with which I gained hands-on experience.

### Core Funcullality: 
- Opens a new thread (configurable day and time) in a designated “orders” channel with simple ordering instructions.
- Collects user orders via free form messages (no strict form is require)
- Allows “+1” reactions on others’ orders to indicate support and popularity of items.
- Closes the thread later in the week (configurable day and time) and posts a formatted summary showing per-user totals and reaction counts.
- Provides admin tools including a slash command to trigger on-demand summaries.
- Interactive UI, differant view for user's and admins.
- LLM generated summaries alongside manual formatted summary
- Configurable scheduler so workspace admin can choose when to open/close the grocery thread.

### Admin Features
- Interactive UI: Block Kit interface for managing default grocery items and configurable scheduler
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

 ### LLM Integration 
 - Ollama’s deepseek-r1:1.5b and OpenAI’s ChatGPT API's integrations for weekly summary generation


## Using GrocFriend: Screenshots & Flows


- Manual + ChatGPT summary
![chatsum](https://github.com/user-attachments/assets/06719272-5966-402a-9cc1-671b36adb5b2)
  
- Orders, admin summary invoke
![+1](https://github.com/user-attachments/assets/d1396560-71f4-497f-bb69-e716a078835d)

- DM summary admin invoke
![DM](https://github.com/user-attachments/assets/81aca1d8-d012-4426-bdb2-1253bf047a27)

- Home tab for admin
![admin home](https://github.com/user-attachments/assets/6a60a215-6b0e-4494-8948-1a3f5ed3015f)
![admin home edit](https://github.com/user-attachments/assets/eaf5a89e-828d-4ee4-b170-5d835da764f2)

- Home tab for not-admins
![users home](https://github.com/user-attachments/assets/d1f50a6e-e5fb-4e25-bc76-1dd41ba791d0)









   
