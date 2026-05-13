package com.sypztep.plateau.client.v2.ui.layout;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.BaseContainerComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;

/**
 * Two-child split container for dashboard-style UI.
 *
 * <p>The split ratio always means "first child size / available size" on the main axis.
 * Use {@link #limits(float, float)} to keep either panel from collapsing.
 */
@Environment(EnvType.CLIENT)
public class SplitLayout extends BaseContainerComponent<SplitLayout> {
    private static final int DEFAULT_HANDLE_SIZE = 5;

    public enum Axis { HORIZONTAL, VERTICAL }

    public enum HoverMode {
        NONE,
        EXPAND_HOVERED_PANEL
    }

    @FunctionalInterface
    public interface SplitAnimator {
        float animate(float current, float target, float delta, float speed);
    }

    private final Axis axis;
    private float split;
    private float animatedSplit;
    private float minSplit = 0.1f;
    private float maxSplit = 0.9f;
    private int handleSize = DEFAULT_HANDLE_SIZE;
    private boolean draggable = true;
    private boolean handleVisible = true;
    private boolean dragging;
    private HoverMode hoverMode = HoverMode.NONE;
    private float hoverExpandFirst = 0.04f;
    private float hoverExpandSecond = 0.04f;
    private float animationSpeed = 0.5f;
    private SplitAnimator animator = SplitLayout::defaultAnimate;

    public SplitLayout(Axis axis, float split) {
        this.axis = axis;
        this.split = clamp01(split);
        this.animatedSplit = this.split;
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing = Sizing.fill();
    }

    public static SplitLayout horizontal(float split) {
        return new SplitLayout(Axis.HORIZONTAL, split);
    }

    public static SplitLayout vertical(float split) {
        return new SplitLayout(Axis.VERTICAL, split);
    }

    public SplitLayout split(float split) {
        this.split = clamp(split);
        this.animatedSplit = this.split;
        return this;
    }

    public SplitLayout limits(float minSplit, float maxSplit) {
        this.minSplit = clamp01(Math.min(minSplit, maxSplit));
        this.maxSplit = clamp01(Math.max(minSplit, maxSplit));
        this.split = clamp(split);
        this.animatedSplit = clamp(animatedSplit);
        return this;
    }

    public SplitLayout handleSize(int handleSize) {
        this.handleSize = Math.max(0, handleSize);
        return this;
    }

    public SplitLayout draggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public SplitLayout handleVisible(boolean handleVisible) {
        this.handleVisible = handleVisible;
        return this;
    }

    public SplitLayout hoverExpand(float amount) {
        return hoverExpand(amount, amount);
    }

    public SplitLayout hoverExpand(float firstPanelAmount, float secondPanelAmount) {
        this.hoverMode = HoverMode.EXPAND_HOVERED_PANEL;
        this.hoverExpandFirst = Math.max(0f, firstPanelAmount);
        this.hoverExpandSecond = Math.max(0f, secondPanelAmount);
        return this;
    }

    public SplitLayout hoverMode(HoverMode hoverMode) {
        this.hoverMode = hoverMode == null ? HoverMode.NONE : hoverMode;
        return this;
    }

    /**
     * Controls how quickly {@code animatedSplit} approaches the target split.
     * The value is multiplied by render {@code delta}.
     */
    public SplitLayout animationSpeed(float animationSpeed) {
        this.animationSpeed = Math.max(0f, animationSpeed);
        return this;
    }

    /**
     * Advanced hook for custom animation. The callback receives current, target,
     * render delta, and the configured speed.
     */
    public SplitLayout animator(SplitAnimator animator) {
        this.animator = animator == null ? SplitLayout::defaultAnimate : animator;
        return this;
    }

