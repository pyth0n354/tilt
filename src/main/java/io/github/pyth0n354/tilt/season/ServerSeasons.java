package io.github.pyth0n354.tilt.season;

import io.github.pyth0n354.tilt.Season;
import io.github.pyth0n354.tilt.SeasonClock;
import io.github.pyth0n354.tilt.config.TiltConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

/**
 * Owns the season on the server and keeps clients in step.
 *
 * Nothing is stored in the world. The season is a function of world time, so a world carries its
 * season with it and needs no migration.
 */
public final class ServerSeasons {

    /** Last day broadcast, so a packet only goes out when the day actually rolls over. */
    private static long lastBroadcastDay = Long.MIN_VALUE;

    private ServerSeasons() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendTo(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel overworld = server.overworld();
            if (overworld == null) {
                return;
            }
            long day = SeasonClock.dayOf(overworld.getOverworldClockTime());
            if (day == lastBroadcastDay) {
                return;
            }
            lastBroadcastDay = day;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendTo(player);
            }
        });
    }

    /** The season the year is currently in, ignoring where anyone is standing. */
    public static Season yearSeason(ServerLevel level) {
        TiltConfig cfg = TiltConfig.get();
        return SeasonClock.seasonFor(SeasonClock.dayOf(level.getOverworldClockTime()),
                cfg.daysPerSeason, cfg.startingSeason());
    }

    private static void sendTo(ServerPlayer player) {
        TiltConfig cfg = TiltConfig.get();
        ServerPlayNetworking.send(player, new SeasonSync(
                SeasonClock.dayOf(player.level().getOverworldClockTime()),
                cfg.daysPerSeason,
                cfg.startingSeason().ordinal()));
    }
}
