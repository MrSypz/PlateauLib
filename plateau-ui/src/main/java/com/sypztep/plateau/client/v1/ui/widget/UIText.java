package com.sypztep.plateau.client.v1.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.SoundConfig;
import com.sypztep.plateau.client.v1.ui.core.UIComponent;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Rich text component with full Minecraft text feature support.
 *
 * <h3>Supports:</h3>
 * <ul>
 *   <li><b>Hover tooltips</b> — from Component's HoverEvent</li>
 *   <li><b>Click actions</b> — open URL, copy to clipboard, run command, suggest command</li>
 *   <li><b>Sound on hover</b> — configurable via SoundConfig</li>
 *   <li><b>Word wrapping</b> — auto-wraps to fit width</li>
 *   <li><b>Link styling</b> — clickable text gets underline on hover</li>
 * </ul>
 */
public class UIText extends UIComponent {
    private Component text;
    private int color = UITheme.current().textPrimary();
    private boolean shadow = true;
    private boolean centered = false;

    // Wrapping
    private List<FormattedCharSequence> wrappedLines = new ArrayList<>();
    private int lineSpacing = 2;
    private boolean dirty = true;

    // Hover state
    @Nullable private Style hoveredStyle = null;
    @Nullable private Style lastHoveredStyle = null;
    private float linkHoverAnimation = 0f;
    private boolean wasHoveringText = false;

    // Tooltip delay
    private int hoverTicks = 0;
    private static final int TOOLTIP_DELAY = 0;

    public UIText(int x, int y, int width, Component text) {
        super(x, y, width, 0);
        this.text = text;
        this.focusable = false;
        this.soundConfig = SoundConfig.subtle();
        markDirty();
    }

    public UIText(int x, int y, int width, int height, Component text) {
        super(x, y, width, height);
        this.text = text;
        this.focusable = false;
        this.soundConfig = SoundConfig.subtle();
        markDirty();
    }

    @Override
    protected void renderComponent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (dirty) rebuildLines();

        hoveredStyle = getStyleAt(mouseX, mouseY);

        boolean hoveringText = hoveredStyle != null;

        // Sound on entering interactive text
        if (hoveringText && !wasHoveringText) {
            if (hasInteraction(hoveredStyle)) {
                soundConfig.playHover();
            }
        }

        // Sound on moving between interactive segments
        if (hoveredStyle != lastHoveredStyle) {
            if (lastHoveredStyle != null
                    && hasInteraction(hoveredStyle) && hasInteraction(lastHoveredStyle)) {
                soundConfig.playHover();
            }
            lastHoveredStyle = hoveredStyle;
        }
        wasHoveringText = hoveringText;

        linkHoverAnimation = stepAnimation(linkHoverAnimation, hoveringText && hasInteraction(hoveredStyle), 0.1f);

        if (hoveringText) {
            hoverTicks++;
        } else {
            hoverTicks = 0;
        }

        // Render text lines
        int lineH = font.lineHeight + lineSpacing;
        int ty = y;

        for (FormattedCharSequence line : wrappedLines) {
            if (centered) {
                int lineW = font.width(line);
                graphics.text(font, line, x + (width - lineW) / 2, ty, color, shadow);
            } else {
                graphics.text(font, line, x, ty, color, shadow);
            }

            if (hasInteraction(hoveredStyle)) {
                renderLineHighlight(graphics, line, ty, mouseX, mouseY);
            }

            ty += lineH;
        }

