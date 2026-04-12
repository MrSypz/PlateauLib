package com.sypztep.plateauPostprocess.client.v1.postprocess;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface PostEffectLayer {
    void apply(Minecraft mc, float partialTick, GraphicsResourceAllocator allocator);
}
