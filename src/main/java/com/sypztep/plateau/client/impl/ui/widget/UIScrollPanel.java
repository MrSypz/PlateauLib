package com.sypztep.plateau.client.impl.ui.widget;

import com.sypztep.plateau.client.impl.ui.behavior.ScrollBehavior;
import com.sypztep.plateau.client.impl.ui.core.UIColors;
import com.sypztep.plateau.client.impl.ui.core.UIComponent;
import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A panel with scrollable content area.
 * Content that exceeds the panel height gets clipped and a scrollbar appears.
 *
 * <p>Override {@link #renderScrollContent} to draw your scrollable content.
 * Use {@link #getScrollOffset()} to offset your Y positions.</p>
 *
 * <h3>Usage:</h3>
 * <pre>
 * UIScrollPanel panel = new UIScrollPanel(10, 40, 200, 150, Component.literal("My List")) {
 *     {@literal @}Override
 *     protected void renderScrollContent(GuiGraphics graphics, int mouseX, int mouseY, float delta,
 *                                        int contentX, int contentY, int contentWidth) {
 *         for (int i = 0; i {@literal <} items.size(); i++) {
 *             int itemY = contentY + i * 20 - getScrollOffset();
 *             graphics.drawString(font, items.get(i), contentX, itemY, 0xFFFFFFFF, true);
 *         }
 *         setTotalContentHeight(items.size() * 20);
 *     }
 * };
 * </pre>
 */
public abstract class UIScrollPanel extends UIComponent {
    @Nullable protected Component title;
    protected boolean drawHeader;
    protected boolean drawBorder = true;
    protected float hoverAnimation = 0f;

    protected final ScrollBehavior scroll = new ScrollBehavior();

    public UIScrollPanel(int x, int y, int width, int height, @Nullable Component title) {
        super(x, y, width, height);
        this.title = title;
        this.drawHeader = title != null;
        this.padding = 10;
    }

    public UIScrollPanel(int x, int y, int width, int height) {
        this(x, y, width, height, null);
    }

    @Override
    protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        hoverAnimation = stepAnimation(hoverAnimation, isMouseOver(mouseX, mouseY), 0.05f);

        // Background
        int bg = UIColors.interpolate(UITheme.PANEL_BG, UITheme.PANEL_BG_HOVER, hoverAnimation);
        graphics.fill(x, y, x + width, y + height, bg);

        // Border
        if (drawBorder) {
            int border = UIColors.interpolate(UITheme.PANEL_BORDER, UITheme.PANEL_BORDER_HOVER, hoverAnimation);
            graphics.fill(x, y, x + width, y + 1, border);
            graphics.fill(x, y + height - 1, x + width, y + height, border);
            graphics.fill(x, y, x + 1, y + height, border);
            graphics.fill(x + width - 1, y, x + width, y + height, border);
        }

        // Header
        int scrollAreaY = y;
        int scrollAreaH = height;
        if (drawHeader && title != null) {
            int headerH = font.lineHeight + padding * 2;
            int headerBg = UIColors.interpolate(UITheme.PANEL_HEADER_BG, 0xFF2A2A2A, hoverAnimation);
            graphics.fill(x + 1, y + 1, x + width - 1, y + headerH, headerBg);
            int titleColor = UIColors.interpolate(UITheme.TEXT_ACCENT, UITheme.TEXT_PRIMARY, hoverAnimation * 0.3f);
            graphics.drawCenteredString(font, title, x + width / 2, y + padding, titleColor);

            scrollAreaY = y + headerH;
            scrollAreaH = height - headerH;
        }

        // Set scroll bounds to content area (below header)
        int contentX = x + padding;
        int contentW = width - padding * 2;
        scroll.setBounds(x + 1, scrollAreaY, width - 2, scrollAreaH);
        scroll.update(delta);

        // Scissor clip + render scrollable content
        scroll.enableScissor(graphics);
        renderScrollContent(graphics, mouseX, mouseY, delta,
                contentX, scrollAreaY + padding, scroll.getContentWidth() - padding * 2);
        scroll.disableScissor(graphics);

        // Scrollbar on top
        scroll.renderScrollbar(graphics, mouseX, mouseY);
    }

    /**
     * Render your scrollable content here.
     * Offset your Y positions by subtracting {@link #getScrollOffset()}.
     *
     * @param contentX     left edge of content area
     * @param contentY     top edge of content area (before scroll offset)
     * @param contentWidth available width (accounts for scrollbar)
     */
    protected abstract void renderScrollContent(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                                int contentX, int contentY, int contentWidth);

    /** Call this from renderScrollContent to tell the scroll behavior how tall your content is. */
    protected void setTotalContentHeight(int height) {
        scroll.setContentHeight(height);
    }

    /** Current scroll offset — subtract this from your Y positions. */
    protected int getScrollOffset() {
        return scroll.getScrollOffset();
    }

    // ═══════════════════════════════════════════
    // Input delegation
    // ═══════════════════════════════════════════

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        return scroll.mouseScrolled(mouseX, mouseY, vAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (scroll.mouseClicked(event, false)) return true;
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (scroll.mouseDragged(event)) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scroll.mouseReleased(event)) return true;
        return super.mouseReleased(event);
    }

    // ═══════════════════════════════════════════
    // Access to scroll behavior for configuration
    // ═══════════════════════════════════════════

    public ScrollBehavior getScrollBehavior() { return scroll; }

    // Fluent setters
    public UIScrollPanel setTitle(@Nullable Component title) { this.title = title; this.drawHeader = title != null; return this; }
    public UIScrollPanel setDrawHeader(boolean draw) { this.drawHeader = draw; return this; }
    public UIScrollPanel setDrawBorder(boolean draw) { this.drawBorder = draw; return this; }
}