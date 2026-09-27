package org.unitedlands.unitedlands.utils;

import java.util.Collection;

import org.unitedlands.unitedlands.classes.PlaytimeRecord;

/**
 * Computes an uncapped "activity score" from a player's recent playtime history.
 *
 * The model: each day in the window contributes sqrt(minutes played that day),
 * discounted by an exponential half-life so recent days count more than older
 * ones. The raw sum is then normalized against a reference "ideal" player who
 * logs in every day of the window for IDEAL_MINUTES_PER_DAY minutes, so that
 * player always scores exactly 100. Nothing is capped — an above-ideal player
 * will score above 100.
 *
 * Assumes the incoming collection is already trimmed to the last WINDOW_DAYS
 * days (per the caller's own filtering), and that PlaytimeRecord's logon/logoff
 * timestamps and playtime are all in epoch milliseconds.
 */
public final class ActivityScoreCalculator {

    /** Rolling window size, in days. Must match how the caller trims records. */
    private static final int WINDOW_DAYS = 60;

    /** Half-life of the recency decay, in days. Lower = old sessions fade faster. */
    private static final double HALF_LIFE_DAYS = 20.0;

    /** Minutes/day that define a "100" score for a player who logs in daily. */
    private static final double IDEAL_MINUTES_PER_DAY = 30.0;

    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    // Precomputed once: the ideal player's score never changes, since it doesn't
    // depend on any player's actual data.
    private static final double IDEAL_SCORE = computeIdealScore();

    private ActivityScoreCalculator() {
    }

    /** Convenience overload that uses the current time as "now". */
    public static double calculateActivityScore(Collection<PlaytimeRecord> records) {
        return calculateActivityScore(records, System.currentTimeMillis());
    }

    /**
     * @param records playtime records already limited to the last WINDOW_DAYS days
     * @param now     reference timestamp (epoch millis) that "days ago" is measured against
     * @return the uncapped activity score; 100 == the reference "ideal" daily player
     */
    public static double calculateActivityScore(Collection<PlaytimeRecord> records, long now) {
        double[] minutesPerDay = bucketMinutesByDay(records, now);
        double rawScore = decayedSqrtSum(minutesPerDay);
        return IDEAL_SCORE > 0 ? 100.0 * rawScore / IDEAL_SCORE : 0.0;
    }

    /**
     * Buckets each record's playtime into a "days ago" slot (0 = today's rolling
     * 24h, WINDOW_DAYS-1 = the oldest day in the window). A session that spans a
     * bucket boundary is counted entirely in the bucket containing its logon time
     * — good enough at day granularity, but split it proportionally instead if
     * players regularly play through the boundary and precision matters to you.
     */
    private static double[] bucketMinutesByDay(Collection<PlaytimeRecord> records, long now) {
        double[] minutesPerDay = new double[WINDOW_DAYS];
        for (PlaytimeRecord record : records) {
            long ageMillis = now - record.getLogon();
            int dayIndex = (int) (ageMillis / MILLIS_PER_DAY);
            if (dayIndex < 0 || dayIndex >= WINDOW_DAYS) {
                continue; // outside the window; shouldn't happen if already trimmed
            }
            double minutes = record.getPlaytime() / 60000.0; // adjust divisor if playtime isn't stored in millis
            minutesPerDay[dayIndex] += minutes;
        }
        return minutesPerDay;
    }

    private static double decayedSqrtSum(double[] minutesPerDay) {
        double decay = decayFactor();
        double sum = 0.0;
        for (int day = 0; day < minutesPerDay.length; day++) {
            sum += Math.sqrt(minutesPerDay[day]) * Math.pow(decay, day);
        }
        return sum;
    }

    private static double computeIdealScore() {
        double decay = decayFactor();
        double sqrtIdeal = Math.sqrt(IDEAL_MINUTES_PER_DAY);
        double sum = 0.0;
        for (int day = 0; day < WINDOW_DAYS; day++) {
            sum += sqrtIdeal * Math.pow(decay, day);
        }
        return sum;
    }

    private static double decayFactor() {
        return Math.pow(2.0, -1.0 / HALF_LIFE_DAYS);
    }
}
