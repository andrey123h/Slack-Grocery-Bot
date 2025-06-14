package com.andreycorp.slack_grocery_bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for scheduling tasks programmatically.
 * Exposes a TaskScheduler bean for ScheduleSettingsService.
 */
@Configuration
public class SchedulerConfig {

    /**
     * Defines a TaskScheduler with a small thread pool to run CronTrigger tasks.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("weekly-order-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
