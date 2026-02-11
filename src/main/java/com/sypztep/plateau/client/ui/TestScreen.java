package com.sypztep.plateau.client.ui;

import com.sypztep.plateau.client.ui.screen.PlateauScreen;
import com.sypztep.plateau.client.ui.screen.TabManager;
import com.sypztep.plateau.client.ui.screen.tab.ButtonTab;
import com.sypztep.plateau.client.ui.screen.tab.InfoTab;
import com.sypztep.plateau.client.ui.screen.tab.PartyTab;
import net.minecraft.network.chat.Component;

public class TestScreen extends PlateauScreen {
    public TestScreen() {
        super(Component.literal("PlateauLib Test"));

        tabManager = new TabManager(this);
        tabManager.registerTab(new ButtonTab());
        tabManager.registerTab(new PartyTab());
        tabManager.registerTab(new InfoTab());
    }

    @Override
    protected void initComponents() {
    }
}