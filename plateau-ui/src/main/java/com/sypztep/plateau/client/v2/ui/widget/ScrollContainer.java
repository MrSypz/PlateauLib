package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.behavior.ScrollBehavior;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A vertically scrollable container. Children are stacked top-to-bottom, each sized by their own
 * {@link Sizing}. Fill children take a proportional share of whatever space they collectively need —
 * but since content can overflow, fill in a ScrollContainer means "fill the viewport height shared
 * among fill siblings", which is usually not what you want. Prefer fixed or content sizing here.
 *
 * <p>Usage:
 * <pre>{@code
 * Components.scrollable(Sizing.fill(), Sizing.fill())
 *     .child(Components.label(...))
 *     .child(Components.button(...))
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class ScrollContainer extends BaseComponent {

    private final List<BaseComponent> children = new ArrayList<>();
    private int gap = 0;
    private int contentHeight = 0;
    private final ScrollBehavior scroll = new ScrollBehavior();

    public ScrollContainer(Sizing horizontal, Sizing vertical) {
        this.horizontalSizing = horizontal;
        this.verticalSizing   = vertical;
    }

    // ── Child management ─────────────────────────────────────

    public ScrollContainer child(BaseComponent child) {
        children.add(child);
        return this;
    }

    public ScrollContainer children(BaseComponent... components) {
        Collections.addAll(children, components);
        return this;
    }

    public ScrollContainer children(Iterable<? extends BaseComponent> components) {
        for (BaseComponent c : components) children.add(c);
        return this;
    }

    public ScrollContainer gap(int gap) { this.gap = gap; return this; }

    // Override fluent base methods to preserve ScrollContainer return type in chains
    @Override public ScrollContainer padding(Insets padding)  { super.padding(padding); return this; }
    @Override public ScrollContainer margins(Insets margins)  { super.margins(margins); return this; }
    @Override public ScrollContainer surface(Surface surface) { super.surface(surface); return this; }
    @Override public ScrollContainer id(String id)            { super.id(id);           return this; }
    @Override public ScrollContainer visible(boolean visible) { super.visible(visible); return this; }
    @Override public ScrollContainer sizing(Sizing h, Sizing v){ super.sizing(h, v);    return this; }
    @Override public ScrollContainer sizing(Sizing both)      { super.sizing(both);     return this; }

    // ── Layout ───────────────────────────────────────────────

    @Override
    protected void onMounted() {
        layoutChildren();
    }

    private void layoutChildren() {
        int innerX = x + padding.left();
        int innerW = width - padding.horizontal() - SCROLLBAR_RESERVED;

        int curY = y + padding.top();
        for (int i = 0; i < children.size(); i++) {
            BaseComponent c = children.get(i);
            if (!c.isVisible()) continue;

            int childAvailW = innerW - c.margins().horizontal();
            int childW = resolveWidth(c, childAvailW);
            int childH = resolveHeight(c, childAvailW);
            int childX = innerX + c.margins().left();
            int childY = curY + c.margins().top();

            c.mount(childX, childY, childW, childH);
            curY += childH + c.margins().vertical() + (i < children.size() - 1 ? gap : 0);
        }

        contentHeight = curY - (y + padding.top()) + padding.bottom();
    }

    private int resolveWidth(BaseComponent c, int availW) {
        return switch (c.horizontalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Fill    ignored -> availW;
            case Sizing.Content ignored -> c.determineHorizontalContentSize(availW);
        };
    }

    private int resolveHeight(BaseComponent c, int availW) {
        return switch (c.verticalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Content ignored -> c.determineVerticalContentSize(availW);
            case Sizing.Fill    ignored -> c.determineVerticalContentSize(availW); // no fill in scroll
        };
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        scroll.setBounds(x, y, width, height);
        scroll.setContentHeight(contentHeight);
        scroll.update(delta);
        scroll.enableScissor(g);

        int scrollOffset = scroll.getScrollOffset();
        int adjustedMouseY = mouseY + scrollOffset;

        g.pose().pushMatrix();
        g.pose().translate(0f, -(float) scrollOffset);

        for (BaseComponent child : children) {
            if (child.isVisible()) {
                child.extractRenderState(g, mouseX, adjustedMouseY, delta);
            }
        }

        g.pose().popMatrix();

        scroll.disableScissor(g);
        scroll.renderScrollbar(g, mouseX, mouseY);
    }

    // ── Input ─────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        return scroll.mouseScrolled(mouseX, mouseY, vAmount);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;
        if (scroll.mouseClicked(event, doubleClick)) return true;

        int scrollOffset = scroll.getScrollOffset();
        double contentY  = event.y() + scrollOffset;

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent child = children.get(i);
            if (!child.isVisible()) continue;
            if (event.x() >= child.x() && event.x() < child.x() + child.width()
                    && contentY >= child.y() && contentY < child.y() + child.height()) {
                if (child.mouseClicked(event, doubleClick)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        scroll.mouseReleased(event);
        for (BaseComponent child : children) {
            if (child.isVisible()) child.mouseReleased(event);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (scroll.mouseDragged(event)) return true;
        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (!focused) return false;
        int key = keyEvent.key();
        if (key == 265) { scroll.scrollBy(-20);           return true; } // UP
        if (key == 264) { scroll.scrollBy( 20);           return true; } // DOWN
        if (key == 266) { scroll.scrollBy(-(height - 20)); return true; } // PAGE_UP
        if (key == 267) { scroll.scrollBy( height - 20);  return true; } // PAGE_DOWN
        if (key == 268) { scroll.scrollTo(0);             return true; } // HOME
        if (key == 269) { scroll.scrollToEnd();           return true; } // END
        return false;
    }

    @Override
    protected boolean isFocusable() { return true; }

    // ── Scroll helpers ────────────────────────────────────────

    public int getScrollOffset()  { return scroll.getScrollOffset(); }
    public void scrollTo(int pos) { scroll.scrollTo(pos); }
    public void resetScroll()     { scroll.resetScroll(); }
    public ScrollBehavior getScrollBehavior() { return scroll; }

    // default matches ScrollBehavior defaults (width=6, padding=2)
    private static final int SCROLLBAR_RESERVED = 8;
}
