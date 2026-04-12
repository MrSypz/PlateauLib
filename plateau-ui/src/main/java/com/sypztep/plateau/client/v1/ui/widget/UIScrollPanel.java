package com.sypztep.plateau.client.v1.ui.widget;

import com.sypztep.plateau.client.v1.ui.behavior.ScrollBehavior;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A panel with scrollable content area. Extends UIPanel to inherit
 * background, border, and header rendering without duplication.
 *
 * <p>Scrolling is handled by the engine — keyboard (arrow/page/home/end),
 * mouse wheel, and scrollbar drag all work automatically when focused.
 * Just override {@link #renderScrollContent} to draw your content.</p>
 *
 * <h3>Usage:</h3>
 * <pre>
 * UIScrollPanel panel = new UIScrollPanel(10, 40, 200, 150, Component.literal("My List")) {
 *     {@literal @}Override
 *     protected void renderScrollContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta,
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

    public UIScrollPanel(int x, int y, int width, int height, @Nullable Component title) {
        super(x, y, width, height, title);
        setInteractable(true);
        this.focusable = true;
        enableScrolling(); // uses engine-level scroll from UIComponent
    }

    public UIScrollPanel(int x, int y, int width, int height) {
        this(x, y, width, height, null);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
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

        renderFocusRing(graphics);
    }

    protected abstract void renderScrollContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta,
                                                int contentX, int contentY, int contentWidth);

    protected void setTotalContentHeight(int height) {
        scroll.setContentHeight(height);
    }

    protected int getScrollOffset() {
        return scroll.getScrollOffset();
    }

    /**
     * Scroll to make a specific Y offset visible within the content area.
     */
    public void scrollToVisible(int targetY, int targetHeight) {
        scroll.scrollToItem(targetY, targetHeight);
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
