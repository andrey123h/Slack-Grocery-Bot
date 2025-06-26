package com.andreycorp.slack_grocery_bot.controllers;

import com.andreycorp.slack_grocery_bot.scheduler.WeeklyOrderScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual test endpoints to trigger the open/close logic for a given workspace.
 */
@RestController
@RequestMapping("/slack/test")
public class TestController {

    private final WeeklyOrderScheduler scheduler;

    public TestController(WeeklyOrderScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Immediately opens a new grocery order thread for the specified team.
     * Example: GET /slack/test/open?teamId=T12345
     */
    @GetMapping("/open")
    public ResponseEntity<String> openThread(
            @RequestParam("teamId") String teamId
    ) throws Exception {
        scheduler.openOrderThreadFor(teamId);
        String ts = scheduler.getCurrentThreadTsFor(teamId);
        return ResponseEntity.ok("Opened thread for team " + teamId + " at ts=" + ts);
    }

    /**
     * Immediately closes the current grocery order thread for the specified team and posts the summary.
     * Example: GET /slack/test/close?teamId=T12345
     */
    @GetMapping("/close")
    public ResponseEntity<String> closeThread(
            @RequestParam("teamId") String teamId
    ) throws Exception {
        String ts = scheduler.getCurrentThreadTsFor(teamId);
        scheduler.closeOrderThreadFor(teamId);
        return ResponseEntity.ok("Closed thread for team " + teamId + " that started at ts=" + ts);
    }
}
