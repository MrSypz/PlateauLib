package com.sypztep.plateau.client.ui.widget;

import com.sypztep.plateau.client.ui.core.UIColors;
import com.sypztep.plateau.client.ui.core.UIComponent;
import com.sypztep.plateau.client.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Rich text component with full Minecraft text feature support.
 *
 * <h3>Supports:</h3>
 * <ul>
 *   <li><b>Hover tooltips</b> — from Component's HoverEvent (show text, show item, show entity)</li>
 *   <li><b>Click actions</b> — open URL, copy to clipboard, run command, suggest command</li>
 *   <li><b>Sound on hover</b> — plays a sound when mouse enters the text area</li>
 *   <li><b>Word wrapping</b> — auto-wraps to fit width</li>
 *   <li><b>Link styling</b> — clickable text gets underline on hover</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>
 * // Simple text with tooltip
 * new UIText(10, 10, 200, Component.literal("Hover me!")
 *         .withStyle(style -> style.withHoverEvent(
 *             new HoverEvent.ShowText(Component.literal("Hello!")))));
 *
 * // Clickable link
 * new UIText(10, 30, 200, Component.literal("Open Website")
 *         .withStyle(style -> style
 *             .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com")))
 *             .withColor(ChatFormatting.AQUA)
 *             .withUnderlined(true)));
 *
 * // Mixed content
 * Component mixed = Component.literal("Normal text. ")
 *         .append(Component.literal("[Click Here]")
 *             .withStyle(style -> style
 *                 .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://example.com")))
 *                 .withHoverEvent(new HoverEvent.ShowText(Component.literal("Opens example.com")))
 *                 .withColor(ChatFormatting.YELLOW)));
 * new UIText(10, 50, 200, mixed);
 * </pre>
 */
public class UIText extends UIComponent {
    private Component text;
    private int color = UITheme.TEXT_PRIMARY;
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
    private boolean hoverSoundEnabled = true;
    private boolean wasHoveringText = false;

    // Sound
    @Nullable private net.minecraft.sounds.SoundEvent hoverSound = SoundEvents.NOTE_BLOCK_HAT.value();
    private float hoverSoundPitch = 1.8f;
    private float hoverSoundVolume = 0.5f;

    // Tooltip delay
    private int hoverTicks = 0;
    private static final int TOOLTIP_DELAY = 0; // frames before tooltip shows (0 = instant)

    public UIText(int x, int y, int width, Component text) {
        super(x, y, width, 0);
        this.text = text;
        this.focusable = false;
        markDirty();
    }

    public UIText(int x, int y, int width, int height, Component text) {
        super(x, y, width, height);
        this.text = text;
        this.focusable = false;
        markDirty();
    }

    @Override
    protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (dirty) rebuildLines();

        // Resolve hovered style from mouse position
        hoveredStyle = getStyleAt(mouseX, mouseY);

