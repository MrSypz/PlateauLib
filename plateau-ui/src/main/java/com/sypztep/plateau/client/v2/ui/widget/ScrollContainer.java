package com.sypztep.plateau.client.v2.ui.widget;

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
@Deprecated(forRemoval = false)
public class ScrollContainer extends BaseContainerComponent {
    private int gap = 0;
    private int contentHeight = 0;
    private final ScrollBehavior scroll = new ScrollBehavior();

    public ScrollContainer(Sizing horizontal, Sizing vertical) {
        this.horizontalSizing = horizontal;
        this.verticalSizing   = vertical;
    }

    // ── Child management ─────────────────────────────────────

    @Override
    public ScrollContainer child(BaseComponent child) {
        super.child(child);
        return this;
    }

    @Override
    public ScrollContainer children(BaseComponent... components) {
        super.children(components);
        return this;
    }

    @Override
    public ScrollContainer children(Iterable<? extends BaseComponent> components) {
        super.children(components);
        return this;
    }

    public ScrollContainer gap(int gap) { this.gap = gap; return this; }

    @Override public ScrollContainer padding(Insets padding)  { super.padding(padding); return this; }
    @Override public ScrollContainer margins(Insets margins)  { super.margins(margins); return this; }
    @Override public ScrollContainer surface(Surface surface) { super.surface(surface); return this; }
    @Override public ScrollContainer id(String id)            { super.id(id);           return this; }
    @Override public ScrollContainer visible(boolean visible) { super.visible(visible); return this; }
    @Override public ScrollContainer sizing(Sizing h, Sizing v){ super.sizing(h, v);    return this; }
    @Override public ScrollContainer sizing(Sizing both)      { super.sizing(both);     return this; }

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
            BaseComponent child = children.get(i);
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
            BaseComponent child = children.get(i);
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
            BaseComponent child = children.get(i);
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
            BaseComponent child = children.get(i);
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
            BaseComponent child = children.get(i);
            if (!child.isVisible() || !child.rendersAboveSiblings()) continue;

            if (child.hitTest(mouseX, contentY)) {
                if (child.mouseScrolled(mouseX, contentY, hAmount, vAmount)) return true;
                if (child.blocksLowerInput()) return true;
            }
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent child = children.get(i);
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
        for (BaseComponent c : children) {
            if (c.isVisible()) c.mouseMoved(mouseX, adjustedY);
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
        int innerX = x + padding.left();
        int innerW = Math.max(0, width - padding.horizontal());

        contentHeight = layoutChildren(innerX, innerW);
        if (contentHeight > height) {
            contentHeight = layoutChildren(innerX, Math.max(0, innerW - SCROLLBAR_RESERVED));
        }

        syncScrollState();
    }

    private int layoutChildren(int innerX, int innerW) {
        List<BaseComponent> visible = children.stream()
                .filter(BaseComponent::isVisible)
                .toList();

        int curY = y + padding.top();
        for (int i = 0; i < visible.size(); i++) {
            BaseComponent c = visible.get(i);

            int childAvailW = Math.max(0, innerW - c.margins().horizontal());
            int childW = resolveWidth(c, childAvailW);
            int childH = resolveHeight(c, childAvailW);
            c.mount(innerX + c.margins().left(), curY + c.margins().top(), childW, childH);
            curY += childH + c.margins().vertical() + (i < visible.size() - 1 ? gap : 0);
        }

        return curY - (y + padding.top()) + padding.bottom();
    }

    private void syncScrollState() {
        scroll.setBounds(x, y, width, height);
        scroll.setContentHeight(contentHeight);
    }

    private int resolveWidth(BaseComponent c, int availW) {
        return switch (c.horizontalSizing()) {
            case Sizing.Fixed   f -> Math.max(0, f.value());
            case Sizing.Fill    ignored -> availW;
            case Sizing.Content ignored -> Math.max(0, c.determineHorizontalContentSize(availW));
        };
    }

    private int resolveHeight(BaseComponent c, int availW) {
        return switch (c.verticalSizing()) {
            case Sizing.Fixed   f -> Math.max(0, f.value());
            case Sizing.Content ignored -> Math.max(0, c.determineVerticalContentSize(availW));
            case Sizing.Fill    ignored -> Math.max(0, c.determineVerticalContentSize(availW));
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

        for (BaseComponent child : children) {
            if (child.isVisible() && !child.rendersAboveSiblings()) {
                child.extractRenderState(g, mouseX, adjustedMouseY, delta);
            }
        }

        for (BaseComponent child : children) {
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
