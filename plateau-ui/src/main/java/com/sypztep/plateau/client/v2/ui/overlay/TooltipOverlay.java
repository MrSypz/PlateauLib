package com.sypztep.plateau.client.v2.ui.overlay;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class TooltipOverlay {
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static int mouseX;
    private static int mouseY;

    private TooltipOverlay() {}

    public static void beginFrame(int mouseX, int mouseY) {
        TooltipOverlay.mouseX = mouseX;
        TooltipOverlay.mouseY = mouseY;
        ENTRIES.clear();
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static void show(Component text, int mouseX, int mouseY, int preferredWidth) {
        ENTRIES.add(new Entry(text, mouseX, mouseY, preferredWidth));
    }

    public static void showAtMouse(Component text, int preferredWidth) {
        show(text, mouseX, mouseY, preferredWidth);
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (ENTRIES.isEmpty()) return;

        Font font = Minecraft.getInstance().font;

        Entry entry = ENTRIES.getFirst();
        int wrapWidth = Math.max(entry.preferredWidth(), 200);
        List<FormattedCharSequence> lines = font.split(entry.text(), wrapWidth);
        graphics.setTooltipForNextFrame(font, lines, entry.mouseX(), entry.mouseY());
        ENTRIES.clear();
    }

    private record Entry(Component text, int mouseX, int mouseY, int preferredWidth) {}
}
