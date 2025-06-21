package com.andreycorp.slack_grocery_bot.controllers;

import com.andreycorp.slack_grocery_bot.scheduler.WeeklyOrderScheduler;
import com.andreycorp.slack_grocery_bot.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual test endpoints to trigger the open/close logic.
 */
@RestController
@RequestMapping("/slack/test")
public class TestController {

    private final WeeklyOrderScheduler scheduler;
    private final TenantContext tenantContext;

    public TestController(WeeklyOrderScheduler scheduler, TenantContext tenantContext) {
        this.scheduler = scheduler;
        this.tenantContext = tenantContext;
    }
    // curl.exe https:(ngrok url)/slack/test/open
    /** Immecdiately opens a new grocery order thread. */

    @GetMapping("/open")
    public ResponseEntity<String> openThread(
            @RequestParam("teamId") String teamId
    ) throws Exception {

        tenantContext.setTeamId(teamId);

        scheduler.openOrderThread();
        return ResponseEntity.ok("Opened thread at ts=" + scheduler.getCurrentThreadTs());
    }

    /** Immediately closes the current grocery order thread and posts the summary. */
    @GetMapping("/close")
    public ResponseEntity<String> closeThread(
            @RequestParam("teamId") String teamId
    ) throws Exception {
        tenantContext.setTeamId(teamId);

        String ts = scheduler.getCurrentThreadTs();
        scheduler.closeOrderThread();
        return ResponseEntity.ok("Closed thread that started at ts=" + ts);
    }
}
