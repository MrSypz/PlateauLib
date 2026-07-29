package com.sypztep.plateau.client.v1.vfx;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A post chain (JSON at {@code assets/<ns>/post_effect/<id>.json}) wired to an
 * arbitrary set of external targets — the {@code main} target is always
 * included implicitly. Thin wrapper over {@code PostChain.load}/{@code addToFrame}:
 * it removes the per-consumer hand-built {@code TargetBundle} and owns the
 * per-frame chain fetch (the {@code ShaderManager} cache invalidates on
 * resource reload, so a chain must be re-fetched every frame, never held).
 *
 * <p>Typical per-frame use inside {@link VfxEffect#contribute}:
 * <pre>{@code
 * if (!chain.prepare(frame)) return FrameContribution.NONE;
 * config.write(b -> { b.putFloat(time); });
 * chain.run(frame);
 * return FrameContribution.RAN;
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class VfxPostChain implements AutoCloseable {
    private final Identifier chainId;
    private final Map<Identifier, Supplier<RenderTarget>> externalTargets;
    private final Set<Identifier> allowList;
    private final List<ManagedUniform> uniforms = new ArrayList<>();
    private PostChain current;

    private VfxPostChain(Identifier chainId, Map<Identifier, Supplier<RenderTarget>> externalTargets) {
        this.chainId = chainId;
        this.externalTargets = externalTargets;
        Set<Identifier> allowed = new HashSet<>(externalTargets.keySet());
        allowed.add(PostChain.MAIN_TARGET_ID);
        this.allowList = Set.copyOf(allowed);
    }

    public static Builder builder(Identifier postChainId) {
        return new Builder(postChainId);
    }

    public static final class Builder {
        private final Identifier chainId;
        private final Map<Identifier, Supplier<RenderTarget>> targets = new LinkedHashMap<>();

        private Builder(Identifier chainId) {
            this.chainId = chainId;
        }

        /** Declare an external target the chain's JSON may reference. Resolved fresh each frame. */
        public Builder target(Identifier id, Supplier<RenderTarget> target) {
            targets.put(id, target);
            return this;
        }

        /** Declare every target of a {@link VfxTargetSet} as an external target. */
        public Builder targets(VfxTargetSet set) {
            for (Identifier id : set.ids()) targets.put(id, () -> set.get(id));
            return this;
        }

        public VfxPostChain build() {
            return new VfxPostChain(chainId, new LinkedHashMap<>(targets));
        }
    }

    /**
     * Declare a dynamic uniform group (must match a {@code "uniforms"} group
     * name in the chain's JSON). Call once at init; the returned handle
     * self-heals across chain recreation.
     */
    public ManagedUniform uniform(String groupName, int byteSize) {
        ManagedUniform uniform = new ManagedUniform(groupName, byteSize);
        uniforms.add(uniform);
        return uniform;
    }

    /**
     * Fetch this frame's chain instance and reconcile uniforms. Returns false
     * (recording a skip reason on the frame) when the chain isn't loaded — e.g.
     * shader compile failure — or an external target is currently null.
     */
    public boolean prepare(VfxFrame frame) {
        current = frame.mc().getShaderManager().getPostChain(chainId, allowList);
        if (current == null) {
            frame.skip("post chain not loaded: " + chainId);
            return false;
        }
        for (Map.Entry<Identifier, Supplier<RenderTarget>> entry : externalTargets.entrySet()) {
            if (entry.getValue().get() == null) {
                frame.skip("external target missing: " + entry.getKey());
                return false;
            }
        }
        for (ManagedUniform uniform : uniforms) uniform.reconcile(current);
        return true;
    }

    /** Run the chain as its own one-shot frame graph. Call only after {@link #prepare} returned true. */
    public void run(VfxFrame frame) {
        FrameGraphBuilder graph = new FrameGraphBuilder();
        addToFrame(graph, frame);
        graph.execute(frame.allocator());
    }

    /**
     * Graft this chain's passes into an existing frame graph instead of
     * building a standalone one — for effects that must compose with other
     * passes' ordering. Call only after {@link #prepare} returned true.
     */
    public void addToFrame(FrameGraphBuilder graph, VfxFrame frame) {
        if (current == null) throw new IllegalStateException("run() before successful prepare()");
        RenderTarget main = frame.mc().getMainRenderTarget();
        Map<Identifier, ResourceHandle<RenderTarget>> handles = new HashMap<>();
        handles.put(PostChain.MAIN_TARGET_ID, graph.importExternal("main", main));
        for (Map.Entry<Identifier, Supplier<RenderTarget>> entry : externalTargets.entrySet()) {
            handles.put(entry.getKey(), graph.importExternal(entry.getKey().toString(), entry.getValue().get()));
        }
        PostChain.TargetBundle bundle = new PostChain.TargetBundle() {
            @Override
            public void replace(Identifier id, @NonNull ResourceHandle<RenderTarget> handle) {
                handles.put(id, handle);
            }

            @Override
            public ResourceHandle<RenderTarget> get(Identifier id) {
                return handles.get(id);
            }
        };
        current.addToFrame(graph, main.width, main.height, bundle);
    }

    @Override
    public void close() {
        for (ManagedUniform uniform : uniforms) uniform.close();
        current = null;
    }
}
