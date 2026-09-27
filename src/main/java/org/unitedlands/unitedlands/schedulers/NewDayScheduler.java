package org.unitedlands.unitedlands.schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.unitedlands.unitedlands.UnitedLands;
import org.unitedlands.unitedlands.classes.configs.GeneralConfig;
import org.unitedlands.unitedlands.tasks.NewDayTask;
import org.unitedlands.utils.United;

public class NewDayScheduler {

    private static NewDayScheduler instance;

    public static NewDayScheduler instance() {
        return instance;
    }

    private long lastExecutionTime = System.currentTimeMillis();

    private BukkitTask scheduleTask;
    private BukkitTask newDayTask;

    public NewDayScheduler() {
        instance = this;
    }

    public void scheduleNewDay() {

        var useInterval = GeneralConfig.get().general().newDay().useInterval();
        long interval = GeneralConfig.get().general().newDay().intervalSeconds();
        String newDayTime = GeneralConfig.get().general().newDay().newDayTime();

        long secondsToNewDay = 0;

        if (!useInterval) {
            secondsToNewDay = getSecondsUntilTime(newDayTime);
        } else {
            var elapsedSecondsSinceLastNewDay = (System.currentTimeMillis() - lastExecutionTime) / 1000;
            secondsToNewDay = interval - elapsedSecondsSinceLastNewDay;
        }

        if (secondsToNewDay > 120) {
            if (scheduleTask != null)
                scheduleTask.cancel();
            var newScheduleSeconds = secondsToNewDay / 2;
            scheduleTask = Bukkit.getScheduler().runTaskLater(UnitedLands.instance(), () -> {
                scheduleNewDay();
            }, newScheduleSeconds * 20L);
            United.logger().info("Rescheduling new day in " + United.formatter().formatDuration(newScheduleSeconds * 1000), "UnitedLands");
        } else {
            if (scheduleTask != null)
                scheduleTask.cancel();
            newDayTask = Bukkit.getScheduler().runTaskLater(UnitedLands.instance(), new NewDayTask(), secondsToNewDay * 20L);
            United.logger().info("Scheduling new day in " + United.formatter().formatDuration(secondsToNewDay * 1000), "UnitedLands");
        }

    }

    public void finishNewDay() {
        lastExecutionTime = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskLater(UnitedLands.instance(), () -> {
            scheduleNewDay();
        }, 200L);
    }

    public void stopScheduler() {
        if (scheduleTask != null)
            scheduleTask.cancel();
        if (newDayTask != null)
            newDayTask.cancel();
        United.logger().info("New day scheduler stoped", "UnitedLands");
    }

    public long getSecondsUntilTime(String time) {
        LocalTime targetTime;
        try {
            targetTime = LocalTime.parse(time); // expects "HH:mm" (24h) by default
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Time must be in \"HH:mm\" format, e.g. \"06:30\"", e);
        }

        LocalDateTime now = LocalDateTime.now(); // or LocalDateTime.now(ZoneId.of("Europe/Berlin"))
        LocalDateTime target = now.withHour(targetTime.getHour()).withMinute(targetTime.getMinute()).withSecond(0).withNano(0);

        if (!target.isAfter(now)) {
            target = target.plusDays(1);
        }

        return Duration.between(now, target).getSeconds();
    }

}
