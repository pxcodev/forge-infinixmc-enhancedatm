package com.infinix.enhancedatm.common.network.packets;

import com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet for handling deposit operations through the Enhanced ATM
 */
public class DepositPacket {
    
    private final double amount;
    private final String sourceCurrency;
    private final String targetCurrency;
    
    public DepositPacket(double amount, String sourceCurrency, String targetCurrency) {
        this.amount = amount;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
    }
    
    public static void encode(DepositPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.amount);
        buffer.writeUtf(packet.sourceCurrency);
        buffer.writeUtf(packet.targetCurrency);
    }
    
    public static DepositPacket decode(FriendlyByteBuf buffer) {
        double amount = buffer.readDouble();
        String sourceCurrency = buffer.readUtf();
        String targetCurrency = buffer.readUtf();
        return new DepositPacket(amount, sourceCurrency, targetCurrency);
    }
    
    public static void handle(DepositPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Find the Enhanced ATM container and get the card from it
                if (player.containerMenu instanceof com.infinix.enhancedatm.common.container.EnhancedATMContainer) {
                    com.infinix.enhancedatm.common.container.EnhancedATMContainer atmContainer = 
                        (com.infinix.enhancedatm.common.container.EnhancedATMContainer) player.containerMenu;
                    
                    net.minecraft.world.item.ItemStack cardStack = atmContainer.getCardInSlot();
                    
                    // Execute the physical deposit with currency conversion (source to target)
                    boolean success = BubusteinMoneyIntegration.executePhysicalDepositWithConversion(
                        player, cardStack, packet.amount, packet.sourceCurrency, packet.targetCurrency);
                    
                    if (success) {
                        // Add transaction to history
                        com.infinix.enhancedatm.common.data.TransactionHistory.addTransaction(
                            player, "deposit", packet.amount, packet.sourceCurrency + " -> " + packet.targetCurrency, 
                            "ATM Physical Deposit with Conversion"
                        );
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}