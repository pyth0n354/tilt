package io.github.pyth0n354.tilt.mixin;

import io.github.pyth0n354.tilt.client.TiltClient;
import io.github.pyth0n354.tilt.config.TiltConfig;
import net.minecraft.client.renderer.BiomeColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the season tint to biome-derived grass and foliage colours.
 *
 * <p>These two public statics are the shared entry point for both vanilla and Sodium — Sodium's
 * {@code DefaultColorProviders} calls {@code BiomeColors.getAverageGrassColor(slice, pos)}
 * directly. Hooking here therefore covers both renderers with one mixin.
 *
 * <p>Deliberately <em>not</em> hooking the {@code ColorResolver} lambdas: Fabric Seasons injected
 * into {@code method_23791}, a synthetic lambda whose name shifts whenever the class is
 * recompiled. These method names are stable API.
 */
@Mixin(BiomeColors.class)
public class BiomeColorsMixin {

    @Inject(method = "getAverageGrassColor", at = @At("RETURN"), cancellable = true)
    private static void tilt$tintGrass(CallbackInfoReturnable<Integer> cir) {
        if (!TiltConfig.get().tintEnabled) {
            return;
        }
        cir.setReturnValue(TiltClient.current().tintGrass(cir.getReturnValue()));
    }

    @Inject(method = "getAverageDryFoliageColor", at = @At("RETURN"), cancellable = true)
    private static void tilt$tintDryFoliage(CallbackInfoReturnable<Integer> cir) {
        if (!TiltConfig.get().tintEnabled) {
            return;
        }
        cir.setReturnValue(TiltClient.current().tintDryFoliage(cir.getReturnValue()));
    }

    @Inject(method = "getAverageFoliageColor", at = @At("RETURN"), cancellable = true)
    private static void tilt$tintFoliage(CallbackInfoReturnable<Integer> cir) {
        if (!TiltConfig.get().tintEnabled) {
            return;
        }
        cir.setReturnValue(TiltClient.current().tintFoliage(cir.getReturnValue()));
    }
}
