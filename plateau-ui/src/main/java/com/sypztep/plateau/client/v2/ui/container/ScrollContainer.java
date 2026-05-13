package com.sypztep.plateau.client.v2.ui.container;

import com.sypztep.plateau.client.v1.ui.behavior.ScrollBehavior;
import com.sypztep.plateau.client.v2.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;


/**
 * A vertically scrollable container. Children are stacked top-to-bottom, each sized by their
 * own {@link Sizing}. Implements {@link ContainerEventHandler} so MC's Tab and Arrow key
 * navigation can descend into children. Arrow keys navigate between focusable children;
 * Page Up/Down and Home/End scroll the viewport.
 */
@Environment(EnvType.CLIENT)
public class ScrollContainer extends BaseContainerComponent<ScrollContainer> {
    private int gap = 0;
    private int contentHeight = 0;
    private final ScrollBehavior scroll = new ScrollBehavior();

    public ScrollContainer(Sizing horizontal, Sizing vertical) {
        this.horizontalSizing = horizontal;
        this.verticalSizing   = vertical;
    }

    // ── Child management ─────────────────────────────────────

    public ScrollContainer gap(int gap) { this.gap = gap; return this; }

    // ── Input ─────────────────────────────────────────────────

    // mouseClicked dispatches through the PointerInteractable chain using content-space
    // coordinates so that hit-testing works correctly regardless of scroll offset.
    // Screen-space mouse Y and layout-space child Y only agree when scroll offset is zero,
    // so contentY (= screenY + scrollOffset) is computed once here and passed all the way down.
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;
        if (scroll.mouseClicked(event, doubleClick)) return true;

