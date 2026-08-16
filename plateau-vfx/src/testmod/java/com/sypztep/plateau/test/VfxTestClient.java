package com.sypztep.plateau.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sypztep.plateau.client.v1.vfx.mesh.Anicca;
import com.sypztep.plateau.client.v1.vfx.mesh.PrimitiveMeshes;
import com.sypztep.plateau.client.v1.vfx.mesh.VfxAtlas;
import com.sypztep.plateau.client.v1.vfx.particle.ExternalForcesModule;
import com.sypztep.plateau.client.v1.vfx.particle.ParticleSystem;
import com.sypztep.plateau.client.v1.vfx.particle.SimulationSpace;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Target validation from {@code implementation.md}'s Phase 3 plan: reproduce
 * knella's {@code FireballRenderer} three-sphere buildup (exact same
 * constants/formula/`PrimitiveMeshes` calls — see
 * {@code G:\Coding\knella-26.1\...\client\renderer\entity\FireballRenderer.java})
 * as a {@link ParticleSystem}, side by side against a hand-rolled reference
 * using the identical draw calls but tracking age/position with plain fields
 * instead of going through {@code ParticleSystem}/{@code SimulationSpace}.
 * If the two ever look different, the bug is in the new plumbing, not the
 * (already-proven) geometry math.
 *
 * <p>Left fireball = {@code ParticleSystem} (capacity 1, one particle
 * standing in for "the fireball body itself", {@link SimulationSpace#local}
 * anchored to a point in front of the player so it rigidly follows like a
 * real emitter). Right fireball = the hand-rolled reference.
 */
@Environment(EnvType.CLIENT)
public class VfxTestClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("PlateauVfxTestmod");

    private static final float SPHERE_RADIUS = 0.8f;
    private static final float BUILDUP_TICKS = 4f;
    private static final int LIGHT = LightCoordsUtil.FULL_BRIGHT;

    private ParticleSystem system;
    private float referenceAge = 0f;
    private LevelRenderContext currentCtx;
    private Anicca testMesh;

    @Override
    public void onInitializeClient() {
        system = new ParticleSystem(1, SimulationSpace.local(this::anchor),
                (random, outPos, outVel) -> {
                    outPos.set(0f, 0f, 0f);
                    outVel.set(0f, 0f, 0f);
                },
                new ExternalForcesModule(), RandomSource.create());
        // Lifetime is a large placeholder, not a real despawn timer — this
        // testmod has nothing else that removes the particle, and buildup
        // (below) is computed from raw age against BUILDUP_TICKS, not
        // against this lifetime.
        system.spawn(new Matrix4f(), 1, 1_000_000f, 1f);

        // Anicca must be created before the client's first resource
        // reload finishes — doing it here in onInitializeClient() (not
        // lazily on first draw) satisfies that.
        testMesh = Anicca.of(Identifier.fromNamespaceAndPath("plateau-vfx-testmod", "block/test_mesh"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level == null || mc.isPaused()) return;
            system.tick(1f);
            referenceAge += 1f;
        });

        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(ctx -> {
            currentCtx = ctx;
            drawComparison();
            drawTestMesh();
        });

        LOGGER.info("[VfxTestmod] Comparing ParticleSystem-driven fireball (left) "
                + "against a hand-rolled reference (right) — they should look identical. "
                + "Also drawing the Anicca test asset (block/test_mesh) 4 blocks up.");
    }

    /** 1 block left of the player's look-anchor — re-sampled every {@link ParticleSystem#worldPosition} read. */
    private Matrix4f anchor() {
        Matrix4f m = new Matrix4f();
        Player player = Minecraft.getInstance().player;
        if (player == null) return m;
        Vec3 pos = player.getEyePosition().add(player.getLookAngle().scale(4.0)).add(-1.0, 0.0, 0.0);
        return m.translate((float) pos.x, (float) pos.y, (float) pos.z);
    }

    private void drawComparison() {
        if (currentCtx == null) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        PoseStack poseStack = currentCtx.poseStack();
        SubmitNodeCollector collector = currentCtx.submitNodeCollector();

        Vector3f worldPos = new Vector3f();
        system.worldPosition(0, worldPos);
        drawFireball(poseStack, collector,
                worldPos.x() - (float) camera.x, worldPos.y() - (float) camera.y, worldPos.z() - (float) camera.z,
                system.age(0));

        Vec3 refPos = player.getEyePosition().add(player.getLookAngle().scale(4.0)).add(1.0, 0.0, 0.0);
        drawFireball(poseStack, collector,
                (float) (refPos.x - camera.x), (float) (refPos.y - camera.y), (float) (refPos.z - camera.z),
                referenceAge);
    }

    /**
     * Draws the {@link Anicca} test asset ({@code block/test_mesh.json}
     * — a Blockbench-exported "+"-cross shape) 4 blocks above the anchor
     * point. {@link VfxAtlas#renderType()} is the render type here (not
     * {@code RenderTypes.debugQuads()} like the fireballs above) because
     * {@link Anicca#draw} emits real UVs into {@link VfxAtlas} — it
     * needs an atlas-sampling pipeline, not a flat position-color one.
     */
    private void drawTestMesh() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        Vec3 pos = player.getEyePosition().add(player.getLookAngle().scale(4.0)).add(0.0, 2.0, 0.0);

        PoseStack poseStack = currentCtx.poseStack();

        poseStack.pushPose();
        poseStack.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
