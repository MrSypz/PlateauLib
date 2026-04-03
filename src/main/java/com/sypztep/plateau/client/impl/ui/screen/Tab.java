package com.sypztep.plateau.client.impl.ui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    private final List<GuiEventListener> trackedWidgets = new ArrayList<>();
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

    /**
     * Build widgets for this tab. Use the TabContext to avoid boilerplate
     * layout calculations.
     */
    protected abstract void buildWidgets(TabContext ctx);

    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addWidget(T widget) {
        parentScreen.addTabWidget(widget);
        trackedWidgets.add(widget);
        return widget;
    }

    protected <T extends Renderable> T addRenderable(T renderable) {
        parentScreen.addTabRenderable(renderable);
        trackedRenderables.add(renderable);
        return renderable;
    }

    public void onActivate() {
        active = true;
        TabContext ctx = TabContext.from(parentScreen);
        buildWidgets(ctx);
    }

    public void onDeactivate() {
        active = false;
        for (GuiEventListener widget : trackedWidgets) {
            parentScreen.removeTabWidget(widget);
        }
        trackedWidgets.clear();
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

    public void renderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}

    public String getId() { return id; }
    public Component getLabel() { return label; }
    @Nullable public Identifier getIcon() { return icon; }
    public boolean isActive() { return active; }
}
