package com.andreycorp.slack_grocery_bot.context;


import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Context for the current tenant (team) in a request scope.
 * This allows us to store and retrieve the team ID for the current request.
 * Holds the Slack workspace ID (team_id) for the duration of a single HTTP request.
 */

@Component
@RequestScope // Spring creates one instance per incoming request and discards it afterward.
public class TenantContext {
    private String teamId;

    public void setTeamId(String teamId)   { this.teamId = teamId; } //called in the controllers when a webhook arrives.
    public String getTeamId()              { return teamId; } // used everywhere downstream (DAOs, services) to fetch the right tenant
}


