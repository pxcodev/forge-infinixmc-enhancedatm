package com.infinix.enhancedatm.common.network.packets;

import com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet for handling currency exchange operations through the Enhanced ATM
 */
public class CurrencyExchangePacket {
    
    private final double amount;
    private final String fromCurrency;
    private final String toCurrency;
    
    public CurrencyExchangePacket(double amount, String fromCurrency, String toCurrency) {
        this.amount = amount;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
    }
    
    public static void encode(CurrencyExchangePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.amount);
        buffer.writeUtf(packet.fromCurrency);
        buffer.writeUtf(packet.toCurrency);
    }
    
    public static CurrencyExchangePacket decode(FriendlyByteBuf buffer) {
        double amount = buffer.readDouble();
        String fromCurrency = buffer.readUtf();
        String toCurrency = buffer.readUtf();
        return new CurrencyExchangePacket(amount, fromCurrency, toCurrency);
    }
    
    public static void handle(CurrencyExchangePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Execute currency exchange through BubusteinMoney integration  
                BubusteinMoneyIntegration.executeCurrencyExchange(player, packet.amount, packet.fromCurrency, packet.toCurrency);
                
                // Add transaction to history
                double exchangeRate = BubusteinMoneyIntegration.getExchangeRate(packet.fromCurrency, packet.toCurrency);
                double convertedAmount = packet.amount * exchangeRate;
                String details = String.format("%.2f %s → %.2f %s", 
                    packet.amount, packet.fromCurrency, convertedAmount, packet.toCurrency);
                    
                com.infinix.enhancedatm.common.data.TransactionHistory.addTransaction(
                    player, "exchange", packet.amount, packet.fromCurrency, details
                );
            }
        });
        context.setPacketHandled(true);
    }
}