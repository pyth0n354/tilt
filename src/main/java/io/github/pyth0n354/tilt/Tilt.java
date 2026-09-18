package io.github.pyth0n354.tilt;

import io.github.pyth0n354.tilt.config.TiltConfig;
import io.github.pyth0n354.tilt.season.SeasonSync;
import io.github.pyth0n354.tilt.season.ServerSeasons;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class Tilt implements ModInitializer {
    public static final String MOD_ID = "tilt";

    @Override
    public void onInitialize() {
        // Load up front so the colour mixins never hit the filesystem on a chunk build thread.
        TiltConfig.init();
        PayloadTypeRegistry.clientboundPlay().register(SeasonSync.ID, SeasonSync.CODEC);
        ServerSeasons.register();
        // Spike: no server-side behaviour yet. Season is client-local until 0.1 makes the
        // server authoritative.
    }
}
