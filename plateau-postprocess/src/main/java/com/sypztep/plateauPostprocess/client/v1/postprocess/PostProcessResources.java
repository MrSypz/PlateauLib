package com.sypztep.plateauPostprocess.client.v1.postprocess;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sypztep.plateauPostprocess.mixin.postprocess.PostChainAccessor;
import com.sypztep.plateauPostprocess.mixin.postprocess.PostPassAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public final class PostProcessResources implements AutoCloseable {
    private final String maskName;
    private final String uniformName;
    private final Identifier maskTargetId;
    private final int uniformBufferSize;

    private TextureTarget maskTarget;
    private GpuBuffer uniformBuffer;
    private int lastWidth;
    private int lastHeight;

    public PostProcessResources(String maskName, Identifier maskTargetId, String uniformName, int uniformBufferSize) {
        this.maskName = maskName;
        this.maskTargetId = maskTargetId;
        this.uniformName = uniformName;
        this.uniformBufferSize = uniformBufferSize;
        this.uniformBuffer = createUniformBuffer();
    }

    private GpuBuffer createUniformBuffer() {
        return RenderSystem.getDevice().createBuffer(
                () -> maskName + "_config",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                uniformBufferSize
        );
    }

    public TextureTarget getMaskTarget() { return maskTarget; }
    public GpuBuffer getUniformBuffer() { return uniformBuffer; }

    /** Ensure mask target matches main render target dimensions. */
    public void ensureMask(Minecraft mc) {
        RenderTarget mainTarget = mc.getMainRenderTarget();
        int w = mainTarget.width;
        int h = mainTarget.height;
        if (maskTarget == null || w != lastWidth || h != lastHeight) {
            if (maskTarget != null) maskTarget.destroyBuffers();
            maskTarget = new TextureTarget(maskName, w, h, false);
            lastWidth = w;
            lastHeight = h;
        }
    }

    /** Clear the mask target to transparent black. */
    public void clearMask() {
        if (maskTarget == null || maskTarget.getColorTextureView() == null) return;
        try (RenderPass ignored = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(
                        () -> "Clear " + maskName,
                        maskTarget.getColorTextureView(),
                        OptionalInt.of(0),
                        null,
                        OptionalDouble.empty()
                )) {
            // Empty pass — just clears
        }
    }

    /**
     * Swap the PostChain's default uniform buffer with our writable one.
     * If the PostChain was recreated (e.g. F3+T resource reload), our old buffer
     * was closed by MC — detect this and recreate.
     */
    public void patchUniforms(PostChain postChain) {
        List<PostPass> passes = ((PostChainAccessor) postChain).getPasses();

        // Detect if PostChain was recreated (it has a different buffer than ours)
        boolean needsRecreate = false;
        for (PostPass pass : passes) {
            Map<String, GpuBuffer> uniforms = ((PostPassAccessor) pass).getCustomUniforms();
            GpuBuffer existing = uniforms.get(uniformName);
            if (existing != null && existing != uniformBuffer) {
                needsRecreate = true;
                break;
            }
        }

        if (needsRecreate) {
            // Old uniformBuffer was closed by MC when PostChain was destroyed — recreate
            uniformBuffer = createUniformBuffer();
        }

        for (PostPass pass : passes) {
            Map<String, GpuBuffer> uniforms = ((PostPassAccessor) pass).getCustomUniforms();
            GpuBuffer existing = uniforms.get(uniformName);
            if (existing != null && existing != uniformBuffer) {
                existing.close();
                uniforms.put(uniformName, uniformBuffer);
            }
        }
    }

    /** Execute a post chain with main + mask targets, using the provided allocator for GPU resources. */
    public void executePostChain(PostChain postChain, Identifier postChainMaskId, GraphicsResourceAllocator allocator) {
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        FrameGraphBuilder fgb = new FrameGraphBuilder();

        ResourceHandle<RenderTarget> mainHandle = fgb.importExternal("main", mainTarget);
        ResourceHandle<RenderTarget> maskHandle = fgb.importExternal(postChainMaskId.getPath(), maskTarget);

        PostChain.TargetBundle bundle = new PostChain.TargetBundle() {
            private ResourceHandle<RenderTarget> main = mainHandle;
            private ResourceHandle<RenderTarget> mask = maskHandle;

            @Override
            public void replace(Identifier id, @NonNull ResourceHandle<RenderTarget> handle) {
                if (id.equals(PostChain.MAIN_TARGET_ID)) this.main = handle;
                else if (id.equals(maskTargetId)) this.mask = handle;
            }

            @Override
            public ResourceHandle<RenderTarget> get(Identifier id) {
                if (id.equals(PostChain.MAIN_TARGET_ID)) return this.main;
                if (id.equals(maskTargetId)) return this.mask;
                return null;
            }
        };

        postChain.addToFrame(fgb, mainTarget.width, mainTarget.height, bundle);
        fgb.execute(allocator);
    }

    @Override
    public void close() {
        if (maskTarget != null) { maskTarget.destroyBuffers(); maskTarget = null; }
        if (uniformBuffer != null) { uniformBuffer.close(); uniformBuffer = null; }
        lastWidth = 0;
        lastHeight = 0;
    }
}
