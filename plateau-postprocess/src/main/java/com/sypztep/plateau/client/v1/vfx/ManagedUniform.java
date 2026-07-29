package com.sypztep.plateau.client.v1.vfx;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sypztep.plateau.mixin.postprocess.PostChainAccessor;
import com.sypztep.plateau.mixin.postprocess.PostPassAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;

import java.util.Map;
import java.util.function.Consumer;

/**
 * A self-healing custom-uniform slot for a post chain.
 *
 * <p>MC bakes {@code PostPass.customUniforms} once from static JSON literals;
 * the only dynamic path is swapping the {@code GpuBuffer} in that (private)
 * map. This type owns that mechanism: it installs a writable buffer into every
 * pass that declares the uniform group, detects when the {@code PostChain} was
 * recreated (F3+T / {@code /reload} closed our old buffer along with the
 * chain) and transparently recreates + reinstalls. Consumers only ever call
 * {@link #write} — the accessor mixins never leak past this class.
 *
 * <p>Obtain via {@link VfxPostChain#uniform(String, int)}; reconciliation runs
 * inside {@link VfxPostChain#prepare}.
 */
@Environment(EnvType.CLIENT)
public final class ManagedUniform {
    private final String groupName;
    private final int byteSize;
    private GpuBuffer buffer;
    private PostChain boundChain;

    ManagedUniform(String groupName, int byteSize) {
        this.groupName = groupName;
        this.byteSize = byteSize;
    }

    /** Write this frame's values. The buffer is mapped and handed to you as a {@link Std140Builder}. */
    public void write(Consumer<Std140Builder> writer) {
        if (buffer == null) buffer = createBuffer();
        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(buffer, false, true)) {
            writer.accept(Std140Builder.intoBuffer(view.data()));
        }
    }

    /**
     * Install our buffer into {@code chain}, recreating it if the chain is a
     * new instance (MC closed the old buffer when it closed the old chain).
     * Called by {@link VfxPostChain#prepare} each frame.
     */
    void reconcile(PostChain chain) {
        if (chain == boundChain && buffer != null) return;
        if (boundChain != null) {
            // Chain was recreated — our buffer was installed in the old chain's
            // passes and closed together with it. The reference is stale.
            buffer = null;
        }
        if (buffer == null) buffer = createBuffer();
        for (PostPass pass : ((PostChainAccessor) chain).getPasses()) {
            Map<String, GpuBuffer> uniforms = ((PostPassAccessor) pass).getCustomUniforms();
            GpuBuffer existing = uniforms.get(groupName);
            if (existing == null || existing == buffer) continue;
            existing.close();
            uniforms.put(groupName, buffer);
        }
        boundChain = chain;
    }

    private GpuBuffer createBuffer() {
        return RenderSystem.getDevice().createBuffer(
                () -> groupName + "_vfx",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                byteSize
        );
    }

    void close() {
        // If installed in a still-cached chain, the chain now holds a closed
        // buffer — reconcile() replaces it on next use (buffer identity check).
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
        boundChain = null;
    }
}
