package io.github.pyth0n354.tilt;

import io.github.pyth0n354.tilt.config.SeasonPalette;
import io.github.pyth0n354.tilt.config.TiltConfig;

/**
 * The four seasons, and how each recolours grass and foliage.
 *
 * <p>Each season blends the vanilla biome colour <em>toward</em> a target rather than replacing
 * it. Blending preserves biome identity — a swamp in autumn still reads as a swamp — which a flat
 * palette swap would erase.
 *
 * <p><b>Grass and foliage have separate targets.</b> Applying one tint to both produces
 * yellow-olive ground plus yellow-olive leaves, which is precisely the savanna palette; the 0.0.1
 * spike did exactly that and looked like savanna rather than autumn. Autumn needs straw-brown
 * ground under orange leaves.
 *
 * <p>Autumn's targets are Mojang's own, taken from the Dappled Forest biome added in 26.3
 * (grass {@code #df6827}, foliage {@code #e68e30}) — so autumn looks like the game's own autumn
 * rather than an invented palette.
 *
 * <p>Winter additionally blends toward white, mirroring Bedrock Edition where "all biome-tinted
 * leaves gradually fade to white once snowfall begins", in eight steps. Java has no equivalent.
 */
public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    /**
     * Per thread memo of recent blends, as interleaved key and value pairs.
     *
     * The blend runs once per block colour lookup, outside Sodium's own colour cache, and Sodium
     * resolves colours on its chunk build threads. On a machine with few worker threads the HSV
     * conversion was enough to make mesh builds fall behind whenever the camera moved, and the
     * transparent layer was what went missing. Adjacent blocks almost always share a colour, so a
     * tiny direct mapped cache removes nearly all of the work.
     *
     * Thread local rather than shared, so there are no races and no synchronisation on the hot
     * path. Cleared wholesale when the config changes.
     */
    private static final int MEMO_SLOTS = 256;
    private static final ThreadLocal<int[]> MEMO =
            ThreadLocal.withInitial(() -> new int[MEMO_SLOTS * 3]);
    private static volatile int memoGeneration;

    /** Invalidates every memo. Call when the palette changes. */
    public static void clearCaches() {
        memoGeneration++;
    }

    /** Bedrock blends frost in eight discrete steps; matching that keeps the look familiar. */
    public static final int FROST_STEPS = ColourBlend.FROST_STEPS;

    /** Live palette for this season, read from config so it can be tuned without a rebuild. */
    private SeasonPalette palette() {
        return TiltConfig.get().palette(ordinal());
    }

    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public float frost() {
        return palette().frost;
    }

    /** Recolours a packed grass colour for this season. */
    public int tintGrass(int packed) {
        return memoised(packed, 0);
    }

    /** Recolours a packed foliage (leaf) colour for this season. */
    public int tintFoliage(int packed) {
        return memoised(packed, 1);
    }

    /**
     * Recolours dry foliage, which covers leaf litter and dead bushes. Frost is skipped: litter
     * sits under the snow rather than catching it.
     */
    public int tintDryFoliage(int packed) {
        return memoised(packed, 2);
    }

    /**
     * Blends {@code packed} toward {@code target}, then applies frost.
     *
     * <p>The top 8 bits are preserved verbatim. Vanilla's biome colour methods return 0xRRGGBB
     * with no alpha (Sodium ORs in {@code 0xFF000000} itself), but discarding those bits sets
     * alpha to zero wherever they <em>are</em> populated, rendering the block fully transparent —
     * observed as see-through leaves during the 0.0.1 spike.
     */
    /**
     * Looks the blend up in the per thread memo, computing it only on a miss.
     *
     * The slot holds a tag combining the season, which of the three colour paths this is, and the
     * memo generation, so a stale entry can never be served after the palette changes.
     */
    private int memoised(int packed, int path) {
        int tag = (ordinal() << 28) | (path << 26) | (memoGeneration & 0x03FFFFFF);
        int[] memo = MEMO.get();
        int slot = ((packed * 0x9E3779B1) >>> 24) * 3;

        if (memo[slot] == packed && memo[slot + 1] == tag) {
            return memo[slot + 2];
        }

        SeasonPalette p = palette();
        int result = switch (path) {
            case 1 -> apply(packed, p.foliageTarget, p.strength, p.frost);
            case 2 -> apply(packed, p.dryFoliageTarget, p.strength, 0.0f);
            default -> apply(packed, p.grassTarget, p.strength, p.frost);
        };

        memo[slot] = packed;
        memo[slot + 1] = tag;
        memo[slot + 2] = result;
        return result;
    }
    private int apply(int packed, int target, float strength, float frost) {
        return ColourBlend.apply(packed, target, strength, frost);
    }
}
