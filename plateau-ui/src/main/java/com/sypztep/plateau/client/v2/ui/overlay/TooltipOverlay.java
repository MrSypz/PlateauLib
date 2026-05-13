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

    private TooltipOverlay() {}

    public static void clear() {
        ENTRIES.clear();
    }

    public static void show(Component text, int mouseX, int mouseY, int preferredWidth) {
        ENTRIES.add(new Entry(text, mouseX, mouseY, preferredWidth));
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (ENTRIES.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenWidth = minecraft.screen != null ? minecraft.screen.width : minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.screen != null ? minecraft.screen.height : minecraft.getWindow().getGuiScaledHeight();

        for (Entry entry : ENTRIES) {
            renderEntry(graphics, font, screenWidth, screenHeight, entry);
        }
        ENTRIES.clear();
    }

    private static void renderEntry(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, Entry entry) {
        List<FormattedCharSequence> lines = font.split(entry.text(), Math.max(entry.preferredWidth(), 200));
        int lineHeight = font.lineHeight + 2;
        int tooltipWidth = 0;
        for (FormattedCharSequence line : lines) {
            tooltipWidth = Math.max(tooltipWidth, font.width(line));
        }
        int tooltipHeight = lines.size() * lineHeight;

        int padding = 4;
        int tooltipX = entry.mouseX() + 12;
        int tooltipY = entry.mouseY() - tooltipHeight - 4;

        if (tooltipX + tooltipWidth + padding * 2 > screenWidth) {
            tooltipX = entry.mouseX() - tooltipWidth - padding * 2 - 4;
        }
        if (tooltipY < 2) tooltipY = entry.mouseY() + 16;
        if (tooltipY + tooltipHeight + padding * 2 > screenHeight) {
            tooltipY = Math.max(2, screenHeight - tooltipHeight - padding * 2 - 2);
        }
        if (tooltipX < 2) tooltipX = 2;

        int bgColor = 0xF0100010;
        int borderTop = 0x505000FF;
        int borderBottom = 0x5028007F;

        graphics.fill(tooltipX - padding - 1, tooltipY - padding - 1,
                tooltipX + tooltipWidth + padding + 1, tooltipY + tooltipHeight + padding + 1, bgColor);
        graphics.fill(tooltipX - padding - 1, tooltipY - padding, tooltipX - padding, tooltipY + tooltipHeight + padding, borderTop);
        graphics.fill(tooltipX + tooltipWidth + padding, tooltipY - padding, tooltipX + tooltipWidth + padding + 1, tooltipY + tooltipHeight + padding, borderTop);
        graphics.fill(tooltipX - padding - 1, tooltipY - padding - 1, tooltipX + tooltipWidth + padding + 1, tooltipY - padding, borderTop);
        graphics.fill(tooltipX - padding - 1, tooltipY + tooltipHeight + padding, tooltipX + tooltipWidth + padding + 1, tooltipY + tooltipHeight + padding + 1, borderBottom);

        int lineY = tooltipY;
        for (FormattedCharSequence line : lines) {
            graphics.text(font, line, tooltipX, lineY, 0xFFFFFFFF, true);
            lineY += lineHeight;
        }
    }

    private record Entry(Component text, int mouseX, int mouseY, int preferredWidth) {}
}
