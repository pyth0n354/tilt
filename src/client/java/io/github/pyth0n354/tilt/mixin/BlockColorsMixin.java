package io.github.pyth0n354.tilt.mixin;

import io.github.pyth0n354.tilt.client.TiltClient;
import io.github.pyth0n354.tilt.config.TiltConfig;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Brings spruce and birch leaves into the seasonal tint.
 *
 * Oak, jungle, acacia, dark oak and mangrove leaves resolve through BiomeColors, so
 * BiomeColorsMixin already covers them. Spruce and birch instead get a lambda from
 * BlockTintSources.constant(int) holding a fixed value, #619961 and #80A755, which ignores the
 * biome entirely. Reaching them needs this second hook.
 *
 * Both overloads are wrapped. getTintSources returns the whole list and getTintSource indexes
 * into it, and different call sites use different ones, so hooking only the indexed form left
 * spruce and birch untinted in practice.
 *
 * Cherry, azalea, pale oak and poplar are deliberately untouched, since their colour lives in the
 * texture. That matches Bedrock, which excludes the same set from its snow frosting, and leaves
 * builders a set of leaves that never change with the season.
 */
@Mixin(BlockColors.class)
public class BlockColorsMixin {

    @Inject(method = "getTintSources", at = @At("RETURN"), cancellable = true)
    private void tilt$wrapAll(BlockState state, CallbackInfoReturnable<List<BlockTintSource>> cir) {
        if (!tilt$applies(state)) {
            return;
        }
        List<BlockTintSource> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        List<BlockTintSource> wrapped = new ArrayList<>(original.size());
        for (BlockTintSource s : original) {
            wrapped.add(new SeasonalTint(s));
        }
        cir.setReturnValue(List.copyOf(wrapped));
    }

    @Inject(method = "getTintSource", at = @At("RETURN"), cancellable = true)
    private void tilt$wrapOne(BlockState state, int index,
                              CallbackInfoReturnable<BlockTintSource> cir) {
        if (!tilt$applies(state)) {
            return;
        }
        BlockTintSource original = cir.getReturnValue();
        if (original != null && !(original instanceof SeasonalTint)) {
            cir.setReturnValue(new SeasonalTint(original));
        }
    }

    private static boolean tilt$applies(BlockState state) {
        TiltConfig cfg = TiltConfig.get();
        if (!cfg.tintEnabled || !cfg.tintFixedColourLeaves) {
            return false;
        }
        return state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.BIRCH_LEAVES);
    }

    /** Wraps a fixed colour source so it follows the season. */
    private record SeasonalTint(BlockTintSource delegate) implements BlockTintSource {

        @Override
        public int color(BlockState state) {
            return TiltClient.current().tintFoliage(delegate.color(state));
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return TiltClient.current().tintFoliage(delegate.colorInWorld(state, level, pos));
        }

        @Override
        public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return TiltClient.current().tintFoliage(delegate.colorAsTerrainParticle(state, level, pos));
        }
    }
}
