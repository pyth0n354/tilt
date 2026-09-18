package io.github.pyth0n354.tilt.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Adds Tilt's config button to Mod Menu.
 *
 * <p>Declared as a {@code modmenu} entrypoint, so Fabric only loads this class when Mod Menu is
 * present. Mod Menu is therefore optional — Tilt runs standalone without it.
 */
public class TiltModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TiltConfigScreen::create;
    }
}