//        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(referenceAge * 0.05f));
        // No extra scale here: FaceBakery already divides every baked vertex
        // by 16 (Blockbench's 16-unit grid -> MC's 1-block-unit space), so
        // Anicca's quads arrive already in real block-size units.
        currentCtx.submitNodeCollector().submitCustomGeometry(poseStack, VfxAtlas.renderType(),
                (pose, buffer) -> testMesh.draw(pose, buffer, 0xFFFFFFFF, LIGHT));
        poseStack.popPose();
    }

    /**
     * Exact same buildup curve + three-sphere layering as {@code
     * FireballRenderer.submit}, drawn via {@link SubmitNodeCollector
     * #submitCustomGeometry} the same way that renderer does — the level
     * renderer defers and batches the actual draw itself, instead of us
     * pulling a {@code VertexConsumer} straight off the buffer source and
     * managing its batch lifecycle by hand.
     */
    private void drawFireball(PoseStack poseStack, SubmitNodeCollector collector, float x, float y, float z, float ageTicks) {
        float buildupProgress = Math.min(ageTicks / BUILDUP_TICKS, 1.0f);
        float scale = buildupProgress * buildupProgress * (3.0f - 2.0f * buildupProgress);
        if (scale <= 0.001f) return;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(scale, scale, scale);

        float t = ageTicks;
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
            PrimitiveMeshes.rotatingSphere(buffer, pose, LIGHT,   // hot core
                    SPHERE_RADIUS * 0.35f,
                    1.0f, 1.0f, 0.9f, 1f,
                    t * 0.15f, t * -0.10f,
                    6, 8,
                    t * 0.12f, 3.0f);
            PrimitiveMeshes.rotatingSphere(buffer, pose, LIGHT,   // mid crackle
                    SPHERE_RADIUS * 0.65f,
                    1.0f, 0.3f, 0.0f, 0.85f,
                    t * -0.08f, t * 0.06f,
                    8, 10,
                    t * -0.07f, 2.0f);
            PrimitiveMeshes.rotatingSphere(buffer, pose, LIGHT,   // outer textured shell
                    SPHERE_RADIUS,
                    1.0f, 0.3f, 0.0f, 0.25f,
                    t * 0.05f, t * 0.03f,
                    10, 14,
                    t * 0.04f, 1.5f);
        });

        poseStack.popPose();
    }
}
