package com.sypztep.plateau.common.v1.network;

import com.google.gson.Gson;
import com.sypztep.plateau.PlateauSyncConfig;
import com.sypztep.plateau.common.v1.config.ConfigSyncUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Central registry for the universal server→client config sync system.
 *
 * <p>Any mod that depends on this library registers its server config here once
 * during mod initialization. The sync system then batches <em>all</em> registered
 * configs into a single handshake per player join — regardless of how many mods
 * have registered. Joining a server with 10 mods costs one player freeze, one
 * {@code SyncHelloS2C}, one {@code SyncResponseC2S}, and optionally one
 * {@code SyncDataS2C} + {@code SyncAckC2S}. Not 10 of each.
 *
 * <hr>
 * <h3>Quickstart — preferred 3-argument form:</h3>
 *
 * <p>If your config uses {@link com.sypztep.plateau.common.v1.config.RequireSync @RequireSync}
 * on its fields (which is the recommended pattern), the library can derive the applier and
 * hasher automatically. Use the short overload:
 *
 * <pre>{@code
 * // In your ModInitializer.onInitialize():
 * ConfigSyncRegistry.register(
 *     MyMod.MODID,               // namespace — must be unique, use your mod ID
 *     MyServerConfig::getInstance, // how to get the live singleton
 *     MyServerConfig.class         // class token for Gson deserialization
 * );
 * }</pre>
 *
 * <h3>Advanced 5-argument form:</h3>
 *
 * <p>Use the full overload when you need custom apply or hash logic:
 *
 * <pre>{@code
 * ConfigSyncRegistry.register(
 *     MyMod.MODID,
 *     MyServerConfig::getInstance,
 *     MyServerConfig.class,
 *     receivedConfig -> {
 *         MyServerConfig.applyFrom(receivedConfig);
 *         MyServerConfig.setSyncedFromServer(true); // optional legacy flag
 *     },
 *     ConfigSyncUtil::syncHashCode
 * );
 * }</pre>
 *
 * <h3>Checking sync status:</h3>
 *
 * <pre>{@code
 * if (ConfigSyncRegistry.isSyncedFromServer(MyMod.MODID)) {
 *     // client is using server-provided values
 * }
 * }</pre>
 *
 * <hr>
 * <p><b>Thread safety:</b> {@link #REGISTRY} is written only during mod init (single-threaded)
 * and read afterwards. {@link #syncedNamespaces} is a concurrent set and safe to read/write
 * from any thread. {@link #applyBatch} must be called on the client render thread; this is
 * enforced by the library's packet receivers.
 *
 * @see com.sypztep.plateau.common.v1.config.SyncConfig @SyncConfig
 * @see com.sypztep.plateau.common.v1.config.RequireSync @RequireSync
 * @see ConfigSyncUtil
 */
public final class ConfigSyncRegistry {

    private ConfigSyncRegistry() {}

    private static final Gson GSON = new Gson();

    // -------------------------------------------------------------------------
    // Internal entry — one per registered namespace
    // -------------------------------------------------------------------------

    /**
     * Internal record holding everything the sync system needs for one registered config.
     *
     * @param <GenericConfig> the config type
     * @param namespace       unique key for this config in all packets and cache files
     * @param instanceSupplier supplier of the live server-side singleton
     * @param configClass     class token used by Gson to deserialize received JSON
     * @param configApplier   how to apply a freshly deserialized value; runs on the render thread
     * @param configHasher    how to produce a hash of the live config; used for fast-path comparison
     */
    record Entry<GenericConfig>(
            String namespace,
            Supplier<GenericConfig> instanceSupplier,
            Class<GenericConfig> configClass,
            Consumer<GenericConfig> configApplier,
            ToIntFunction<GenericConfig> configHasher
    ) {
        /** Returns the hash of the current live config value. */
        int currentHash() {
            return configHasher.applyAsInt(instanceSupplier.get());
        }

        /** Serializes the current live config value to a JSON string. */
        String toJson() {
            return GSON.toJson(instanceSupplier.get());
        }

        /**
         * Deserializes {@code json} and passes the result to {@code configApplier}.
         * The unchecked cast is safe because the entry's type parameters are consistent.
         */
        @SuppressWarnings("unchecked")
        static void applyJson(Entry<?> entry, String json) {
            Entry<Object> cast = (Entry<Object>) entry;
            cast.configApplier().accept(GSON.fromJson(json, cast.configClass()));
        }
    }

    /**
     * The main registry map. Uses {@link LinkedHashMap} to preserve insertion order;
     * master hash computation uses {@link TreeMap} for alphabetical determinism.
     *
     * <p>Written only during mod initialization (before any player joins).
     * Read concurrently afterwards from packet handler threads and the server thread.
     */
    private static final Map<String, Entry<?>> REGISTRY = new LinkedHashMap<>();

    /**
     * Tracks which namespaces have been applied from a server in the current session.
     * Written by {@link #applyBatch} and cleared by {@link #clearSyncedState}.
     */
    private static final Set<String> syncedNamespaces = ConcurrentHashMap.newKeySet();

    // -------------------------------------------------------------------------
    // Registration — public API
    // -------------------------------------------------------------------------

    /**
     * Registers a config for server→client sync using the standard {@link RequireSync}-based
     * applier and hasher provided by this library.
     *
     * <p>This is the <b>recommended overload</b> for any config that uses
     * {@link com.sypztep.plateau.common.v1.config.RequireSync @RequireSync} on its fields.
     * You do not need to write {@code applyFrom()}, {@code hashCode()}, or manage a
     * {@code syncedFromServer} flag yourself — the library handles all of that.
     *
     * <p>Must be called during mod initialization, before any player joins the server.
     *
     * @param <GenericConfig>  the config type; inferred from the arguments
     * @param namespace        unique key for this config — use your mod ID (e.g. {@code "temporature"}).
     *                         Changing this after release will invalidate all existing client caches.
     * @param instanceSupplier a {@link Supplier} that returns the <em>live server-side singleton</em>
     *                         (e.g. {@code MyServerConfig::getInstance}). Called on demand — do not
     *                         pass a captured snapshot.
     * @param configClass      the {@link Class} token for {@code GenericConfig}, used by Gson to
     *                         deserialize the received JSON on the client
     *                         (e.g. {@code MyServerConfig.class})
     * @throws IllegalStateException if {@code namespace} is already registered
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
     * Registers a config for server→client sync with explicit applier and hasher functions.
     *
     * <p>Use this overload when you need custom apply or hash logic — for example, if your
     * config has post-apply side effects or uses a different hashing strategy.
     *
     * <p>For most configs annotated with {@link com.sypztep.plateau.common.v1.config.RequireSync @RequireSync},
     * the 3-argument overload is simpler and preferred.
     *
     * <p>Must be called during mod initialization, before any player joins the server.
     *
     * @param <GenericConfig>  the config type; inferred from the arguments
     * @param namespace        unique key for this config — use your mod ID
     * @param instanceSupplier a {@link Supplier} that returns the live server-side singleton
     * @param configClass      the {@link Class} token for {@code GenericConfig}, used by Gson
     * @param configApplier    a {@link Consumer} that applies a freshly deserialized config value
     *                         to the live singleton. Called on the <b>client render thread</b>.
     * @param configHasher     a {@link ToIntFunction} that produces an {@code int} hash of the config.
     *                         Must be deterministic and produce the same result on both server and client
     *                         for the same logical config state. {@link ConfigSyncUtil#syncHashCode}
     *                         satisfies this requirement for {@link RequireSync}-annotated fields.
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
                    "[ConfigSync] Namespace already registered: '" + namespace + "'. "
                    + "Each mod may only register once. Check for duplicate register() calls.");

        REGISTRY.put(namespace, new Entry<>(namespace, instanceSupplier, configClass, configApplier, configHasher));
        PlateauSyncConfig.LOGGER.debug("[ConfigSync] Registered config namespace '{}'", namespace);
    }

    // -------------------------------------------------------------------------
    // Sync state — public API
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the config for {@code namespace} was applied from a server
     * during the current session, rather than loaded from the local config file.
     *
     * <p>Useful for gating client-side behavior that depends on whether the server
     * has overridden the local defaults — for example, disabling a local config screen.
     *
     * <pre>{@code
     * if (ConfigSyncRegistry.isSyncedFromServer(MyMod.MODID)) {
     *     // using server config — don't let player change these values locally
     * }
     * }</pre>
     *
     * @param namespace the namespace to check; typically your mod ID
     * @return {@code true} if this namespace has been synced from the server this session
     */
    public static boolean isSyncedFromServer(String namespace) {
        return syncedNamespaces.contains(namespace);
    }

    /**
     * Clears the set of namespaces marked as synced from the server.
     *
     * <p>Called automatically by the library on client disconnect.
     * Consumer mods do not need to call this directly.
     */
    public static void clearSyncedState() {
        syncedNamespaces.clear();
    }

    // -------------------------------------------------------------------------
    // Internal helpers — used by the network layer; not part of public API
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot of every registered namespace mapped to its current hash.
     *
     * <p>Used by the server when building {@code SyncHelloS2C} and by
     * {@link #masterHash()} when computing the combined hash. The snapshot is
     * collected at call time; callers that need a consistent view should capture
     * this result and pass it to {@link #masterHash(Map)} rather than calling
     * {@link #masterHash()} separately.
     *
     * @return an unmodifiable, insertion-ordered map of namespace → current hash
     */
    public static Map<String, Integer> collectHashes() {
        Map<String, Integer> snapshot = new LinkedHashMap<>(REGISTRY.size());
        REGISTRY.forEach((namespace, entry) -> snapshot.put(namespace, entry.currentHash()));
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Computes a single deterministic master hash across all registered configs.
     *
     * <p>Namespaces are iterated in alphabetical order (via {@link TreeMap}) so the
     * result is the same regardless of registration order. Both server and client must
     * compute the master hash using identical inputs for the fast-path comparison to work.
     *
     * <p>Prefer {@link #masterHash(Map)} when you have already called {@link #collectHashes()}
     * to avoid a redundant second round of hash computation.
     *
     * @return the combined master hash of all registered configs
     */
    public static int masterHash() {
        return masterHash(collectHashes());
    }

    /**
     * Computes the master hash from an already-collected hash map.
     *
     * <p>This overload exists so that the server can collect hashes once for the
     * {@code SyncHelloS2C} packet and reuse the same values for the master hash
     * logged at join time, without triggering a second round of hash computation.
     *
     * <p>The client uses this overload to compute its local master hash from the
     * hashes received in the {@code SyncHelloS2C} packet, ensuring both sides
     * use exactly the same values.
     *
     * @param hashes a map of namespace → individual hash (e.g. from {@link #collectHashes()})
     * @return the combined master hash, computed in alphabetical namespace order
     */
    public static int masterHash(Map<String, Integer> hashes) {
        int result = 1;
        // TreeMap enforces alphabetical order — deterministic regardless of insertion order
        for (int individualHash : new TreeMap<>(hashes).values()) {
            result = 31 * result + individualHash;
        }
        return result;
    }

    /**
     * Serializes the configs for the requested namespaces to JSON strings.
     *
     * <p>Used by the server when building a {@code SyncDataS2C} packet.
     * Unknown namespaces (not present in the registry) are silently skipped.
     *
     * @param namespaces the namespace keys to serialize
     * @return an unmodifiable, insertion-ordered map of namespace → JSON string
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
     * Applies a batch of namespace→JSON pairs on the client and marks each applied
     * namespace as synced from the server.
     *
     * <p><b>Must be called on the client render thread.</b> The library's packet
     * receivers wrap calls to this method in {@code client.execute()} — consumer mods
     * do not need to do this themselves.
     *
     * <p>Unknown namespaces (not present in the registry) are silently skipped,
     * which is safe when a server has a mod the client does not.
     *
     * @param receivedData a map of namespace → JSON string, as received in {@code SyncDataS2C}
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
     * Returns an unmodifiable view of all registered namespace keys.
     *
     * <p>Used by the server to enumerate all configs when a client requests a full sync.
     *
     * @return the set of all registered namespaces, in insertion order
     */
    public static Set<String> namespaces() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
