package com.sypztep.plateau.common.v1.network;

import com.google.gson.Gson;
import com.sypztep.plateau.PlateauSyncConfig;
import com.sypztep.plateau.common.v1.config.ConfigSyncUtil;
import com.sypztep.plateau.common.v1.config.RequireSync;
import com.sypztep.plateau.common.v1.config.SyncConfigEntrypoint;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Central registry for the universal server→client config sync system.
 *
 * <p>Any mod that depends on this library registers its server config by implementing
 * {@link SyncConfigEntrypoint} and declaring it in {@code fabric.mod.json}.
 * The lib bootstraps all registrations automatically — no call to
 * {@link #register} inside your own {@code onInitialize()} is needed.
 *
 * <h3>Quickstart:</h3>
 *
 * <p><b>1. Implement {@link SyncConfigEntrypoint}:</b>
 * <pre>{@code
 * public class MyConfigEntrypoint implements SyncConfigEntrypoint {
 *     @Override
 *     public void registerSyncConfigs() {
 *         ConfigSyncRegistry.register(
 *             MyMod.MODID,
 *             MyServerConfig::getInstance,
 *             MyServerConfig.class
 *         );
 *     }
 * }
 * }</pre>
 *
 * <p><b>or the Config class itself</b></p>
 *
 * <pre>{@code
 * // Config class implements the entrypoint itself
 * public final class MyServerConfig implements SyncConfigEntrypoint {
 *
 *     public static MyServerConfig getInstance() { return HANDLER.instance(); }
 *
 *     @Override
 *     public void registerSyncConfigs() {
 *         ConfigSyncRegistry.register(
 *             MyMod.MODID,
 *             MyServerConfig::getInstance,
 *             MyServerConfig.class
 *         );
 *     }
 * }
 * }</pre>
 *
 * <p><b>2. Declare it in {@code fabric.mod.json}:</b>
 * <pre>{@code
 * "entrypoints": {
 *     "plateau-sync-config": [
 *         "com.example.mymod.MyConfigEntrypoint"
 *     ]
 * }
 * }</pre>
 *
 * <p>That's it. The lib calls your entrypoint during its own init, before any player joins.
 *
 * <h3>Checking sync status:</h3>
 * <pre>{@code
 * if (ConfigSyncRegistry.isSyncedFromServer(MyMod.MODID)) {
 *     // client is using server-provided values
 * }
 * }</pre>
 *
 * @see SyncConfigEntrypoint
 * @see ConfigSyncUtil
 */
public final class ConfigSyncRegistry {

    private ConfigSyncRegistry() {
    }

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = PlateauSyncConfig.LOGGER;

    /**
     * Guards against double-bootstrap (e.g. integrated server + client both calling init).
     */
    private static boolean bootstrapped = false;

    // -------------------------------------------------------------------------
    // Internal entry — one per registered namespace
    // -------------------------------------------------------------------------

    /**
     * Holds everything the sync system needs for one registered config namespace.
     *
     * @param <GenericConfig>  the config type
     * @param namespace        unique key used in all packets and cache files
     * @param instanceSupplier supplier of the live server-side singleton
     * @param configClass      class token used by Gson to deserialize received JSON
     * @param configApplier    how to apply a deserialized value; runs on the render thread
     * @param configHasher     how to hash the live config; must be deterministic
     */
    record Entry<GenericConfig>(
            String namespace,
            Supplier<GenericConfig> instanceSupplier,
            Class<GenericConfig> configClass,
            Consumer<GenericConfig> configApplier,
            ToIntFunction<GenericConfig> configHasher
    ) {
        int currentHash() {
            return configHasher.applyAsInt(instanceSupplier.get());
        }

        String toJson() {
            return GSON.toJson(instanceSupplier.get());
        }

        @SuppressWarnings("unchecked")
        static void applyJson(Entry<?> entry, String json) {
            Entry<Object> cast = (Entry<Object>) entry;
            cast.configApplier().accept(GSON.fromJson(json, cast.configClass()));
        }
    }

    private static final Map<String, Entry<?>> REGISTRY = new LinkedHashMap<>();
    private static final Set<String> syncedNamespaces = ConcurrentHashMap.newKeySet();

    // -------------------------------------------------------------------------
    // Bootstrap — called once by PlateauSyncConfig.onInitialize()
    // -------------------------------------------------------------------------

    /**
     * Loads all {@link SyncConfigEntrypoint} implementations declared under
     * {@code "plateau-sync-config"} in consumer mods' {@code fabric.mod.json},
     * then calls {@link SyncConfigEntrypoint#registerSyncConfigs()} on each.
     *
     * <p>Mirrors the pattern used by {@code PlateauAttributeRegistry.bootstrap()}.
     * Called automatically by the lib — consumer mods must not call this.
     * Safe to call multiple times; subsequent calls are no-ops.
     */
    public static void bootstrap() {
        if (bootstrapped) return;

        long start = System.nanoTime();

        List<SyncConfigEntrypoint> entrypoints = FabricLoader.getInstance()
                .getEntrypoints("plateau-sync-config", SyncConfigEntrypoint.class);

        entrypoints.forEach(SyncConfigEntrypoint::registerSyncConfigs);
        bootstrapped = true;

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        if (REGISTRY.isEmpty())
            LOGGER.warn("No configs registered — lib is installed but no entrypoints declared. "
                    + "Players will join normally with no sync. ms: [{}]", elapsedMs);
        else
            LOGGER.info("Bootstrapped {} namespace(s) from {} entrypoint(s) in {} ms: [{}]",
                REGISTRY.size(), entrypoints.size(), elapsedMs,
                String.join(", ", REGISTRY.keySet()));
    }

    // -------------------------------------------------------------------------
    // Registration — called from SyncConfigEntrypoint implementations
    // -------------------------------------------------------------------------

    /**
     * Registers a config using the standard {@link RequireSync }
     *
     * @param <GenericConfig>  the config type; inferred from the arguments
     * @param namespace        unique key — use your mod ID (e.g. {@code "temporature"})
     * @param instanceSupplier returns the live server-side singleton (e.g. {@code MyConfig::getInstance})
     * @param configClass      the {@link Class} token for {@code GenericConfig} (e.g. {@code MyConfig.class})
     * @throws IllegalStateException if {@code namespace} is already registered
     * RequireSync-based applier and hasher. This is the recommended overload.
     *
     * <p>Call this from your {@link SyncConfigEntrypoint#registerSyncConfigs()} implementation.
     */
    public static <GenericConfig> void register(
            String namespace,
            Supplier<GenericConfig> instanceSupplier,
            Class<GenericConfig> configClass) {

        register(
                namespace,
                instanceSupplier,
                configClass,
                received -> ConfigSyncUtil.applyFrom(received, instanceSupplier.get()),
                ConfigSyncUtil::syncHashCode
        );
    }

    /**
     * Registers a config with explicit applier and hasher functions.
     *
     * <p>Use this overload when you need custom apply or hash logic.
     * For most configs annotated with {@code @RequireSync}, the 3-argument overload is preferred.
     *
     * @param <GenericConfig>  the config type; inferred from the arguments
     * @param namespace        unique key — use your mod ID
     * @param instanceSupplier returns the live server-side singleton
     * @param configClass      the {@link Class} token for {@code GenericConfig}
     * @param configApplier    applies a deserialized config to the live singleton; runs on render thread
     * @param configHasher     produces a deterministic {@code int} hash of the config
     * @throws IllegalStateException if {@code namespace} is already registered
     */
    public static <GenericConfig> void register(
            String namespace,
            Supplier<GenericConfig> instanceSupplier,
            Class<GenericConfig> configClass,
            Consumer<GenericConfig> configApplier,
            ToIntFunction<GenericConfig> configHasher) {

        if (REGISTRY.containsKey(namespace))
            throw new IllegalStateException(
                    "Namespace already registered: '" + namespace + "'. "
                            + "Each mod may only call register() once per namespace.");

        REGISTRY.put(namespace, new Entry<>(namespace, instanceSupplier, configClass, configApplier, configHasher));
        LOGGER.debug("Registered namespace '{}'", namespace);
    }

    // -------------------------------------------------------------------------
    // Sync state
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the config for {@code namespace} was applied from a server
     * during the current play session.
     *
     * @param namespace the namespace to check; typically your mod ID
     * @return {@code true} if this namespace has been synced from the server this session
     */
    public static boolean isSyncedFromServer(String namespace) {
        return syncedNamespaces.contains(namespace);
    }

    /**
     * Clears all synced-from-server flags.
     * Called automatically on client disconnect — consumer mods do not need this.
     */
    public static void clearSyncedState() {
        syncedNamespaces.clear();
    }

    // -------------------------------------------------------------------------
    // Internal helpers — network layer only
    // -------------------------------------------------------------------------

    /**
     * Snapshot of every registered namespace mapped to its current hash.
     */
    public static Map<String, Integer> collectHashes() {
        Map<String, Integer> snapshot = new LinkedHashMap<>(REGISTRY.size());
        REGISTRY.forEach((namespace, entry) -> snapshot.put(namespace, entry.currentHash()));
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Single deterministic master hash across all registered configs.
     * Uses alphabetical namespace order — deterministic regardless of registration order.
     * Prefer {@link #masterHash(Map)} if you already have the hash map.
     */
    public static int masterHash() {
        return masterHash(collectHashes());
    }

    /**
     * Computes master hash from an already-collected snapshot.
     */
    public static int masterHash(Map<String, Integer> hashes) {
        int result = 1;
        for (int individualHash : new TreeMap<>(hashes).values())
            result = 31 * result + individualHash;
        return result;
    }

    /**
     * Serializes only the requested namespaces to JSON. Unknown namespaces are skipped.
     */
    public static Map<String, String> serializeFor(Collection<String> namespaces) {
        Map<String, String> result = new LinkedHashMap<>(namespaces.size());
        for (String namespace : namespaces) {
            Entry<?> entry = REGISTRY.get(namespace);
            if (entry != null) result.put(namespace, entry.toJson());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Applies received namespace→JSON pairs on the client and marks each as synced.
     * <b>Must be called on the client render thread.</b>
     *
     * @param receivedData namespace → config JSON, as received in {@code SyncDataS2C}
     */
    public static void applyBatch(Map<String, String> receivedData) {
        receivedData.forEach((namespace, json) -> {
            Entry<?> entry = REGISTRY.get(namespace);
            if (entry != null) {
                Entry.applyJson(entry, json);
                syncedNamespaces.add(namespace);
            }
        });
    }

    /**
     * Unmodifiable view of all registered namespace keys, in insertion order.
     */
    public static Set<String> namespaces() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}