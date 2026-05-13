package com.sypztep.plateau.client.v2.ui.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
    public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
        for (BaseComponent<?> child : childComponents()) {
            child.visitWidgets(widgetVisitor);
        }
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

    protected void focusAfterInteraction(BaseComponent<?> child, int button) {
        if (child != getFocused() && child.shouldTakeFocusAfterInteraction()) {
            setFocused(child);
        }

        if (button == 0) setDragging(true);
    }
}
