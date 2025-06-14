package com.andreycorp.slack_grocery_bot.Services;

import com.andreycorp.slack_grocery_bot.model.ScheduleSettings;
import com.andreycorp.slack_grocery_bot.scheduler.WeeklyOrderScheduler;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.concurrent.ScheduledFuture;

/**
 * Service that holds the admin-configured schedule settings
 * (day-of-week + time for opening/closing the grocery order thread),
 * and programmatically schedules the WeeklyOrderScheduler jobs
 * via CronTriggers. Allows batching of picker changes until an explicit
 * 'apply()' call.
 */

@Service
public class ScheduleSettingsService {

    //Time-zone for all triggers
    private static final String JERUSALEM_ZONE      = "Asia/Jerusalem";
    // Defaults used on startup if admin hasn’t changed anything
    private static final String DEFAULT_OPEN_DAY    = "MON";
    private static final String DEFAULT_CLOSE_DAY   = "THU";
    private static final String DEFAULT_OPEN_TIME   = "09:00";
    private static final String DEFAULT_CLOSE_TIME  = "17:00";

    private final TaskScheduler        taskScheduler; // Spring’s TaskScheduler (backed by a ThreadPoolTaskScheduler)
    private final WeeklyOrderScheduler weeklyOrderScheduler; // Runs the actual Slack thread open/close logic
    private final ZoneId               zoneId = ZoneId.of(JERUSALEM_ZONE);

    // Current schedule settings, initialized to defaults.
    private String openDay   = DEFAULT_OPEN_DAY;
    private String openTime  = DEFAULT_OPEN_TIME;
    private String closeDay  = DEFAULT_CLOSE_DAY;
    private String closeTime = DEFAULT_CLOSE_TIME;
    // Scheduled futures for the open/close jobs, allowing cancellation/rescheduling
    private ScheduledFuture<?> openFuture;
    private ScheduledFuture<?> closeFuture;

    public ScheduleSettingsService(TaskScheduler taskScheduler,
                                   WeeklyOrderScheduler weeklyOrderScheduler) {
        this.taskScheduler        = taskScheduler;
        this.weeklyOrderScheduler = weeklyOrderScheduler;
    }

    @PostConstruct
    public void init() {
        scheduleTasks(); //  On application startup, schedule with the defaults
    }

    /**
     * Cancels any previously scheduled open/close jobs
     * and re-registers them with new CronTriggers based on
     * the current in-memory day/time values.
     */

    private synchronized void scheduleTasks() {
        // Cancel any existing scheduled tasks
        if (openFuture  != null) openFuture.cancel(false);
        if (closeFuture != null) closeFuture.cancel(false);

        // Schedule open-thread
        String[] ot = openTime.split(":");
        int oh = Integer.parseInt(ot[0]);
        int om = Integer.parseInt(ot[1]);
        // Convert openDay to a cron-compatible format (e.g. "MON" -> "MONDAY")
        String openCron = String.format("0 %d %d * * %s", om, oh, openDay);
        // Schedule that job, capturing its ScheduledFuture so we can cancel it later
        openFuture = taskScheduler.schedule(() -> {
            try {
                weeklyOrderScheduler.openOrderThread();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, new CronTrigger(openCron, zoneId));

        // Schedule close-thread
        String[] ct = closeTime.split(":");
        int   ch = Integer.parseInt(ct[0]);
        int   cm = Integer.parseInt(ct[1]);
        String closeCron = String.format("0 %d %d * * %s", cm, ch, closeDay);
        closeFuture = taskScheduler.schedule(() -> {
            try {
                weeklyOrderScheduler.closeOrderThread();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, new CronTrigger(closeCron, zoneId));
    }

    /** @return configured open-day (e.g. \"MON\") */
    public String getOpenDay() {
        return openDay;
    }

    /** @return configured open-time (HH:mm) */
    public String getOpenTime() {
        return openTime;
    }

    /** @return configured close-day (e.g. \"THU\") */
    public String getCloseDay() {
        return closeDay;
    }

    /** @return configured close-time (HH:mm) */
    public String getCloseTime() {
        return closeTime;
    }

    /** just set the new day, don’t reschedule yet, until "apply" clicked  */
    public void updateOpenDay(String newOpenDay) {
        this.openDay = newOpenDay;
        //scheduleTasks();
    }

    /** just set the new day, don’t reschedule yet, until "apply" clicked  */
    public void updateOpenTime(String newOpenTime) {
        this.openTime = newOpenTime;
        //scheduleTasks();
    }

    /** just set the new day, don’t reschedule yet, until "apply" clicked  */
    public void updateCloseDay(String newCloseDay) {
        this.closeDay = newCloseDay;
        //scheduleTasks();
    }
    /** just set the new day, don’t reschedule yet, until "apply" clicked  */
    public void updateCloseTime(String newCloseTime) {
        this.closeTime = newCloseTime;
       // scheduleTasks();
    }


    /** this is called when the admin clicks "apply" */
    public synchronized void apply() {
        scheduleTasks();
    }

    /**
     * Return the current schedule settings model
     * open/close day & time for rendering the Home tab
     */
    public ScheduleSettings get() {
        return new ScheduleSettings(openDay, openTime, closeDay, closeTime);
    }

}
