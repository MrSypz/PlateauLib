package com.sypztep.plateauPostprocess.client.v1.postprocess;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.sypztep.plateauPostprocess.client.PlateauPostprocessClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordered registry of screen-space post-process layers.
 *
 * <p>All layers are applied after {@code GameRenderer.renderLevel()} returns so each layer
 * sees the complete composited frame (terrain, clouds, weather, hand, screen effects).
 *
 * <p>A shared {@link CrossFrameResourcePool} is passed to every layer so temporary GPU
 * resources (swap render targets etc.) are reused across frames instead of being
 * allocated and freed on every {@code FrameGraphBuilder.execute()} call.
 */
@Environment(EnvType.CLIENT)
public final class PostEffectManager {
    private static final CrossFrameResourcePool POOL = new CrossFrameResourcePool(3);
    private static final List<PostEffectHandle> HANDLES = new ArrayList<>();

    private PostEffectManager() {}

    /**
     * Register a post-process layer.
     * Returns a {@link PostEffectHandle} for priority tuning or later removal.
     *
     * @param layer   called each frame after {@code renderLevel()}
     * @param onClose called on disconnect to release GPU resources
     */
    public static PostEffectHandle register(PostEffectLayer layer, Runnable onClose) {
        PostEffectHandle handle = new PostEffectHandle(layer, onClose);
        HANDLES.add(handle);
        PlateauPostprocessClient.LOGGER.info("[PostEffect] Registered layer: {}", layer.getClass().getSimpleName());
        return handle;
    }

    /** Re-sort by priority (called automatically when a handle's priority changes). */
    static void resort() {
        HANDLES.sort(Comparator.comparingInt(PostEffectHandle::getPriority).reversed());
    }

    /** Remove and close a specific layer. */
    static void unregister(PostEffectHandle handle) {
        if (HANDLES.remove(handle)) handle.getOnClose().run();
    }

    /**
     * Apply all layers in priority order, then age the resource pool by one frame.
     * Called by {@code GameRendererMixin} — do not call manually.
     */
    public static void applyAll(Minecraft mc, float partialTick) {
        if (HANDLES.isEmpty()) return;

        for (PostEffectHandle handle : HANDLES) {
            if (!handle.getCondition().getAsBoolean()) continue;
            handle.getLayer().apply(mc, partialTick, POOL);
        }

        POOL.endFrame();
    }

    /** Release all GPU resources. Call on disconnect. */
    public static void closeAll() {
        for (PostEffectHandle handle : HANDLES) handle.getOnClose().run();
        PlateauPostprocessClient.LOGGER.info("[PostEffect] Close all layers");
        POOL.close();
    }
}
