package com.sypztep.plateau.client.v2.ui.layout;

import com.sypztep.plateau.client.v2.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * A vertical or horizontal flex container. Children declare their size via {@link Sizing}:
 * <ul>
 *   <li>{@code Sizing.fixed(n)} — always n pixels</li>
 *   <li>{@code Sizing.fill()} — takes a proportional share of remaining space</li>
 *   <li>{@code Sizing.content()} — shrinks to fit its own content</li>
 * </ul>
 * Inherits event routing from {@link BaseContainerComponent}, so nested components
 * receive mouse and keyboard events without one-off forwarding in each layout.
 */
@Environment(EnvType.CLIENT)
public class FlowLayout extends BaseContainerComponent<FlowLayout> {

    public enum Direction { HORIZONTAL, VERTICAL }

    public enum Align { START, CENTER, END }

    private final Direction direction;
    private int gap = 0;
    private Align crossAlign = Align.START;

    public FlowLayout(Direction direction, Sizing horizontal, Sizing vertical) {
        this.direction        = direction;
        this.horizontalSizing = horizontal;
        this.verticalSizing   = vertical;
    }

    public FlowLayout gap(int gap)               { this.gap = gap; return this; }
    public FlowLayout crossAlign(Align align)    { this.crossAlign = align; return this; }

    // ── Layout ───────────────────────────────────────────────

    @Override
    protected void onMounted() {
        layout();
    }

    private void layout() {
        List<BaseComponent<?>> visibleChildren = visibleChildren();
        if (visibleChildren.isEmpty()) return;

        if (direction == Direction.VERTICAL) {
            layoutVertical(visibleChildren);
        } else {
            layoutHorizontal(visibleChildren);
        }
    }

    private void layoutVertical(List<BaseComponent<?>> visibleChildren) {
        int contentX = innerX();
        int contentY = innerY();
        int contentWidth = innerWidth();
        int contentHeight = innerHeight();

        int fixedHeight = totalGapSize(visibleChildren);
        int totalFillWeight = 0;

        int[] childHeights = new int[visibleChildren.size()];
        int[] childWidths = new int[visibleChildren.size()];

        for (int index = 0; index < visibleChildren.size(); index++) {
            BaseComponent<?> child = visibleChildren.get(index);
            int childAvailableWidth = Math.max(0, contentWidth - child.margins().horizontal());
            childWidths[index] = resolveWidth(child, childAvailableWidth);

            switch (child.verticalSizing()) {
                case Sizing.Fixed fixed -> {
                    childHeights[index] = fixed.value();
                    fixedHeight += fixed.value() + child.margins().vertical();
                }
                case Sizing.Content ignored -> {
                    childHeights[index] = child.determineVerticalContentSize(childAvailableWidth);
                    fixedHeight += childHeights[index] + child.margins().vertical();
                }
                case Sizing.Fill fill -> totalFillWeight += Math.max(0, fill.weight());
            }
        }

        distributeFillSizes(visibleChildren, childHeights, Math.max(0, contentHeight - fixedHeight), totalFillWeight, Direction.VERTICAL);

        int nextY = contentY;
        for (int index = 0; index < visibleChildren.size(); index++) {
            BaseComponent<?> child = visibleChildren.get(index);
            int childX = resolveChildX(contentX, contentWidth, child, childWidths[index]);
            child.mount(childX, nextY + child.margins().top(), childWidths[index], childHeights[index]);
            nextY += childHeights[index] + child.margins().vertical() + gapAfter(visibleChildren, index);
        }
    }

