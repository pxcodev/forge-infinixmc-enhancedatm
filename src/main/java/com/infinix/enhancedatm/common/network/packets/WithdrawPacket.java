package com.infinix.enhancedatm.common.network.packets;

import com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet for handling withdraw operations through the Enhanced ATM
 */
public class WithdrawPacket {
    
    private final double amount;
    private final String sourceCurrency; // Currency of the card
    private final String targetCurrency; // Currency for physical withdrawal
    
    public WithdrawPacket(double amount, String sourceCurrency, String targetCurrency) {
        this.amount = amount;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
    }
    
    public static void encode(WithdrawPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.amount);
        buffer.writeUtf(packet.sourceCurrency);
        buffer.writeUtf(packet.targetCurrency);
    }
    
    public static WithdrawPacket decode(FriendlyByteBuf buffer) {
        double amount = buffer.readDouble();
        String sourceCurrency = buffer.readUtf();
        String targetCurrency = buffer.readUtf();
        return new WithdrawPacket(amount, sourceCurrency, targetCurrency);
    }
    
    public static void handle(WithdrawPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Get the card from the ATM container slot (not main hand)
                ItemStack cardStack = null;
                if (player.containerMenu instanceof com.infinix.enhancedatm.common.container.EnhancedATMContainer) {
                    com.infinix.enhancedatm.common.container.EnhancedATMContainer container = 
                        (com.infinix.enhancedatm.common.container.EnhancedATMContainer) player.containerMenu;
                    cardStack = container.getCardInSlot();
                }
                
                if (cardStack != null && !cardStack.isEmpty()) {
                    // Try to withdraw money from card with currency conversion
                    boolean success = BubusteinMoneyIntegration.withdrawMoneyFromCardWithConversion(
                        player, cardStack, packet.amount, packet.sourceCurrency, packet.targetCurrency);
                    
                    if (success) {
                        // Add transaction to history
                        com.infinix.enhancedatm.common.data.TransactionHistory.addTransaction(
                            player, "withdraw", packet.amount, packet.sourceCurrency + " -> " + packet.targetCurrency, 
                            "ATM Withdrawal with Conversion"
                        );
                    }
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cNo card found in ATM slot!").withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
        });
        context.setPacketHandled(true);
    }
}