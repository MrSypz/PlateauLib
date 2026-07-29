package com.sypztep.plateau.client.v1.vfx;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.sypztep.plateau.client.PlateauPostprocessClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Priority-ordered registry of managed screen-space effects.
 *
 * <h2>API Version: v1 (vfx)</h2>
 * <p>Public API surface (safe to use in your mod):
 * <ul>
 *   <li>{@link #register(VfxEffect)} — register an effect, get a {@link VfxHandle} back</li>
 *   <li>{@link VfxHandle#priority(int)} / {@link VfxHandle#when} / {@link VfxHandle#unregister()}</li>
 *   <li>{@link VfxHandle#diagnostics()} — per-effect skip reason + frame cost</li>
 *   <li>{@link #shaderPackActive()} — one shared Iris/shader-pack check</li>
 * </ul>
 *
 * <p><strong>Internal API</strong> — do not call from outside this module:
 * {@link #applyAll}, {@link #dispatchPrepare}, {@link #closeAll}, {@link #resort}.
 */
@Environment(EnvType.CLIENT)
public final class VfxManager {
    public static final int API_VERSION = 2;

    private static final CrossFrameResourcePool POOL = new CrossFrameResourcePool(3);
    private static final List<VfxHandle> HANDLES = new ArrayList<>();

    private VfxManager() {}

    /**
     * Register a managed effect. Call once from {@code onInitializeClient()}.
     *
     * <p>Example:
     * <pre>{@code
     * VfxManager.register(new HeatDistortionRenderer()).priority(10);
     * }</pre>
     */
    public static VfxHandle register(VfxEffect effect) {
        VfxHandle handle = new VfxHandle(effect);
        HANDLES.add(handle);
        resort();
        PlateauPostprocessClient.LOGGER.info("[Vfx] Registered effect: {}", effect.getClass().getSimpleName());
        return handle;
    }

    /**
     * True when a shader pack (Iris) is active and expected to own frame
     * composition. Effects opting in via {@link VfxEffect#skipUnderShaderPacks()}
     * are skipped automatically while this is true.
     */
    public static boolean shaderPackActive() {
        return IrisCheck.ACTIVE.getAsBoolean();
    }

    /**
     * Re-sort handles by priority descending.
     *
     * @apiNote Internal — called by {@link VfxHandle#priority(int)} only.
     */
    static void resort() {
        HANDLES.sort(Comparator.comparingInt(VfxHandle::getPriority).reversed());
    }

    /**
     * Remove and close a specific handle.
     *
     * @apiNote Internal — called by {@link VfxHandle#unregister()} only.
     */
    static void unregister(VfxHandle handle) {
        if (HANDLES.remove(handle)) handle.closeEffect();
    }

    /**
     * Dispatch a level-render phase to every effect that declared it.
     *
     * @apiNote Internal — called by {@code PlateauPostprocessClient}'s Fabric
     *          event listeners only.
     */
    static void dispatchPrepare(VfxLevelPhase phase, LevelRenderContext context) {
        if (HANDLES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        boolean shaderPack = shaderPackActive();
        for (VfxHandle handle : HANDLES) {
            VfxEffect effect = handle.effect();
            if (effect.preparePhase() != phase) continue;
            if (skipByGates(handle, shaderPack)) {
                handle.setProduced(false);
                continue;
            }
            long t0 = System.nanoTime();
            try {
                handle.ensureInit(mc);
                handle.ensureTargetsSized(mc);
                VfxPrepareFrame frame = new VfxPrepareFrame(handle, mc, partialTick, POOL, context);
                handle.setProduced(effect.prepare(frame));
            } catch (Exception e) {
                handle.recordError("prepare()", e);
                handle.setProduced(false);
            }
            handle.setPrepareCost(System.nanoTime() - t0);
        }
    }

    /**
     * Run every effect's contribute pass in priority order for this frame.
     *
     * @apiNote Internal — called by {@code GameRendererMixin} after
     *          {@code renderLevel()} only. Do not call manually.
     */
    static void applyAll(Minecraft mc, float partialTick) {
        if (HANDLES.isEmpty()) return;
        boolean shaderPack = shaderPackActive();
        for (VfxHandle handle : HANDLES) {
            VfxEffect effect = handle.effect();
            boolean produced = handle.consumeProduced();
            long prepareCost = handle.consumePrepareCost();
            if (skipByGates(handle, shaderPack)) continue;
            if (effect.preparePhase() != null && !produced) {
                handle.setSkipReason("prepare produced nothing");
                handle.setLastFrameCost(prepareCost);
                continue;
            }
            long t0 = System.nanoTime();
            try {
                handle.ensureInit(mc);
                handle.ensureTargetsSized(mc);
                VfxFrame frame = new VfxFrame(handle, mc, partialTick, POOL);
                handle.clearSkipReason();
                FrameContribution result = effect.contribute(frame);
                if (result == FrameContribution.NONE && handle.lastSkipReason().isEmpty()) {
                    handle.setSkipReason("no contribution");
                }
            } catch (Exception e) {
                handle.recordError("contribute()", e);
            }
            handle.setLastFrameCost(prepareCost + (System.nanoTime() - t0));
        }
        POOL.endFrame();
    }

    private static boolean skipByGates(VfxHandle handle, boolean shaderPack) {
        if (!handle.passesGate()) {
            handle.setSkipReason("gated by when()");
            return true;
        }
        if (shaderPack && handle.effect().skipUnderShaderPacks()) {
            handle.setSkipReason("shader pack active");
            return true;
        }
        return false;
    }

    /**
     * Release all GPU resources of every registered effect. Effects stay
     * registered and re-init lazily on the next frame they run.
     *
     * @apiNote Internal — called by {@code PlateauPostprocessClient} on
     *          disconnect only. Do not call manually.
     */
    static void closeAll() {
        for (VfxHandle handle : HANDLES) handle.closeEffect();
        PlateauPostprocessClient.LOGGER.info("[Vfx v{}] Closed all effects", API_VERSION);
        POOL.close();
    }

    /** Lazy, reflection-based Iris probe — no compile-time Iris dependency. */
    private static final class IrisCheck {
        static final BooleanSupplier ACTIVE = resolve();

        private static BooleanSupplier resolve() {
            if (!FabricLoader.getInstance().isModLoaded("iris")) return () -> false;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = api.getMethod("getInstance").invoke(null);
                Method inUse = api.getMethod("isShaderPackInUse");
                return () -> {
                    try {
                        return (boolean) inUse.invoke(instance);
                    } catch (Exception e) {
                        return false;
                    }
                };
            } catch (Exception e) {
                PlateauPostprocessClient.LOGGER.warn("[Vfx] Iris detected but IrisApi probe failed", e);
                return () -> false;
            }
        }
    }
}
