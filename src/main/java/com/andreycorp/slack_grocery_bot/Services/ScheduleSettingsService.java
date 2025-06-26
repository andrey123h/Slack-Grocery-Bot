package com.andreycorp.slack_grocery_bot.Services;

import com.andreycorp.slack_grocery_bot.context.TenantContext;
import com.andreycorp.slack_grocery_bot.jdbc.JdbcScheduleSettingsService;
import com.andreycorp.slack_grocery_bot.model.ScheduleSettings;
import com.andreycorp.slack_grocery_bot.scheduler.WeeklyOrderScheduler;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Service that manages scheduling of the weekly order thread for each workspace (tenant).
 *
 * On startup, it loads all tenant IDs and schedules open/close tasks per tenant.
 * When an admin updates their schedule pickers, it persists to the DB and reschedules only that tenant's jobs.
 *
 * Cron tasks are not using the TenantContext directly, as they are not run in the context of HTTP request.
 */

@Service

public class ScheduleSettingsService {

    private static final String JERUSALEM_ZONE     = "Asia/Jerusalem";
    private static final String DEFAULT_OPEN_DAY   = "MON";
    private static final String DEFAULT_CLOSE_DAY  = "THU";
    private static final String DEFAULT_OPEN_TIME  = "09:00";
    private static final String DEFAULT_CLOSE_TIME = "17:00";

    private final TaskScheduler               taskScheduler;
    private final WeeklyOrderScheduler        weeklyOrderScheduler;
    private final JdbcScheduleSettingsService dao;
    private final TenantContext               tenantContext;
    private final ZoneId                      zoneId = ZoneId.of(JERUSALEM_ZONE);

    // Track scheduled jobs per tenant
    private final Map<String, ScheduledFuture<?>> openJobs  = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> closeJobs = new ConcurrentHashMap<>();

    public ScheduleSettingsService(
            TaskScheduler taskScheduler,
            WeeklyOrderScheduler weeklyOrderScheduler,
            JdbcScheduleSettingsService dao,
            TenantContext tenantContext
    ) {
        this.taskScheduler        = taskScheduler;
        this.weeklyOrderScheduler = weeklyOrderScheduler;
        this.dao                  = dao;
        this.tenantContext        = tenantContext;
    }

    /**
     * Initialize the service by scheduling jobs for all existing tenants.
     * This runs once at application startup to ensure all teams have their jobs scheduled.
     */
    @PostConstruct
    public void init() {
        List<String> teamIds = dao.findAllTeamIds();
        for (String teamId : teamIds) {
            scheduleForTenant(teamId);
        }
    }

    /**
     * Load schedule settings for the given tenant and (re)register Cron jobs.
     * Never references TenantContext here—uses explicit teamId.
     */
    private synchronized void scheduleForTenant(String teamId) {
        // Cancel any existing jobs
        ScheduledFuture<?> oldOpen = openJobs.remove(teamId);
        if (oldOpen != null) oldOpen.cancel(false);
        ScheduledFuture<?> oldClose = closeJobs.remove(teamId);
        if (oldClose != null) oldClose.cancel(false);

        // Fetch settings or fall back to defaults
        ScheduleSettings s = dao.findByTeamId(teamId);
        if (s == null) {
            s = new ScheduleSettings(
                    DEFAULT_OPEN_DAY, DEFAULT_OPEN_TIME,
                    DEFAULT_CLOSE_DAY, DEFAULT_CLOSE_TIME
            );
        }

        // Schedule open-thread job
        String[] ot = s.getOpenTime().split(":");
        int oh = Integer.parseInt(ot[0]), om = Integer.parseInt(ot[1]);
        String openCron = String.format("0 %d %d * * %s", om, oh, s.getOpenDay());
        ScheduledFuture<?> openFuture = taskScheduler.schedule(() -> {
            try {
                weeklyOrderScheduler.openOrderThreadFor(teamId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, new CronTrigger(openCron, zoneId));
        openJobs.put(teamId, openFuture);

        // Schedule close-thread job
        String[] ct = s.getCloseTime().split(":");
        int ch = Integer.parseInt(ct[0]), cm = Integer.parseInt(ct[1]);
        String closeCron = String.format("0 %d %d * * %s", cm, ch, s.getCloseDay());
        ScheduledFuture<?> closeFuture = taskScheduler.schedule(() -> {
            try {
                weeklyOrderScheduler.closeOrderThreadFor(teamId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, new CronTrigger(closeCron, zoneId));
        closeJobs.put(teamId, closeFuture);
    }

    /**
     * Returns current settings for rendering Home-tab pickers, with defaults if none in DB.
     */
    public ScheduleSettings get() {
        String teamId = tenantContext.getTeamId();
        ScheduleSettings s = dao.findByTeamId(teamId);
        if (s == null) {
            s = new ScheduleSettings(
                    DEFAULT_OPEN_DAY, DEFAULT_OPEN_TIME,
                    DEFAULT_CLOSE_DAY, DEFAULT_CLOSE_TIME
            );
        }
        return s;
    }

    /**
     * Update open-day for the current tenant, persist and reschedule.
     */
    public void updateOpenDay(String newOpenDay) {
        String teamId = tenantContext.getTeamId();
        ScheduleSettings old = get();
        dao.upsert(
                teamId,
                newOpenDay,
                LocalTime.parse(old.getOpenTime()),
                old.getCloseDay(),
                LocalTime.parse(old.getCloseTime())
        );
        scheduleForTenant(teamId);
    }

    /**
     * Update open-time for the current tenant, persist and reschedule.
     */
    public void updateOpenTime(String newOpenTime) {
        String teamId = tenantContext.getTeamId();
        ScheduleSettings old = get();
        dao.upsert(
                teamId,
                old.getOpenDay(),
                LocalTime.parse(newOpenTime),
                old.getCloseDay(),
                LocalTime.parse(old.getCloseTime())
        );
        scheduleForTenant(teamId);
    }

    /**
     * Update close-day for the current tenant, persist and reschedule.
     */
    public void updateCloseDay(String newCloseDay) {
        String teamId = tenantContext.getTeamId();
        ScheduleSettings old = get();
        dao.upsert(
                teamId,
                old.getOpenDay(),
                LocalTime.parse(old.getOpenTime()),
                newCloseDay,
                LocalTime.parse(old.getCloseTime())
        );
        scheduleForTenant(teamId);
    }

    /**
     * Update close-time for the current tenant, persist and reschedule.
     */
    public void updateCloseTime(String newCloseTime) {
        String teamId = tenantContext.getTeamId();
        ScheduleSettings old = get();
        dao.upsert(
                teamId,
                old.getOpenDay(),
                LocalTime.parse(old.getOpenTime()),
                old.getCloseDay(),
                LocalTime.parse(newCloseTime)
        );
        scheduleForTenant(teamId);
    }

    /**
     * Persist the current DB settings for this tenant & reschedule.
     */
    public synchronized void apply() {
        String teamId = tenantContext.getTeamId();
        ScheduleSettings s = get();
        dao.upsert(
                teamId,
                s.getOpenDay(),
                LocalTime.parse(s.getOpenTime()),
                s.getCloseDay(),
                LocalTime.parse(s.getCloseTime())
        );
        scheduleForTenant(teamId);
    }
}
