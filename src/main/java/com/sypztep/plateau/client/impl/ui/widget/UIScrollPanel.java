package com.sypztep.plateau.client.impl.ui.widget;

import com.sypztep.plateau.client.impl.ui.behavior.ScrollBehavior;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A panel with scrollable content area. Extends UIPanel to inherit
 * background, border, and header rendering without duplication.
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
public abstract class UIScrollPanel extends UIPanel {
    protected final ScrollBehavior scroll = new ScrollBehavior();

    public UIScrollPanel(int x, int y, int width, int height, @Nullable Component title) {
        super(x, y, width, height, title);
        setInteractable(true);
    }

    public UIScrollPanel(int x, int y, int width, int height) {
        this(x, y, width, height, null);
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Calculate scroll area below header
        int scrollAreaY = getContentY();
        int scrollAreaH = y + height - scrollAreaY;

        scroll.setBounds(x + 1, scrollAreaY, width - 2, scrollAreaH);
        scroll.update(delta);

        int contentX = x + padding;
        int contentW = scroll.getContentWidth() - padding * 2;

        scroll.enableScissor(graphics);
        renderScrollContent(graphics, mouseX, mouseY, delta,
                contentX, scrollAreaY + padding, contentW);
        scroll.disableScissor(graphics);

        scroll.renderScrollbar(graphics, mouseX, mouseY);
    }

    protected abstract void renderScrollContent(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                                int contentX, int contentY, int contentWidth);

    protected void setTotalContentHeight(int height) {
        scroll.setContentHeight(height);
    }

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

    public ScrollBehavior getScrollBehavior() { return scroll; }

    // Override to return UIScrollPanel for chaining
    @Override
    public UIScrollPanel setTitle(@Nullable Component title) { super.setTitle(title); return this; }
    @Override
    public UIScrollPanel setDrawHeader(boolean draw) { super.setDrawHeader(draw); return this; }
    @Override
    public UIScrollPanel setDrawBorder(boolean draw) { super.setDrawBorder(draw); return this; }
}
