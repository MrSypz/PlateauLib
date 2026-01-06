package com.sypztep.plateau.common.reloadlistener;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class ComplexPreparableDataReloadListener<K, V, E>
        extends SimpleResourceReloader<Map<Identifier, V>> {

    protected final String folder;
    protected final Registry<K> registry;
    protected final Codec<V> codec;
    protected final String logName;

    protected ComplexPreparableDataReloadListener(String folder, Registry<K> registry,
                                                  Codec<V> codec, String logName) {
        this.folder = folder;
        this.registry = registry;
        this.codec = codec;
        this.logName = logName;
    }

    @Override
    protected Map<Identifier, V> prepare(SharedState store) {
        return ReloadHelper.loadJsonResourcesComplex(
                store.resourceManager(),
                folder,
                codec,
                getIdExtractor(),
                logName,
                getLogger()
        );
    }

    @Override
    protected void apply(Map<Identifier, V> prepared, SharedState store) {
        getDataMap().clear();
        AtomicInteger appliedCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        for (Map.Entry<Identifier, V> entry : prepared.entrySet()) {
            Identifier entryId = entry.getKey();
            V data = entry.getValue();

            K key = resolveTarget(entryId);
            if (key == null) {
                errorCount.incrementAndGet();
                continue;
            }

            try {
                processCodecData(key, data);
                appliedCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
                getLogger().error("Failed to process {} '{}' : {}", logName, entryId, e.getMessage());
            }
        }

        getLogger().info("Applied {} {} entries ({} errors)", appliedCount.get(), logName, errorCount.get());
        onReloadComplete();
    }

    // Abstracts
    protected abstract Map<K, E> getDataMap();
    protected abstract Logger getLogger();
    protected abstract void processCodecData(K key, V codecData);  // Put E in concrete impl

    protected void onReloadComplete() {
        getLogger().info("Loaded {} {} entries", getDataMap().size(), logName);
    }

    protected Function<String, Identifier> getIdExtractor() {
        return ReloadHelper.createDefaultIdExtractor();
    }

    private K resolveTarget(Identifier entryId) {
        K target = registry.getValue(entryId);
        if (target == null) {
            getLogger().warn("Unknown {} '{}' ", logName, entryId);
            return null;
        }
        return target;
    }
}