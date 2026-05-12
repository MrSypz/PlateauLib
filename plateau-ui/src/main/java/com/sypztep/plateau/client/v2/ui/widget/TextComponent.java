package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TextComponent extends BaseComponent {

    private Component text;
    private int color;
    private boolean shadow    = true;
    private boolean centered  = false;
    private boolean wrap      = true;
    private int lineSpacing   = 2;

    private boolean dirty = true;
    private int cachedWrapWidth = -1;
    private final List<FormattedCharSequence> lines = new ArrayList<>();

    public TextComponent(Component text) {
        this.text  = text;
        this.color = UITheme.current().text().primary();

        this.horizontalSizing = Sizing.fill();
        this.verticalSizing   = Sizing.content();
    }

    @Override
    public int determineHorizontalContentSize(int space) {
        int available = Math.max(0, space);
        if (wrap) return available;
        return Math.min(font.width(text) + padding.horizontal(), available);
    }

    @Override
    public int determineVerticalContentSize(int space) {
        int wrapWidth = Math.max(1, space - padding.horizontal());
        rebuildIfNeeded(wrapWidth);

        int lineCount = Math.max(1, lines.size());
        return padding.vertical()
                + lineCount * font.lineHeight
                + Math.max(0, lineCount - 1) * lineSpacing;
    }

    @Override
    protected void onMounted() {
        rebuildIfNeeded(Math.max(1, innerWidth()));
    }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        int wrapWidth = Math.max(1, innerWidth());
        rebuildIfNeeded(wrapWidth);

        int drawY = innerY();
        int maxY = y + height;

        g.enableScissor(x, y, x + width, y + height);
        for (FormattedCharSequence line : lines) {
            if (drawY >= maxY) break;
            int lineWidth = font.width(line);
            int drawX = centered ? innerX() + (innerWidth() - lineWidth) / 2 : innerX();

            g.text(font, line, drawX, drawY, color, shadow);

            drawY += font.lineHeight + lineSpacing;
        }
        g.disableScissor();
    }

    private void rebuildIfNeeded(int wrapWidth) {
        if (!dirty && cachedWrapWidth == wrapWidth) return;

        lines.clear();

        if (wrap) {
            lines.addAll(font.split(text, wrapWidth));
        } else {
            lines.add(text.getVisualOrderText());
        }

        cachedWrapWidth = wrapWidth;
        dirty = false;
    }

    private void markDirty() {
        dirty = true;
        cachedWrapWidth = -1;
    }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        if (isMouseOver(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos())) {
            return NarrationPriority.HOVERED;
        }

        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, text);
    }

    // Fluent

    public TextComponent text(Component text) {
        this.text = text;
        markDirty();
        return this;
    }

    public TextComponent color(int color) {
        this.color = color;
        return this;
    }

    public TextComponent shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public TextComponent centered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public TextComponent wrap(boolean wrap) {
        this.wrap = wrap;
        markDirty();
        return this;
    }

    public TextComponent lineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
        markDirty();
        return this;
    }

    public TextComponent secondary() {
        return color(UITheme.current().text().secondary());
    }

    public TextComponent accent() {
        return color(UITheme.current().text().accent());
    }

    public TextComponent disabled() {
        return color(UITheme.current().text().disabled());
    }

    @Override public TextComponent padding(Insets padding)    { super.padding(padding); return this; }
    @Override public TextComponent margins(Insets margins)    { super.margins(margins); return this; }
    @Override public TextComponent surface(Surface surface)   { super.surface(surface); return this; }
    @Override public TextComponent id(String id)              { super.id(id);           return this; }
    @Override public TextComponent visible(boolean visible)   { super.visible(visible); return this; }
    @Override public TextComponent sizing(Sizing h, Sizing v) { super.sizing(h, v);     return this; }
    @Override public TextComponent sizing(Sizing both)        { super.sizing(both);     return this; }
}
