package com.andreycorp.slack_grocery_bot.controllers;

import com.andreycorp.slack_grocery_bot.scheduler.WeeklyOrderScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual test endpoints to trigger the open/close logic.
 */
@RestController
@RequestMapping("/slack/test")
public class TestController {

    private final WeeklyOrderScheduler scheduler;

    public TestController(WeeklyOrderScheduler scheduler) {
        this.scheduler = scheduler;
    }
    // curl.exe https:(ngrok url)/slack/test/open
    /** Immecdiately opens a new grocery order thread. */
    @GetMapping("/open")
    public ResponseEntity<String> openThread() throws Exception {
        scheduler.openOrderThread();
        return ResponseEntity.ok("Opened thread at ts=" + scheduler.getCurrentThreadTs());
    }

    /** Immediately closes the current grocery order thread and posts the summary. */
    @GetMapping("/close")
    public ResponseEntity<String> closeThread() throws Exception {
        String ts = scheduler.getCurrentThreadTs();
        scheduler.closeOrderThread();
        return ResponseEntity.ok("Closed thread that started at ts=" + ts);
    }
}
