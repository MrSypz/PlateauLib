package com.sypztep.plateau.client.v2.ui.layout;

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
 * A vertical or horizontal flex container. Children declare their size via {@link Sizing}:
 * <ul>
 *   <li>{@code Sizing.fixed(n)} — always n pixels</li>
 *   <li>{@code Sizing.fill()} — takes a proportional share of remaining space</li>
 *   <li>{@code Sizing.content()} — shrinks to fit its own content</li>
 * </ul>
 * Cross-axis (the non-flow axis) defaults to fill the container width/height. Override
 * with a fixed or content sizing on the child's cross axis.
 */
@Environment(EnvType.CLIENT)
public class FlowLayout extends BaseComponent {

    public enum Direction { HORIZONTAL, VERTICAL }

    public enum Align { START, CENTER, END }

    private final Direction direction;
    private final List<BaseComponent> children = new ArrayList<>();
    private int gap = 0;
    private Align crossAlign = Align.START;

    public FlowLayout(Direction direction, Sizing horizontal, Sizing vertical) {
        this.direction        = direction;
        this.horizontalSizing = horizontal;
        this.verticalSizing   = vertical;
    }

    // ── Child management ─────────────────────────────────────

    public FlowLayout child(BaseComponent child) {
        children.add(child);
        return this;
    }

    public FlowLayout children(BaseComponent... components) {
        Collections.addAll(children, components);
        return this;
    }

    public FlowLayout children(Iterable<? extends BaseComponent> components) {
        for (BaseComponent c : components) children.add(c);
        return this;
    }

    public FlowLayout gap(int gap)               { this.gap = gap; return this; }
    public FlowLayout crossAlign(Align align)    { this.crossAlign = align; return this; }
    public List<BaseComponent> getChildren()     { return children; }

    // Override fluent base methods to preserve FlowLayout return type in chains
    @Override public FlowLayout padding(Insets padding)   { super.padding(padding);  return this; }
    @Override public FlowLayout margins(Insets margins)   { super.margins(margins);  return this; }
    @Override public FlowLayout surface(Surface surface)  { super.surface(surface);  return this; }
    @Override public FlowLayout id(String id)             { super.id(id);            return this; }
    @Override public FlowLayout visible(boolean visible)  { super.visible(visible);  return this; }
    @Override public FlowLayout sizing(Sizing h, Sizing v){ super.sizing(h, v);      return this; }
    @Override public FlowLayout sizing(Sizing both)       { super.sizing(both);      return this; }

    // ── Layout ───────────────────────────────────────────────

    @Override
    protected void onMounted() {
        layout();
    }

    private void layout() {
        if (direction == Direction.VERTICAL) {
            layoutVertical();
        } else {
            layoutHorizontal();
        }
    }

    private void layoutVertical() {
        int innerX = x + padding.left();
        int innerY = y + padding.top();
        int innerW = width  - padding.horizontal();
        int innerH = height - padding.vertical();

        List<BaseComponent> vis = visible();
        if (vis.isEmpty()) return;

        int totalGaps      = gap * (vis.size() - 1);
        int fixedH         = totalGaps;
        int totalFillWeight = 0;

        int[] heights = new int[vis.size()];
        int[] widths  = new int[vis.size()];

        // First pass — measure fixed & content children
        for (int i = 0; i < vis.size(); i++) {
            BaseComponent c = vis.get(i);
            int childAvailW = innerW - c.margins().horizontal();
            widths[i] = resolveWidth(c, childAvailW);

            switch (c.verticalSizing()) {
                case Sizing.Fixed  f -> { heights[i] = f.value(); fixedH += f.value() + c.margins().vertical(); }
                case Sizing.Content ignored -> {
                    heights[i] = c.determineVerticalContentSize(childAvailW);
                    fixedH += heights[i] + c.margins().vertical();
                }
                case Sizing.Fill   f -> totalFillWeight += f.weight();
            }
        }

        // Distribute remaining space to fill children
        int remaining  = Math.max(0, innerH - fixedH);
        float fillUnit = totalFillWeight > 0 ? (float) remaining / totalFillWeight : 0;

        for (int i = 0; i < vis.size(); i++) {
            if (vis.get(i).verticalSizing() instanceof Sizing.Fill(int weight)) {
                heights[i] = Math.max(0, (int)(weight * fillUnit));
            }
        }

        // Position each child
        int curY = innerY;
        for (int i = 0; i < vis.size(); i++) {
            BaseComponent c = vis.get(i);
            int childW = widths[i];
            int childH = heights[i];
            int childX = resolveChildX(innerX, innerW, c, childW);
            int childY = curY + c.margins().top();
            c.mount(childX, childY, childW, childH);
            curY += childH + c.margins().vertical() + (i < vis.size() - 1 ? gap : 0);
        }
    }

