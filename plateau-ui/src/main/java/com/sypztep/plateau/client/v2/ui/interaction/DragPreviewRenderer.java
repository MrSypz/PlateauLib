package com.sypztep.plateau.client.v2.ui.interaction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface DragPreviewRenderer<PayloadValue> {
    void render(GuiGraphicsExtractor graphics, DragPayload<PayloadValue> payload, int mouseX, int mouseY, float delta);
}