    @Override
    protected void onMounted() {
        layoutChildren(animatedSplit);
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        float targetSplit = dragging ? split : hoverTarget(mouseX, mouseY);
        animatedSplit = clamp(animator.animate(animatedSplit, targetSplit, delta, animationSpeed));
        layoutChildren(animatedSplit);

        for (BaseComponent<?> child : children) {
            if (child.isVisible()) child.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        if (handleVisible && handleSize > 0) drawHandle(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (draggable && event.button() == 0 && isOverHandle(event.x(), event.y())) {
            dragging = true;
            setDragging(true);
            UISounds.playClick();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            updateSplitFromMouse(event.x(), event.y());
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (event.button() == 0 && dragging) {
            dragging = false;
            setDragging(false);
            return true;
        }

        return super.mouseReleased(event);
    }

    private float hoverTarget(double mouseX, double mouseY) {
        if (hoverMode == HoverMode.NONE || !isMouseOver(mouseX, mouseY)) return split;

        if (axis == Axis.HORIZONTAL) {
            int handleX = handleX();
            if (mouseX < handleX) return clamp(split + hoverExpandFirst);
            if (mouseX > handleX + handleSize) return clamp(split - hoverExpandSecond);
            return split;
        }

        int handleY = handleY();
        if (mouseY < handleY) return clamp(split + hoverExpandFirst);
        if (mouseY > handleY + handleSize) return clamp(split - hoverExpandSecond);
        return split;
    }

    private void updateSplitFromMouse(double mouseX, double mouseY) {
        int available = axis == Axis.HORIZONTAL
                ? Math.max(1, innerWidth() - handleSize)
                : Math.max(1, innerHeight() - handleSize);
        float raw = axis == Axis.HORIZONTAL
                ? (float) ((mouseX - innerX()) / available)
                : (float) ((mouseY - innerY()) / available);
        split = clamp(raw);
        animatedSplit = split;
        layoutChildren(animatedSplit);
    }

    private void layoutChildren(float ratio) {
        if (children.size() < 2) return;

        BaseComponent<?> first = children.get(0);
        BaseComponent<?> second = children.get(1);
        int contentX = innerX();
        int contentY = innerY();
        int contentWidth = innerWidth();
        int contentHeight = innerHeight();

        if (axis == Axis.HORIZONTAL) {
            int availableWidth = Math.max(0, contentWidth - handleSize);
            int firstWidth = Math.round(availableWidth * clamp(ratio));
            int secondWidth = Math.max(0, availableWidth - firstWidth);
            first.mount(contentX, contentY, firstWidth, contentHeight);
            second.mount(contentX + firstWidth + handleSize, contentY, secondWidth, contentHeight);
        } else {
            int availableHeight = Math.max(0, contentHeight - handleSize);
            int firstHeight = Math.round(availableHeight * clamp(ratio));
            int secondHeight = Math.max(0, availableHeight - firstHeight);
            first.mount(contentX, contentY, contentWidth, firstHeight);
            second.mount(contentX, contentY + firstHeight + handleSize, contentWidth, secondHeight);
        }
    }

    private void drawHandle(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        UITheme theme = UITheme.current();
        boolean hovered = isOverHandle(mouseX, mouseY) || dragging;
        int color = hovered ? theme.panel().borderHover() : theme.panel().border();

        if (axis == Axis.HORIZONTAL) {
            int handleX = handleX();
            graphics.fill(handleX + 1, innerY(), handleX + Math.max(1, handleSize - 1), innerY() + innerHeight(), color);
        } else {
            int handleY = handleY();
            graphics.fill(innerX(), handleY + 1, innerX() + innerWidth(), handleY + Math.max(1, handleSize - 1), color);
        }
    }

    private boolean isOverHandle(double mouseX, double mouseY) {
        if (handleSize <= 0) return false;

        if (axis == Axis.HORIZONTAL) {
            int handleX = handleX();
            return mouseX >= handleX && mouseX < handleX + handleSize && mouseY >= innerY() && mouseY < innerY() + innerHeight();
        }

        int handleY = handleY();
        return mouseX >= innerX() && mouseX < innerX() + innerWidth() && mouseY >= handleY && mouseY < handleY + handleSize;
    }

    private int handleX() {
        return innerX() + Math.round(Math.max(0, innerWidth() - handleSize) * animatedSplit);
    }

    private int handleY() {
        return innerY() + Math.round(Math.max(0, innerHeight() - handleSize) * animatedSplit);
    }

    private float clamp(float value) {
        return Math.max(minSplit, Math.min(maxSplit, value));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float defaultAnimate(float current, float target, float delta, float speed) {
        return current + (target - current) * Math.min(1f, speed * delta);
    }
}
