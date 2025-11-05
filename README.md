# GrocFriend

# Quick start 
Note: Because this application is hosted on Render’s free tier, you may experience cold-start delays of up to several minutes :)
### Step 1. Create new channel named grocery-office on your Slack workspace.
### Step 2. Click the following link, choose #grocery-office and click allow
[Add GrocFriend to your Slack workspace](https://slack.com/oauth/v2/authorize?client_id=8817422810738.8817462125314&scope=app_mentions:read,channels:history,chat:write,groups:history,im:history,im:write,pins:write,reactions:read,reminders:write,users:read,reactions:write,incoming-webhook&user_scope=)
### Step 3. Enjoy and contact with me for feedback.

![allow](https://github.com/user-attachments/assets/a9073e1e-3ca7-47d4-96d1-6d91365eb154)



# Core Funcullality: 

- ### Multi-tenant support
   Serve multiple Slack workspaces (tenants) from a single codebase: isolate per-workspace data in PostgreSQL, dynamically retrieve each workspace’s OAuth token, and scope all database queries and API calls to the current workspace context via a request-scoped tenant context.

- ### Scheduled Thread Management
  Automatically open a new ordering thread each week (at a configurable day & time) in your designated #orders channel with clear instructions.

- ### Free-form Order Collection
  Let users submit orders in plain language, in flexible format, capture them directly from the thread.

- ### Community Voting
   Enable "thubms up" reactions on any order to signal interest and item popularity.

- ### Automated Closure & Summary
   At a configurable day & time later in the week, automatically close the thread and post a neatly formatted summary showing per-user totals alongside reaction counts.

- ### On-demand Admin Summaries (not in production, yet)
   Provide a slash command for workspace admins to trigger instant DM + in-thread summaries whenever needed.

- ### Real-time summary
  Updated summary is published in the the App Home tab Interface, showing current per-user totals and reaction counts in real time

- ### Role-specific Interfaces
  Deliver a streamlined ordering view for regular users and a interactive dashboard for admins.

- ### AI-enhanced Summaries
  LLM generated summaries alongside manual formatted summary

- ### Customizable Schedule
   Give admins full control over when the ordering threads open and close via an easy configuration.

- ### Default Item Management
   Maintain a customizable list of default grocery items that admins can add, edit, or remove.



### Admin Features
- Interactive UI: Block Kit interface for managing default grocery items and configurable scheduler alongisde real time summary
- On-Demand Summaries: Slash command to generate summaries anytime, DM and thread support.


### User Experience
- Simple Ordering: Users just mention the bot and list items (e.g - '@GrocFriend 10 apple, 2.5 kg sugar, Milk') 
- Format Flexibility: Supports varied input formats, default quantity handling
- Interactive UI with instructions and real time summary

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

### User Interface
![realtime](https://github.com/user-attachments/assets/b7c354ee-ba54-4dc3-9165-7af314eb5695)
### User's Making Orders
![newmasg](https://github.com/user-attachments/assets/f0d279f7-5215-45da-bcae-1d2756dab409)

![2 members](https://github.com/user-attachments/assets/01785016-2aec-4cf0-bf1a-f1ee963c474a)
### Admin Control Dashborad
![new dash](https://github.com/user-attachments/assets/7e9265f2-777e-475e-9d38-b22e703e2ba1)
### ChatGPT Summary
![chatsum](https://github.com/user-attachments/assets/29366fee-8a36-4ab8-8825-d8727b433217)

![+1](https://github.com/user-attachments/assets/bb1ec34c-f1fa-425c-b625-8345b94e46a0)
### Admin DM Summary Slash Command
![DM](https://github.com/user-attachments/assets/5d7cc799-28dc-4e45-81a5-bff3e95d12e2)
### Admin Edit
![new dash edit](https://github.com/user-attachments/assets/4e6c0e74-c935-43d9-82e8-cee5f4c3039a)

![generating](https://github.com/user-attachments/assets/880fa90b-3157-4f40-9cb2-83be0e4c8bc2)

# Architecture

The system I built follows an event-driven REST API architecture based on Spring MVC, focusing on modular design, multi-tenant support and clear separation of concerns. It uses Spring’s IoC and Dependency Injection to keep components lightweight, testable, and easy to extend. Each module is responsible for a specific part of the system: from handling Slack events to managing integrations, persistence, and background jobs, ensuring maintainability and scalability.

## Core Components
- Controllers: Handeling incoming Slack events (slash commands, message events, and interactive actions). Each event runs in its own request-scoped context for isolation.
- Service Layer - Contains business logic for parsing messages, calling LLMs, managing scheduled tasks and DB interactions.

## Integrations
- Slack - Wraps Slack Web API calls for posting messages and updating the Slack UI.
- LLMs backend - Ollama’s DeepSeek-R1:1.5B (local model) and OpenAI’s API, used for generating summaries and responses.
- Persistence - PostgreSQL database with multi-tenant isolation, accessed via Spring JDBC.
- Middleware - Custom HTTP layer that validates Slack request signatures before processing.
- Scheduler - Uses Spring’s TaskScheduler and CronTrigger for periodic jobs (like cleanup and automated summaries).

## Infrastructure
- Deployment: Dockerized, Hosted on Render, connected to a PostgreSQL database hosted on Neon.
- Testing: Core logic tested with JUnit framework and Mockito.

Educational Focus: I deliberately chose to build my Slack integration with plain Spring MVC instead of using the Bolt framework to gain hands-on experience with HTTP internals, middleware, and request lifecycle.




   
