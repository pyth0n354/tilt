package io.github.pyth0n354.tilt.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.pyth0n354.tilt.Season;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Spike harness: set the season from a client command and force the world to rebuild, so the
 * colour change is visible immediately instead of waiting for chunks to reload.
 *
 * <p>Throwaway. 0.1 replaces this with a server-authoritative season derived from world time.
 */
public class TiltClient implements ClientModInitializer {

    /** Client-local season. Becomes server-authoritative in 0.1. */
    private static volatile Season current = Season.SUMMER;

    public static Season current() {
        return current;
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) ->
            dispatcher.register(ClientCommands.literal("tiltseason")
                .then(ClientCommands.argument("season", StringArgumentType.word())
                    .executes(c -> {
                        String raw = StringArgumentType.getString(c, "season");
                        Season season;
                        try {
                            season = Season.valueOf(raw.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            c.getSource().sendError(Component.literal(
                                    "Unknown season '" + raw + "' — expected spring, summer, autumn or winter"));
                            return 0;
                        }
                        current = season;
                        c.getSource().sendFeedback(Component.literal(
                                "Season set to " + season + " — press F3+A to reload chunks"));
                        return 1;
                    }))));

        // Diagnostic: /tiltdebug minecraft:spruce_leaves
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) ->
            dispatcher.register(ClientCommands.literal("tiltdebug")
                .then(ClientCommands.argument("block", StringArgumentType.greedyString())
                    .executes(c -> TiltDebug.dump(StringArgumentType.getString(c, "block"))))));
    }
}
