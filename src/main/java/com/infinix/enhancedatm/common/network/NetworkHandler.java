package com.infinix.enhancedatm.common.network;

import com.infinix.enhancedatm.EnhancedATMMod;
import com.infinix.enhancedatm.common.network.packets.CurrencyExchangePacket;
import com.infinix.enhancedatm.common.network.packets.DenominationExchangePacket;
import com.infinix.enhancedatm.common.network.packets.DepositPacket;
import com.infinix.enhancedatm.common.network.packets.DetectGridMoneyPacket;
import com.infinix.enhancedatm.common.network.packets.SyncGuiConfigPacket;
import com.infinix.enhancedatm.common.network.packets.WithdrawPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Network handler for Enhanced ATM mod
 * Manages client-server communication for ATM operations
 */
public class NetworkHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(EnhancedATMMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    /**
     * Initialize network packets
     */
    public static void init() {
        // Register packets
        INSTANCE.registerMessage(nextId(), DepositPacket.class, 
            DepositPacket::encode, DepositPacket::decode, DepositPacket::handle);
            
        INSTANCE.registerMessage(nextId(), WithdrawPacket.class,
            WithdrawPacket::encode, WithdrawPacket::decode, WithdrawPacket::handle);
            
        INSTANCE.registerMessage(nextId(), CurrencyExchangePacket.class,
            CurrencyExchangePacket::encode, CurrencyExchangePacket::decode, CurrencyExchangePacket::handle);
            
        INSTANCE.registerMessage(nextId(), DenominationExchangePacket.class,
            DenominationExchangePacket::encode, DenominationExchangePacket::decode, DenominationExchangePacket::handle);
            
        INSTANCE.registerMessage(nextId(), DetectGridMoneyPacket.class,
            DetectGridMoneyPacket::encode, DetectGridMoneyPacket::decode, DetectGridMoneyPacket::handle);
            
        INSTANCE.registerMessage(nextId(), SyncGuiConfigPacket.class,
            SyncGuiConfigPacket::encode, SyncGuiConfigPacket::new, SyncGuiConfigPacket::handle);
    }
    
    private static int nextId() {
        return packetId++;
    }
}