package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class DialogComponent extends com.sypztep.plateau.client.v2.ui.widget.DialogComponent {

    @Override public DialogComponent title(Component title) { super.title(title); return this; }
    @Override public DialogComponent title(String title) { super.title(title); return this; }
    @Override public DialogComponent dialogWidth(int width) { super.dialogWidth(width); return this; }
    @Override public DialogComponent dialogHeight(int height) { super.dialogHeight(height); return this; }
    @Override public DialogComponent closeOnBackdrop(boolean onClose) { super.closeOnBackdrop(onClose); return this; }

    @Deprecated()
    @Override public DialogComponent contentHeight(int height) { super.contentHeight(height); return this; }
    @Override public DialogComponent content(@Nullable BaseComponent content) { super.content(content); return this; }
    @Override public DialogComponent button(Component label, Consumer<com.sypztep.plateau.client.v2.ui.widget.DialogComponent> action) { super.button(label, action); return this; }
    @Override public DialogComponent button(String label, Consumer<com.sypztep.plateau.client.v2.ui.widget.DialogComponent> action) { super.button(label, action); return this; }
    @Override public DialogComponent open() { super.open(); return this; }
    @Override public DialogComponent close() { super.close(); return this; }
    @Override public DialogComponent toggle() { super.toggle(); return this; }

    @Override public DialogComponent padding(Insets padding) { super.padding(padding); return this; }
    @Override public DialogComponent margins(Insets margins) { super.margins(margins); return this; }
    @Override public DialogComponent surface(Surface surface) { super.surface(surface); return this; }
    @Override public DialogComponent id(String id) { super.id(id); return this; }
    @Override public DialogComponent visible(boolean visible) { super.visible(visible); return this; }
    @Override public DialogComponent sizing(Sizing h, Sizing v) { super.sizing(h, v); return this; }
    @Override public DialogComponent sizing(Sizing both) { super.sizing(both); return this; }
}
