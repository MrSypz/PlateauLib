package com.sypztep.plateau.client.v2.ui;

import com.sypztep.plateau.client.v2.ui.overlay.ContextMenuComponent;
import com.sypztep.plateau.client.v2.ui.overlay.DialogComponent;
import com.sypztep.plateau.client.v2.ui.overlay.DetachablePanel;
import com.sypztep.plateau.client.v2.ui.overlay.DropdownPopup;
import com.sypztep.plateau.client.v2.ui.overlay.HoverCardComponent;
import com.sypztep.plateau.client.v2.ui.overlay.WindowLayer;

import java.util.List;
import java.util.function.Function;
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

    public static WindowLayer windowLayer() {
        return new WindowLayer();
    }

    public static DetachablePanel detachable(String title) {
        return new DetachablePanel(title);
    }

    public static DetachablePanel detachable(Component title) {
        return new DetachablePanel(title);
    }

    public static ContextMenuComponent contextMenu() {
        return new ContextMenuComponent();
    }

    public static HoverCardComponent hoverCard() {
        return new HoverCardComponent();
    }

    public static <T> DropdownPopup<T> dropdownPopup(List<T> values, Function<T, String> labeler) {
        return new DropdownPopup<>(values, labeler);
    }

    public static DropdownPopup<String> dropdownPopup(String... values) {
        return new DropdownPopup<>(List.of(values), s -> s);
    }
}
