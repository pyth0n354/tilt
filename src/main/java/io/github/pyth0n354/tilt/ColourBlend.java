package io.github.pyth0n354.tilt;

/**
 * The seasonal colour maths, with no dependency on config, Minecraft or any mod loader.
 *
 * Kept separate from {@link Season} so it can be unit tested directly. Every colour bug found
 * during the 0.0.1 spike lived in here and cost a full game launch to spot: alpha being dropped
 * so leaves rendered invisible, and autumn landing on vanilla's desert foliage colour.
 */
public final class ColourBlend {

    /** Bedrock blends frost in eight discrete steps; matching that keeps the look familiar. */
    public static final int FROST_STEPS = 8;

    private ColourBlend() {
    }

    /**
     * Blends {@code packed} toward {@code target} by {@code strength}, then toward white by
     * {@code frost}.
     *
     * The top 8 bits are preserved verbatim. Vanilla's biome colour methods return 0xRRGGBB with
     * no alpha, and Sodium ORs in 0xFF000000 itself, but discarding those bits sets alpha to zero
     * wherever they are populated, which renders the block fully transparent.
     */
    public static int apply(int packed, int target, float strength, float frost) {
        if (strength <= 0.0f && frost <= 0.0f) {
            return packed;
        }
        int high = packed & 0xFF000000;
        int blended = strength > 0.0f ? hueBlend(packed, target, strength) : packed;

        int red = (blended >> 16) & 0xFF;
        int green = (blended >> 8) & 0xFF;
        int blue = blended & 0xFF;

        if (frost > 0.0f) {
            float f = Math.round(Math.min(frost, 1.0f) * FROST_STEPS) / (float) FROST_STEPS;
            red = lerp(red, 255, f);
            green = lerp(green, 255, f);
            blue = lerp(blue, 255, f);
        }
        return high | (red << 16) | (green << 8) | blue;
    }

    /**
     * Blends two colours through HSV, rotating hue the short way round.
     *
     * Interpolating green toward orange channel by channel in RGB passes through a desaturated
     * yellow. At the strength originally used that landed on almost exactly vanilla's desert
     * foliage colour, so autumn forests rendered as though they were arid.
     *
     * Written without allocating, since this runs on Sodium's chunk build threads.
     */
    public static int hueBlend(int from, int to, float t) {
        float ar = ((from >> 16) & 0xFF) / 255.0f;
        float ag = ((from >> 8) & 0xFF) / 255.0f;
        float ab = (from & 0xFF) / 255.0f;
        float amax = Math.max(ar, Math.max(ag, ab));
        float ad = amax - Math.min(ar, Math.min(ag, ab));
        float ah = hue(ar, ag, ab, amax, ad);
        float as = amax == 0.0f ? 0.0f : ad / amax;

        float br = ((to >> 16) & 0xFF) / 255.0f;
        float bg = ((to >> 8) & 0xFF) / 255.0f;
        float bb = (to & 0xFF) / 255.0f;
        float bmax = Math.max(br, Math.max(bg, bb));
        float bd = bmax - Math.min(br, Math.min(bg, bb));
        float bh = hue(br, bg, bb, bmax, bd);
        float bs = bmax == 0.0f ? 0.0f : bd / bmax;

        float dh = bh - ah;
        if (dh > 0.5f) {
            dh -= 1.0f;
        } else if (dh < -0.5f) {
            dh += 1.0f;
        }
        return fromHsv((ah + dh * t + 1.0f) % 1.0f, as + (bs - as) * t, amax + (bmax - amax) * t);
    }

    private static float hue(float r, float g, float b, float max, float d) {
        if (d <= 0.0f) {
            return 0.0f;
        }
        float h;
        if (max == r) {
            h = ((g - b) / d) / 6.0f;
        } else if (max == g) {
            h = (2.0f + (b - r) / d) / 6.0f;
        } else {
            h = (4.0f + (r - g) / d) / 6.0f;
        }
        return h < 0.0f ? h + 1.0f : h;
    }

    private static int fromHsv(float h, float s, float v) {
        int i = (int) Math.floor(h * 6.0f);
        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);
        float r;
        float g;
        float b;
        switch (Math.floorMod(i, 6)) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (clamp(Math.round(r * 255)) << 16)
             | (clamp(Math.round(g * 255)) << 8)
             | clamp(Math.round(b * 255));
    }

    private static int lerp(int from, int to, float amount) {
        return clamp(Math.round(from + (to - from) * amount));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }
}
