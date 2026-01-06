package com.sypztep.plateau.common.reloadlistener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import org.slf4j.Logger;

public final class ReloadHelper {

    // ============================================
    // DEPRECATED: Synchronous processing modes
    // Use loadJsonResources* + preparable listeners for async
    // ============================================
    @Deprecated
    public static <K> void processJsonResourcesSimple(ResourceManager manager, String folder, Registry<K> registry, Predicate<JsonObject> validator, Function<String, Identifier> idExtractor, BiConsumer<K, JsonObject> processor, String dataType, Logger logger) {
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
    @Deprecated
    public static <K, V> void processJsonResourcesComplex(ResourceManager manager, String folder, Registry<K> registry, Codec<V> codec, Function<String, Identifier> idExtractor, BiConsumer<K, V> processor, String dataType, Logger logger) {
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

    // ============================================
    // SIMPLE MAP MODE: JsonObject -> Map<Id, JsonObject>
    // Async prepare: Parse/validate, no resolution
    // ============================================
    public static Map<Identifier, JsonObject> loadJsonResourcesSimple(
            ResourceManager manager, String folder, Predicate<JsonObject> validator,
            Function<String, Identifier> idExtractor, String dataType, Logger logger) {

        Map<Identifier, JsonObject> result = new HashMap<>();
        AtomicInteger loadedCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        manager.listResourceStacks(folder, path -> path.getPath().endsWith(".json"))
                .forEach((identifier, resources) -> {
                    for (Resource resource : resources) {
                        try (InputStream stream = resource.open()) {
                            JsonObject jsonObject = JsonParser.parseReader(
                                    new JsonReader(new InputStreamReader(stream))
                            ).getAsJsonObject();

                            if (!validator.test(jsonObject)) {
                                logger.error("Invalid {} data in '{}'", dataType, identifier);
                                errorCount.incrementAndGet();
                                continue;
                            }

                            String filePath = identifier.getPath();
                            Identifier entryId = idExtractor.apply(filePath);
                            result.put(entryId, jsonObject);
                            loadedCount.incrementAndGet();

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            logger.error("Failed to prepare {} from '{}': {}", dataType, identifier, e.getMessage());
                            if (logger.isDebugEnabled()) {
                                logger.error("Exception details: ", e);
                            }
                        }
                    }
                });

        logger.info("Prepared {} {} entries ({} errors)", loadedCount.get(), dataType, errorCount.get());
        return result;
    }

    // ============================================
    // COMPLEX MAP MODE: Codec<V> -> Map<Id, V>
    // Async prepare: Parse/decode, no resolution
    // ============================================
    public static <V> Map<Identifier, V> loadJsonResourcesComplex(
            ResourceManager manager, String folder, Codec<V> codec,
            Function<String, Identifier> idExtractor, String dataType, Logger logger) {

        Map<Identifier, V> result = new HashMap<>();
        AtomicInteger loadedCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        manager.listResourceStacks(folder, path -> path.getPath().endsWith(".json"))
                .forEach((identifier, resources) -> {
                    for (Resource resource : resources) {
                        try (InputStream stream = resource.open()) {
                            JsonElement jsonElement = JsonParser.parseReader(
                                    new JsonReader(new InputStreamReader(stream))
                            );

                            String filePath = identifier.getPath();
                            Identifier entryId = idExtractor.apply(filePath);

                            V data = codec.parse(JsonOps.INSTANCE, jsonElement)
                                    .getOrThrow(error -> new RuntimeException("Failed to parse " + dataType + " '" + identifier + "': " + error));

                            result.put(entryId, data);
                            loadedCount.incrementAndGet();

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            logger.error("Failed to prepare {} from '{}': {}", dataType, identifier, e.getMessage());
                            if (logger.isDebugEnabled()) {
                                logger.error("Exception details: ", e);
                            }
                        }
                    }
                });

        logger.info("Prepared {} {} entries ({} errors)", loadedCount.get(), dataType, errorCount.get());
        return result;
    }

    // ============================================
    // ID EXTRACTORS
    // ============================================
    public static Function<String, Identifier> createDefaultIdExtractor() {
        return filePath -> {
            String pathWithoutExt = filePath.substring(0, filePath.length() - 5);
            int folderIndex = pathWithoutExt.indexOf("/");
            if (folderIndex >= 0) {
                String remaining = pathWithoutExt.substring(folderIndex + 1);
                return Identifier.parse(remaining.replace("/", ":"));
            }
            return Identifier.parse(pathWithoutExt);
        };
    }

    public static Function<String, Identifier> createStrictIdExtractor(String folder) {
        return filePath -> {
            if (!filePath.endsWith(".json")) {
                throw new IllegalStateException("Invalid resource path: " + filePath);
            }
            String path = filePath.substring(0, filePath.length() - 5);
            String folderPrefix = folder + "/";
            if (!path.startsWith(folderPrefix)) {
                throw new IllegalStateException("Invalid resource root for " + filePath);
            }
            path = path.substring(folderPrefix.length());
            int slash = path.indexOf('/');
            if (slash < 0) {
                throw new IllegalStateException("Expected " + folder + "/<namespace>/<path>.json but got " + filePath);
            }
            String ns = path.substring(0, slash);
            String p = path.substring(slash + 1);
            return Identifier.fromNamespaceAndPath(ns, p);
        };
    }
    
    private static <K> K resolveTarget(String filePath, Identifier identifier, Registry<K> registry, Function<String, Identifier> idExtractor, String dataType, Logger logger, AtomicInteger errorCount) {
        Identifier entryId = idExtractor.apply(filePath);

        K target = registry.getValue(entryId);
        if (target == null) {
            logger.warn("Unknown {} '{}' in file '{}'", dataType, entryId, identifier);
            errorCount.incrementAndGet();
            return null;
        }
        return target;
    }
}