        // Hover sound on text area
        boolean hoveringText = hoveredStyle != null;
        if (hoveringText && !wasHoveringText && hoverSoundEnabled && hoverSound != null) {
            // Only play sound if the style has a hover or click event (interactive text)
            if (hoveredStyle.getHoverEvent() != null || hoveredStyle.getClickEvent() != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(hoverSound, hoverSoundPitch, hoverSoundVolume));
            }
        }

        // Track style transitions for sound (different clickable segment = new sound)
        if (hoveredStyle != lastHoveredStyle) {
            if (lastHoveredStyle != null
                    && hasInteraction(hoveredStyle) && hasInteraction(lastHoveredStyle)) {
                // Moved between two different interactive segments
                if (hoverSoundEnabled && hoverSound != null) {
                    minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(hoverSound, hoverSoundPitch + 0.1f, hoverSoundVolume * 0.7f));
                }
            }
            lastHoveredStyle = hoveredStyle;
        }
        wasHoveringText = hoveringText;

        // Link hover animation
        linkHoverAnimation = stepAnimation(linkHoverAnimation, hoveringText && hasInteraction(hoveredStyle), 0.1f);

        // Track hover duration for tooltip delay
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
                graphics.drawString(font, line, x + (width - lineW) / 2, ty, color, shadow);
            } else {
                graphics.drawString(font, line, x, ty, color, shadow);
            }

            // Draw underline on hovered clickable text segments
            if (hasInteraction(hoveredStyle)) {
                renderLineHighlight(graphics, line, ty, mouseX, mouseY);
            }

            ty += lineH;
        }

        // Render tooltip
        if (hoveredStyle != null && hoverTicks >= TOOLTIP_DELAY) {
            renderTooltip(graphics, hoveredStyle, mouseX, mouseY);
        }
    }

    /**
     * Highlight the hovered interactive segment with an underline.
     */
    private void renderLineHighlight(GuiGraphics graphics, FormattedCharSequence line, int lineY, int mouseX, int mouseY) {
        if (hoveredStyle == null) return;

        // Walk the line to find segments matching the hovered style
        int[] segStart = {0};
        int[] charX = {centered ? x + (width - font.width(line)) / 2 : x};

        line.accept((index, style, codepoint) -> {
            int charW = font.width(Character.toString(codepoint));

            if (isSameInteraction(style, hoveredStyle)) {
                // Draw underline under this character
                int alpha = (int)(200 * linkHoverAnimation);
                int underlineColor = UIColors.withAlpha(0xFFFFFF, alpha);
                graphics.fill(charX[0], lineY + font.lineHeight, charX[0] + charW, lineY + font.lineHeight + 1, underlineColor);
            }

            charX[0] += charW;
            return true;
        });
    }

    /**
     * Render tooltip from HoverEvent.
     */
    private void renderTooltip(GuiGraphics graphics, Style style, int mouseX, int mouseY) {
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

            // Clamp to screen
            if (tx + tooltipW + pad * 2 > parentScreenWidth()) {
                tx = mouseX - tooltipW - pad * 2 - 4;
            }
            if (ty < 2) {
                ty = mouseY + 16;
            }
            if (tx < 2) tx = 2;

            // Background
            int bgColor = 0xF0100010;
            int borderTop = 0x505000FF;
            int borderBot = 0x5028007F;

            graphics.fill(tx - pad - 1, ty - pad - 1,
                    tx + tooltipW + pad + 1, ty + tooltipH + pad + 1, bgColor);
            // Gradient border (Minecraft-style tooltip)
            graphics.fill(tx - pad - 1, ty - pad, tx - pad, ty + tooltipH + pad, borderTop);
            graphics.fill(tx + tooltipW + pad, ty - pad, tx + tooltipW + pad + 1, ty + tooltipH + pad, borderTop);
            graphics.fill(tx - pad - 1, ty - pad - 1, tx + tooltipW + pad + 1, ty - pad, borderTop);
            graphics.fill(tx - pad - 1, ty + tooltipH + pad, tx + tooltipW + pad + 1, ty + tooltipH + pad + 1, borderBot);

            // Text
            int ly = ty;
            for (FormattedCharSequence tl : tooltipLines) {
                graphics.drawString(font, tl, tx, ly, 0xFFFFFFFF, true);
                ly += lineH;
            }
        }
    }

    /**
     * Get the Style at a screen position by walking wrapped lines.
     */
    @Nullable
    private Style getStyleAt(int mouseX, int mouseY) {
        if (wrappedLines.isEmpty()) return null;

        int lineH = font.lineHeight + lineSpacing;
        int lineIndex = (mouseY - y) / lineH;

        if (lineIndex < 0 || lineIndex >= wrappedLines.size()) return null;

        FormattedCharSequence line = wrappedLines.get(lineIndex);
        int lineStartX = centered ? x + (width - font.width(line)) / 2 : x;

        if (mouseX < lineStartX || mouseX > lineStartX + font.width(line)) return null;

        // Walk characters to find which one the mouse is over
        int targetX = mouseX - lineStartX;
        final Style[] found = {null};
        final int[] currentX = {0};

        line.accept((index, style, codepoint) -> {
            int charW = font.width(Character.toString(codepoint));
            if (targetX >= currentX[0] && targetX < currentX[0] + charW) {
                found[0] = style;
                return false; // stop
            }
            currentX[0] += charW;
            return true;
        });

        return found[0];
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (!isMouseOver(event.x(), event.y())) return false;

        Style style = getStyleAt((int) event.x(), (int) event.y());
        if (style == null) return false;

        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) return false;

        if (event.button() == 0) {
            // Play click sound
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));

            // Delegate to Screen's click handler — handles URL confirmation dialogs, commands, etc.
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

    // ═══════════════════════════════════════════
    // Text management
    // ═══════════════════════════════════════════

    private void rebuildLines() {
        wrappedLines = font.split(text, width);
        // Auto-height if not explicitly set
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
        // Same click event = same interactive segment
        ClickEvent ca = a.getClickEvent();
        ClickEvent cb = b.getClickEvent();
        if (ca != null && ca.equals(cb)) return true;
        // Same hover event
        HoverEvent ha = a.getHoverEvent();
        HoverEvent hb = b.getHoverEvent();
        return ha != null && ha.equals(hb);
    }

    private int parentScreenWidth() {
        return minecraft.screen != null ? minecraft.screen.width : minecraft.getWindow().getGuiScaledWidth();
    }

    // ═══════════════════════════════════════════
    // Fluent setters
    // ═══════════════════════════════════════════

    public UIText setText(Component text) {
        this.text = text;
        markDirty();
        return this;
    }

    public UIText setColor(int color) { this.color = color; return this; }
    public UIText setShadow(boolean shadow) { this.shadow = shadow; return this; }
    public UIText setCentered(boolean centered) { this.centered = centered; return this; }
    public UIText setLineSpacing(int spacing) { this.lineSpacing = spacing; markDirty(); return this; }
    public UIText setHoverSoundEnabled(boolean enabled) { this.hoverSoundEnabled = enabled; return this; }
    public UIText setHoverSound(@Nullable net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        this.hoverSound = sound;
        this.hoverSoundPitch = pitch;
        this.hoverSoundVolume = volume;
        return this;
    }
}