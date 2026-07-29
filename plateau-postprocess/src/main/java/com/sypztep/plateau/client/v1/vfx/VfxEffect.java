package com.sypztep.plateau.client.v1.vfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

/**
 * One managed post-process effect. The effect owns its GPU resources; the
 * {@link VfxManager} owns its lifecycle.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link VfxManager#register(VfxEffect)} once at client init.</li>
 *   <li>{@link #init(VfxContext)} — called lazily on the first frame the effect
 *       actually runs. Allocate GPU resources here; resources registered via
 *       {@link VfxContext#own} are closed automatically.</li>
 *   <li>{@link #prepare(VfxPrepareFrame)} — only if {@link #preparePhase()} is
 *       non-null: called during that level-render phase. Draw masks / run
 *       mid-level passes here. Return {@code true} if this frame produced work.</li>
 *   <li>{@link #contribute(VfxFrame)} — called after {@code renderLevel()},
 *       in priority order. Skipped automatically when a declared prepare phase
 *       produced nothing this frame.</li>
 *   <li>{@link #close()} — called on disconnect and on unregister. After close,
 *       the effect stays registered and re-inits lazily on the next frame it runs.</li>
 * </ol>
 */
@Environment(EnvType.CLIENT)
public interface VfxEffect extends AutoCloseable {

    /** Called once, lazily, on the first frame this effect actually runs (and again after {@link #close()}). */
    void init(VfxContext ctx);

    /**
     * The level-render phase {@link #prepare} should run in, or {@code null}
     * (default) if this effect has no mid-level pass and only contributes after
     * {@code renderLevel()}.
     */
    default @Nullable VfxLevelPhase preparePhase() {
        return null;
    }

    /**
     * Called during the declared {@link #preparePhase()}. Draw into mask
     * targets (see {@link VfxScope}) or run passes that must happen mid-level
     * (e.g. before translucent terrain).
     *
     * @return {@code true} if this frame produced work — the framework then
     *         calls {@link #contribute}; {@code false} skips it. This replaces
     *         the hand-rolled "hasMask" latch: the framework asks at the one
     *         moment the answer is knowable.
     */
    default boolean prepare(VfxPrepareFrame frame) {
        return false;
    }

    /**
     * Called after {@code renderLevel()} in priority order. Run the effect's
     * post chain here. Return {@link FrameContribution#NONE} to skip.
     */
    FrameContribution contribute(VfxFrame frame);

    /**
     * Opt out of running while a shader pack (Iris/Oculus) owns frame
     * composition. Checked once per frame by the manager via
     * {@link VfxManager#shaderPackActive()}.
     */
    default boolean skipUnderShaderPacks() {
        return false;
    }

    /** Release GPU resources not registered via {@link VfxContext#own}. Idempotent. */
    @Override
    void close();
}
