package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.SoundConfig;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.overlay.TooltipOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TextComponent extends BaseComponent<TextComponent> {

    private Component text;
    private int color;
    private boolean shadow    = true;
    private boolean centered  = false;
    private boolean wrap      = true;
    private int lineSpacing   = 2;
    private SoundConfig soundConfig = SoundConfig.subtle();

    private boolean dirty = true;
    private int cachedWrapWidth = -1;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    @Nullable private Style hoveredStyle;
    @Nullable private Style lastHoveredStyle;
    private boolean wasHoveringText = false;
    private float linkHoverProgress = 0f;

    private record StyleRun(Style style, String text) {}

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

        hoveredStyle = styleAt(mouseX, mouseY);
        boolean hoveringText = hoveredStyle != null;
        boolean hoveringInteraction = hasInteraction(hoveredStyle);

        if (hoveringInteraction && (!wasHoveringText || hoveredStyle != lastHoveredStyle)) {
            soundConfig.playHover();
        }
        wasHoveringText = hoveringText;
        lastHoveredStyle = hoveredStyle;
        linkHoverProgress = stepAnimation(linkHoverProgress, hoveringInteraction, 0.5f, delta);

        int drawY = innerY();
        int maxY = y + height;

        g.enableScissor(x, y, x + width, y + height);
        for (FormattedCharSequence line : lines) {
            if (drawY >= maxY) break;
            int lineWidth = font.width(line);
            int drawX = centered ? innerX() + (innerWidth() - lineWidth) / 2 : innerX();

            g.text(font, line, drawX, drawY, color, shadow);
            if (hoveringInteraction) renderLineHighlight(g, line, drawX, drawY);

            drawY += font.lineHeight + lineSpacing;
        }
        g.disableScissor();

        if (hoveredStyle != null) queueTooltip(hoveredStyle, mouseX, mouseY);
    }

    private void renderLineHighlight(GuiGraphicsExtractor graphics, FormattedCharSequence line, int lineX, int lineY) {
        if (hoveredStyle == null) return;

        int runX = lineX;
        int underlineColor = ARGB.white((int) (200 * linkHoverProgress));
        for (StyleRun run : styleRuns(line)) {
            int runWidth = font.width(FormattedText.of(run.text(), run.style()));
            if (isSameInteraction(run.style(), hoveredStyle)) {
                graphics.fill(runX, lineY + font.lineHeight, runX + runWidth, lineY + font.lineHeight + 1, underlineColor);
            }
            runX += runWidth;
        }
    }

    private void queueTooltip(Style style, int mouseX, int mouseY) {
        HoverEvent hoverEvent = style.getHoverEvent();
        if (!(hoverEvent instanceof HoverEvent.ShowText(Component tooltipText))) return;
        TooltipOverlay.show(tooltipText, mouseX, mouseY, width);
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

    @Nullable
    private Style styleAt(int mouseX, int mouseY) {
        if (lines.isEmpty()) return null;
        if (!bounds().containsPoint(mouseX, mouseY)) return null;

        int lineHeight = font.lineHeight + lineSpacing;
        int lineIndex = (mouseY - innerY()) / lineHeight;
        if (lineIndex < 0 || lineIndex >= lines.size()) return null;

        FormattedCharSequence line = lines.get(lineIndex);
        int lineWidth = font.width(line);
        int lineStartX = centered ? innerX() + (innerWidth() - lineWidth) / 2 : innerX();
        if (mouseX < lineStartX || mouseX >= lineStartX + lineWidth) return null;

        int targetX = mouseX - lineStartX;
        int runX = 0;
        for (StyleRun run : styleRuns(line)) {
            int runWidth = font.width(FormattedText.of(run.text(), run.style()));
            if (targetX >= runX && targetX < runX + runWidth) return run.style();
            runX += runWidth;
        }
        return null;
    }

    private List<StyleRun> styleRuns(FormattedCharSequence line) {
        List<StyleRun> runs = new ArrayList<>();
        StringBuilder runText = new StringBuilder();
        Style[] currentStyle = {null};

        line.accept((_, style, codepoint) -> {
            if (currentStyle[0] != null && !currentStyle[0].equals(style)) {
                runs.add(new StyleRun(currentStyle[0], runText.toString()));
                runText.setLength(0);
            }
            currentStyle[0] = style;
            runText.appendCodePoint(codepoint);
            return true;
        });

        if (currentStyle[0] != null && !runText.isEmpty()) {
            runs.add(new StyleRun(currentStyle[0], runText.toString()));
        }
        return runs;
    }

    private boolean hasInteraction(@Nullable Style style) {
        return style != null && (style.getClickEvent() != null || style.getHoverEvent() != null);
    }

    private boolean isSameInteraction(@Nullable Style first, @Nullable Style second) {
        if (first == null || second == null) return false;
        ClickEvent firstClick = first.getClickEvent();
        ClickEvent secondClick = second.getClickEvent();
        if (firstClick != null && firstClick.equals(secondClick)) return true;
        HoverEvent firstHover = first.getHoverEvent();
        HoverEvent secondHover = second.getHoverEvent();
        return firstHover != null && firstHover.equals(secondHover);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;

        Style style = styleAt((int) event.x(), (int) event.y());
        if (style == null || style.getClickEvent() == null) return false;

        soundConfig.playClick();
        Screen screen = minecraft.screen;
        if (screen != null) {
            Screen.defaultHandleClickEvent(style.getClickEvent(), minecraft, screen);
        }
        return true;
    }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        if (isMouseOver(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos())) {
            return NarrationPriority.HOVERED;
        }

        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput output) {
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

    public TextComponent hoverSoundEnabled(boolean enabled) {
        soundConfig.setHoverEnabled(enabled);
        return this;
    }

    public TextComponent clickSoundEnabled(boolean enabled) {
        soundConfig.setClickEnabled(enabled);
        return this;
    }

    public TextComponent hoverSound(@Nullable SoundEvent sound, float pitch, float volume) {
        soundConfig.setHoverSound(sound, pitch, volume);
        return this;
    }

    public TextComponent clickSound(@Nullable SoundEvent sound, float pitch, float volume) {
        soundConfig.setClickSound(sound, pitch, volume);
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
}
