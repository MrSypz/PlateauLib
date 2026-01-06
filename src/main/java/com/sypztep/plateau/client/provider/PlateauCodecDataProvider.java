package com.sypztep.plateau.client.provider;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Async-optimized codec data provider for datagen.
 * Batches writes with CompletableFuture.allOf for parallelism.
 * No runtime reloading—pure gen-time efficiency.
 */
public abstract class PlateauCodecDataProvider<T> implements DataProvider {

    protected final FabricDataOutput output;
    protected final CompletableFuture<HolderLookup.Provider> registries;
    protected final Codec<T> codec;
    protected final String folder;
    protected final String modId;

    protected PlateauCodecDataProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                       String modId, String folder, Codec<T> codec) {
        this.output = output;
        this.registries = registries;
        this.modId = modId;
        this.folder = folder;
        this.codec = codec;
    }

    @Override
    public @NonNull CompletableFuture<?> run(@NonNull CachedOutput cache) {
        return registries.thenCompose(lookup -> {
            Map<Identifier, JsonElement> entries = new HashMap<>();
            DynamicOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);

            BiConsumer<Identifier, T> writer = (id, value) -> {
                JsonElement json = codec.encodeStart(ops, value)
                        .mapError(msg -> "Invalid entry %s: %s".formatted(id, msg))
                        .getOrThrow();
                if (entries.put(id, json) != null) {
                    throw new IllegalArgumentException("Duplicate entry " + id);
                }
            };

            configure(writer, lookup);

            // Parallel writes: CompletableFuture.allOf for async batching
            return CompletableFuture.allOf(
                    entries.entrySet().parallelStream()
                            .map(entry -> DataProvider.saveStable(cache, entry.getValue(), getPath(entry.getKey())))
                            .toList()
                            .toArray(new CompletableFuture[0])
            );
        });
    }

    protected abstract void configure(BiConsumer<Identifier, T> writer, HolderLookup.Provider lookup);

    private Path getPath(Identifier id) {
        return output.getOutputFolder()
                .resolve("data").resolve(modId).resolve(folder)
                .resolve(id.getNamespace()).resolve(id.getPath() + ".json");
    }

    @Override
    public String getName() {
        return "Plateau Codec Data (" + folder + ")";
    }
}