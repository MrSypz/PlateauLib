package com.sypztep.plateau.client;

import com.sypztep.plateau.common.v1.config.ServerConfigCache;
import com.sypztep.plateau.common.v1.network.ConfigSyncRegistry;
import com.sypztep.plateau.common.v1.network.payload.SyncDataS2C;
import com.sypztep.plateau.common.v1.network.payload.SyncHelloS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.Objects;

/**
 * Client-side entry point for the {@code plateau-sync-config} library.
 *
 * <p>Registers client-side packet receivers and the disconnect cleanup hook.
 * Consumer mods do not need to do any of this themselves.
 */
public class PlateauSyncConfigClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerClientReceivers();
        registerClientEvents();
    }

    // -------------------------------------------------------------------------

    /**
     * Registers the two client-side packet receivers.
     *
     * <ul>
     *   <li>{@link SyncHelloS2C} — first packet; client checks its cache and replies.</li>
     *   <li>{@link SyncDataS2C} — slow-path data; client applies, caches, and acks.</li>
     * </ul>
     */
    private static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SyncHelloS2C.ID, new SyncHelloS2C.Receiver());
        ClientPlayNetworking.registerGlobalReceiver(SyncDataS2C.ID,  new SyncDataS2C.Receiver());
    }

    /**
     * Registers the client disconnect hook.
     *
     * <p>On disconnect:
     * <ul>
     *   <li>{@link ServerConfigCache#evict} removes the in-memory cache for this server
     *       so the next session re-reads from disk.</li>
     *   <li>{@link ConfigSyncRegistry#clearSyncedState} clears the set of namespaces
     *       marked as synced-from-server so {@link ConfigSyncRegistry#isSyncedFromServer}
     *       returns {@code false} until the next successful handshake.</li>
     * </ul>
     */
    private static void registerClientEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            String serverAddress = client.getCurrentServer() != null
                    ? client.getCurrentServer().ip
                    : null;

            if (serverAddress != null) ServerConfigCache.evict(serverAddress);
            ConfigSyncRegistry.clearSyncedState();
        });
    }
}