    private void layoutHorizontal(List<BaseComponent<?>> visibleChildren) {
        int contentX = innerX();
        int contentY = innerY();
        int contentWidth = innerWidth();
        int contentHeight = innerHeight();

        int fixedWidth = totalGapSize(visibleChildren);
        int totalFillWeight = 0;

        int[] childWidths = new int[visibleChildren.size()];
        int[] childHeights = new int[visibleChildren.size()];

        for (int index = 0; index < visibleChildren.size(); index++) {
            BaseComponent<?> child = visibleChildren.get(index);
            int childAvailableHeight = Math.max(0, contentHeight - child.margins().vertical());
            int childAvailableWidth = Math.max(0, contentWidth - child.margins().horizontal());
            childHeights[index] = resolveHeight(child, childAvailableHeight, childAvailableWidth);

            switch (child.horizontalSizing()) {
                case Sizing.Fixed fixed -> {
                    childWidths[index] = fixed.value();
                    fixedWidth += fixed.value() + child.margins().horizontal();
                }
                case Sizing.Content ignored -> {
                    childWidths[index] = child.determineHorizontalContentSize(childAvailableWidth);
                    fixedWidth += childWidths[index] + child.margins().horizontal();
                }
                case Sizing.Fill fill -> totalFillWeight += Math.max(0, fill.weight());
            }
        }

        distributeFillSizes(visibleChildren, childWidths, Math.max(0, contentWidth - fixedWidth), totalFillWeight, Direction.HORIZONTAL);

        int nextX = contentX;
        for (int index = 0; index < visibleChildren.size(); index++) {
            BaseComponent<?> child = visibleChildren.get(index);
            int childY = resolveChildY(contentY, contentHeight, child, childHeights[index]);
            child.mount(nextX + child.margins().left(), childY, childWidths[index], childHeights[index]);
            nextX += childWidths[index] + child.margins().horizontal() + gapAfter(visibleChildren, index);
        }
    }

    private void distributeFillSizes(List<BaseComponent<?>> visibleChildren, int[] mainAxisSizes, int remaining, int totalFillWeight, Direction fillDirection) {
        float fillUnit = totalFillWeight > 0 ? (float) remaining / totalFillWeight : 0;

        for (int index = 0; index < visibleChildren.size(); index++) {
            Sizing sizing = fillDirection == Direction.VERTICAL
                    ? visibleChildren.get(index).verticalSizing()
                    : visibleChildren.get(index).horizontalSizing();
            if (sizing instanceof Sizing.Fill(int weight)) {
                mainAxisSizes[index] = Math.max(0, (int)(Math.max(0, weight) * fillUnit));
            }
        }
    }

    private int totalGapSize(List<BaseComponent<?>> visibleChildren) {
        return gap * Math.max(0, visibleChildren.size() - 1);
    }

    private int gapAfter(List<BaseComponent<?>> visibleChildren, int index) {
        return index < visibleChildren.size() - 1 ? gap : 0;
    }

    // ── Sizing helpers ────────────────────────────────────────

    private int resolveWidth(BaseComponent<?> child, int availableWidth) {
        return switch (child.horizontalSizing()) {
            case Sizing.Fixed fixed -> fixed.value();
            case Sizing.Fill ignored -> availableWidth;
            case Sizing.Content ignored -> child.determineHorizontalContentSize(availableWidth);
        };
    }

    private int resolveHeight(BaseComponent<?> child, int availableHeight, int availableWidth) {
        return switch (child.verticalSizing()) {
            case Sizing.Fixed fixed -> fixed.value();
            case Sizing.Fill ignored -> availableHeight;
            case Sizing.Content ignored -> child.determineVerticalContentSize(availableWidth);
        };
    }

    private int resolveChildX(int contentX, int contentWidth, BaseComponent<?> child, int childWidth) {
        int leadingMargin = child.margins().left();
        return switch (crossAlign) {
            case START -> contentX + leadingMargin;
            case CENTER -> contentX + (contentWidth - childWidth - child.margins().horizontal()) / 2 + leadingMargin;
            case END -> contentX + contentWidth - childWidth - child.margins().right();
        };
    }

    private int resolveChildY(int contentY, int contentHeight, BaseComponent<?> child, int childHeight) {
        int leadingMargin = child.margins().top();
        return switch (crossAlign) {
            case START -> contentY + leadingMargin;
            case CENTER -> contentY + (contentHeight - childHeight - child.margins().vertical()) / 2 + leadingMargin;
            case END -> contentY + contentHeight - childHeight - child.margins().bottom();
        };
    }

    private List<BaseComponent<?>> visibleChildren() {
        return children.stream().filter(BaseComponent::isVisible).toList();
    }

    @Override
    public int determineVerticalContentSize(int availableWidth) {
        int contentWidth = Math.max(0, availableWidth - padding.horizontal());
        List<BaseComponent<?>> visibleChildren = visibleChildren();
        if (visibleChildren.isEmpty()) return padding.vertical();

        if (direction == Direction.VERTICAL) {
            int total = padding.vertical() + totalGapSize(visibleChildren);
            for (BaseComponent<?> child : visibleChildren) {
                int childAvailableWidth = Math.max(0, contentWidth - child.margins().horizontal());
                total += childContentH(child, childAvailableWidth) + child.margins().vertical();
            }
            return total;
        } else {
            int maxHeight = 0;
            for (BaseComponent<?> child : visibleChildren) {
                int childAvailableWidth = Math.max(0, contentWidth - child.margins().horizontal());
                maxHeight = Math.max(maxHeight, childContentH(child, childAvailableWidth) + child.margins().vertical());
            }
            return maxHeight + padding.vertical();
        }
    }

