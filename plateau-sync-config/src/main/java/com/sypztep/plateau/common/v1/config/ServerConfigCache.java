package com.sypztep.plateau.common.v1.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sypztep.plateau.PlateauSyncConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side persistent cache for server configs.
 *
 * <p>Each server the client has connected to gets its own JSON file on disk.
 * All registered namespaces for a given server are stored in a single file —
 * so joining a server that has 10 synced configs still costs only one disk read.
 *
 * <h3>File layout on disk:</h3>
 * <pre>
 *   .minecraft/config/plateau-sync-config/servers/{md5(serverAddress)}.json
 * </pre>
 *
 * <h3>File format (one file per server address):</h3>
 * <pre>{@code
 * {
 *   "temporature": { "hash": 123456789, "json": "{...config fields...}" },
 *   "leklai":      { "hash": 987654321, "json": "{...config fields...}" }
 * }
 * }</pre>
 *
 * <h3>Cache lifecycle:</h3>
 * <ol>
 *   <li>On join — {@link #isValid} checks if the stored hash matches the server's hello hash.</li>
 *   <li>Fast path — {@link #tryLoad} returns the stored JSON; no data packet needed.</li>
 *   <li>Slow path — after receiving new data, {@link #save} writes the updated entry.</li>
 *   <li>On disconnect — {@link #evict} removes the in-memory entry so the next session
 *       re-reads from disk (picks up any changes written by the slow path).</li>
 * </ol>
 *
 * <h3>Thread safety:</h3>
 * <p>The in-memory cache ({@link #memoryCache}) is a {@link ConcurrentHashMap} of server
 * address → namespace map. The inner maps are plain {@link LinkedHashMap}s that are only
 * written inside {@link #writeAll}, which is always called from the client render thread.
 * Reads from {@link #readAll} are safe because {@code computeIfAbsent} is atomic.
 */
public final class ServerConfigCache {

    private ServerConfigCache() {}

    private static final Gson GSON = new Gson();
    private static final Type CACHE_FILE_TYPE = new TypeToken<Map<String, NamespaceEntry>>(){}.getType();

    /**
     * One entry per namespace inside a per-server cache file.
     *
     * @param hash the config hash at the time this entry was written
     * @param json the serialized config JSON
     */
    private record NamespaceEntry(int hash, String json) {}

    /**
     * In-memory write-through cache: server address → (namespace → entry).
     * Avoids redundant disk reads during the same play session.
     */
    private static final Map<String, Map<String, NamespaceEntry>> memoryCache = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the client has a cached entry for {@code namespace}
     * on {@code serverAddress} whose stored hash matches {@code expectedHash}.
     *
     * <p>Does not deserialize the config JSON — use this for the fast-path check
     * where you only need to know whether the cache is still valid.
     *
     * @param serverAddress the server's IP/address string (from {@code getCurrentServer().ip})
     * @param namespace     the config namespace to check (typically a mod ID)
     * @param expectedHash  the hash sent by the server in {@code SyncHelloS2C}
     * @return {@code true} if a valid, non-stale cache entry exists for this namespace
     */
    public static boolean isValid(String serverAddress, String namespace, int expectedHash) {
        NamespaceEntry entry = readAll(serverAddress).get(namespace);
        return entry != null && entry.hash() == expectedHash;
    }

    /**
     * Returns the cached config JSON for {@code namespace} on {@code serverAddress}
     * if the stored hash matches {@code expectedHash}.
     *
     * <p>Returns {@link Optional#empty()} if:
     * <ul>
     *   <li>no cache file exists for this server</li>
     *   <li>the namespace is not present in the cache file</li>
     *   <li>the stored hash does not match {@code expectedHash} (stale cache)</li>
     * </ul>
     *
     * @param serverAddress the server's IP/address string
     * @param namespace     the config namespace to look up
     * @param expectedHash  the hash sent by the server in {@code SyncHelloS2C}
     * @return an {@link Optional} containing the cached JSON, or empty if not valid
     */
    public static Optional<String> tryLoad(String serverAddress, String namespace, int expectedHash) {
        NamespaceEntry entry = readAll(serverAddress).get(namespace);
        if (entry == null || entry.hash() != expectedHash) return Optional.empty();
        return Optional.of(entry.json());
    }

    /**
     * Saves (or overwrites) the cache entry for {@code namespace} on {@code serverAddress}.
     *
     * <p>The full per-server file is re-written on each call so all namespaces remain
     * in a single file. Existing entries for other namespaces on the same server are preserved.
     *
     * <p>Should be called from the client render thread after applying new config data
     * received in a {@code SyncDataS2C} packet.
     *
     * @param serverAddress the server's IP/address string
     * @param namespace     the config namespace being saved
     * @param json          the serialized config JSON received from the server
     * @param hash          the hash of the config at the time it was received
     */
    public static void save(String serverAddress, String namespace, String json, int hash) {
        Map<String, NamespaceEntry> current = new LinkedHashMap<>(readAll(serverAddress));
        current.put(namespace, new NamespaceEntry(hash, json));
        writeAll(serverAddress, current);
    }

    /**
     * Removes the in-memory cache entry for {@code serverAddress}.
     *
     * <p>Called automatically by the library on client disconnect so that the next
     * session re-reads from disk, picking up any updates written during the slow path.
     *
     * <p>Consumer mods do not need to call this directly.
     *
     * @param serverAddress the server's IP/address string to evict
     */
    public static void evict(String serverAddress) {
        memoryCache.remove(serverAddress);
        PlateauSyncConfig.LOGGER.debug("Evicted cache for '{}'", serverAddress);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Reads the per-server cache file into memory, or returns the in-memory copy
     * if it has already been loaded this session.
     *
     * <p>If the file cannot be read or is in an unrecognized format (e.g. from an
     * older version of the library), it is deleted and an empty map is returned.
     * The server will then fall through to the slow path and write a fresh file.
     *
     * @param serverAddress the server address used to locate the cache file
     * @return a mutable map of namespace → entry (not the cached reference — callers may copy it)
     */
    private static Map<String, NamespaceEntry> readAll(String serverAddress) {
        return memoryCache.computeIfAbsent(serverAddress, address -> {
            Path cachePath = resolvedCachePath(address);
            if (!Files.exists(cachePath)) return new LinkedHashMap<>();

            try {
                String rawJson = Files.readString(cachePath, StandardCharsets.UTF_8);
                Map<String, NamespaceEntry> parsed = GSON.fromJson(rawJson, CACHE_FILE_TYPE);
                return parsed != null ? parsed : new LinkedHashMap<>();
            } catch (IOException ioException) {
                PlateauSyncConfig.LOGGER.warn(
                        "Could not read cache file for '{}': {}",
                        address, ioException.getMessage());
                return new LinkedHashMap<>();
            } catch (Exception parseException) {
                // Catches JsonSyntaxException / IllegalStateException from stale/corrupt formats.
                // Safe to treat as a miss — server will send fresh data on slow path.
                PlateauSyncConfig.LOGGER.warn(
                        "Cache file for '{}' is unreadable ({}), resetting.",
                        address, parseException.getMessage());
                deleteSilently(cachePath);
                return new LinkedHashMap<>();
            }
        });
    }

    /**
     * Writes the full namespace map to the per-server cache file and updates the in-memory cache.
     *
     * @param serverAddress the server address used to locate the cache file
     * @param entries       the complete, updated namespace map to persist
     */
    private static void writeAll(String serverAddress, Map<String, NamespaceEntry> entries) {
        memoryCache.put(serverAddress, entries);
        Path cachePath = resolvedCachePath(serverAddress);
        try {
            Files.createDirectories(cachePath.getParent());
            Files.writeString(cachePath, GSON.toJson(entries), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            PlateauSyncConfig.LOGGER.error(
                    "Failed to write cache file for '{}': {}",
                    serverAddress, ioException.getMessage());
        }
    }

    /**
     * Resolves the path of the cache file for a given server address.
     *
     * <p>Server addresses may contain characters that are illegal in file paths
     * (colons, slashes, brackets for IPv6). The address is hashed to a short,
     * safe hex string using MD5.
     *
     * @param serverAddress the raw server address string
     * @return the resolved {@link Path} for the cache file
     */
    private static Path resolvedCachePath(String serverAddress) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(PlateauSyncConfig.MODID)
                .resolve("servers")
                .resolve(addressToFilename(serverAddress) + ".json");
    }

    /**
     * Hashes {@code serverAddress} to an 8-character lowercase hex string safe for use
     * as a filename on all operating systems.
     *
     * <p>Only the first 8 bytes of the MD5 digest are used (16 hex chars) — this is
     * sufficient to distinguish servers in practice while keeping filenames short.
     * Full collision resistance is not required; the stored {@code serverAddress} field
     * inside the file is the authoritative key.
     *
     * @param serverAddress the raw server address string (e.g. {@code "play.example.com:25565"})
     * @return an 8-character hex string derived from the address
     * @throws AssertionError if MD5 is not available (guaranteed by the JDK spec, cannot happen)
     */
    private static String addressToFilename(String serverAddress) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(serverAddress.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexBuilder = new StringBuilder(16);
            for (int i = 0; i < 8; i++) hexBuilder.append(String.format("%02x", digest[i]));
            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException unreachable) {
            throw new AssertionError("MD5 is guaranteed by the JDK spec", unreachable);
        }
    }

    /**
     * Deletes {@code path} without throwing. Used to clean up unreadable cache files.
     *
     * @param path the file to delete
     */
    private static void deleteSilently(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }
}
