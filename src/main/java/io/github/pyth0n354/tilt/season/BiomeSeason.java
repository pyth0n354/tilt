package io.github.pyth0n354.tilt.season;

import io.github.pyth0n354.tilt.Season;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Which biomes follow the year, and which are locked to one season.
 *
 * Only temperate biomes cycle. Everything else keeps the season it already looks like: cherry
 * grove is eternally spring, dappled forest eternally autumn, snowy biomes eternally winter,
 * desert and jungle eternally summer.
 *
 * This removes the ugliest failure mode in the category, snow in the desert and bare jungles,
 * because those biomes never participate. It also cuts the work from every biome in the game to
 * the handful that are temperate.
 *
 * Membership is driven by tags rather than hardcoded biome ids, so modded biomes that carry the
 * usual vanilla tags are classified correctly with no code, and a pack can correct any that are
 * not by shipping a tag file.
 */
public enum BiomeSeason {

    /** Follows the year. */
    CYCLES(null),
    ETERNAL_SPRING(Season.SPRING),
    ETERNAL_SUMMER(Season.SUMMER),
    ETERNAL_AUTUMN(Season.AUTUMN),
    ETERNAL_WINTER(Season.WINTER);

    private final Season locked;

    BiomeSeason(Season locked) {
        this.locked = locked;
    }

    public boolean cycles() {
        return locked == null;
    }

    /** The season this class shows, given what the year is currently doing. */
    public Season resolve(Season yearSeason) {
        return locked == null ? yearSeason : locked;
    }

    private static TagKey<Biome> tag(String name) {
        return TagKey.create(net.minecraft.core.registries.Registries.BIOME,
                Identifier.fromNamespaceAndPath("tilt", name));
    }

    public static final TagKey<Biome> ETERNAL_SPRING_TAG = tag("eternal_spring");
    public static final TagKey<Biome> ETERNAL_SUMMER_TAG = tag("eternal_summer");
    public static final TagKey<Biome> ETERNAL_AUTUMN_TAG = tag("eternal_autumn");
    public static final TagKey<Biome> ETERNAL_WINTER_TAG = tag("eternal_winter");

    /**
     * Classifies a biome.
     *
     * Checked most specific first. A biome in more than one tag takes the first match rather than
     * failing, since a pack author combining tag files should get a usable result rather than a
     * crash.
     */
    public static BiomeSeason of(Holder<Biome> biome) {
        if (biome.is(ETERNAL_WINTER_TAG)) {
            return ETERNAL_WINTER;
        }
        if (biome.is(ETERNAL_AUTUMN_TAG)) {
            return ETERNAL_AUTUMN;
        }
        if (biome.is(ETERNAL_SPRING_TAG)) {
            return ETERNAL_SPRING;
        }
        if (biome.is(ETERNAL_SUMMER_TAG)) {
            return ETERNAL_SUMMER;
        }
        return CYCLES;
    }
}
