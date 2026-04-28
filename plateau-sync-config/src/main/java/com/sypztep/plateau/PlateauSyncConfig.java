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

public class PlateauSyncConfig implements ModInitializer {
    public static final String MODID = "plateau-sync-config";
    public static final Logger LOGGER = LoggerFactory.getLogger("SyncConfig");
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        registerPayloads();
        registerEvents();
        LOGGER.info("Initialized — waiting for consumer mods to register configs.");
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(SyncHelloS2C.ID, SyncHelloS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncDataS2C.ID,  SyncDataS2C.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(SyncResponseC2S.ID, SyncResponseC2S.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SyncAckC2S.ID,      SyncAckC2S.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SyncResponseC2S.ID,
                (packet, ctx) -> ConfigSyncManager.onSyncResponse(ctx.player(), packet));
        ServerPlayNetworking.registerGlobalReceiver(SyncAckC2S.ID,
                (packet, ctx) -> ConfigSyncManager.onSyncAck(ctx.player(), packet));
    }

    private static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, _, server) -> {
            if (server.isDedicatedServer()) ConfigSyncManager.onPlayerJoin(handler.getPlayer(), server);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) ->
                ConfigSyncManager.onPlayerDisconnect(handler.getPlayer()));
    }
}