    private void layoutHorizontal() {
        int innerX = x + padding.left();
        int innerY = y + padding.top();
        int innerW = width  - padding.horizontal();
        int innerH = height - padding.vertical();

        List<BaseComponent> vis = visible();
        if (vis.isEmpty()) return;

        int totalGaps       = gap * (vis.size() - 1);
        int fixedW          = totalGaps;
        int totalFillWeight = 0;

        int[] widths  = new int[vis.size()];
        int[] heights = new int[vis.size()];

        for (int i = 0; i < vis.size(); i++) {
            BaseComponent c = vis.get(i);
            int childAvailH = innerH - c.margins().vertical();
            heights[i] = resolveHeight(c, childAvailH, innerW - c.margins().horizontal());

            switch (c.horizontalSizing()) {
                case Sizing.Fixed  f -> { widths[i] = f.value(); fixedW += f.value() + c.margins().horizontal(); }
                case Sizing.Content ignored -> {
                    widths[i] = c.determineHorizontalContentSize(innerH);
                    fixedW += widths[i] + c.margins().horizontal();
                }
                case Sizing.Fill   f -> totalFillWeight += f.weight();
            }
        }

        int remaining  = Math.max(0, innerW - fixedW);
        float fillUnit = totalFillWeight > 0 ? (float) remaining / totalFillWeight : 0;

        for (int i = 0; i < vis.size(); i++) {
            if (vis.get(i).horizontalSizing() instanceof Sizing.Fill(int weight)) {
                widths[i] = Math.max(0, (int)(weight * fillUnit));
            }
        }

        int curX = innerX;
        for (int i = 0; i < vis.size(); i++) {
            BaseComponent c = vis.get(i);
            int childW = widths[i];
            int childH = heights[i];
            int childX = curX + c.margins().left();
            int childY = resolveChildY(innerY, innerH, c, childH);
            c.mount(childX, childY, childW, childH);
            curX += childW + c.margins().horizontal() + (i < vis.size() - 1 ? gap : 0);
        }
    }

    // ── Sizing helpers ────────────────────────────────────────

    private int resolveWidth(BaseComponent c, int availW) {
        return switch (c.horizontalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Fill    ignored -> availW; // fill cross-axis = full available
            case Sizing.Content ignored -> c.determineHorizontalContentSize(availW);
        };
    }

    private int resolveHeight(BaseComponent c, int availH, int availW) {
        return switch (c.verticalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Fill    ignored -> availH;
            case Sizing.Content ignored -> c.determineVerticalContentSize(availW);
        };
    }

    private int resolveChildX(int innerX, int innerW, BaseComponent c, int childW) {
        int margin = c.margins().left();
        return switch (crossAlign) {
            case START  -> innerX + margin;
            case CENTER -> innerX + (innerW - childW - c.margins().horizontal()) / 2 + margin;
            case END    -> innerX + innerW - childW - c.margins().right();
        };
    }

    private int resolveChildY(int innerY, int innerH, BaseComponent c, int childH) {
        int margin = c.margins().top();
        return switch (crossAlign) {
            case START  -> innerY + margin;
            case CENTER -> innerY + (innerH - childH - c.margins().vertical()) / 2 + margin;
            case END    -> innerY + innerH - childH - c.margins().bottom();
        };
    }

    private List<BaseComponent> visible() {
        return children.stream().filter(BaseComponent::isVisible).toList();
    }

    // ── Content size (for Sizing.content() on this FlowLayout) ─

    @Override
    public int determineVerticalContentSize(int availableWidth) {
        int innerW = availableWidth - padding.horizontal();
        List<BaseComponent> vis = visible();
        if (vis.isEmpty()) return padding.vertical();

        if (direction == Direction.VERTICAL) {
            int total = padding.vertical() + gap * Math.max(0, vis.size() - 1);
            for (BaseComponent c : vis) {
                int childAvailW = innerW - c.margins().horizontal();
                total += childContentH(c, childAvailW) + c.margins().vertical();
            }
            return total;
        } else {
            // For horizontal layout, height = max child height + padding
            int maxH = 0;
            for (BaseComponent c : vis) {
                int childAvailW = innerW - c.margins().horizontal();
                maxH = Math.max(maxH, childContentH(c, childAvailW) + c.margins().vertical());
            }
            return maxH + padding.vertical();
        }
    }

    @Override
    public int determineHorizontalContentSize(int availableHeight) {
        int innerH = availableHeight - padding.vertical();
        List<BaseComponent> vis = visible();
        if (vis.isEmpty()) return padding.horizontal();

        if (direction == Direction.HORIZONTAL) {
            int total = padding.horizontal() + gap * Math.max(0, vis.size() - 1);
            for (BaseComponent c : vis) {
                int childAvailH = innerH - c.margins().vertical();
                total += childContentW(c, childAvailH) + c.margins().horizontal();
            }
            return total;
        } else {
            int maxW = 0;
            for (BaseComponent c : vis) {
                int childAvailH = innerH - c.margins().vertical();
                maxW = Math.max(maxW, childContentW(c, childAvailH) + c.margins().horizontal());
            }
            return maxW + padding.horizontal();
        }
    }

    private static int childContentH(BaseComponent c, int availW) {
        return switch (c.verticalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Content ignored -> c.determineVerticalContentSize(availW);
            case Sizing.Fill    ignored -> 0;
        };
    }

    private static int childContentW(BaseComponent c, int availH) {
        return switch (c.horizontalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Content ignored -> c.determineHorizontalContentSize(availH);
            case Sizing.Fill    ignored -> 0;
        };
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        for (BaseComponent child : children) {
            if (child.isVisible()) {
                child.extractRenderState(g, mouseX, mouseY, delta);
            }
        }
    }

    // ── Input — forward to children ───────────────────────────

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent c = children.get(i);
            if (c.isVisible() && c.mouseClicked(event, doubleClick)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        for (BaseComponent c : children) {
            if (c.isVisible()) c.mouseReleased(event);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        for (BaseComponent c : children) {
            if (c.isVisible() && c.mouseDragged(event, dx, dy)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent c = children.get(i);
            if (c.isVisible() && c.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        for (BaseComponent c : children) {
            if (c.isVisible() && c.keyPressed(keyEvent)) return true;
        }
        return false;
    }
}
