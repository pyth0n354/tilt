package io.github.pyth0n354.tilt;

/**
 * Turns world time into a season.
 *
 * Pure arithmetic with no dependency on config, Minecraft or the loader, so it is unit testable.
 * Date maths fails quietly and late, typically at a year boundary long after anyone is watching,
 * which is exactly the kind of bug worth pinning down with tests rather than play testing.
 *
 * The season is derived rather than stored. Nothing needs saving to the world, and a world moved
 * between installs stays consistent as long as the season length matches.
 */
public final class SeasonClock {

    public static final int TICKS_PER_DAY = 24000;

    private SeasonClock() {
    }

    /**
     * Whole days elapsed, from a raw clock value.
     *
     * In 26.x this comes from {@code level.getOverworldClockTime()}. The older
     * {@code getDayTime()} no longer exists, replaced by the WorldClock system.
     */
    public static long dayOf(long dayTime) {
        return Math.floorDiv(dayTime, TICKS_PER_DAY);
    }

    /**
     * The season on a given day.
     *
     * @param day            whole days elapsed, may be negative if the world time was set backwards
     * @param daysPerSeason  length of one season, clamped to at least 1
     * @param startingSeason season on day 0
     */
    public static Season seasonFor(long day, int daysPerSeason, Season startingSeason) {
        int length = Math.max(1, daysPerSeason);
        long index = Math.floorDiv(day, length) + startingSeason.ordinal();
        return Season.values()[(int) Math.floorMod(index, Season.values().length)];
    }

    /**
     * How far through the current season a given day is, from 0 up to but not including 1.
     *
     * Used for gradual transitions, so that colour can drift across the season rather than
     * snapping on the day it changes.
     */
    public static float progress(long day, int daysPerSeason) {
        int length = Math.max(1, daysPerSeason);
        return Math.floorMod(day, length) / (float) length;
    }

    /**
     * Position in the year as a continuous value from 0 up to but not including 4, where the whole
     * part is the season and the fraction is the progress through it.
     */
    public static float yearPosition(long day, int daysPerSeason, Season startingSeason) {
        return seasonFor(day, daysPerSeason, startingSeason).ordinal() + progress(day, daysPerSeason);
    }

    /** Length of a full year in days. */
    public static int yearLength(int daysPerSeason) {
        return Math.max(1, daysPerSeason) * Season.values().length;
    }
}
