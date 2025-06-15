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
- Scheduled Thread Management
 Automatically open a new ordering thread each week (at a configurable day & time) in your designated #orders channel with clear instructions.

- Free-form Order Collection
 Let users submit orders in plain language, in flexible format, capture them directly from the thread.

- Community Voting
 Enable "thubms up" reactions on any order to signal interest and item popularity.

- Automated Closure & Summary
 At a configurable day & time later in the week, automatically close the thread and post a neatly formatted summary showing per-user totals alongside reaction counts.

- On-demand Admin Summaries
 Provide a slash command for workspace admins to trigger instant DM + in-thread summaries whenever needed. 

-Role-specific Interfaces
 Deliver a streamlined ordering view for regular users and a interactive dashboard for admins.

- AI-enhanced Summaries
 LLM generated summaries alongside manual formatted summary

- Customizable Schedule
 Give admins full control over when the ordering threads open and close via an easy configuration.

- Default Item Management
 Maintain a customizable list of default grocery items that admins can add, edit, or remove.

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



   
