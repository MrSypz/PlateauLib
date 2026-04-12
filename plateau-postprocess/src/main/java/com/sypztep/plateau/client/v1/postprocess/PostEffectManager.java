package com.sypztep.plateau.client.v1.postprocess;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.sypztep.plateau.client.PlateauPostprocessClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordered registry of screen-space post-process layers.
 *
 * <h2>API Version: v1</h2>
 * <p>Public API surface (safe to use in your mod):
 * <ul>
 *   <li>{@link #register(PostEffectLayer, Runnable)} — register a layer, get a handle back</li>
 *   <li>{@link PostEffectHandle#priority(int)} — control render order</li>
 *   <li>{@link PostEffectHandle#when(java.util.function.BooleanSupplier)} — conditional gating</li>
 *   <li>{@link PostEffectHandle#unregister()} — remove and release GPU resources</li>
 * </ul>
 *
 * <p><strong>Internal API</strong> — do not call from outside this module:
 * <ul>
 *   <li>{@link #applyAll} — called by {@code GameRendererMixin} only</li>
 *   <li>{@link #closeAll} — called by {@code PlateauPostprocessClient} only</li>
 *   <li>{@link #resort} — called by {@code PostEffectHandle} only</li>
 *   <li>{@link #unregister} — called by {@code PostEffectHandle} only</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class PostEffectManager {
    public static final int API_VERSION = 1;

    private static final CrossFrameResourcePool POOL = new CrossFrameResourcePool(3);
    private static final List<PostEffectHandle> HANDLES = new ArrayList<>();

    private PostEffectManager() {}

    /**
     * Register a post-process layer.
     *
     * <p>Example usage:
     * <pre>{@code
     * PostEffectManager.register(
     *     (mc, partialTick, allocator) -> myEffect.apply(mc, partialTick, allocator),
     *     myEffect::close
     * ).priority(10).when(() -> someCondition);
     * }</pre>
     *
     * this must be called in method {@code onInitializeClient()}
     * @param layer   called each frame after {@code renderLevel()}
     * @param onClose called on disconnect to release GPU resources
     * @return a {@link PostEffectHandle} for priority tuning or later removal
     */
    public static PostEffectHandle register(PostEffectLayer layer, Runnable onClose) {
        PostEffectHandle handle = new PostEffectHandle(layer, onClose);
        HANDLES.add(handle);
        PlateauPostprocessClient.LOGGER.info("[PostEffect] Registered layer: {}", layer.getClass().getSimpleName());
        return handle;
    }

    /**
     * Re-sort handles by priority descending.
     *
     * @apiNote Internal — called by {@link PostEffectHandle#priority(int)} only.
     */
    static void resort() {
        HANDLES.sort(Comparator.comparingInt(PostEffectHandle::getPriority).reversed());
    }

    /**
     * Remove and close a specific handle.
     *
     * @apiNote Internal — called by {@link PostEffectHandle#unregister()} only.
     */
    static void unregister(PostEffectHandle handle) {
        if (HANDLES.remove(handle)) handle.getOnClose().run();
    }

    /**
     * Apply all layers in priority order for this frame.
     *
     * @apiNote Internal — called by {@code GameRendererMixin} only. Do not call manually.
     */
    static void applyAll(Minecraft mc, float partialTick) {
        if (HANDLES.isEmpty()) return;
        for (PostEffectHandle handle : HANDLES) {
            if (!handle.getCondition().getAsBoolean()) continue;
            handle.getLayer().apply(mc, partialTick, POOL);
        }
        POOL.endFrame();
    }

    /**
     * Release all GPU resources and clear all registered layers.
     *
     * @apiNote Internal — called by {@code PlateauPostprocessClient} on disconnect only.
     *          Do not call manually — the engine handles this lifecycle automatically.
     */
    static void closeAll() {
        for (PostEffectHandle handle : HANDLES) handle.getOnClose().run();
        HANDLES.clear();
        PlateauPostprocessClient.LOGGER.info("[PostEffect v{}] Closed all layers", API_VERSION);
        POOL.close();
    }
}
