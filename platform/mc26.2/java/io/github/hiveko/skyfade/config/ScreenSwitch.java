package io.github.hiveko.skyfade.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Opens a screen (Minecraft 26.2 and later: through Gui). */
final class ScreenSwitch {
    private ScreenSwitch() {}

    static void open(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }
}
