package io.github.pyth0n354.tilt.client;

import io.github.pyth0n354.tilt.Season;
import io.github.pyth0n354.tilt.SeasonClock;
import io.github.pyth0n354.tilt.season.BiomeSeason;
import io.github.pyth0n354.tilt.season.SeasonSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TiltClient implements ClientModInitializer {

    /** The season used for rendering. Position aware, so an eternal biome keeps its own season. */
    public static Season current() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return ClientSeasons.yearSeason();
        }
        return ClientSeasons.at(client.player.blockPosition());
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SeasonSync.ID,
                (payload, context) -> context.client().execute(() -> ClientSeasons.accept(payload)));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) ->
                dispatcher.register(ClientCommands.literal("season")
                        .executes(c -> report(c.getSource().getClient()))));
    }

    private static int report(Minecraft client) {
        if (client.player == null) {
            return 0;
        }
        BiomeSeason biomeClass = ClientSeasons.biomeClassAt(client.player.blockPosition());
        Season here = ClientSeasons.at(client.player.blockPosition());
        Season year = ClientSeasons.yearSeason();
        int dayInSeason = (int) Math.floorMod(ClientSeasons.day(), ClientSeasons.daysPerSeason()) + 1;

        say(client, Component.literal("Season here: ")
                .append(Component.literal(name(here)).withStyle(ChatFormatting.GOLD)));

        if (biomeClass.cycles()) {
            say(client, Component.literal("  day " + dayInSeason + " of "
                    + ClientSeasons.daysPerSeason() + ", "
                    + Math.round(ClientSeasons.progress() * 100) + "% through")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            say(client, Component.literal("  this biome is always " + name(here)
                    + ", while the year is in " + name(year))
                    .withStyle(ChatFormatting.GRAY));
        }
        say(client, Component.literal("  day " + ClientSeasons.day() + ", year "
                + (Math.floorDiv(ClientSeasons.day(),
                        SeasonClock.yearLength(ClientSeasons.daysPerSeason())) + 1))
                .withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static String name(Season season) {
        String s = season.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void say(Minecraft client, Component message) {
        if (client.player != null) {
            client.player.sendSystemMessage(message);
        }
    }
}
