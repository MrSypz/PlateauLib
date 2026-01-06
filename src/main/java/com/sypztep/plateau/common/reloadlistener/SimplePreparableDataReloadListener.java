package com.sypztep.plateau.common.reloadlistener;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class SimplePreparableDataReloadListener<K, E>
        extends SimpleResourceReloader<Map<Identifier, JsonObject>> {

    protected final String folder;
    protected final Registry<K> registry;
    protected final String logName;

    protected SimplePreparableDataReloadListener(String folder, Registry<K> registry, String logName) {
        this.folder = folder;
        this.registry = registry;
        this.logName = logName;
    }

    @Override
    protected Map<Identifier, JsonObject> prepare(SharedState store) {
        return ReloadHelper.loadJsonResourcesSimple(
                store.resourceManager(),
                folder,
                getValidator(),
                getIdExtractor(),
                logName,
                getLogger()
        );
    }

    @Override
    protected void apply(Map<Identifier, JsonObject> prepared, SharedState store) {
        getDataMap().clear();
        AtomicInteger appliedCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        for (Map.Entry<Identifier, JsonObject> entry : prepared.entrySet()) {
            Identifier entryId = entry.getKey();
            JsonObject json = entry.getValue();

            K key = resolveTarget(entryId);
            if (key == null) {
                errorCount.incrementAndGet();
                continue;
            }

            try {
                processEntry(key, json);
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
    protected abstract Predicate<JsonObject> getValidator();
    protected abstract void processEntry(K key, JsonObject json);  // Put E in concrete impl

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