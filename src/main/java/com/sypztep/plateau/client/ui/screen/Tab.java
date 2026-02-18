package com.sypztep.plateau.client.ui.screen;

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

    // Widgets added via addWidget — full input/narration
    private final List<GuiEventListener> trackedWidgets = new ArrayList<>();
    // Renderables added via addRenderable — display only, tracked separately for cleanup
    private final List<Renderable> trackedRenderables = new ArrayList<>();

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
    }

    protected abstract void buildWidgets();

    /**
     * Add a full widget — receives input, focus, narration, and renders.
     * Added to Screen's children + renderables + narratables lists.
     */
    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addWidget(T widget) {
        parentScreen.addTabWidget(widget);
        trackedWidgets.add(widget);
        return widget;
    }

    /**
     * Add a renderable-only element — renders but does NOT receive input.
     * Not in Screen's children list, so getChildAt() skips it.
     * Use for panels, backgrounds, decorations.
     */
    protected <T extends Renderable> T addRenderable(T renderable) {
        parentScreen.addTabRenderable(renderable);
        trackedRenderables.add(renderable);
        return renderable;
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
        // Renderables need removal too — Screen.addRenderableOnly adds to renderables list
        for (Renderable r : trackedRenderables) {
            if (r instanceof GuiEventListener gel) {
                parentScreen.removeTabWidget(gel);
            }
        }
        trackedRenderables.clear();
    }

    void clearTrackedWidgets() {
        trackedWidgets.clear();
        trackedRenderables.clear();
        active = false;
    }

    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

    public String getId() { return id; }
    public Component getLabel() { return label; }
    @Nullable public Identifier getIcon() { return icon; }
    public boolean isActive() { return active; }
}