package io.github.hiveko.skyfade.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu's Configure button for Sky Fade. Mod Menu is optional; without it the file still works. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SkyFadeScreen::new;
    }
}
