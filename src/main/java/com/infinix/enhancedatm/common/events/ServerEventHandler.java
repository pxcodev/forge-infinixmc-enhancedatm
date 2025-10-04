package com.infinix.enhancedatm.common.events;

import com.infinix.enhancedatm.common.config.ATMGuiConfig;
import com.infinix.enhancedatm.common.network.NetworkHandler;
import com.infinix.enhancedatm.common.network.packets.SyncGuiConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Handles server events for Enhanced ATM
 */
@Mod.EventBusSubscriber(modid = "enhancedatm", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    
    /**
     * Sync GUI configuration when player joins server
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Send current GUI configuration to the joining player
            String configJson = ATMGuiConfig.getConfigAsJson();
            SyncGuiConfigPacket syncPacket = new SyncGuiConfigPacket(configJson);
            
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), syncPacket);
            
            System.out.println("Enhanced ATM: Synced GUI configuration to player " + player.getName().getString());
        }
    }
}