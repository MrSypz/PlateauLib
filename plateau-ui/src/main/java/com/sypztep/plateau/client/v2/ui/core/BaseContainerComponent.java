package com.sypztep.plateau.client.v2.ui.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public abstract class BaseContainerComponent extends BaseComponent implements ContainerEventHandler {

    protected final List<BaseComponent> children = new ArrayList<>();

    @Nullable
    private GuiEventListener focusedChild;
    private boolean dragging;

    // ── Child management ─────────────────────────────────────

    public BaseContainerComponent child(BaseComponent child) {
        children.add(child);
        return this;
    }

    public BaseContainerComponent children(BaseComponent... components) {
        Collections.addAll(children, components);
        return this;
    }

    public BaseContainerComponent children(Iterable<? extends BaseComponent> components) {
        for (BaseComponent component : components) {
            children.add(component);
        }
        return this;
    }

    public List<BaseComponent> getChildren() {
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
}