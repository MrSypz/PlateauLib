package com.sypztep.plateau.client.v2.ui.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class BaseContainerComponent extends BaseComponent implements ContainerEventHandler {

    @Nullable
    private GuiEventListener focusedChild;
    private boolean dragging;

    @Override
    public @Nullable GuiEventListener getFocused() { return focusedChild; }

    @Override
    public void setFocused(@Nullable GuiEventListener listener) {
        if (focusedChild != null) focusedChild.setFocused(false);
        focusedChild = listener;
        if (listener != null) listener.setFocused(true);
    }

    @Override public final boolean isDragging()        { return dragging; }
    @Override public final void setDragging(boolean v) { dragging = v; }

    @Override public boolean isFocused()  { return focusedChild != null; }
    @Override public void setFocused(boolean v) { if (!v) setFocused(null); }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        return ContainerEventHandler.super.nextFocusPath(event);
    }

    @Override
    protected boolean isFocusable() { return true; } // containers are always focusable

    /** Helper: transfer focus safely, updating focused flag on both old and new child. */
    protected void transferFocus() {
        setFocused(null);
    }
}