        if (hoveredStyle != null && hoverTicks >= TOOLTIP_DELAY) {
            renderTooltip(graphics, hoveredStyle, mouseX, mouseY);
        }
    }

    private void renderLineHighlight(GuiGraphicsExtractor graphics, FormattedCharSequence line, int lineY, int mouseX, int mouseY) {
        if (hoveredStyle == null) return;

        int[] charX = {centered ? x + (width - font.width(line)) / 2 : x};

        line.accept((_, style, codepoint) -> {
            int charW = font.width(Character.toString(codepoint));

            if (isSameInteraction(style, hoveredStyle)) {
                int underlineColor = ARGB.white((int)(200 * linkHoverAnimation));
                graphics.fill(charX[0], lineY + font.lineHeight, charX[0] + charW, lineY + font.lineHeight + 1, underlineColor);
            }

            charX[0] += charW;
            return true;
        });
    }

    private void renderTooltip(GuiGraphicsExtractor graphics, Style style, int mouseX, int mouseY) {
        HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent == null) return;

        if (hoverEvent instanceof HoverEvent.ShowText(Component tooltipText)) {
            List<FormattedCharSequence> tooltipLines = font.split(tooltipText, Math.max(width, 200));

            int lineH = font.lineHeight + 2;
            int tooltipW = 0;
            for (FormattedCharSequence tl : tooltipLines) {
                tooltipW = Math.max(tooltipW, font.width(tl));
            }
            int tooltipH = tooltipLines.size() * lineH;

            int pad = 4;
            int tx = mouseX + 12;
            int ty = mouseY - tooltipH - 4;

            if (tx + tooltipW + pad * 2 > parentScreenWidth()) {
                tx = mouseX - tooltipW - pad * 2 - 4;
            }
            if (ty < 2) {
                ty = mouseY + 16;
            }
            if (tx < 2) tx = 2;

            int bgColor = 0xF0100010;
            int borderTop = 0x505000FF;
            int borderBot = 0x5028007F;

            graphics.fill(tx - pad - 1, ty - pad - 1,
                    tx + tooltipW + pad + 1, ty + tooltipH + pad + 1, bgColor);
            graphics.fill(tx - pad - 1, ty - pad, tx - pad, ty + tooltipH + pad, borderTop);
            graphics.fill(tx + tooltipW + pad, ty - pad, tx + tooltipW + pad + 1, ty + tooltipH + pad, borderTop);
            graphics.fill(tx - pad - 1, ty - pad - 1, tx + tooltipW + pad + 1, ty - pad, borderTop);
            graphics.fill(tx - pad - 1, ty + tooltipH + pad, tx + tooltipW + pad + 1, ty + tooltipH + pad + 1, borderBot);

            int ly = ty;
            for (FormattedCharSequence tl : tooltipLines) {
                graphics.text(font, tl, tx, ly, 0xFFFFFFFF, true);
                ly += lineH;
            }
        }
    }

    @Nullable
    private Style getStyleAt(int mouseX, int mouseY) {
        if (wrappedLines.isEmpty()) return null;

        int lineH = font.lineHeight + lineSpacing;
        int lineIndex = (mouseY - y) / lineH;

        if (lineIndex < 0 || lineIndex >= wrappedLines.size()) return null;

        FormattedCharSequence line = wrappedLines.get(lineIndex);
        int lineStartX = centered ? x + (width - font.width(line)) / 2 : x;

        if (mouseX < lineStartX || mouseX > lineStartX + font.width(line)) return null;

        int targetX = mouseX - lineStartX;
        final Style[] found = {null};
        final int[] currentX = {0};

        line.accept((index, style, codepoint) -> {
            int charW = font.width(Character.toString(codepoint));
            if (targetX >= currentX[0] && targetX < currentX[0] + charW) {
                found[0] = style;
                return false;
            }
            currentX[0] += charW;
            return true;
        });

        return found[0];
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;

        Style style = getStyleAt((int) event.x(), (int) event.y());
        if (style == null) return false;

        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) return false;

        if (event.button() == 0) {
            soundConfig.playClick();

            Screen currentScreen = minecraft.screen;
            if (currentScreen != null) {
                Screen.defaultHandleClickEvent(clickEvent, minecraft, currentScreen);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) return false;
        int totalH = wrappedLines.size() * (font.lineHeight + lineSpacing);
        int actualH = height > 0 ? height : totalH;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + actualH;
    }

    private void rebuildLines() {
        wrappedLines = font.split(text, width);
        if (height == 0 || dirty) {
            height = wrappedLines.size() * (font.lineHeight + lineSpacing);
        }
        dirty = false;
    }

    private void markDirty() {
        dirty = true;
    }

    private boolean hasInteraction(@Nullable Style style) {
        if (style == null) return false;
        return style.getClickEvent() != null || style.getHoverEvent() != null;
    }

    private boolean isSameInteraction(@Nullable Style a, @Nullable Style b) {
        if (a == null || b == null) return false;
        ClickEvent ca = a.getClickEvent();
        ClickEvent cb = b.getClickEvent();
        if (ca != null && ca.equals(cb)) return true;
        HoverEvent ha = a.getHoverEvent();
        HoverEvent hb = b.getHoverEvent();
        return ha != null && ha.equals(hb);
    }

    private int parentScreenWidth() {
        return minecraft.screen != null ? minecraft.screen.width : minecraft.getWindow().getGuiScaledWidth();
    }

    // Fluent setters
    public UIText setText(Component text) {
        this.text = text;
        markDirty();
        return this;
    }
    public UIText setColor(int color) { this.color = color; return this; }
    public UIText setShadow(boolean shadow) { this.shadow = shadow; return this; }
    public UIText setCentered(boolean centered) { this.centered = centered; return this; }
    public UIText setLineSpacing(int spacing) { this.lineSpacing = spacing; markDirty(); return this; }
    public UIText setHoverSoundEnabled(boolean enabled) {
        this.soundConfig.setHoverEnabled(enabled);
        return this;
    }
    public UIText setHoverSound(@Nullable net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        this.soundConfig.setHoverSound(sound, pitch, volume);
        return this;
    }
}
