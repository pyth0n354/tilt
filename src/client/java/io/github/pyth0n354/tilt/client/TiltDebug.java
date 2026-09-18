package io.github.pyth0n354.tilt.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Diagnostic for the spruce and birch problem.
 *
 * Reports which tint sources a block actually has, and what colour each returns. If spruce and
 * birch report no sources, their colour lives in the texture rather than in a tint, and
 * BlockColorsMixin can never affect them.
 */
public final class TiltDebug {

    private TiltDebug() {
    }

    public static int dump(String blockId) {
        Minecraft client = Minecraft.getInstance();
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
        BlockState state = block.defaultBlockState();
        List<BlockTintSource> sources = client.getBlockColors().getTintSources(state);

        say(client, "§e" + blockId + "§r: " + sources.size() + " tint source(s)");
        for (int i = 0; i < sources.size(); i++) {
            BlockTintSource s = sources.get(i);
            int colour = s.color(state);
            say(client, "  [" + i + "] " + s.getClass().getSimpleName()
                    + " -> #" + String.format("%06X", colour & 0xFFFFFF));
        }
        if (sources.isEmpty()) {
            say(client, "  §cno tint sources: colour comes from the texture, cannot be hooked");
        }
        return sources.size();
    }

    public static String arg() {
        return "block";
    }

    public static com.mojang.brigadier.arguments.ArgumentType<String> type() {
        return StringArgumentType.greedyString();
    }

    private static void say(Minecraft client, String msg) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(msg));
        }
    }
}
