package com.andreycorp.slack_grocery_bot.model;

/**
 * Model representing the admin-configurable schedule for opening and closing
 * the weekly grocery order thread.
 *
 * Contains both the day of week (MON, TUE, ... SUN) and time (HH:mm) for
 * opening and closing the thread.
 */
public class ScheduleSettings {

    /** Day code for when the order thread opens (MON, TUE, ..., SUN) */
    private String openDay;
    /** Time (HH:mm) for when the order thread opens */
    private String openTime;

    /** Day code for when the order thread closes */
    private String closeDay;
    /** Time (HH:mm) for when the order thread closes */
    private String closeTime;

    public ScheduleSettings() {
    }

    public ScheduleSettings(String openDay, String openTime, String closeDay, String closeTime) {
        this.openDay = openDay;
        this.openTime = openTime;
        this.closeDay = closeDay;
        this.closeTime = closeTime;
    }

    public String getOpenDay() {
        return openDay;
    }

    public void setOpenDay(String openDay) {
        this.openDay = openDay;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseDay() {
        return closeDay;
    }

    public void setCloseDay(String closeDay) {
        this.closeDay = closeDay;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }

    @Override
    public String toString() {
        return "ScheduleSettings{" +
                "openDay='" + openDay + '\'' +
                ", openTime='" + openTime + '\'' +
                ", closeDay='" + closeDay + '\'' +
                ", closeTime='" + closeTime + '\'' +
                '}';
    }
}
