package com.sypztep.plateau.common.reloadlistener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import org.slf4j.Logger;

public final class ReloadHelper {

    // ============================================
    // SIMPLE MODE - Direct JsonObject processing
    // Fast for prototyping and simple structures
    // ============================================
    public static <K> void processJsonResourcesSimple(ResourceManager manager, String folder, Registry<K> registry, Predicate<JsonObject> validator, Function<String, ResourceLocation> idExtractor, BiConsumer<K, JsonObject> processor, String dataType, Logger logger) {
        AtomicInteger loadedCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        manager.listResourceStacks(folder, path -> path.getPath().endsWith(".json")).forEach((identifier, resources) -> {
            for (Resource resource : resources) {
                try (InputStream stream = resource.open()) {
                    JsonObject jsonObject = JsonParser.parseReader(new JsonReader(new InputStreamReader(stream))).getAsJsonObject();

                    String filePath = identifier.getPath();
                    K target = resolveTarget(filePath, identifier, registry, idExtractor, dataType, logger, errorCount);
                    if (target == null) {
                        continue;
                    }


                    if (!validator.test(jsonObject)) {
                        logger.error("Invalid {} data in '{}'", dataType, identifier);
                        errorCount.incrementAndGet();
                        continue;
                    }

                    processor.accept(target, jsonObject);
                    loadedCount.incrementAndGet();

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("Failed to load {} from '{}': {}", dataType, identifier, e.getMessage());
                    if (logger.isDebugEnabled()) {
                        logger.error("Exception details: ", e);
                    }
                }
            }
        });

        logger.info("Successfully loaded {} {} entries", loadedCount.get(), dataType);
        if (errorCount.get() > 0) {
            logger.warn("Failed to load {} {} entries", errorCount.get(), dataType);
        }
    }

    // ============================================
    // COMPLEX MODE - Codec-based processing
    // Type-safe, validated, production-ready
    // ============================================
    public static <K, V> void processJsonResourcesComplex(ResourceManager manager, String folder, Registry<K> registry, Codec<V> codec, Function<String, ResourceLocation> idExtractor, BiConsumer<K, V> processor, String dataType, Logger logger) {
        AtomicInteger loadedCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        manager.listResourceStacks(folder, path -> path.getPath().endsWith(".json")).forEach((identifier, resources) -> {
            for (Resource resource : resources) {
                try (InputStream stream = resource.open()) {
                    JsonElement jsonElement = JsonParser.parseReader(new JsonReader(new InputStreamReader(stream)));

                    K target = resolveTarget(identifier.getPath(), identifier, registry, idExtractor, dataType, logger, errorCount);
                    if (target == null) continue;

                    V data = codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(error -> new RuntimeException("Failed to parse " + dataType + " '" + identifier + "': " + error));

                    processor.accept(target, data);
                    loadedCount.incrementAndGet();

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("Failed to load {} from '{}': {}", dataType, identifier, e.getMessage());
                    if (logger.isDebugEnabled()) {
                        logger.error("Exception details: ", e);
                    }
                }
            }
        });

        logger.info("Successfully loaded {} {} entries", loadedCount.get(), dataType);
        if (errorCount.get() > 0) {
            logger.warn("Failed to load {} {} entries", errorCount.get(), dataType);
        }
    }

    public static Function<String, ResourceLocation> createDefaultIdExtractor() {
        return filePath -> {
            String pathWithoutExt = filePath.substring(0, filePath.length() - 5);
            int folderIndex = pathWithoutExt.indexOf("/");
            if (folderIndex >= 0) {
                String remaining = pathWithoutExt.substring(folderIndex + 1);
                return ResourceLocation.parse(remaining.replace("/", ":"));
            }
            return ResourceLocation.parse(pathWithoutExt);
        };
    }

    private static <K> K resolveTarget(String filePath, ResourceLocation identifier, Registry<K> registry, Function<String, ResourceLocation> idExtractor, String dataType, Logger logger, AtomicInteger errorCount) {
        ResourceLocation entryId = idExtractor.apply(filePath);

        Optional<Holder.Reference<K>> holderOpt = registry.get(entryId);
        if (holderOpt.isEmpty()) {
            logger.warn("Unknown {} '{}' in file '{}'", dataType, entryId, identifier);
            errorCount.incrementAndGet();
            return null;
        }

        return holderOpt.get().value();
    }
}

