package com.sypztep.plateau.common.reloadlistener;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class ComplexDataReloadListener<K, V, E> implements SimpleSynchronousResourceReloadListener {

    private final ResourceLocation id;
    private final String resourceLocation;
    private final Registry<K> registry;
    private final Codec<V> codec;
    private final String logName;

    protected ComplexDataReloadListener(ResourceLocation id, String resourceLocation,
                                        Registry<K> registry, Codec<V> codec, String logName) {
        this.id = id;
        this.resourceLocation = resourceLocation;
        this.registry = registry;
        this.codec = codec;
        this.logName = logName;
    }

    @Override
    public ResourceLocation getFabricId() {
        return id;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        getDataMap().clear();

        ReloadHelper.processJsonResourcesComplex(
                manager,
                resourceLocation,
                registry,
                codec,
                ReloadHelper.createDefaultIdExtractor(),
                this::processCodecData,
                logName,
                getLogger()
        );

        onReloadComplete();
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {
        return SimpleSynchronousResourceReloadListener.super.reload(preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2);
    }

    protected abstract Map<K, E> getDataMap();
    protected abstract Logger getLogger();
    protected abstract void processCodecData(K key, V codecData);

    protected void onReloadComplete() {
        getLogger().info("Loaded {} {} entries", getDataMap().size(), logName);
    }
}