package com.sypztep.plateau.client.v1.ui.screen;

import com.sypztep.plateau.client.PlateauUIClient;
import com.sypztep.plateau.client.v1.ui.widget.UINavBar;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
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
    private final Map<Identifier, Tab> tabs = new LinkedHashMap<>();
    private @Nullable Identifier activeTabId = null;
    private @Nullable UINavBar navBar = null;
    private int navBarHeight = 25;

    public TabManager(PlateauScreen screen) {
        this.screen = screen;
    }

    public TabManager registerTab(Tab tab) {
        Identifier id = tab.getId();
        if (tabs.containsKey(id))
            PlateauUIClient.LOGGER.warn("[TabManager] Duplicate tab ID '{}' — previous registration will be replaced.", id);
        tabs.put(id, tab);
        return this;
    }

    public void clearTracking() {
        tabs.values().forEach(Tab::clearTrackedWidgets);
        navBar = null;
    }

    public void init() {
        navBar = new UINavBar(10, 5, screen.width - 20, navBarHeight);

        for (Tab tab : tabs.values()) {
            navBar.addItem(tab.getId(), tab.getLabel(), tab.getIcon(), this::activateTab);
            tab.init(screen);
        }

        screen.addTabWidget(navBar);

        Identifier target = tabs.containsKey(activeTabId) ? activeTabId
                : tabs.isEmpty() ? null
                : tabs.keySet().iterator().next();

        if (target != null) {
            activeTabId = null;
            activateTab(target);
        }
    }

    public void activateTab(Identifier tabId) {
        if (!tabs.containsKey(tabId) || tabId.equals(activeTabId)) return;

        if (activeTabId != null) {
            Tab current = tabs.get(activeTabId);
            if (current != null) current.onDeactivate();
        }

        activeTabId = tabId;
        tabs.get(tabId).onActivate();

        if (navBar != null) navBar.setActive(tabId);
    }

    public void renderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Tab active = getActiveTab();
        if (active != null) active.renderOverlay(graphics, mouseX, mouseY, delta);
    }

    public int getNavBarHeight() { return navBarHeight; }
    public TabManager setNavBarHeight(int height) { this.navBarHeight = height; return this; }
    public @Nullable Tab getActiveTab() { return activeTabId != null ? tabs.get(activeTabId) : null; }
    public @Nullable Identifier getActiveTabId() { return activeTabId; }
    public List<Tab> getTabs() { return new ArrayList<>(tabs.values()); }
    public int getTabCount() { return tabs.size(); }
}