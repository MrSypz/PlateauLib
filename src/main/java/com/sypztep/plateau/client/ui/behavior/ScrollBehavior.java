package com.sypztep.plateau.client.ui.behavior;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Composable scroll behavior — attach to any component that needs scrollable content.
 *
 * <p>Not a widget, not a base class. Just a behavior you call from your component's
 * render/mouseClicked/etc methods. Handles:</p>
 * <ul>
 *   <li>Smooth animated scrolling with momentum</li>
 *   <li>Scrollbar rendering with hover/glow animations</li>
 *   <li>Scrollbar dragging</li>
 *   <li>Scissor clipping setup</li>
 *   <li>Scroll-to-item for keyboard navigation</li>
 * </ul>
 *
 * <h3>Usage in a UIComponent:</h3>
 * <pre>
 * private final ScrollBehavior scroll = new ScrollBehavior();
 *
 * {@literal @}Override
 * protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
 *     scroll.setBounds(x, y, width, height);
 *     scroll.setContentHeight(totalContentHeight);
 *
 *     scroll.enableScissor(graphics);
 *     // render content at: y - scroll.getScrollOffset()
 *     scroll.disableScissor(graphics);
 *
 *     scroll.renderScrollbar(graphics, mouseX, mouseY);
 * }
 *
 * {@literal @}Override
 * public boolean mouseScrolled(double mx, double my, double h, double v) {
 *     return scroll.mouseScrolled(mx, my, v);
 * }
 * </pre>
 */
public final class ScrollBehavior {
    // Scroll state — smooth animation without a wrapper class
    private double scrollValue = 0;
    private double scrollTarget = 0;
    private int contentTotalHeight = 0;
    private int maxScroll = 0;

    // Smooth scroll config
    private float lerpSpeed = 0.3f;
    private double snapThreshold = 0.1;
    private double scrollSensitivity = 20.0;

    // Scrollbar config
    private boolean enableScrollbar = true;
    private int scrollbarWidth = 6;
    private int scrollbarPadding = 2;
    private int minHandleSize = 20;

    // Scrollbar animation
    private float scrollbarHoverAnimation = 0f;
    private boolean isDragging = false;

    // Bounds — set each frame or when layout changes
    private int x, y, width, height;

    public ScrollBehavior() {}

