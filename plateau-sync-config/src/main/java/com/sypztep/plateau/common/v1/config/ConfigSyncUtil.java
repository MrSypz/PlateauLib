package com.sypztep.plateau.common.v1.config;

import com.sypztep.plateau.PlateauSyncConfig;
import com.sypztep.plateau.common.v1.network.ConfigSyncRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection-based utility that drives the field-level sync and hashing logic.
 *
 * <p>Both methods operate exclusively on fields annotated with {@link RequireSync}.
 * The set of sync fields for each class is scanned once on first access and cached
 * in a {@link ConcurrentHashMap}, so the reflection overhead is paid only once per
 * config type per JVM session.
 *
 * <p>Fields are sorted alphabetically by name before caching. This ensures that
 * {@link #syncHashCode} produces the same result regardless of the order in which
 * fields are declared in the source file.
 *
 * <h3>Typical usage (in your mod's config class):</h3>
 * <pre>{@code
 * // applyFrom — delegate to this instead of writing field-by-field assignment
 * public static void applyFrom(MyServerConfig source) {
 *     ConfigSyncUtil.applyFrom(source, getInstance());
 * }
 *
 * // hashCode — delegate to this instead of a manual Objects.hash(...) list
 * @Override
 * public int hashCode() {
 *     return ConfigSyncUtil.syncHashCode(this);
 * }
 * }</pre>
 *
 * <p>When using {@link ConfigSyncRegistry#register(String, java.util.function.Supplier, Class)
 * ConfigSyncRegistry.register()} with the 3-argument shorthand, both of the above
 * are provided by the library automatically and do not need to appear in your config class.
 *
 * @see RequireSync
 * @see ConfigSyncRegistry
 */
public final class ConfigSyncUtil {

    private ConfigSyncUtil() {}

    /** Cache of discovered {@link RequireSync} fields, keyed by config class. */
    private static final Map<Class<?>, List<Field>> SYNC_FIELD_CACHE = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Copies all {@link RequireSync}-annotated fields from {@code source} into {@code destination}.
     *
     * <p>Both objects must be instances of the exact same class. Superclass fields are not
     * currently traversed; all synced fields must be declared directly on the config class.
     *
     * <p>If a field cannot be accessed (which should not happen in normal usage, since all
     * discovered fields are made accessible during the scan), an error is logged and that
     * field is skipped. The remaining fields are still copied.
     *
     * @param <GenericConfig> the config type; inferred from the arguments
     * @param source          the config object to copy values from (typically a freshly deserialized instance)
     * @param destination     the live config singleton to copy values into
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IllegalArgumentException if {@code source} and {@code destination} are not the same class
     */
    public static <GenericConfig> void applyFrom(GenericConfig source, GenericConfig destination) {
        Objects.requireNonNull(source, "source config must not be null");
        Objects.requireNonNull(destination, "destination config must not be null");

        if (!source.getClass().equals(destination.getClass())) {
            throw new IllegalArgumentException(
                    "source and destination must be the same class, got: "
                    + source.getClass().getName() + " vs " + destination.getClass().getName());
        }

        for (Field field : cachedSyncFields(source.getClass())) {
            try {
                field.set(destination, field.get(source));
            } catch (IllegalAccessException exception) {
                PlateauSyncConfig.LOGGER.error(
                        "Failed to copy @RequireSync field '{}' on {} — skipping",
                        field.getName(), source.getClass().getSimpleName(), exception);
            }
        }
    }

    /**
     * Computes a hash code from all {@link RequireSync}-annotated fields on {@code config}.
     *
     * <p>Fields are visited in alphabetical order (established at scan time) so the hash
     * is deterministic regardless of declaration order in the source file. This is important
     * for the server/client hash comparison to be reliable.
     *
     * <p>Uses the same polynomial rolling hash as {@link Arrays#hashCode(Object[])}
     * ({@code result = 31 * result + hash(field)}) for consistency with standard Java conventions.
     *
     * @param config the config object to hash; must not be {@code null}
     * @return a hash code reflecting the current values of all {@link RequireSync} fields
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public static int syncHashCode(Object config) {
        Objects.requireNonNull(config, "config must not be null");

        int result = 1;
        for (Field field : cachedSyncFields(config.getClass())) {
            try {
                result = 31 * result + Objects.hashCode(field.get(config));
            } catch (IllegalAccessException exception) {
                PlateauSyncConfig.LOGGER.error(
                        "Failed to hash @RequireSync field '{}' on {} — using 0",
                        field.getName(), config.getClass().getSimpleName(), exception);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the cached list of sync fields for the given class,
     * scanning and caching them on first access.
     *
     * @param configClass the config class to introspect
     * @return an immutable, alphabetically-sorted list of accessible {@link RequireSync} fields
     */
    static List<Field> cachedSyncFields(Class<?> configClass) {
        return SYNC_FIELD_CACHE.computeIfAbsent(configClass, ConfigSyncUtil::scanSyncFields);
    }

    /**
     * Scans {@code configClass} for fields eligible for sync.
     *
     * <p>A field is eligible if it:
     * <ul>
     *   <li>is annotated with {@link RequireSync}</li>
     *   <li>is not {@code static}</li>
     *   <li>is not {@code final}</li>
     * </ul>
     *
     * <p>Eligible fields are sorted alphabetically by name to guarantee a deterministic
     * field-visit order independent of JVM field ordering (which is not guaranteed by spec).
     * Each field is made accessible before being added to the list so that subsequent
     * {@code field.get()} / {@code field.set()} calls do not throw.
     *
     * @param configClass the class to scan
     * @return an unmodifiable, alphabetically-sorted list of eligible fields
     */
    private static List<Field> scanSyncFields(Class<?> configClass) {
        return Arrays.stream(configClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(RequireSync.class))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !Modifier.isFinal(field.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .peek(field -> field.setAccessible(true))
                .toList();
    }
}
