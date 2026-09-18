package io.github.pyth0n354.tilt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonClockTest {

    private static final int LENGTH = 24;

    @Test
    @DisplayName("day 0 is the configured starting season")
    void startsOnTheConfiguredSeason() {
        assertSame(Season.SPRING, SeasonClock.seasonFor(0, LENGTH, Season.SPRING));
        assertSame(Season.WINTER, SeasonClock.seasonFor(0, LENGTH, Season.WINTER));
    }

    @Test
    @DisplayName("seasons advance in order and wrap after a full year")
    void advancesAndWraps() {
        assertSame(Season.SPRING, SeasonClock.seasonFor(23, LENGTH, Season.SPRING));
        assertSame(Season.SUMMER, SeasonClock.seasonFor(24, LENGTH, Season.SPRING));
        assertSame(Season.AUTUMN, SeasonClock.seasonFor(48, LENGTH, Season.SPRING));
        assertSame(Season.WINTER, SeasonClock.seasonFor(72, LENGTH, Season.SPRING));
        assertSame(Season.SPRING, SeasonClock.seasonFor(96, LENGTH, Season.SPRING),
                "day 96 begins the second year and must return to spring");
    }

    @Test
    @DisplayName("the year keeps cycling far out, not just once")
    void keepsCyclingOverManyYears() {
        for (int year = 0; year < 50; year++) {
            long day = (long) year * SeasonClock.yearLength(LENGTH);
            assertSame(Season.SPRING, SeasonClock.seasonFor(day, LENGTH, Season.SPRING),
                    "year " + year + " should start in spring");
        }
    }

    @Test
    @DisplayName("negative days do not throw or land on a nonsense season")
    void handlesTimeSetBackwards() {
        // /time set can move world time backwards, and floorDiv must not round toward zero here.
        assertSame(Season.WINTER, SeasonClock.seasonFor(-1, LENGTH, Season.SPRING),
                "the day before day 0 is the last day of the previous year");
        assertSame(Season.AUTUMN, SeasonClock.seasonFor(-25, LENGTH, Season.SPRING));
        assertTrue(SeasonClock.progress(-1, LENGTH) >= 0.0f, "progress must never go negative");
    }

    @Test
    @DisplayName("a season length of zero or less cannot divide by zero")
    void guardsAgainstZeroLength() {
        assertSame(Season.SUMMER, SeasonClock.seasonFor(1, 0, Season.SPRING));
        assertSame(Season.SUMMER, SeasonClock.seasonFor(1, -5, Season.SPRING));
        assertEquals(0.0f, SeasonClock.progress(5, 0));
    }

    @Test
    @DisplayName("progress runs from 0 up to but never reaching 1")
    void progressStaysInRange() {
        for (long day = 0; day < LENGTH * 4L; day++) {
            float p = SeasonClock.progress(day, LENGTH);
            assertTrue(p >= 0.0f && p < 1.0f, "day " + day + " gave progress " + p);
        }
        assertEquals(0.0f, SeasonClock.progress(0, LENGTH));
        assertEquals(0.0f, SeasonClock.progress(LENGTH, LENGTH), "a new season restarts progress");
    }

    @Test
    @DisplayName("year position is continuous and never jumps backwards within a year")
    void yearPositionIsMonotonic() {
        float previous = -1.0f;
        for (long day = 0; day < SeasonClock.yearLength(LENGTH); day++) {
            float pos = SeasonClock.yearPosition(day, LENGTH, Season.SPRING);
            assertTrue(pos > previous, "day " + day + " went backwards, " + previous + " to " + pos);
            assertTrue(pos >= 0.0f && pos < 4.0f, "day " + day + " gave " + pos);
            previous = pos;
        }
    }

    @Test
    @DisplayName("day count comes from world time, and partial days do not advance it")
    void convertsWorldTime() {
        assertEquals(0, SeasonClock.dayOf(0));
        assertEquals(0, SeasonClock.dayOf(23999), "one tick short of dawn is still day 0");
        assertEquals(1, SeasonClock.dayOf(24000));
        assertEquals(100, SeasonClock.dayOf(24000L * 100));
        assertEquals(-1, SeasonClock.dayOf(-1), "any tick before day 0 belongs to day -1");
    }

    @Test
    @DisplayName("a year is four seasons long")
    void yearLength() {
        assertEquals(96, SeasonClock.yearLength(24));
        assertEquals(4, SeasonClock.yearLength(1));
        assertEquals(4, SeasonClock.yearLength(0), "clamped, not zero");
    }
}
