package io.github.pyth0n354.tilt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every case here is a bug that actually shipped during the 0.0.1 spike and took a full game
 * launch to find.
 */
class ColourBlendTest {

    // Vanilla plains values, taken from /tiltdebug in game.
    private static final int PLAINS_GRASS = 0x91BD59;
    private static final int PLAINS_FOLIAGE = 0x48B518;

    // Dappled Forest, Mojang's own autumn, added in 26.3.
    private static final int AUTUMN_FOLIAGE = 0xE68E30;
    private static final int AUTUMN_LITTER = 0x8C3A04;
    private static final int AUTUMN_GRASS = 0xB79A4E;

    // What autumn must not be mistaken for.
    private static final int DESERT_FOLIAGE = 0xAEA42A;

    private static int red(int c) {
        return (c >> 16) & 0xFF;
    }

    private static int green(int c) {
        return (c >> 8) & 0xFF;
    }

    private static int blue(int c) {
        return c & 0xFF;
    }

    private static int distance(int a, int b) {
        return Math.abs(red(a) - red(b)) + Math.abs(green(a) - green(b)) + Math.abs(blue(a) - blue(b));
    }

    @Test
    @DisplayName("alpha bits survive the blend, or leaves render invisible")
    void preservesAlpha() {
        int result = ColourBlend.apply(0xFF000000 | PLAINS_FOLIAGE, AUTUMN_FOLIAGE, 0.85f, 0.0f);
        assertEquals(0xFF000000, result & 0xFF000000,
                "alpha was dropped, which renders the block fully transparent");
    }

    @Test
    @DisplayName("no alpha in, no alpha out")
    void leavesMissingAlphaAlone() {
        int result = ColourBlend.apply(PLAINS_FOLIAGE, AUTUMN_FOLIAGE, 0.85f, 0.0f);
        assertEquals(0, result & 0xFF000000);
    }

    @Test
    @DisplayName("summer is a pass through, so vanilla colours are untouched")
    void zeroStrengthIsIdentity() {
        assertEquals(PLAINS_GRASS, ColourBlend.apply(PLAINS_GRASS, 0x000000, 0.0f, 0.0f));
        assertEquals(PLAINS_FOLIAGE, ColourBlend.apply(PLAINS_FOLIAGE, 0xFFFFFF, 0.0f, 0.0f));
    }

    @Test
    @DisplayName("autumn leaves are not vanilla's desert foliage colour")
    void autumnIsNotDesert() {
        int autumn = ColourBlend.apply(PLAINS_FOLIAGE, AUTUMN_FOLIAGE, 0.85f, 0.0f);

        // Summed channel distance is the wrong measure here. Autumn and desert share almost
        // identical green and blue; what separates amber from olive is red. The original RGB
        // interpolation bug produced roughly #9FA025, whose red sat *below* desert's.
        assertTrue(red(autumn) - red(DESERT_FOLIAGE) > 30,
                "autumn foliage resolved to " + hex(autumn) + " with red " + red(autumn)
                        + ", against desert " + hex(DESERT_FOLIAGE) + " with red "
                        + red(DESERT_FOLIAGE) + ". Too little red reads as arid, not autumnal. "
                        + "This is the RGB interpolation bug returning.");
        assertTrue(red(autumn) > green(autumn) + 40,
                "amber needs red clearly ahead of green, got " + hex(autumn));
    }

    @Test
    @DisplayName("the old RGB interpolation would fail the desert check")
    void rgbInterpolationWouldStillBeCaught() {
        // What channel by channel interpolation produced at the strength originally shipped.
        int naive = 0x9FA025;
        assertTrue(red(naive) - red(DESERT_FOLIAGE) <= 30,
                "this regression guard no longer reproduces the original bug, so it is not "
                        + "actually testing anything");
    }

    @Test
    @DisplayName("autumn leaves end up warmer than they started")
    void autumnShiftsWarm() {
        int autumn = ColourBlend.apply(PLAINS_FOLIAGE, AUTUMN_FOLIAGE, 0.85f, 0.0f);
        assertTrue(red(autumn) > red(PLAINS_FOLIAGE), "red should rise");
        assertTrue(blue(autumn) < green(autumn), "blue should stay below green for a warm tone");
    }

