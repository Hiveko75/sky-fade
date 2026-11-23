package io.github.hiveko.skyfade.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Opens a screen (Minecraft.setScreen). */
final class ScreenSwitch {
    private ScreenSwitch() {}

    static void open(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }
}
