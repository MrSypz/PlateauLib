package com.sypztep.plateau.client;

import com.sypztep.plateau.client.v1.vfx.mesh.VfxAtlas;
import net.fabricmc.api.ClientModInitializer;

public class PlateauVfxClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        VfxAtlas.register();
    }
}
