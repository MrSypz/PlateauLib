package com.sypztep.plateau.client.v2.ui.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public abstract class BaseContainerComponent<GenericBaseComponent extends BaseContainerComponent<GenericBaseComponent>> extends BaseComponent<GenericBaseComponent> implements ContainerEventHandler {

    protected final List<BaseComponent<?>> children = new ArrayList<>();

    @Nullable
    private GuiEventListener focusedChild;
    private boolean dragging;

    // ── Child management ─────────────────────────────────────

    public GenericBaseComponent child(BaseComponent<?> child) {
        children.add(child);
        return self();
    }

    public GenericBaseComponent children(BaseComponent<?>... components) {
        Collections.addAll(children, components);
        return self();
    }

    public GenericBaseComponent children(Iterable<? extends BaseComponent<?>> components) {
        for (BaseComponent<?> component : components) {
            children.add(component);
        }
        return self();
    }

    public List<BaseComponent<?>> getChildren() {
        return children;
    }

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        return children;
    }

    // ── Focus / dragging ─────────────────────────────────────

    @Override
    public @Nullable GuiEventListener getFocused() {
        return focusedChild;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener listener) {
        if (focusedChild == listener) return;
        if (focusedChild != null) focusedChild.setFocused(false);

        focusedChild = listener;

        if (listener != null) listener.setFocused(true);
    }

    @Override
    public final boolean isDragging() {
        return dragging;
    }

    @Override
    public final void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public boolean isFocused() {
        return focusedChild != null;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) setFocused(null);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        return ContainerEventHandler.super.nextFocusPath(event);
    }

    @Override
    protected boolean isFocusable() {
        return true;
    }

    protected void transferFocus() {
        setFocused(null);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;

        for (BaseComponent<?> child : childrenBackToFront()) {
            if (!child.isVisible() || !child.rendersAboveSiblings() || !child.isMouseOver(event.x(), event.y())) continue;
            if (child.mouseClicked(event, doubleClick)) {
                focusAfterInteraction(child, event.button());
                return true;
            }
            if (child.blocksLowerInput()) return true;
        }

        for (BaseComponent<?> child : childrenBackToFront()) {
            if (!child.isVisible() || !child.isMouseOver(event.x(), event.y())) continue;
            if (child.mouseClicked(event, doubleClick)) {
                focusAfterInteraction(child, event.button());
                return true;
            }
        }

        setFocused(null);
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        boolean wasDragging = isDragging();
        if (event.button() == 0) setDragging(false);

        GuiEventListener focused = getFocused();
        if (focused != null && (wasDragging || focused.isMouseOver(event.x(), event.y()))) {
            if (focused.mouseReleased(event)) return true;
        }

        for (BaseComponent<?> child : childrenBackToFront()) {
            if (!child.isVisible() || !child.isMouseOver(event.x(), event.y())) continue;
            return child.mouseReleased(event);
        }

        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        GuiEventListener focused = getFocused();
        return focused != null && isDragging() && focused.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        for (BaseComponent<?> child : childrenBackToFront()) {
            if (!child.isVisible() || !child.rendersAboveSiblings() || !child.isMouseOver(mouseX, mouseY)) continue;
            if (child.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) return true;
            if (child.blocksLowerInput()) return true;
        }

        for (BaseComponent<?> child : childrenBackToFront()) {
            if (!child.isVisible() || !child.isMouseOver(mouseX, mouseY)) continue;
            return child.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (BaseComponent<?> child : childComponents()) {
            if (child.isVisible()) child.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        return getFocused() != null && getFocused().keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent event) {
        return getFocused() != null && getFocused().keyReleased(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        return getFocused() != null && getFocused().charTyped(event);
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        return getFocused() != null && getFocused().preeditUpdated(event);
    }

    @Override
    public @Nullable ComponentPath getCurrentFocusPath() {
        if (focusedChild == null) return null;
        ComponentPath childPath = focusedChild.getCurrentFocusPath();
        return childPath != null ? ComponentPath.path(this, childPath) : null;
    }

    protected List<BaseComponent<?>> childComponents() {
        List<BaseComponent<?>> result = new ArrayList<>();
        for (GuiEventListener listener : children()) {
            if (listener instanceof BaseComponent<?> component) {
                result.add(component);
            }
        }
        return result;
    }

    protected List<BaseComponent<?>> childrenBackToFront() {
        List<BaseComponent<?>> result = childComponents();
        Collections.reverse(result);
        return result;
    }

    protected final void extractChildrenInLayerOrder(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        for (BaseComponent<?> child : children) {
            if (child.isVisible() && !child.rendersAboveSiblings()) {
                extractChild(graphics, child, mouseX, mouseY, delta);
            }
        }

        for (BaseComponent<?> child : children) {
            if (child.isVisible() && child.rendersAboveSiblings()) {
                extractChild(graphics, child, mouseX, mouseY, delta);
            }
        }
    }

    protected void extractChild(GuiGraphicsExtractor graphics, BaseComponent<?> child, int mouseX, int mouseY, float delta) {
        enableChildScissor(graphics, child);
        boolean hoverBlocked = isMouseBlockedByTopChild(child, mouseX, mouseY);
        child.extractRenderState(graphics,
                hoverBlocked ? hoverSuppressedMouse() : mouseX,
                hoverBlocked ? hoverSuppressedMouse() : mouseY,
                delta);
        graphics.disableScissor();
    }

    protected void enableChildScissor(GuiGraphicsExtractor graphics, BaseComponent<?> child) {
        int clipLeft = Math.max(x, child.x() - child.renderClipLeftOutset());
        int clipTop = Math.max(y - child.renderClipTopOutset(), child.y() - child.renderClipTopOutset());
        int clipRight = Math.min(x + width, child.x() + child.width() + child.renderClipRightOutset());
        int clipBottom = Math.min(y + height + child.renderClipBottomOutset(), child.y() + child.height() + child.renderClipBottomOutset());
        graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
    }

    protected final boolean isMouseBlockedByTopChild(BaseComponent<?> target, int mouseX, int mouseY) {
        for (BaseComponent<?> child : children) {
            if (child != target
                    && child.isVisible()
                    && child.rendersAboveSiblings()
                    && child.blocksLowerInput()
                    && child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }

        for (int index = children.size() - 1; index >= 0; index--) {
            BaseComponent<?> child = children.get(index);
            if (child == target) return false;
            if (child.isVisible()
                    && !child.rendersAboveSiblings()
                    && child.blocksLowerInput()
                    && child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    protected void focusAfterInteraction(BaseComponent<?> child, int button) {
        if (child != getFocused() && child.shouldTakeFocusAfterInteraction()) {
            setFocused(child);
        }

        if (button == 0) setDragging(true);
    }

    /**
     * Safe helper for custom mouseClicked overrides: dispatches the click to the child and
     * automatically calls focusAfterInteraction if it was handled. Use this instead of calling
     * child.mouseClicked() directly so the focus/drag state is never accidentally skipped.
     */
    protected boolean dispatchClick(BaseComponent<?> child, MouseButtonEvent event, boolean doubleClick) {
        if (!child.isVisible() || !child.isMouseOver(event.x(), event.y())) return false;
        if (child.mouseClicked(event, doubleClick)) {
            focusAfterInteraction(child, event.button());
            return true;
        }
        return child.blocksLowerInput();
    }
}
