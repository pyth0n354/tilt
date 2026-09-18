package io.github.pyth0n354.tilt.client.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import io.github.pyth0n354.tilt.config.SeasonPalette;
import io.github.pyth0n354.tilt.config.TiltConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

/**
 * The Mod Menu config screen.
 *
 * <p>A thin skin over {@link TiltConfig}; all state lives in the JSON, so this library is
 * swappable and the settings still apply on a dedicated server with no GUI.
 */
public final class TiltConfigScreen {

    private TiltConfigScreen() {
    }

    public static Screen create(Screen parent) {
        TiltConfig cfg = TiltConfig.get();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Tilt"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("General"))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Days per season"))
                                .description(OptionDescription.of(Component.literal(
                                        "In-game days each season lasts. A full year is four times this.")))
                                .binding(24, () -> cfg.daysPerSeason, v -> cfg.daysPerSeason = v)
                                .controller(o -> IntegerSliderControllerBuilder.create(o).range(1, 120).step(1))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Seasonal colours"))
                                .description(OptionDescription.of(Component.literal(
                                        "Recolour grass and leaves with the season.")))
                                .binding(true, () -> cfg.tintEnabled, v -> cfg.tintEnabled = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Tint spruce and birch"))
                                .description(OptionDescription.of(Component.literal(
                                        "Vanilla gives spruce and birch leaves fixed colours that ignore the biome. "
                                        + "Turn this off to restore that, giving builders leaves that never change "
                                        + "with the season — as cherry, azalea, pale oak and poplar always do.")))
                                .binding(true, () -> cfg.tintFixedColourLeaves, v -> cfg.tintFixedColourLeaves = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Advanced"))
                        .tooltip(Component.literal(
                                "Tilt ships a deliberate palette. Autumn uses Mojang's own colours from the "
                                + "Dappled Forest biome. These are here for modpack authors who need to match a "
                                + "particular look, not because the defaults need changing."))
                        .group(palette("Spring", cfg.spring))
                        .group(palette("Summer", cfg.summer))
                        .group(palette("Autumn", cfg.autumn))
                        .group(palette("Winter", cfg.winter))
                        .build())
                .save(cfg::save)
                .build()
                .generateScreen(parent);
    }

    private static OptionGroup palette(String name, SeasonPalette p) {
        return OptionGroup.createBuilder()
                        .name(Component.literal(name))
                        .description(OptionDescription.of(Component.literal(
                                "Grass, leaves and litter blend toward these colours. Separate targets matter: "
                                + "what reads as autumn is straw ground under amber leaves. Straw under straw is "
                                + "savanna.")))
                        .option(Option.<Color>createBuilder()
                                .name(Component.literal("Grass"))
                                .binding(new Color(p.grassTarget), () -> new Color(p.grassTarget),
                                        v -> p.grassTarget = v.getRGB() & 0xFFFFFF)
                                .controller(ColorControllerBuilder::create)
                                .build())
                        .option(Option.<Color>createBuilder()
                                .name(Component.literal("Leaves"))
                                .binding(new Color(p.foliageTarget), () -> new Color(p.foliageTarget),
                                        v -> p.foliageTarget = v.getRGB() & 0xFFFFFF)
                                .controller(ColorControllerBuilder::create)
                                .build())
                        .option(Option.<Color>createBuilder()
                                .name(Component.literal("Leaf litter"))
                                .description(OptionDescription.of(Component.literal(
                                        "Dry foliage: leaf litter and dead bushes on the ground.")))
                                .binding(new Color(p.dryFoliageTarget), () -> new Color(p.dryFoliageTarget),
                                        v -> p.dryFoliageTarget = v.getRGB() & 0xFFFFFF)
                                .controller(ColorControllerBuilder::create)
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.literal("Strength"))
                                .description(OptionDescription.of(Component.literal(
                                        "How far to blend toward those colours. 0 keeps vanilla.")))
                                .binding(p.strength, () -> p.strength, v -> p.strength = v)
                                .controller(o -> FloatSliderControllerBuilder.create(o).range(0f, 1f).step(0.05f))
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.literal("Frost"))
                                .description(OptionDescription.of(Component.literal(
                                        "Blend toward white afterwards, in eight steps — Bedrock Edition's "
                                        + "snow-frosted leaves, which Java has never had.")))
                                .binding(p.frost, () -> p.frost, v -> p.frost = v)
                                .controller(o -> FloatSliderControllerBuilder.create(o).range(0f, 1f).step(0.05f))
                                .build())
                        .build();
    }
}
