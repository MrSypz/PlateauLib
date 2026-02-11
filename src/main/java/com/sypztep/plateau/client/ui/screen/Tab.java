package com.sypztep.plateau.client.ui.screen;

import com.sypztep.plateau.client.ui.core.UIComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public abstract class Tab {
    protected final String id;
    protected final Component label;
    @Nullable protected final Identifier icon;

    protected final List<GuiEventListener> trackedWidgets = new ArrayList<>();
    protected final List<UIComponent> trackedComponents = new ArrayList<>();

    protected PlateauScreen parentScreen;
    protected Minecraft minecraft;
    protected boolean active = false;

    public Tab(String id, Component label, @Nullable Identifier icon) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.minecraft = Minecraft.getInstance();
    }

    public Tab(String id, Component label) {
        this(id, label, null);
    }

    public void init(PlateauScreen screen) {
        this.parentScreen = screen;
        trackedWidgets.clear();
        trackedComponents.clear();
    }

    protected abstract void buildWidgets();

    /**
     * Add a Screen widget (input + render + narration). Auto-removed on deactivate.
     */
    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addWidget(T widget) {
        parentScreen.addTabWidget(widget);
        trackedWidgets.add(widget);
        return widget;
    }

    /**
     * Add a render-only element. Auto-removed on deactivate.
     */
    protected <T extends Renderable> T addRenderable(T renderable) {
        parentScreen.addTabRenderable(renderable);
        if (renderable instanceof GuiEventListener listener) {
            trackedWidgets.add(listener);
        }
        return renderable;
    }

    /**
     * Add a UIComponent (PlateauLib component system). Auto-removed on deactivate.
     */
    protected <T extends UIComponent> T addComponent(T component) {
        parentScreen.addComponent(component);
        trackedComponents.add(component);
        return component;
    }

    public void onActivate() {
        active = true;
        buildWidgets();
    }

    public void onDeactivate() {
        active = false;
        for (GuiEventListener widget : trackedWidgets) {
            parentScreen.removeTabWidget(widget);
        }
        trackedWidgets.clear();

        for (UIComponent component : trackedComponents) {
            parentScreen.removeComponent(component);
        }
        trackedComponents.clear();
    }

    /**
     * Clear tracking lists without removing from screen.
     * Called by TabManager.clearTracking() during screen re-init
     * (Screen.init() already clears all children, so we just need to
     * forget our references to avoid stale removal attempts).
     */
    public void clearTrackedWidgets() {
        trackedWidgets.clear();
        trackedComponents.clear();
    }

    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    public String getId() { return id; }
    public Component getLabel() { return label; }
    @Nullable public Identifier getIcon() { return icon; }
    public boolean isActive() { return active; }
}