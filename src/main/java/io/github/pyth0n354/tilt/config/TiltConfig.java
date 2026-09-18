package io.github.pyth0n354.tilt.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Tilt's settings, stored as plain JSON.
 *
 * <p>Deliberately independent of any config UI library. The screen in {@code TiltConfigScreen} is
 * a thin skin over this, so the library is swappable and the config still works on a dedicated
 * server where no GUI exists.
 *
 * <p>Config depth is not a nicety here: across 1,936 open feature requests on the most-followed
 * Fabric mods, "config / customisation options" was the single most-requested theme (192 issues).
 * Every abandoned seasons mod shipped without meaningful configuration.
 */
public class TiltConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("tilt");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("tilt.json");

    /**
     * Volatile because Sodium fills its biome colour cache on worker threads, so the colour
     * mixins read this from several threads at once. The lazy check below was previously racy:
     * concurrent readers could each begin a load, perform file I/O on a hot path, and observe a
     * partially constructed object. That showed up as rendering corruption under Sodium only,
     * since vanilla builds chunks with far less concurrency.
     */
    private static volatile TiltConfig instance;

    /**
     * Schema version. Bumped whenever fields are added or defaults change meaningfully.
     *
     * Gson rebuilds nested objects through their no-arg constructor, so any field absent from an
     * older file comes back as zero rather than as the initializer value. That is silent and
     * looks like a rendering bug: after dryFoliageTarget was added, existing configs blended leaf
     * litter toward black, and stale strength values left autumn stuck on the old washed out
     * palette. When this does not match CURRENT, the file is backed up and defaults are restored.
     */
    public int configVersion = CURRENT_VERSION;

    private static final int CURRENT_VERSION = 2;

    // --- season length -------------------------------------------------------

    /** In-game days per season. Four seasons, so a year is four times this. */
    public int daysPerSeason = 24;

    // --- appearance ----------------------------------------------------------

    /** Recolour grass and foliage with the season. */
    public boolean tintEnabled = true;

    /**
     * Autumn's leaf and litter colours are Mojang's own, from the Dappled Forest biome added in
     * 26.3 (foliage #e68e30, dry foliage #8c3a04).
     *
     * Grass deliberately does not use that biome's #df6827. Dappled Forest is permanently
     * autumnal and stylised to match, but real grass does not turn orange in autumn, it dries to
     * straw. Using the biome's own grass colour made temperate meadows look like a fantasy biome.
     * What reads as autumn is the combination: straw ground under amber leaves. Straw under straw
     * is savanna.
     */
    public SeasonPalette spring = new SeasonPalette(0x8FD44B, 0x7FC94A, 0x7A6A38, 0.35f, 0.00f);
    public SeasonPalette summer = new SeasonPalette(0x000000, 0x000000, 0x000000, 0.00f, 0.00f);
    public SeasonPalette autumn = new SeasonPalette(0xB79A4E, 0xE68E30, 0x8C3A04, 0.85f, 0.00f);
    public SeasonPalette winter = new SeasonPalette(0x9A9480, 0x9AA89A, 0x6B5236, 0.50f, 0.45f);

    /**
     * Include spruce and birch in the seasonal tint.
     *
     * <p>Vanilla gives these two fixed colours (#619961 and #80a755) that ignore biome entirely,
     * so they need a separate hook. Turning this off restores that vanilla behaviour and gives
     * builders leaves that never change — alongside cherry, azalea, pale oak and poplar, which are
     * never tinted because their colour lives in the texture.
     */
    public boolean tintFixedColourLeaves = true;

    // --- load / save ---------------------------------------------------------

    public static TiltConfig get() {
        TiltConfig local = instance;
        if (local == null) {
            synchronized (TiltConfig.class) {
                local = instance;
                if (local == null) {
                    local = load();
                    instance = local;
                }
            }
        }
        return local;
    }

    /**
     * Loads the config up front so the colour mixins never trigger file I/O while building
     * chunks. Call once during mod init.
     */
    public static void init() {
        get();
    }

    /**
     * Palette for an ordinal of {@code Season}. Indexed rather than switched on a name because
     * this runs once per block colour lookup, which is about as hot as client code gets.
     */
    public SeasonPalette palette(int ordinal) {
        return switch (ordinal) {
            case 0 -> spring;
            case 2 -> autumn;
            case 3 -> winter;
            default -> summer;
        };
    }

    private static TiltConfig load() {
        if (Files.exists(PATH)) {
            try {
                TiltConfig loaded = GSON.fromJson(Files.readString(PATH), TiltConfig.class);
                if (loaded != null && loaded.configVersion == CURRENT_VERSION) {
                    return loaded;
                }
                if (loaded != null) {
                    Path backup = PATH.resolveSibling("tilt.json.v" + loaded.configVersion + ".bak");
                    Files.move(PATH, backup, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.warn("tilt.json was version {} but this build expects {}. "
                            + "Your old settings were kept at {} and defaults restored.",
                            loaded.configVersion, CURRENT_VERSION, backup.getFileName());
                } else {
                    LOGGER.warn("tilt.json was empty; using defaults");
                }
            } catch (Exception e) {
                // Keep the broken file so the user can recover their values by hand.
                LOGGER.error("Could not read tilt.json, using defaults. The file was left in place.", e);
            }
        }
        TiltConfig fresh = new TiltConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.error("Could not write tilt.json", e);
        }
    }
}
