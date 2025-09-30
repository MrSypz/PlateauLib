package com.sypztep.plateau.common.reloadlistener;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.Predicate;

public abstract class SimpleDataReloadListener<K, E> implements SimpleSynchronousResourceReloadListener {

    private final ResourceLocation id;
    private final String resourceLocation;
    private final Registry<K> registry;
    private final String logName;

    protected SimpleDataReloadListener(ResourceLocation id, String resourceLocation,
                                       Registry<K> registry, String logName) {
        this.id = id;
        this.resourceLocation = resourceLocation;
        this.registry = registry;
        this.logName = logName;
    }

    @Override
    public ResourceLocation getFabricId() {
        return id;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        getDataMap().clear();

        ReloadHelper.processJsonResourcesSimple(
                manager,
                resourceLocation,
                registry,
                getValidator(),
                ReloadHelper.createDefaultIdExtractor(),
                this::processEntry,
                logName,
                getLogger()
        );

        onReloadComplete();
    }

    protected abstract Map<K, E> getDataMap();
    protected abstract Logger getLogger();
    protected abstract Predicate<JsonObject> getValidator();
    protected abstract void processEntry(K key, JsonObject json);

    protected void onReloadComplete() {
        getLogger().info("Loaded {} {} entries", getDataMap().size(), logName);
    }
}