        double cx = event.x();
        double cy = event.y() + scroll.getScrollOffset(); // content-space Y

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible() || !child.rendersAboveSiblings()) continue;
            if (child.hitTest(cx, cy)) {
                if (child.onPointerClicked(event, doubleClick, cx, cy)) {
                    if (child != getFocused() && child.shouldTakeFocusAfterInteraction()) {
                        setFocused(child);
                    }
                    setDragging(true);
                    return true;
                }
                if (child.blocksLowerInput()) return true;
            }
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible()) continue;
            if (child.hitTest(cx, cy)) {
                if (child.onPointerClicked(event, doubleClick, cx, cy)) {
                    // Only update focusedChild when the section changes — re-focusing the same
                    // child would call setFocused(false) on it, erasing the button focus that
                    // onPointerClicked just established inside the FlowLayout.
                    if (child != getFocused() && child.shouldTakeFocusAfterInteraction()) {
                        setFocused(child);
                    }
                    setDragging(true);
                    return true;
                }
                break; // Found the section; it did not consume the click — stop searching.
            }
        }
        // Click on empty area or non-interactive content — clear keyboard focus.
        setFocused(null);
        return false;
    }

    @Override
    public boolean onPointerClicked(MouseButtonEvent event, boolean doubleClick, double x, double y) {
        if (!hitTest(x, y)) return false;
        if (scroll.mouseClicked(event, doubleClick)) return true;

        double cx = x;
        double cy = y + scroll.getScrollOffset();

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible() || !child.rendersAboveSiblings()) continue;

            if (child.hitTest(cx, cy)) {
                if (child.onPointerClicked(event, doubleClick, cx, cy)) {
                    if (child != getFocused() && child.shouldTakeFocusAfterInteraction()) {
                        setFocused(child);
                    }

                    setDragging(true);
                    return true;
                }

                if (child.blocksLowerInput()) return true;
            }
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible()) continue;

            if (child.hitTest(cx, cy)) {
                if (child.onPointerClicked(event, doubleClick, cx, cy)) {
                    if (child != getFocused() && child.shouldTakeFocusAfterInteraction()) {
                        setFocused(child);
                    }

                    setDragging(true);
                    return true;
                }

                break;
            }
        }

        setFocused(null);
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        setDragging(false);
        scroll.mouseReleased(event);
        if (getFocused() != null) getFocused().mouseReleased(event);
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (scroll.mouseDragged(event)) return true;
        return getFocused() != null && isDragging() && getFocused().mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        double contentY = mouseY + scroll.getScrollOffset();

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible() || !child.rendersAboveSiblings()) continue;

            if (child.hitTest(mouseX, contentY)) {
                if (child.mouseScrolled(mouseX, contentY, hAmount, vAmount)) return true;
                if (child.blocksLowerInput()) return true;
            }
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible()) continue;

            if (child.hitTest(mouseX, contentY)) {
                if (child.mouseScrolled(mouseX, contentY, hAmount, vAmount)) return true;
                break;
            }
        }

        return scroll.mouseScrolled(mouseX, mouseY, vAmount);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double adjustedY = mouseY + scroll.getScrollOffset();
        for (BaseComponent<?> child : children) {
            if (child.isVisible()) child.mouseMoved(mouseX, adjustedY);
        }
    }

    // Arrow keys are consumed by MC's ContainerEventHandler navigation (nextFocusPath).
    // Page/Home/End still scroll the viewport when no child handles the key.
    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (getFocused() != null && getFocused().keyPressed(event)) return true;

        int key = event.key();
        if (key == GLFW.GLFW_KEY_PAGE_UP) { scroll.scrollBy(-(height - 20)); return true; } // PAGE_UP
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) { scroll.scrollBy( height - 20); return true; } // PAGE_DOWN
        if (key == GLFW.GLFW_KEY_HOME) { scroll.scrollTo(0); return true; } // HOME
        if (key == GLFW.GLFW_KEY_END) { scroll.scrollToEnd(); return true; } // END
        return false;
    }

    // ── Layout ───────────────────────────────────────────────

    @Override
    protected void onMounted() {
        layoutChildren();
    }

    private void layoutChildren() {
        int contentX = x + padding.left();
        int contentWidth = Math.max(0, width - padding.horizontal());

        contentHeight = layoutChildren(contentX, contentWidth);
        if (contentHeight > height) {
            contentHeight = layoutChildren(contentX, Math.max(0, contentWidth - SCROLLBAR_RESERVED));
        }

        syncScrollState();
    }

    private int layoutChildren(int contentX, int contentWidth) {
        List<BaseComponent<?>> visible = children.stream()
                .filter(BaseComponent::isVisible)
                .toList();

        int nextY = y + padding.top();
        for (int i = 0; i < visible.size(); i++) {
            BaseComponent<?> child = visible.get(i);

            int childAvailableWidth = Math.max(0, contentWidth - child.margins().horizontal());
            int childWidth = resolveWidth(child, childAvailableWidth);
            int childHeight = resolveHeight(child, childAvailableWidth);
            child.mount(contentX + child.margins().left(), nextY + child.margins().top(), childWidth, childHeight);
            nextY += childHeight + child.margins().vertical() + (i < visible.size() - 1 ? gap : 0);
        }

        return nextY - (y + padding.top()) + padding.bottom();
    }

    private void syncScrollState() {
        scroll.setBounds(x, y, width, height);
        scroll.setContentHeight(contentHeight);
    }

    private int resolveWidth(BaseComponent<?> child, int availableWidth) {
        return switch (child.horizontalSizing()) {
            case Sizing.Fixed   f -> Math.max(0, f.value());
            case Sizing.Fill    ignored -> availableWidth;
            case Sizing.Content ignored -> Math.max(0, child.determineHorizontalContentSize(availableWidth));
        };
    }

    private int resolveHeight(BaseComponent<?> child, int availableWidth) {
        return switch (child.verticalSizing()) {
            case Sizing.Fixed   f -> Math.max(0, f.value());
            case Sizing.Content ignored -> Math.max(0, child.determineVerticalContentSize(availableWidth));
            case Sizing.Fill    ignored -> Math.max(0, child.determineVerticalContentSize(availableWidth));
        };
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        syncScrollState();
        scroll.update(delta);
        scroll.enableScissor(g);

        int scrollOffset = scroll.getScrollOffset();
        int adjustedMouseY = mouseY + scrollOffset;

        g.pose().pushMatrix();
        g.pose().translate(0f, -(float) scrollOffset);

        for (BaseComponent<?> child : children) {
            if (child.isVisible() && !child.rendersAboveSiblings()) {
                child.extractRenderState(g, mouseX, adjustedMouseY, delta);
            }
        }

        for (BaseComponent<?> child : children) {
            if (child.isVisible() && child.rendersAboveSiblings()) {
                child.extractRenderState(g, mouseX, adjustedMouseY, delta);
            }
        }

        g.pose().popMatrix();
        scroll.disableScissor(g);
        scroll.renderScrollbar(g, mouseX, mouseY);
    }

    // ── Scroll helpers ────────────────────────────────────────

    public int getScrollOffset()  { return scroll.getScrollOffset(); }
    public void scrollTo(int pos) { scroll.scrollTo(pos); }
    public void resetScroll()     { scroll.resetScroll(); }
    public ScrollBehavior getScrollBehavior() { return scroll; }

    private static final int SCROLLBAR_RESERVED = 8;
}
