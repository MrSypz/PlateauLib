package com.sypztep.plateau.client.v1.vfx;

import com.sypztep.plateau.client.PlateauPostprocessClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Returned from {@link VfxManager#register} — controls a registered effect
 * after the fact (priority, conditional gating, unregister) and exposes its
 * {@link VfxDiagnostics}.
 */
@Environment(EnvType.CLIENT)
public final class VfxHandle implements VfxDiagnostics {

    private static final BooleanSupplier ALWAYS = () -> true;
    private static final long LOG_THROTTLE_MS = 5000;

    private final VfxEffect effect;
    private int priority = 0;
    private BooleanSupplier condition = ALWAYS;

    // Framework-owned lifecycle state
    private boolean inited;
    private boolean produced;
    private long prepareCostNanos;
    private final List<AutoCloseable> owned = new ArrayList<>();
    private final List<VfxTargetSet> targetSets = new ArrayList<>();

    // Diagnostics
    private String skipReason;
    private long lastFrameCostNanos;
    private long lastLogTimeMs;

    VfxHandle(VfxEffect effect) {
        this.effect = effect;
    }

    /** Higher priority effects run first (within a prepare phase and in contribute order). Default is 0. */
    public VfxHandle priority(int priority) {
        this.priority = priority;
        VfxManager.resort();
        return this;
    }

    /**
     * Gate this effect behind a condition, checked before both {@code prepare}
     * and {@code contribute} each frame — no need to unregister/re-register.
     */
    public VfxHandle when(BooleanSupplier condition) {
        this.condition = condition == null ? ALWAYS : condition;
        return this;
    }

    /** Remove this effect and release its GPU resources. Idempotent. */
    public void unregister() {
        VfxManager.unregister(this);
    }

    public VfxDiagnostics diagnostics() {
        return this;
    }

    @Override
    public Optional<String> lastSkipReason() {
        return Optional.ofNullable(skipReason);
    }

    @Override
    public long lastFrameCostNanos() {
        return lastFrameCostNanos;
    }

    // ── Internal — VfxManager only ──

    VfxEffect effect() {
        return effect;
    }

    int getPriority() {
        return priority;
    }

    boolean passesGate() {
        return condition.getAsBoolean();
    }

    void ensureInit(Minecraft mc) {
        if (inited) return;
        effect.init(new VfxContext(this, mc));
        inited = true;
        PlateauPostprocessClient.LOGGER.info("[Vfx] Initialized effect: {}", effect.getClass().getSimpleName());
    }

    void ensureTargetsSized(Minecraft mc) {
        for (VfxTargetSet set : targetSets) set.ensureSized(mc);
    }

    void own(AutoCloseable resource) {
        owned.add(resource);
    }

    void ownTargets(VfxTargetSet targets) {
        owned.add(targets);
        targetSets.add(targets);
    }

    void setProduced(boolean produced) {
        this.produced = produced;
    }

    /** Read and reset the prepare-phase "did this frame produce work" flag. */
    boolean consumeProduced() {
        boolean p = produced;
        produced = false;
        return p;
    }

    void setPrepareCost(long nanos) {
        this.prepareCostNanos = nanos;
    }

    /** Read and reset this frame's prepare cost. */
    long consumePrepareCost() {
        long c = prepareCostNanos;
        prepareCostNanos = 0;
        return c;
    }

    void setLastFrameCost(long nanos) {
        this.lastFrameCostNanos = nanos;
    }

    void setSkipReason(String reason) {
        this.skipReason = reason;
    }

    void clearSkipReason() {
        this.skipReason = null;
    }

    /** Record an exception as the skip reason, with throttled logging. */
    void recordError(String phase, Exception e) {
        skipReason = "error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        long now = System.currentTimeMillis();
        if (now - lastLogTimeMs > LOG_THROTTLE_MS) {
            lastLogTimeMs = now;
            PlateauPostprocessClient.LOGGER.error("[Vfx] {} failed in {}",
                    effect.getClass().getSimpleName(), phase, e);
        }
    }

    /**
     * Close the effect and every owned resource. Idempotent — after this the
     * handle stays registered and the effect re-inits lazily on next use.
     */
    void closeEffect() {
        if (!inited) return;
        inited = false;
        produced = false;
        try {
            effect.close();
        } catch (Exception e) {
            PlateauPostprocessClient.LOGGER.error("[Vfx] {} close() failed", effect.getClass().getSimpleName(), e);
        }
        for (int i = owned.size() - 1; i >= 0; i--) {
            try {
                owned.get(i).close();
            } catch (Exception e) {
                PlateauPostprocessClient.LOGGER.error("[Vfx] owned resource close failed for {}",
                        effect.getClass().getSimpleName(), e);
            }
        }
        owned.clear();
        targetSets.clear();
    }
}