    @Override
    public int determineHorizontalContentSize(int availableWidth) {
        int contentWidth = Math.max(0, availableWidth - padding.horizontal());
        List<BaseComponent<?>> visibleChildren = visibleChildren();
        if (visibleChildren.isEmpty()) return padding.horizontal();

        if (direction == Direction.HORIZONTAL) {
            int total = padding.horizontal() + totalGapSize(visibleChildren);
            for (BaseComponent<?> child : visibleChildren) {
                int childAvailableWidth = Math.max(0, contentWidth - child.margins().horizontal());
                total += childContentW(child, childAvailableWidth) + child.margins().horizontal();
            }
            return total;
        } else {
            int maxWidth = 0;
            for (BaseComponent<?> child : visibleChildren) {
                int childAvailableWidth = Math.max(0, contentWidth - child.margins().horizontal());
                maxWidth = Math.max(maxWidth, childContentW(child, childAvailableWidth) + child.margins().horizontal());
            }
            return maxWidth + padding.horizontal();
        }
    }

    private static int childContentH(BaseComponent<?> child, int availableWidth) {
        return switch (child.verticalSizing()) {
            case Sizing.Fixed fixed -> fixed.value();
            case Sizing.Content ignored -> child.determineVerticalContentSize(availableWidth);
            case Sizing.Fill ignored -> 0;
        };
    }

    private static int childContentW(BaseComponent<?> child, int availableWidth) {
        return switch (child.horizontalSizing()) {
            case Sizing.Fixed fixed -> fixed.value();
            case Sizing.Content ignored -> child.determineHorizontalContentSize(availableWidth);
            case Sizing.Fill ignored -> 0;
        };
    }

    @Override
    public int renderClipTopOutset() {
        return children.stream()
                .filter(BaseComponent::isVisible)
                .mapToInt(BaseComponent::renderClipTopOutset)
                .max()
                .orElse(0);
    }

    @Override
    public int renderClipBottomOutset() {
        return children.stream()
                .filter(BaseComponent::isVisible)
                .mapToInt(BaseComponent::renderClipBottomOutset)
                .max()
                .orElse(0);
    }

    @Override
    public boolean rendersAboveSiblings() {
        return children.stream()
                .filter(BaseComponent::isVisible)
                .anyMatch(BaseComponent::rendersAboveSiblings);
    }

    @Override
    public boolean blocksLowerInput() {
        return children.stream()
                .filter(BaseComponent::isVisible)
                .anyMatch(BaseComponent::blocksLowerInput);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || hasOpenChildAt(mouseX, mouseY);
    }

    @Override
    public boolean hitTest(double x, double y) {
        return super.hitTest(x, y) || hasOpenChildAt(x, y);
    }

    private boolean hasOpenChildAt(double x, double y) {
        for (BaseComponent<?> child : children) {
            if (child.isVisible() && child.rendersAboveSiblings() && child.bounds().containsPoint(Mth.floor(x), Mth.floor(y))) {
                return true;
            }
        }
        return false;
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        for (BaseComponent<?> child : children) {
            if (child.isVisible() && !child.rendersAboveSiblings()) {
                extractChild(g, child, mouseX, mouseY, delta);
            }
        }

        for (BaseComponent<?> child : children) {
            if (child.isVisible() && child.rendersAboveSiblings()) {
                extractChild(g, child, mouseX, mouseY, delta);
            }
        }
    }

    private void extractChild(GuiGraphicsExtractor g, BaseComponent<?> child, int mouseX, int mouseY, float delta) {
        enableChildScissor(g, child);
        child.extractRenderState(g, mouseX, mouseY, delta);
        g.disableScissor();
    }

    private void enableChildScissor(GuiGraphicsExtractor g, BaseComponent<?> child) {
        int clipLeft = Math.max(x, child.x() - child.renderClipLeftOutset());
        int clipTop = Math.max(y - child.renderClipTopOutset(), child.y() - child.renderClipTopOutset());
        int clipRight = Math.min(x + width, child.x() + child.width() + child.renderClipRightOutset());
        int clipBottom = Math.min(y + height + child.renderClipBottomOutset(), child.y() + child.height() + child.renderClipBottomOutset());
        g.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
    }
}
