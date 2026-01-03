package com.sypztep.plateau.common.reloadlistener;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.util.Map;

public abstract class ComplexDataReloadListener<K, V, E> implements ResourceManagerReloadListener {

    private final String Identifier;
    private final Registry<K> registry;
    private final Codec<V> codec;
    private final String logName;

    protected ComplexDataReloadListener(String Identifier,
                                        Registry<K> registry, Codec<V> codec, String logName) {
        this.Identifier = Identifier;
        this.registry = registry;
        this.codec = codec;
        this.logName = logName;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        getDataMap().clear();

        ReloadHelper.processJsonResourcesComplex(
                manager,
                Identifier,
                registry,
                codec,
                ReloadHelper.createDefaultIdExtractor(),
                this::processCodecData,
                logName,
                getLogger()
        );

        onReloadComplete();
    }

    protected abstract Map<K, E> getDataMap();
    protected abstract Logger getLogger();
    protected abstract void processCodecData(K key, V codecData);

    protected void onReloadComplete() {
        getLogger().info("Loaded {} {} entries", getDataMap().size(), logName);
    }
}