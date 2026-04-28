package com.sypztep.plateau;

import com.sypztep.plateau.client.PlateauSyncConfigClient;
import com.sypztep.plateau.common.v1.network.ConfigSyncManager;
import com.sypztep.plateau.common.v1.network.payload.SyncAckC2S;
import com.sypztep.plateau.common.v1.network.payload.SyncDataS2C;
import com.sypztep.plateau.common.v1.network.payload.SyncHelloS2C;
import com.sypztep.plateau.common.v1.network.payload.SyncResponseC2S;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side entry point for the {@code plateau-sync-config} library.
 *
 * <p>Registers all network payloads and server-side connection event listeners.
 * Consumer mods do not need to register any of these themselves — the library
 * handles everything automatically as long as it is listed as a dependency.
 *
 * <p>The only thing consumer mods need to do is call
 * {@link com.sypztep.plateau.common.v1.network.ConfigSyncRegistry#register
 * ConfigSyncRegistry.register()} during their own {@code onInitialize()}.
 */
public class PlateauSyncConfig implements ModInitializer {

    public static final String MODID = "plateau-sync-config";

    /** Shared logger for all internal library classes. Use {@code [ConfigSync]} prefix in messages. */
    public static final Logger LOGGER = LoggerFactory.getLogger("ConfigSync");

    /**
     * Creates a namespaced {@link Identifier} under this library's mod ID.
     *
     * <p>Used by payload {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type Type}
     * declarations to ensure packet IDs do not collide with consumer mod packets.
     *
     * @param path the path component of the identifier (e.g. {@code "sync_hello"})
     * @return an {@link Identifier} of the form {@code plateau-sync-config:path}
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        registerPayloads();
        registerEvents();
        LOGGER.info("[ConfigSync] Initialized — waiting for consumer mods to register configs.");
    }

    // -------------------------------------------------------------------------

    /**
     * Registers all clientbound and serverbound payload types, and attaches
     * server-side packet receivers for the two client→server packets.
     *
     * <p>Client-side receivers ({@link SyncHelloS2C} and {@link SyncDataS2C}) are registered
     * in {@link PlateauSyncConfigClient} to respect the client/server separation.
     */
    private static void registerPayloads() {
        // Server → Client
        PayloadTypeRegistry.clientboundPlay().register(SyncHelloS2C.ID, SyncHelloS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncDataS2C.ID,  SyncDataS2C.CODEC);

        // Client → Server
        PayloadTypeRegistry.serverboundPlay().register(SyncResponseC2S.ID, SyncResponseC2S.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SyncAckC2S.ID,      SyncAckC2S.CODEC);

        // Server-side receivers — routed through the state machine
        ServerPlayNetworking.registerGlobalReceiver(SyncResponseC2S.ID,
                (packet, ctx) -> ConfigSyncManager.onSyncResponse(ctx.player(), packet));
        ServerPlayNetworking.registerGlobalReceiver(SyncAckC2S.ID,
                (packet, ctx) -> ConfigSyncManager.onSyncAck(ctx.player(), packet));
    }

    /**
     * Attaches the server-side connection lifecycle hooks.
     *
     * <ul>
     *   <li>{@code JOIN} — freeze the player and start the handshake (dedicated servers only).</li>
     *   <li>{@code DISCONNECT} — clean up any pending handshake state to prevent memory leaks.</li>
     * </ul>
     */
    private static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, _, server) -> {
            if (server.isDedicatedServer())
                ConfigSyncManager.onPlayerJoin(handler.getPlayer(), server);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) ->
                ConfigSyncManager.onPlayerDisconnect(handler.getPlayer()));
    }
}
