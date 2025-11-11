package com.sypztep.plateau.common.reloadlistener;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class ComplexDataReloadListener<K, V, E> implements ResourceManagerReloadListener {

    private final String resourceLocation;
    private final Registry<K> registry;
    private final Codec<V> codec;
    private final String logName;

    protected ComplexDataReloadListener(String resourceLocation,
                                        Registry<K> registry, Codec<V> codec, String logName) {
        this.resourceLocation = resourceLocation;
        this.registry = registry;
        this.codec = codec;
        this.logName = logName;
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
    public @NotNull CompletableFuture<Void> reload(SharedState sharedState, Executor executor, PreparationBarrier preparationBarrier, Executor executor2) {
        return ResourceManagerReloadListener.super.reload(sharedState, executor, preparationBarrier, executor2);
    }

    protected abstract Map<K, E> getDataMap();
    protected abstract Logger getLogger();
    protected abstract void processCodecData(K key, V codecData);

    protected void onReloadComplete() {
        getLogger().info("Loaded {} {} entries", getDataMap().size(), logName);
    }
}