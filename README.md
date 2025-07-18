# GrocFriend

![GrocFriend](https://github.com/user-attachments/assets/940dce17-7b59-4ded-b9af-60a8b8661b5f)

# Quick start 
Note: Because this application is hosted on Render’s free tier, you may experience cold-start delays of up to several minutes :)
### Step 1. Create new channel named grocery-office on your Slack workspace.
### Step 2. Click the following link, choose #grocery-office and click allow
[Add GrocFriend to your Slack workspace](https://slack.com/oauth/v2/authorize?client_id=8817422810738.8817462125314&scope=app_mentions:read,channels:history,chat:write,groups:history,im:history,im:write,pins:write,reactions:read,reminders:write,users:read,reactions:write,incoming-webhook&user_scope=)
### Step 3. Enjoy and contact with me for feedback.

![allow](https://github.com/user-attachments/assets/a9073e1e-3ca7-47d4-96d1-6d91365eb154)

# Overview
Leverages the Slack Web API alongside Spring MVC 
REST controllers for evet driven webhooks, with a HTTP request-scoped to isolate each HTTP request. integrated
two LLM backends, Ollama’s deepseek-r1:1.5b (local LLM) and OpenAI’s ChatGPT API for summary generation.
Implements custom Slack slash commands,
interactive UI via Slack Block Kit, uses Java’s Regex for
parsing free-form message text, and relies on Jackson
for JSON payload handling. Timed operations are
handled by Spring’s ThreadPoolTaskScheduler and CronTrigger. Tested core logic using JUnit framework & Mockito.
PostgreSQL database with multi-tenant data isolation, accessed via Spring JDBC.
Implementes an custom HTTP middleware for validating Slack request signature.
Dockerized and deployed on Render, backed by a Neon-hosted multi-tenant PostgreSQL.


## In this project, I deliberately chose to build my Slack integration with plain Spring MVC instead of using the Bolt framework, so I could dive deep into the underlying HTTP mechanics, REST controllers, middleware and many other important concepts with which I gained hands-on experience.

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




   
