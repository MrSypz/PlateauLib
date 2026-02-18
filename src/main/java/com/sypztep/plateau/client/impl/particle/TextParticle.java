package com.sypztep.plateau.client.impl.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sypztep.plateau.client.PlateauClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public final class TextParticle extends Particle {
    private static final int FLICK_DURATION = 12;
    private static final int FADE_DURATION = 10;
    private static final float VELOCITY_DAMPEN = 0.9f;
    private static final float FADE_AMOUNT = 0.1f;

    private String text = "";
    private float scale;
    private float maxSize;

    private int targetColor;
    private int currentColor;
    private float alpha = 1f;

    public TextParticle(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
        this.lifetime = 25;
        this.scale = 0.0F;
        this.maxSize = -0.045F;
        this.gravity = -0.125f;
        this.targetColor = 0xFFFFFF;
        this.currentColor = 0xFFFFFF;
    }

    public void setMaxSize(float maxSize) {
        this.maxSize = maxSize;
    }

    public void setColor(int rgbColor) {
        this.targetColor = rgbColor & 0xFFFFFF;
        this.currentColor = 0xFFFFFF; // Start white for flick effect
    }

    public void setText(@NotNull String text) {
        this.text = text;
    }

    @Override
    public void tick() {
        if (this.age++ <= FLICK_DURATION) {
            float progress = age / (float) FLICK_DURATION;
            this.currentColor = ARGB.srgbLerp(progress, 0xFFFFFFFF, targetColor | 0xFF000000) & 0xFFFFFF;
            this.scale = Mth.lerp(ease(progress), 0.0F, this.maxSize);
        } else if (this.age <= this.lifetime) {
            float progress = (age - FLICK_DURATION) / (float) FADE_DURATION;
            int fadedR = (int) (ARGB.red(targetColor) * (1f - progress * FADE_AMOUNT));
            int fadedG = (int) (ARGB.green(targetColor) * (1f - progress * FADE_AMOUNT));
            int fadedB = (int) (ARGB.blue(targetColor) * (1f - progress * FADE_AMOUNT));
            this.currentColor = ARGB.color(fadedR, fadedG, fadedB);
            this.scale = Mth.lerp(progress, this.maxSize, 0.0f);
            this.alpha = Mth.lerp(progress, 1.0f, 0.0f);
        } else {
            this.remove();
        }
        this.yd *= VELOCITY_DAMPEN;
        super.tick();
    }

    public void extract(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Font font, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.position();

        float x = (float) (this.xo + (this.x - this.xo) * partialTick - cameraPos.x());
        float y = (float) (this.yo + (this.y - this.yo) * partialTick - cameraPos.y());
        float z = (float) (this.zo + (this.z - this.zo) * partialTick - cameraPos.z());

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        poseStack.scale(scale, scale, scale);

        int alphaInt = Math.max(1, (int) (alpha * 255));
        int finalColor = (alphaInt << 24) | currentColor;

        font.drawInBatch8xOutline(
                Component.literal(text).getVisualOrderText(),
                -font.width(text) / 2f,
                0,
                finalColor,
                0xFF000000,
                poseStack.last().pose(),
                bufferSource,
                0xF000F0
        );

        poseStack.popPose();
    }

    @Override
    public @NotNull ParticleRenderType getGroup() {
        return PlateauClient.TEXT_PARTICLE;
    }

    private float ease(float t) {
        if (t == 0) return 0;
        if (t >= 1) return 1;
        float p = 0.3f;
        float s = p / 4;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t - s) * (2 * Math.PI) / p) + 1);
    }
}