package io.github.pyth0n354.tilt.client;

import io.github.pyth0n354.tilt.Season;
import io.github.pyth0n354.tilt.SeasonClock;
import io.github.pyth0n354.tilt.season.BiomeSeason;
import io.github.pyth0n354.tilt.season.SeasonSync;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * The client's view of the season, as last told by the server.
 *
 * Volatile throughout because Sodium resolves block colours on its chunk build threads, so these
 * are read from several threads while only ever written from the network thread.
 */
public final class ClientSeasons {

    private static volatile long day;
    private static volatile int daysPerSeason = 24;
    private static volatile Season startingSeason = Season.SPRING;

    private ClientSeasons() {
    }

    public static void accept(SeasonSync packet) {
        day = packet.day();
        daysPerSeason = Math.max(1, packet.daysPerSeason());
        Season[] all = Season.values();
        startingSeason = all[Math.floorMod(packet.startingSeason(), all.length)];
    }

    /** The season the year is in, ignoring where the player is standing. */
    public static Season yearSeason() {
        return SeasonClock.seasonFor(day, daysPerSeason, startingSeason);
    }

    /** How far through the current season the year is, 0 to 1. */
    public static float progress() {
        return SeasonClock.progress(day, daysPerSeason);
    }

    public static long day() {
        return day;
    }

    public static int daysPerSeason() {
        return daysPerSeason;
    }

    /**
     * The season at a position, taking the biome's own class into account.
     *
     * Cherry grove reports spring in midwinter, because that is what it looks like all year.
     */
    public static Season at(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return yearSeason();
        }
        return BiomeSeason.of(client.level.getBiome(pos)).resolve(yearSeason());
    }

    public static BiomeSeason biomeClassAt(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return BiomeSeason.CYCLES;
        }
        return BiomeSeason.of(client.level.getBiome(pos));
    }
}
