package com.sypztep.plateau.common.v1.config;

import com.sypztep.plateau.common.v1.network.ConfigSyncRegistry;

/**
 * Fabric entrypoint interface for registering configs into the sync system.
 *
 * <p>Implement this interface in your mod and list the implementing class
 * under the {@code "plateau-sync-config"} key in your {@code fabric.mod.json}.
 * The lib calls {@link #registerSyncConfigs()} during its own initialization,
 * before any player can join — so registration is always complete on time.
 *
 * <h3>Step 1 — implement the interface:</h3>
 * <pre>{@code
 * public class TemporatureConfigEntrypoint implements SyncConfigEntrypoint {
 *     @Override
 *     public void registerSyncConfigs() {
 *         ConfigSyncRegistry.register(
 *             Temporature.MODID,
 *             TemporatureServerConfig::getInstance,
 *             TemporatureServerConfig.class
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h3>Step 2 — declare it in {@code fabric.mod.json}:</h3>
 * <pre>{@code
 * "entrypoints": {
 *     "plateau-sync-config": [
 *         "com.sypztep.temporature.TemporatureConfigEntrypoint"
 *     ]
 * }
 * }</pre>
 *
 * <p>No call to {@link ConfigSyncRegistry#register} in your {@code onInitialize()}
 * is needed — the entrypoint handles it.
 *
 * @see ConfigSyncRegistry#register(String, java.util.function.Supplier, Class)
 */
@FunctionalInterface
public interface SyncConfigEntrypoint {

    /**
     * Called by the lib during initialization to collect config registrations.
     *
     * <p>Call {@link ConfigSyncRegistry#register} for each config you want to sync.
     * Do not call this method yourself — it is invoked automatically via the
     * Fabric entrypoint system.
     */
    void registerSyncConfigs();
}