    @Test
    @DisplayName("grass and leaves stay distinct, or autumn reads as savanna")
    void grassAndFoliageDiverge() {
        int grass = ColourBlend.apply(PLAINS_GRASS, AUTUMN_GRASS, 0.85f, 0.0f);
        int foliage = ColourBlend.apply(PLAINS_FOLIAGE, AUTUMN_FOLIAGE, 0.85f, 0.0f);
        assertTrue(distance(grass, foliage) > 40,
                "grass " + hex(grass) + " and foliage " + hex(foliage)
                        + " converged. Straw under straw is savanna, not autumn.");
    }

    @Test
    @DisplayName("litter is darker than the grass it lies on")
    void litterIsDarkerThanGrass() {
        int grass = ColourBlend.apply(PLAINS_GRASS, AUTUMN_GRASS, 0.85f, 0.0f);
        int litter = ColourBlend.apply(0x9E9E70, AUTUMN_LITTER, 0.85f, 0.0f);
        assertTrue(red(litter) + green(litter) + blue(litter) < red(grass) + green(grass) + blue(grass),
                "litter " + hex(litter) + " should read darker than grass " + hex(grass));
    }

    @Test
    @DisplayName("frost moves toward white and full frost is white")
    void frostBlendsToWhite() {
        int none = ColourBlend.apply(PLAINS_FOLIAGE, PLAINS_FOLIAGE, 0.0f, 0.0f);
        int some = ColourBlend.apply(PLAINS_FOLIAGE, PLAINS_FOLIAGE, 0.0f, 0.45f);
        assertTrue(red(some) > red(none) && green(some) > green(none) && blue(some) > blue(none),
                "every channel should rise toward white");
        assertEquals(0xFFFFFF, ColourBlend.apply(PLAINS_FOLIAGE, PLAINS_FOLIAGE, 0.0f, 1.0f) & 0xFFFFFF);
    }

    @Test
    @DisplayName("frost is quantised to Bedrock's eight steps")
    void frostIsStepped() {
        int a = ColourBlend.apply(PLAINS_FOLIAGE, PLAINS_FOLIAGE, 0.0f, 0.50f);
        int b = ColourBlend.apply(PLAINS_FOLIAGE, PLAINS_FOLIAGE, 0.0f, 0.51f);
        assertEquals(a, b, "values inside one of the eight steps must produce the same colour");
    }

    @Test
    @DisplayName("full strength lands on the target hue family")
    void fullStrengthReachesTarget() {
        int result = ColourBlend.apply(PLAINS_FOLIAGE, AUTUMN_FOLIAGE, 1.0f, 0.0f);
        assertTrue(distance(result, AUTUMN_FOLIAGE) < 12,
                "expected to arrive near " + hex(AUTUMN_FOLIAGE) + " but got " + hex(result));
    }

    @Test
    @DisplayName("channels never leave 0 to 255 for any input")
    void neverOutOfRange() {
        int[] colours = { 0x000000, 0xFFFFFF, PLAINS_GRASS, PLAINS_FOLIAGE, 0xFF0000, 0x00FF00, 0x0000FF };
        for (int from : colours) {
            for (int to : colours) {
                for (float s = 0.0f; s <= 1.0f; s += 0.1f) {
                    for (float f = 0.0f; f <= 1.0f; f += 0.25f) {
                        int out = ColourBlend.apply(from, to, s, f) & 0xFFFFFF;
                        assertTrue(red(out) <= 255 && green(out) <= 255 && blue(out) <= 255);
                        assertTrue(red(out) >= 0 && green(out) >= 0 && blue(out) >= 0);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("greyscale input has no hue to rotate and must not produce colour noise")
    void greyscaleIsStable() {
        int grey = 0x808080;
        int result = ColourBlend.apply(grey, 0x909090, 0.5f, 0.0f);
        assertTrue(Math.abs(red(result) - green(result)) <= 2
                        && Math.abs(green(result) - blue(result)) <= 2,
                "grey blended toward grey should stay grey, got " + hex(result));
    }

    @Test
    @DisplayName("hue takes the short way round rather than through the whole wheel")
    void hueWrapsShortWay() {
        int red = 0xFF0000;
        int magenta = 0xFF00FF;
        int mid = ColourBlend.hueBlend(red, magenta, 0.5f);
        assertTrue(blue(mid) > 60, "expected to pass through pink, got " + hex(mid));
        assertNotEquals(0, green(mid) > 100 ? 0 : 1,
                "green rising would mean it went the long way through yellow and cyan");
    }

    private static String hex(int c) {
        return String.format("#%06X", c & 0xFFFFFF);
    }
}
