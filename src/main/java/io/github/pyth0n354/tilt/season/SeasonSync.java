package io.github.pyth0n354.tilt.season;

import io.github.pyth0n354.tilt.Tilt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Tells the client what the server thinks the season is.
 *
 * The season is derived from world time, so in principle a client could work it out alone. It is
 * sent anyway because the server owns the config: season length and starting season live there,
 * and a client guessing with different settings would render a different season to the one the
 * world is actually in.
 *
 * Sending the day rather than the season keeps the door open for gradual transitions, which need
 * progress within the season, not just which one it is.
 */
public record SeasonSync(long day, int daysPerSeason, int startingSeason)
        implements CustomPacketPayload {

    public static final Type<SeasonSync> ID =
            new Type<>(Identifier.fromNamespaceAndPath(Tilt.MOD_ID, "season"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeasonSync> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, SeasonSync::day,
                    ByteBufCodecs.VAR_INT, SeasonSync::daysPerSeason,
                    ByteBufCodecs.VAR_INT, SeasonSync::startingSeason,
                    SeasonSync::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
