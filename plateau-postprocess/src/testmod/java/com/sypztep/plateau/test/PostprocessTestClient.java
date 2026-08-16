package com.sypztep.plateau.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sypztep.plateau.client.v1.vfx.VfxMaskGroups;
import com.sypztep.plateau.client.v1.vfx.effects.BlurParams;
import com.sypztep.plateau.client.v1.vfx.effects.BloomParams;
import com.sypztep.plateau.client.v1.vfx.effects.ChromaticAberrationParams;
import com.sypztep.plateau.client.v1.vfx.effects.ColorGradingParams;
import com.sypztep.plateau.client.v1.vfx.effects.DepthOfFieldParams;
import com.sypztep.plateau.client.v1.vfx.effects.FilmGrainParams;
import com.sypztep.plateau.client.v1.vfx.effects.GlitchParams;
import com.sypztep.plateau.client.v1.vfx.effects.OutlineParams;
import com.sypztep.plateau.client.v1.vfx.effects.VfxEffects;
import com.sypztep.plateau.client.v1.vfx.effects.VignetteParams;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Cycles every built-in Vfx effect (see {@code VfxEffects}) a few seconds
 * at a time so each one can be eyeballed in a real client, plus an "N" key
 * to skip ahead on demand. Not a unit test — plateau-postprocess has none;
 * this is the in-game smoke test for the effect library.
 *
 * <p><b>Must request every rendered frame, not every tick.</b>
 * {@code VfxManager} consumes and clears each effect's request queue once
 * per rendered frame (via {@code GameRendererMixin}, right after
 * {@code renderLevel()}); {@code ClientTickEvents} only fires at the fixed
 * 20/s tick rate. Requesting from a tick event starved every frame that
 * wasn't itself a tick, which reads as the whole effect flickering at high
 * framerate — this bit us during actual in-game testing.
 * {@link LevelRenderEvents#AFTER_TRANSLUCENT_FEATURES}
 * runs inside {@code renderLevel()}, once per frame, strictly before the
 * mixin's post-renderLevel consumption point, so the request is always
 * fresh for the frame that's about to consume it.
 */
@Environment(EnvType.CLIENT)
public class PostprocessTestClient implements ClientModInitializer {
    public static final String MODID = "plateau-postprocess-testmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("PlateauPostprocessTestmod");

    private static final int FRAMES_PER_EFFECT = 3000; // a few seconds at typical framerates
    private static final Identifier MASK_GROUP_TEST_BOX = Identifier.fromNamespaceAndPath(MODID, "test_box");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private record TestEffect(String name, Runnable requestFrame) {}

    private List<TestEffect> effects;
    private int frameCounter = 0;
    private int currentIndex = 0;
    private LevelRenderContext currentCtx;

    @Override
    public void onInitializeClient() {
        effects = List.of(
                new TestEffect("bloom", () -> VfxEffects.bloom().requestFrame(1f, BloomParams.DEFAULT)),
                new TestEffect("depthOfField", () -> VfxEffects.depthOfField().requestFrame(1f, DepthOfFieldParams.DEFAULT)),
                new TestEffect("blur", () -> VfxEffects.blur().requestFrame(1f, BlurParams.DEFAULT)),
                new TestEffect("outline", () -> VfxEffects.outline().requestFrame(1f, OutlineParams.DEFAULT)),
                // Phase 2 smoke test: draw a solid box floating in front of
                // the player into a mask group, then restrict Outline to it.
                // Only the box's silhouette should get an edge — not every
                // depth discontinuity in the scene like the plain "outline"
                // case above.
                new TestEffect("maskedOutline", () -> {
                    VfxMaskGroups.draw(MASK_GROUP_TEST_BOX, () -> drawTestBox(currentCtx));
                    VfxEffects.outline().requestFrame(1f, OutlineParams.DEFAULT.withMaskGroup(MASK_GROUP_TEST_BOX));
                }),
                new TestEffect("colorGrading", () -> VfxEffects.colorGrading().requestFrame(1f, ColorGradingParams.DEFAULT)),
                new TestEffect("chromaticAberration", () -> VfxEffects.chromaticAberration().requestFrame(1f, ChromaticAberrationParams.DEFAULT)),
                new TestEffect("glitch", () -> VfxEffects.glitch().requestFrame(1f, GlitchParams.DEFAULT)),
                // Stronger than FilmGrainParams.DEFAULT (0.05) purely so it's
                // obviously visible during this manual smoke test — the
                // library default is intentionally subtle.
                new TestEffect("filmGrain", () -> VfxEffects.filmGrain().requestFrame(1f, new FilmGrainParams(0.25f, 1.0f))),
                new TestEffect("vignette", () -> VfxEffects.vignette().requestFrame(1f, VignetteParams.DEFAULT))
        );

        KeyMapping.Category debug = KeyMapping.Category.register(id("debug"));
        KeyMapping next = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.plateau-postprocess-testmod.next_effect", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, debug));

        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(ctx -> {
            currentCtx = ctx;

            while (next.consumeClick()) {
                advance();
            }

            frameCounter++;
            if (frameCounter >= FRAMES_PER_EFFECT) {
                advance();
            }

            effects.get(currentIndex).requestFrame().run();
        });

        LOGGER.info("[PostprocessTestmod] Cycling {} built-in effects every {} frames. Press N to skip ahead.",
                effects.size(), FRAMES_PER_EFFECT);
    }

    /**
     * Draws a solid red 1-block cube floating 4 blocks in front of the
     * player, for the {@code maskedOutline} test entry. Uses {@code
     * RenderTypes.debugQuads()} (POSITION_COLOR, no declared output target)
     * specifically because it honors whatever target
     * {@code RenderSystem.outputColorTextureOverride} currently points at —
     * unlike e.g. {@code RenderTypes.lines()}, which hardcodes its own
     * output target and would silently ignore {@link VfxMaskGroups#draw}'s
     * redirect.
     */
    private static void drawTestBox(LevelRenderContext ctx) {
        if (ctx == null) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(4.0));

        PoseStack poseStack = ctx.poseStack();
        MultiBufferSource.BufferSource bufferSource = ctx.bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.debugQuads());

        poseStack.pushPose();
        poseStack.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
        addCube(poseStack.last(), buffer, 0.5f, 255, 60, 60, 220);
        poseStack.popPose();
        bufferSource.endBatch(RenderTypes.debugQuads());
    }

    private static void addCube(PoseStack.Pose pose, VertexConsumer buffer, float half,
                                 int r, int g, int b, int a) {
        // -X / +X
        quad(pose, buffer, -half, -half, -half, -half, half, -half, -half, half, half, -half, -half, half, r, g, b, a);
        quad(pose, buffer, half, -half, half, half, half, half, half, half, -half, half, -half, -half, r, g, b, a);
        // -Y / +Y
        quad(pose, buffer, -half, -half, -half, half, -half, -half, half, -half, half, -half, -half, half, r, g, b, a);
        quad(pose, buffer, -half, half, half, half, half, half, half, half, -half, -half, half, -half, r, g, b, a);
        // -Z / +Z
        quad(pose, buffer, -half, -half, -half, -half, half, -half, half, half, -half, half, -half, -half, r, g, b, a);
        quad(pose, buffer, half, -half, half, half, half, half, -half, half, half, -half, -half, half, r, g, b, a);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float x3, float y3, float z3, float x4, float y4, float z4,
                              int r, int g, int b, int a) {
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        buffer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a);
    }

    private void advance() {
        frameCounter = 0;
        currentIndex = (currentIndex + 1) % effects.size();
        String name = effects.get(currentIndex).name();
        LOGGER.info("[PostprocessTestmod] Now testing: {}", name);
        Minecraft.getInstance().gui.setOverlayMessage(Component.literal("[Vfx test] " + name), false);
    }
}
