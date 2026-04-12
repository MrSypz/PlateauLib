package com.sypztep.plateau.client.v1.postprocess;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.BooleanSupplier;

/**
 * Returned from {@link PostEffectManager#register} — lets you control a registered layer
 * after the fact (priority, conditional gating, unregister).
 */
@Environment(EnvType.CLIENT)
public final class PostEffectHandle {

    private static final BooleanSupplier ALWAYS = () -> true;

    private final PostEffectLayer layer;
    private final Runnable onClose;
    private int priority = 0;
    private BooleanSupplier condition = ALWAYS;

    PostEffectHandle(PostEffectLayer layer, Runnable onClose) {
        this.layer = layer;
        this.onClose = onClose;
    }

    /**
     * Higher priority layers run first. Default is 0.
     */
    public PostEffectHandle priority(int priority) {
        this.priority = priority;
        PostEffectManager.resort();
        return this;
    }

    /**
     * Gate this layer behind a condition. When the supplier returns false the
     * layer is skipped for that frame — no need to unregister/re-register.
     * Register once at startup and let the manager decide when to run it.
     */
    public PostEffectHandle when(BooleanSupplier condition) {
        this.condition = condition == null ? ALWAYS : condition;
        return this;
    }

    /**
     * Remove this layer and release its GPU resources. Idempotent.
     */
    public void unregister() {
        PostEffectManager.unregister(this);
    }

    BooleanSupplier getCondition() {
        return condition;
    }

    int getPriority() {
        return priority;
    }

    PostEffectLayer getLayer() {
        return layer;
    }

    Runnable getOnClose() {
        return onClose;
    }
}
