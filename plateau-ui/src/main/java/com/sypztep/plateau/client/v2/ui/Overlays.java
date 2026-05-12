package com.sypztep.plateau.client.v2.ui;

import com.sypztep.plateau.client.v2.ui.overlay.DialogComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

/** Factory methods for v2 overlay components. */
@Environment(EnvType.CLIENT)
public final class Overlays {

    private Overlays() {}

    public static DialogComponent dialog() {
        return new DialogComponent();
    }

    public static DialogComponent dialog(String title) {
        return new DialogComponent().title(title);
    }

    public static DialogComponent dialog(Component title) {
        return new DialogComponent().title(title);
    }
}
