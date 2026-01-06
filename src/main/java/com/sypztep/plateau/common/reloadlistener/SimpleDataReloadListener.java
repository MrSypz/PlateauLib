package com.sypztep.plateau.common.reloadlistener;

import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.Predicate;

@Deprecated
public abstract class SimpleDataReloadListener<K, E> implements ResourceManagerReloadListener {

    private final String Identifier;
    private final Registry<K> registry;
    private final String logName;

    protected SimpleDataReloadListener(String Identifier,
                                       Registry<K> registry, String logName) {
        this.Identifier = Identifier;
        this.registry = registry;
        this.logName = logName;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        getDataMap().clear();

        ReloadHelper.processJsonResourcesSimple(
                manager,
                Identifier,
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