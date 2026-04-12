package com.sypztep.plateau.client.v1.particle.state;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sypztep.plateau.client.v1.particle.TextParticle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public class TextParticleGroup extends ParticleGroup<TextParticle> {

    public TextParticleGroup(ParticleEngine particleEngine) {
        super(particleEngine);
    }

    @Override
    public @NonNull ParticleGroupRenderState extractRenderState(@NonNull Frustum frustum, @NonNull Camera camera, float partialTick) {
        return (submitNodeCollector, _) -> {
            Minecraft client = Minecraft.getInstance();
            PoseStack poseStack = new PoseStack();
            this.particles.forEach(particle -> particle.submit(submitNodeCollector, poseStack, client.font, camera, partialTick));
        };
    }
}