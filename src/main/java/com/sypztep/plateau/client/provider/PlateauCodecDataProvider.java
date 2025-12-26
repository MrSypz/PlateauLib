package com.sypztep.plateau.client.provider;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public abstract class PlateauCodecDataProvider<T> implements DataProvider {

    protected final FabricDataOutput dataOutput;
    private final CompletableFuture<HolderLookup.Provider> registriesFuture;
    private final Codec<T> codec;
    private final String folderName;
    private final String modId;

    protected PlateauCodecDataProvider(FabricDataOutput dataOutput,
                                       CompletableFuture<HolderLookup.Provider> registriesFuture,
                                       String modId,
                                       String folderName,
                                       Codec<T> codec) {
        this.dataOutput = dataOutput;
        this.registriesFuture = registriesFuture;
        this.modId = modId;
        this.folderName = folderName;
        this.codec = codec;
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput writer) {
        return this.registriesFuture.thenCompose(lookup -> {
            Map<ResourceLocation, JsonElement> entries = new HashMap<>();
            DynamicOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);

            BiConsumer<ResourceLocation, T> provider = (id, value) -> {
                JsonElement json = this.convert(id, value, ops);
                JsonElement existingJson = entries.put(id, json);

                if (existingJson != null) {
                    throw new IllegalArgumentException("Duplicate entry " + id);
                }
            };

            this.configure(provider, lookup);
            return this.write(writer, entries);
        });
    }

    protected abstract void configure(BiConsumer<ResourceLocation, T> provider, HolderLookup.Provider lookup);

    private JsonElement convert(ResourceLocation id, T value, DynamicOps<JsonElement> ops) {
        return this.codec.encodeStart(ops, value)
                .mapError(message -> "Invalid entry %s: %s".formatted(id, message))
                .getOrThrow();
    }

    private CompletableFuture<?> write(CachedOutput writer, Map<ResourceLocation, JsonElement> entries) {
        return CompletableFuture.allOf(entries.entrySet().stream().map(entry -> {
            Path path = this.getCustomPath(entry.getKey());
            return DataProvider.saveStable(writer, entry.getValue(), path);
        }).toArray(CompletableFuture[]::new));
    }

    private Path getCustomPath(ResourceLocation id) {
        return this.dataOutput.getOutputFolder()
                .resolve("data")
                .resolve(modId)
                .resolve(folderName)
                .resolve(id.getNamespace())
                .resolve(id.getPath() + ".json");
    }
}