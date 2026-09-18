package io.github.pyth0n354.tilt.config;

/**
 * Tunable appearance of a single season.
 *
 * <p>Grass and foliage carry separate targets on purpose. Applying one tint to both produces
 * yellow-olive ground under yellow-olive leaves, which is the savanna palette — the 0.0.1 spike
 * did exactly that and read as savanna rather than autumn.
 */
public class SeasonPalette {

    /** Colour grass blends toward, as 0xRRGGBB. */
    public int grassTarget;

    /** Colour leaves blend toward, as 0xRRGGBB. */
    public int foliageTarget;

    /**
     * Colour dry foliage blends toward, as 0xRRGGBB. Covers leaf litter and dead bushes, which
     * would otherwise sit on the ground ignoring the season entirely.
     */
    public int dryFoliageTarget;

    /** How far to blend toward the targets, 0..1. 0 leaves vanilla colours untouched. */
    public float strength;

    /** How far to blend toward white afterwards, 0..1. Bedrock's snow-frosting effect. */
    public float frost;

    public SeasonPalette() {
    }

    public SeasonPalette(int grassTarget, int foliageTarget, int dryFoliageTarget,
                        float strength, float frost) {
        this.grassTarget = grassTarget;
        this.foliageTarget = foliageTarget;
        this.dryFoliageTarget = dryFoliageTarget;
        this.strength = strength;
        this.frost = frost;
    }
}
