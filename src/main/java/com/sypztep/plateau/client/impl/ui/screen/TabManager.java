package com.sypztep.plateau.client.impl.ui.screen;

import com.sypztep.plateau.client.impl.ui.widget.UINavBar;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages tabs and their NavBar.
 * The NavBar is created and owned by TabManager — same pattern as Tyrannus.
 */
@Environment(EnvType.CLIENT)
public final class TabManager {
    private final PlateauScreen screen;
    private final Map<String, Tab> tabs = new LinkedHashMap<>();
    private @Nullable String activeTabId = null;

    // NavBar — owned by TabManager, added as screen widget
    private @Nullable UINavBar navBar = null;
    private int navBarHeight = 25;

    public TabManager(PlateauScreen screen) {
        this.screen = screen;
    }

    public TabManager registerTab(Tab tab) {
        tabs.put(tab.getId(), tab);
        return this;
    }

    /**
     * Called by PlateauScreen.init() BEFORE building widgets.
     */
    public void clearTracking() {
        for (Tab tab : tabs.values()) {
            tab.clearTrackedWidgets();
        }
        navBar = null; // will be recreated in init()
    }

    /**
     * Called by PlateauScreen.init() AFTER initComponents().
     * Creates the NavBar and activates the current tab.
     */
    public void init() {
        // Create NavBar across the top
        int navX = 10;
        int navY = 5;
        int navW = screen.width - 20;

        navBar = new UINavBar(navX, navY, navW, navBarHeight);
        navBar.setDebugLabel("NavBar");

        // Add items from tabs
        for (Tab tab : tabs.values()) {
            navBar.addItem(tab.getId(), tab.getLabel(), tab.getIcon(), this::activateTab);
            tab.init(screen);
        }

        // Register NavBar as a screen widget (input + render)
        screen.addTabWidget(navBar);

        // Activate tab
        String target = activeTabId;
        if (target == null || !tabs.containsKey(target)) {
            target = tabs.isEmpty() ? null : tabs.keySet().iterator().next();
        }

        if (target != null) {
            String toActivate = target;
            activeTabId = null;
            activateTab(toActivate);
        }
    }

    /**
     * Switch tabs. NavBar indicator animates automatically.
     */
    public void activateTab(String tabId) {
        if (!tabs.containsKey(tabId)) return;
        if (tabId.equals(activeTabId)) return;

        // Deactivate current
        if (activeTabId != null) {
            Tab current = tabs.get(activeTabId);
            if (current != null) {
                current.onDeactivate();
            }
        }

        // Activate new
        activeTabId = tabId;
        tabs.get(tabId).onActivate();

        // Sync NavBar
        if (navBar != null) {
            navBar.setActive(tabId);
        }
    }

    /**
     * Render tab overlay (tooltips, etc.).
     */
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Tab active = getActiveTab();
        if (active != null) {
            active.renderOverlay(graphics, mouseX, mouseY, delta);
        }
    }

    /**
     * Height of the NavBar — use this to offset tab content below it.
     */
    public int getNavBarHeight() { return navBarHeight; }

    public TabManager setNavBarHeight(int height) { this.navBarHeight = height; return this; }

    public @Nullable Tab getActiveTab() {
        return activeTabId != null ? tabs.get(activeTabId) : null;
    }

    public @Nullable String getActiveTabId() { return activeTabId; }

    public List<Tab> getTabs() {
        return new ArrayList<>(tabs.values());
    }

    public int getTabCount() { return tabs.size(); }
}