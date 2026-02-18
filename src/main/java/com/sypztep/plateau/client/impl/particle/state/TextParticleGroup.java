package com.sypztep.plateau.client.impl.particle.state;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sypztep.plateau.client.impl.particle.TextParticle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class TextParticleGroup extends ParticleGroup<TextParticle> {

    public TextParticleGroup(ParticleEngine particleEngine) {
        super(particleEngine);
    }

    @Override
    public @NotNull ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
        return (submitNodeCollector, cameraRenderState) -> {
            Minecraft client = Minecraft.getInstance();
            PoseStack poseStack = new PoseStack();
            this.particles.forEach(particle -> {
                particle.extract(client.renderBuffers().bufferSource(), poseStack, client.font, camera, partialTick);
            });
        };
    }
}