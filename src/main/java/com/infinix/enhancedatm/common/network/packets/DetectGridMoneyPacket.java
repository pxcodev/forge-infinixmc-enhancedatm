package com.infinix.enhancedatm.common.network.packets;

import com.infinix.enhancedatm.common.container.EnhancedATMContainer;
import com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packet for detecting and displaying money amounts in the denomination grid
 * Now also performs automatic currency exchange to target currency
 */
public class DetectGridMoneyPacket {
    
    private final String targetCurrency;
    
    public DetectGridMoneyPacket() {
        this.targetCurrency = "EUR"; // Default currency
    }
    
    public DetectGridMoneyPacket(String targetCurrency) {
        this.targetCurrency = targetCurrency != null ? targetCurrency : "EUR";
    }
    
    public static void encode(DetectGridMoneyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.targetCurrency);
    }
    
    public static DetectGridMoneyPacket decode(FriendlyByteBuf buffer) {
        String targetCurrency = buffer.readUtf();
        return new DetectGridMoneyPacket(targetCurrency);
    }
    
    public static void handle(DetectGridMoneyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof EnhancedATMContainer) {
                EnhancedATMContainer container = (EnhancedATMContainer) player.containerMenu;
                
                // Get all denomination items from the 3x3 grid
                List<ItemStack> denominationItems = container.getDenominationItems();
                
                if (denominationItems.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§6[Enhanced ATM] §fNo hay dinero en la cuadrícula 3x3."));
                    return;
                }
                
                // Calculate total money by currency
                Map<String, Double> currencyTotals = new HashMap<>();
                Map<String, List<String>> currencyBreakdown = new HashMap<>();
                List<ItemStack> itemsToRemove = new ArrayList<>();
                
                for (ItemStack stack : denominationItems) {
                    if (BubusteinMoneyIntegration.isMoneyItem(stack.getItem())) {
                        // Get the value and currency of this denomination
                        double value = BubusteinMoneyIntegration.getDenominationValue(stack);
                        String currency = extractCurrencyFromItemName(stack);
                        
                        if (value > 0 && !currency.isEmpty()) {
                            double totalValue = value * stack.getCount();
                            
                            // Add to currency totals
                            currencyTotals.merge(currency, totalValue, Double::sum);
                            
                            // Add to breakdown for detailed display
                            currencyBreakdown.computeIfAbsent(currency, k -> new ArrayList<>())
                                .add(String.format("%dx %s (%.2f cada uno)", 
                                    stack.getCount(), 
                                    getItemDisplayName(stack), 
                                    value));
                            
                            // Store items to remove from grid
                            itemsToRemove.add(stack.copy());
                        }
                    }
                }
                
                if (currencyTotals.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§6[Enhanced ATM] §fNo se detectaron denominaciones válidas en la cuadrícula."));
                    return;
                }
                
                // Send summary message of detected money
                player.sendSystemMessage(Component.literal("§6[Enhanced ATM] §aDinero detectado en la cuadrícula 3x3:"));
                
                double grandTotal = 0.0;
                double targetCurrencyTotal = 0.0;
                
                for (Map.Entry<String, Double> entry : currencyTotals.entrySet()) {
                    String currency = entry.getKey();
                    double amount = entry.getValue();
                    
                    // Convert to EUR for grand total
                    double eurValue = amount;
                    if (!currency.equals("EUR")) {
                        double exchangeRate = BubusteinMoneyIntegration.getExchangeRate(currency, "EUR");
                        eurValue = amount * exchangeRate;
                    }
                    grandTotal += eurValue;
                    
                    // Convert to target currency with custom rounding
                    double targetValue = amount;
                    if (!currency.equals(packet.targetCurrency)) {
                        double exchangeRate = BubusteinMoneyIntegration.getExchangeRate(currency, packet.targetCurrency);
                        double rawConversion = amount * exchangeRate;
                        // Apply custom rounding logic for currency conversion
                        targetValue = BubusteinMoneyIntegration.applyCustomRounding(rawConversion);
                        
                        System.out.println("Enhanced ATM: Currency exchange in grid - " + amount + " " + currency + " -> " + targetValue + " " + packet.targetCurrency);
                        System.out.println("Enhanced ATM: Exchange rate: " + exchangeRate + ", Raw: " + rawConversion + ", Rounded: " + targetValue);
                    }
                    targetCurrencyTotal += targetValue;
                    
                    player.sendSystemMessage(Component.literal(String.format("§e  %s: §f%.2f %s", 
                        currency, amount, currency)));
                    
                    // Show breakdown
                    List<String> breakdown = currencyBreakdown.get(currency);
                    if (breakdown != null && breakdown.size() > 1) {
                        for (String detail : breakdown) {
                            player.sendSystemMessage(Component.literal("§7    " + detail));
                        }
                    }
                }
                
                // Show grand total in EUR equivalent
                if (currencyTotals.size() > 1 || !currencyTotals.containsKey("EUR")) {
                    player.sendSystemMessage(Component.literal(String.format("§b  Total equivalente: §f%.2f EUR", grandTotal)));
                }
                
                // Perform currency exchange if target currency is different
                if (!packet.targetCurrency.equals("EUR") || currencyTotals.size() > 1 || !currencyTotals.containsKey(packet.targetCurrency)) {
                    player.sendSystemMessage(Component.literal("§6[Enhanced ATM] §2Realizando cambio de divisas..."));
                    
                    // Remove all denomination items from the grid
                    container.clearDenominationGrid();
                    
                    // Apply custom rounding for final total
                    targetCurrencyTotal = BubusteinMoneyIntegration.applyCustomRounding(targetCurrencyTotal);
                    
                    // Generate new denominations in target currency using the public method
                    BubusteinMoneyIntegration.executeDenominationExchange(player, targetCurrencyTotal, packet.targetCurrency);
                    
                    player.sendSystemMessage(Component.literal(String.format("§a✓ Cambio completado: %.2f %s generado en denominaciones físicas", 
                        targetCurrencyTotal, packet.targetCurrency)));
                    
                    // Add transaction to history
                    com.infinix.enhancedatm.common.data.TransactionHistory.addTransaction(
                        player, "grid_exchange", targetCurrencyTotal, packet.targetCurrency, 
                        String.format("Cambio automático desde cuadrícula 3x3 a %s", packet.targetCurrency)
                    );
                } else {
                    // No exchange needed - just show the total
                    player.sendSystemMessage(Component.literal(String.format("§a✓ Total detectado: %.2f %s (no se requiere cambio)", 
                        targetCurrencyTotal, packet.targetCurrency)));
                }
                
                System.out.println("Enhanced ATM: Money detection and exchange completed for " + player.getName().getString());
            }
        });
        context.setPacketHandled(true);
    }
    
    /**
     * Extract currency code from item name based on actual mod item names
     */
    private static String extractCurrencyFromItemName(ItemStack stack) {
        String itemName = stack.getItem().toString().toLowerCase();
        
        // Map of currency identifiers based on actual mod item names (most specific first)
        
        // EUR - Euro items
        if (itemName.contains("euro") || itemName.contains("ecent")) return "EUR";
        
        // USD - US Dollar items (contains "dollar" but not "cdollar", "adollar", etc)
        if ((itemName.contains("dollar") && !itemName.contains("cdollar") && !itemName.contains("adollar") && !itemName.contains("nz_dollar")) || 
            itemName.contains("cents") && !itemName.contains("ccents") && !itemName.contains("acents") && !itemName.contains("br_centavos") && !itemName.contains("mx_centavos") && !itemName.contains("za_cents") && !itemName.contains("nz_cents") && !itemName.contains("ph_sentimo")) return "USD";
        
        // GBP - British Pound items
        if (itemName.contains("pound") && !itemName.contains("eg_pound") || itemName.contains("pence")) return "GBP";
        
        // CAD - Canadian Dollar items
        if (itemName.contains("cdollar") || itemName.contains("ccents") || itemName.contains("loonie") || itemName.contains("toonie")) return "CAD";
        
        // RON - Romanian Leu items (be very specific to avoid confusion with "krone")
        if (itemName.contains("_lei") && !itemName.contains("_lei_md") || itemName.contains("douazeci_lei") && !itemName.contains("_md") || 
            itemName.contains("cincizeci_lei") && !itemName.contains("_md") || itemName.contains("suta_lei") && !itemName.contains("_md") ||
            itemName.contains("un_leu") && !itemName.contains("_md") || itemName.contains("cinci_lei") && !itemName.contains("_md") ||
            itemName.contains("zece_lei") && !itemName.contains("_md") || itemName.contains("cinci_sute_lei") && !itemName.contains("_md") ||
            itemName.contains("doua_sute_lei") && !itemName.contains("_md") || itemName.contains("_bani") && !itemName.contains("_md")) return "RON";
        
        // MDL - Moldovan Leu items
        if (itemName.contains("_lei_md") || itemName.contains("_leu_md") || itemName.contains("_bani_md")) return "MDL";
        
        // CHF - Swiss Franc items
        if (itemName.contains("franc") && !itemName.contains("adollar")) return "CHF";
        
        // AUD - Australian Dollar items
        if (itemName.contains("adollar") || itemName.contains("acents")) return "AUD";
        
        // JPY - Japanese Yen items
        if (itemName.contains("yen")) return "JPY";
        
        // CZK - Czech Koruna items (be specific to avoid confusion with other krone)
        if (itemName.contains("cz_krone")) return "CZK";
        
        // NOK - Norwegian Krone items
        if (itemName.contains("no_krone")) return "NOK";
        
        // DKK - Danish Krone items
        if (itemName.contains("dk_krone") || itemName.contains("aere_dk")) return "DKK";
        
        // SEK - Swedish Krone items
        if (itemName.contains("se_krone")) return "SEK";
        
        // HUF - Hungarian Forint items
        if (itemName.contains("_ft")) return "HUF";
        
        // PLN - Polish Zloty items
        if (itemName.contains("zloty") || itemName.contains("grosz")) return "PLN";
        
        // RSD - Serbian Dinar items
        if (itemName.contains("rs_dinar")) return "RSD";
        
        // ISK - Icelandic Krone items
        if (itemName.contains("is_krone")) return "ISK";
        
        // CNY - Chinese Yuan items
        if (itemName.contains("cn_yuan") || itemName.contains("cn_jiao")) return "CNY";
        
        // INR - Indian Rupee items
        if (itemName.contains("in_rupee")) return "INR";
        
        // KRW - South Korean Won items
        if (itemName.contains("kr_won")) return "KRW";
        
        // BRL - Brazilian Real items
        if (itemName.contains("br_real") || itemName.contains("br_centavos")) return "BRL";
        
        // MXN - Mexican Peso items
        if (itemName.contains("mx_peso") || itemName.contains("mx_centavos")) return "MXN";
        
        // ZAR - South African Rand items
        if (itemName.contains("za_rand") || itemName.contains("za_cents")) return "ZAR";
        
        // TRY - Turkish Lira items
        if (itemName.contains("tr_lira") || itemName.contains("kurus")) return "TRY";
        
        // NZD - New Zealand Dollar items
        if (itemName.contains("nz_dollar") || itemName.contains("nz_cents")) return "NZD";
        
        // PHP - Philippine Peso items
        if (itemName.contains("ph_piso") || itemName.contains("ph_sentimo")) return "PHP";
        
        // EGP - Egyptian Pound items
        if (itemName.contains("eg_pound") || itemName.contains("eg_piastres")) return "EGP";
        
        // Default to EUR if unknown
        return "EUR";
    }
    
    /**
     * Get a user-friendly display name for the item
     */
    private static String getItemDisplayName(ItemStack stack) {
        String displayName = stack.getHoverName().getString();
        if (displayName.isEmpty()) {
            displayName = stack.getItem().toString();
        }
        return displayName;
    }
}