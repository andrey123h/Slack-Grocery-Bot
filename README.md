# GrocFriend

![GrocFriend](https://github.com/user-attachments/assets/940dce17-7b59-4ded-b9af-60a8b8661b5f)

Leverages the Slack Web API alongside Spring MVC
integratedtwo LLM backends, Ollama’s deepseek-r1:1.5b and OpenAI’s ChatGPT API via clear prompt engineering.
REST controllers for order intake, implements custom Slack slash commands,
interactive UI, uses Java’s regex for
parsing free-form message text, and relies on Jackson
for JSON payload handling. Timed operations are
handled by Spring Scheduling. Testing with JUnit & Mockito.
Implementing an HTTP middleware for authentication and validating of Slack signature 
In progress: deployment on serverless cloud, and implementing multi-tenant database support.


## In this project, I deliberately chose to build my Slack integration with plain Spring MVC instead of using the Bolt framework, so I could dive deep into the underlying HTTP mechanics, REST controllers, middleware and many other important concepts with which I gained hands-on experience.

# Core Funcullality: 
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
- Format Flexibility: Supports varied input formats, default quantity handling
- Interactive UI with instructions 

### Secure Verification:
- HTTP middleware uses Slack’s signing secret to verify each HTTP request is genuinely from Slack before any business logic runs.

### Integration Points
- Event Subscription: Processes Slack events including messages, reactions, and app-clickes
- Interactive Components: Handles button clicks and menu selections of interface
- Command Handling: Responds to slash commands for admim functions
- View Publishing: Dynamically builds and updates Home tab views

 ### LLM Integration 
 - Ollama’s deepseek-r1:1.5b and OpenAI’s ChatGPT API's integrations for weekly summary generation

## Screenshots demo

![orders](https://github.com/user-attachments/assets/948bd91b-719f-4a36-8aa3-de0b213d6c2a)

![admin home](https://github.com/user-attachments/assets/df891eb2-b1e1-4367-9c77-e0855d5aa54b)

![chatsum](https://github.com/user-attachments/assets/29366fee-8a36-4ab8-8825-d8727b433217)

![+1](https://github.com/user-attachments/assets/bb1ec34c-f1fa-425c-b625-8345b94e46a0)

![DM](https://github.com/user-attachments/assets/5d7cc799-28dc-4e45-81a5-bff3e95d12e2)

![admin home edit](https://github.com/user-attachments/assets/005a65bf-77d4-4034-84e4-0776e37cf54a)



   
