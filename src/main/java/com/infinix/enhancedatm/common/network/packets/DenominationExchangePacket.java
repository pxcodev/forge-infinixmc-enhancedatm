package com.infinix.enhancedatm.common.network.packets;

import com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet for handling batch denomination exchange operations through the Enhanced ATM
 */
public class DenominationExchangePacket {
    
    private final List<ItemStack> selectedDenominations;
    private final String targetCurrency;
    
    public DenominationExchangePacket(List<ItemStack> selectedDenominations, String targetCurrency) {
        this.selectedDenominations = new ArrayList<>(selectedDenominations);
        this.targetCurrency = targetCurrency;
    }
    
    public static void encode(DenominationExchangePacket packet, FriendlyByteBuf buffer) {
        // Write the number of denominations
        buffer.writeInt(packet.selectedDenominations.size());
        
        // Write each denomination ItemStack
        for (ItemStack denomination : packet.selectedDenominations) {
            buffer.writeItem(denomination);
        }
        
        // Write target currency
        buffer.writeUtf(packet.targetCurrency);
    }
    
    public static DenominationExchangePacket decode(FriendlyByteBuf buffer) {
        // Read the number of denominations
        int count = buffer.readInt();
        
        // Read each denomination ItemStack
        List<ItemStack> denominations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            denominations.add(buffer.readItem());
        }
        
        // Read target currency
        String targetCurrency = buffer.readUtf();
        
        return new DenominationExchangePacket(denominations, targetCurrency);
    }
    
    public static void handle(DenominationExchangePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Calculate total value of selected denominations
                double totalValue = 0.0;
                List<String> denominationDetails = new ArrayList<>();
                
                for (ItemStack denomination : packet.selectedDenominations) {
                    if (BubusteinMoneyIntegration.isMoneyItem(denomination.getItem())) {
                        // Get the count of this denomination in player's inventory
                        int count = BubusteinMoneyIntegration.getItemCountInInventory(player, denomination.getItem());
                        if (count > 0) {
                            // Get the value of this denomination
                            double denominationValue = BubusteinMoneyIntegration.getDenominationValue(denomination);
                            double denominationTotal = denominationValue * count;
                            totalValue += denominationTotal;
                            
                            // Add to details for transaction history
                            denominationDetails.add(String.format("%dx %s (%.2f each)", 
                                count, denomination.getHoverName().getString(), denominationValue));
                            
                            // Remove the denomination items from player's inventory
                            player.getInventory().clearOrCountMatchingItems(
                                stack -> stack.getItem() == denomination.getItem(), 
                                count, 
                                player.getInventory()
                            );
                        }
                    }
                }
                
                if (totalValue > 0) {
                    // Execute denomination exchange through BubusteinMoney integration
                    BubusteinMoneyIntegration.executeDenominationExchange(player, totalValue, packet.targetCurrency);
                    
                    // Add transaction to history
                    String details = String.format("Batch exchange: %s → %.2f %s", 
                        String.join(", ", denominationDetails), totalValue, packet.targetCurrency);
                        
                    com.infinix.enhancedatm.common.data.TransactionHistory.addTransaction(
                        player, "denomination_exchange", totalValue, packet.targetCurrency, details
                    );
                    
                    System.out.println("Enhanced ATM: Processed denomination exchange for " + player.getName().getString() + 
                        " - Total value: " + totalValue + " " + packet.targetCurrency);
                }
            }
        });
        context.setPacketHandled(true);
    }
}