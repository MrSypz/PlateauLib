package com.sypztep.plateau.test.screen;

import com.sypztep.plateau.client.v1.ui.screen.PlateauScreen;
import com.sypztep.plateau.client.v1.ui.screen.TabManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class UITestScreen extends PlateauScreen {

    public UITestScreen() {
        super(Component.literal("UI Test Screen"));
    }

    @Override
    protected void initComponents() {
        tabManager = new TabManager(this);
        tabManager.registerTab(new WidgetsTab());
        tabManager.registerTab(new ScrollTab());
        tabManager.registerTab(new DragDropStressTab());
        tabManager.registerTab(new PlayerClassStatsTab());
    }
}
