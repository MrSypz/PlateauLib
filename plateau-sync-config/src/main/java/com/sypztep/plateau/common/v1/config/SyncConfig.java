package com.sypztep.plateau.common.v1.config;

import com.sypztep.plateau.common.v1.network.ConfigSyncRegistry;

import java.lang.annotation.*;

/**
 * Marks a config class as a participant in the server→client sync handshake.
 *
 * <p>Placing this annotation on a config class is the first step to making it
 * syncable. The second step is calling
 * {@link ConfigSyncRegistry#register(String, java.util.function.Supplier, Class)
 * ConfigSyncRegistry.register()} during mod initialization.
 *
 * <p>This annotation serves two purposes:
 * <ol>
 *   <li><b>Documentation</b> — makes it immediately visible at the class declaration
 *       that this config travels over the network.</li>
 *   <li><b>Future tooling</b> — annotation processors can validate at compile time
 *       that a {@code getInstance()} method exists and that at least one field is
 *       annotated {@link RequireSync}.</li>
 * </ol>
 *
 * <p>At runtime this annotation has no effect on its own; the sync system is
 * activated by {@link ConfigSyncRegistry#register}.
 *
 * <h3>Minimal usage example:</h3>
 * <pre>{@code
 * @SyncConfig(namespace = MyMod.MODID)
 * public final class MyServerConfig {
 *
 *     public static MyServerConfig getInstance() { return HANDLER.instance(); }
 *
 *     public static final ConfigClassHandler<MyServerConfig> HANDLER = ...;
 *
 *     @SerialEntry @RequireSync
 *     public float damageMultiplier = 1.0f;
 * }
 * }</pre>
 *
 * <h3>Registration (in your ModInitializer):</h3>
 * <pre>{@code
 * ConfigSyncRegistry.register(
 *     MyMod.MODID,
 *     MyServerConfig::getInstance,
 *     MyServerConfig.class
 * );
 * }</pre>
 *
 * @see RequireSync
 * @see ConfigSyncRegistry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SyncConfig {

    /**
     * The unique namespace for this config in the sync registry.
     *
     * <p>Must be unique across all registered configs on both the server and client.
     * Using your mod ID (e.g. {@code "temporature"}) is strongly recommended.
     *
     * <p>This value is used as the map key in all sync packets and in the
     * per-server client-side cache file, so changing it after release will
     * invalidate all existing client caches for this config.
     *
     * @return the namespace string, typically your mod ID
     */
    String namespace();
}
