package io.github.hiveko.skyfade.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Sky Fade's settings screen (Mod Menu → Sky Fade → Configure), made of Minecraft's own widgets: the fade
 * width slider and Done. When the width changed, Done saves it and reloads resources, so Sodium's terrain
 * shader is made again with the new width.
 */
public class SkyFadeScreen extends Screen {
    private final Screen parent;
    private final int before;
    private int chunks;

    public SkyFadeScreen(Screen parent) {
        super(Component.literal("Sky Fade"));
        this.parent = parent;
        this.before = SkyFadeConfig.fadeWidth();
        this.chunks = before / 16;
    }

    @Override
    protected void init() {
        int w = 310;
        int x = (this.width - w) / 2;
        int y = this.height / 4;
        addRenderableWidget(new StringWidget(x, y, w, 9, this.title, this.font));
        addRenderableWidget(new WidthSlider(x, y + 20, w));
        addRenderableWidget(new MultiLineTextWidget(x, y + 48, Component.literal(
                "How far before the edge of your render distance the terrain starts fading into the sky. "
                        + "Game default: a tenth of the render distance (4 to 64 blocks). "
                        + "A wider fade hides the edge more softly. Done reloads the resources once to apply a change."),
                this.font).setMaxWidth(w).setCentered(true));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds((this.width - 200) / 2, this.height - 36, 200, 20).build());
    }

    @Override
    public void onClose() {
        int after = chunks * 16;
        Minecraft mc = Minecraft.getInstance();
        ScreenSwitch.open(parent);
        if (after != before) {
            SkyFadeConfig.setFadeWidth(after);
            mc.reloadResourcePacks();
        }
    }

    private static Component label(int chunks) {
        return Component.literal("Fade width: " + (chunks == 0 ? "Game default"
                : chunks == 1 ? "1 chunk (16 blocks)" : chunks + " chunks (" + chunks * 16 + " blocks)"));
    }

    private class WidthSlider extends AbstractSliderButton {
        WidthSlider(int x, int y, int w) {
            super(x, y, w, 20, label(chunks), chunks / (double) SkyFadeConfig.MAX_CHUNKS);
        }

        @Override
        protected void updateMessage() {
            setMessage(label(chunks));
        }

        @Override
        protected void applyValue() {
            chunks = (int) Math.round(this.value * SkyFadeConfig.MAX_CHUNKS);
        }
    }
}