    // ═══════════════════════════════════════════
    // Setup — call before rendering
    // ═══════════════════════════════════════════

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        updateMaxScroll();
    }

    public void setContentHeight(int contentHeight) {
        this.contentTotalHeight = contentHeight;
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        maxScroll = Math.max(0, contentTotalHeight - height);
        scrollValue = Mth.clamp(scrollValue, 0, maxScroll);
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
    }

    // ═══════════════════════════════════════════
    // Animation tick — call once per frame
    // ═══════════════════════════════════════════

    /**
     * Update smooth scroll animation. Call at the start of your render method.
     */
    public void update(float delta) {
        // Smooth scroll toward target
        if (Math.abs(scrollValue - scrollTarget) > snapThreshold) {
            float lerpFactor = 1.0f - (float) Math.exp(-lerpSpeed * delta);
            scrollValue = Mth.lerp(lerpFactor, scrollValue, scrollTarget);
        } else {
            scrollValue = scrollTarget;
        }
        scrollValue = Mth.clamp(scrollValue, 0, maxScroll);
    }

    // ═══════════════════════════════════════════
    // Scissor helpers
    // ═══════════════════════════════════════════

    public void enableScissor(GuiGraphics graphics) {
        graphics.enableScissor(x, y, x + getContentWidth(), y + height);
    }

    public void disableScissor(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    // ═══════════════════════════════════════════
    // Scrollbar rendering
    // ═══════════════════════════════════════════

    /**
     * Render the scrollbar. Call AFTER disableScissor so the scrollbar isn't clipped.
     */
    public void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!enableScrollbar || maxScroll <= 0) return;

        // Hover animation
        boolean hovered = isScrollbarHovered(mouseX, mouseY);
        float hoverTarget = (hovered || isDragging) ? 1f : 0f;
        if (scrollbarHoverAnimation < hoverTarget) {
            scrollbarHoverAnimation = Math.min(1f, scrollbarHoverAnimation + 0.08f);
        } else if (scrollbarHoverAnimation > hoverTarget) {
            scrollbarHoverAnimation = Math.max(0f, scrollbarHoverAnimation - 0.04f);
        }

        int sbX = x + width - scrollbarWidth - scrollbarPadding;
        int sbY = y;
        int sbH = height;

        // Track background
        int bgAlpha = 25 + (int) (55 * scrollbarHoverAnimation);
        graphics.fill(sbX, sbY, sbX + scrollbarWidth, sbY + sbH, (bgAlpha << 24));

        // Handle size and position
        int handleH = Math.max(minHandleSize, sbH * height / contentTotalHeight);
        int handleY = sbY + (int) ((sbH - handleH) * scrollValue / maxScroll);

        // Expand handle on hover
        int expansion = (int) scrollbarHoverAnimation;
        int animX = sbX - expansion;
        int animH = handleH + expansion * 2;

        // Handle colors
        int baseAlpha = 120 + (int) (135 * scrollbarHoverAnimation);
        int handleBg = (baseAlpha << 24) | 0x666666;
        int handleFg = ARGB.srgbLerp(scrollbarHoverAnimation, 0xFFAAAAAA, 0xFFFFFFFF);

        // Glow
        if (scrollbarHoverAnimation > 0.1f) {
            int glowSize = (int) (4 * scrollbarHoverAnimation);
            int glowAlpha = (int) (40 * scrollbarHoverAnimation);
            int glowColor = (glowAlpha << 24) | 0xFFFFFF;
            graphics.fill(animX - glowSize, handleY - glowSize,
                    animX + scrollbarWidth + glowSize, handleY + animH + glowSize, glowColor);
        }

        // Handle body
        graphics.fill(animX, handleY, animX + scrollbarWidth, handleY + animH, handleBg);
        graphics.fill(animX + 1, handleY + 1, animX + scrollbarWidth - 1, handleY + animH - 1, handleFg);

        // Grip lines when highly hovered
        if (scrollbarHoverAnimation > 0.5f) {
            int lineY = handleY + animH / 2 - 3;
            int lineAlpha = (int) (255 * ((scrollbarHoverAnimation - 0.5f) * 2));
            int lineColor = (lineAlpha << 24) | 0x999999;
            for (int i = 0; i < 3; i++) {
                graphics.fill(animX + 2, lineY + i * 3,
                        animX + scrollbarWidth - 2, lineY + i * 3 + 1, lineColor);
            }
        }
    }

    // ═══════════════════════════════════════════
    // Input handling
    // ═══════════════════════════════════════════

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (maxScroll > 0 && isInBounds(mouseX, mouseY)) {
            scrollTarget = Mth.clamp(scrollTarget - verticalAmount * scrollSensitivity, 0, maxScroll);
            return true;
        }
        return false;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (consumed) return false;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && isScrollbarHovered((int) event.x(), (int) event.y())) {
            isDragging = true;
            updateScrollFromMouse(event.y());
            return true;
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event) {
        if (isDragging && event.button() == GLFW.GLFW_MOUSE_BUTTON_1) {
            updateScrollFromMouse(event.y());
            return true;
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════
    // Getters
    // ═══════════════════════════════════════════

    /** Current scroll offset in pixels. Use this to offset your content rendering. */
    public int getScrollOffset() {
        return (int) scrollValue;
    }

    /** Width available for content (accounts for scrollbar if visible). */
    public int getContentWidth() {
        return (enableScrollbar && maxScroll > 0) ? width - scrollbarWidth - scrollbarPadding : width;
    }

    public boolean hasScrollbar() { return enableScrollbar && maxScroll > 0; }
    public int getMaxScroll() { return maxScroll; }
    public boolean isDragging() { return isDragging; }

    // ═══════════════════════════════════════════
    // Navigation helpers
    // ═══════════════════════════════════════════

    /** Scroll to make an item at the given index visible. */
    public void scrollToItem(int itemIndex, int itemHeight) {
        if (itemIndex < 0) return;
        int itemY = itemIndex * itemHeight;
        int visibleTop = (int) scrollValue;
        int visibleBottom = visibleTop + height;

        if (itemY < visibleTop) {
            scrollTarget = itemY;
        } else if (itemY + itemHeight > visibleBottom) {
            scrollTarget = itemY + itemHeight - height;
        }
    }

    /** Scroll to a specific Y offset. */
    public void scrollTo(double target) {
        scrollTarget = Mth.clamp(target, 0, maxScroll);
    }

    /** Reset scroll to top. */
    public void resetScroll() {
        scrollTarget = 0;
    }

    // ═══════════════════════════════════════════
    // Config — fluent setters
    // ═══════════════════════════════════════════

    public ScrollBehavior setScrollbarWidth(int w) { this.scrollbarWidth = w; return this; }
    public ScrollBehavior setScrollbarPadding(int p) { this.scrollbarPadding = p; return this; }
    public ScrollBehavior setMinHandleSize(int s) { this.minHandleSize = s; return this; }
    public ScrollBehavior setScrollbarEnabled(boolean e) { this.enableScrollbar = e; return this; }
    public ScrollBehavior setScrollSensitivity(double s) { this.scrollSensitivity = s; return this; }
    public ScrollBehavior setLerpSpeed(float s) { this.lerpSpeed = s; return this; }

    // ═══════════════════════════════════════════
    // Internal
    // ═══════════════════════════════════════════

    private boolean isInBounds(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private boolean isScrollbarHovered(int mx, int my) {
        if (!enableScrollbar || maxScroll <= 0) return false;
        int sbX = x + width - scrollbarWidth - scrollbarPadding;
        return mx >= sbX && mx <= sbX + scrollbarWidth && my >= y && my <= y + height;
    }

    private void updateScrollFromMouse(double mouseY) {
        double ratio = (mouseY - y) / (double) height;
        double newScroll = ratio * maxScroll;
        scrollValue = Mth.clamp(newScroll, 0, maxScroll);
        scrollTarget = scrollValue;
    }
}