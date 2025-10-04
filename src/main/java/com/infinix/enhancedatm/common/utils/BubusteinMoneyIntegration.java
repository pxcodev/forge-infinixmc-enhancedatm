package com.infinix.enhancedatm.common.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import static com.infinix.enhancedatm.common.utils.TranslationHelper.*;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Integration utility for BubusteinMoney mod
 * Uses reflection to interact with BubusteinMoney classes safely
 */
public class BubusteinMoneyIntegration {
    
    private static final String CARD_ITEM_CLASS = "tk.bubustein.money.item.CardItem";
    private static final String MOD_ITEMS_CLASS = "tk.bubustein.money.item.ModItems";
    
    // Cached reflection objects
    private static Class<?> cardItemClass;
    private static Class<?> modItemsClass;
    private static Method getMoneyMethod;
    private static Method getCurrencyMethod;
    private static Method setCurrencyMethod;
    private static Method formatMoneyMethod;
    private static Field exchangeRatesField;
    
    static {
        initializeReflection();
    }
    
    /**
     * Initialize reflection objects for BubusteinMoney integration
     */
    private static void initializeReflection() {
        try {
            // Get classes
            cardItemClass = Class.forName(CARD_ITEM_CLASS);
            modItemsClass = Class.forName(MOD_ITEMS_CLASS);
            
            // Get methods
            getMoneyMethod = cardItemClass.getDeclaredMethod("getMoney", ItemStack.class);
            getCurrencyMethod = cardItemClass.getDeclaredMethod("getCurrency", ItemStack.class);
            setCurrencyMethod = cardItemClass.getDeclaredMethod("setCurrency", ItemStack.class, String.class);
            formatMoneyMethod = cardItemClass.getDeclaredMethod("formatMoney", double.class);
            
            // Get exchange rates field
            exchangeRatesField = modItemsClass.getDeclaredField("EXCHANGE_RATES");
            exchangeRatesField.setAccessible(true);
            
        } catch (Exception e) {
            // BubusteinMoney mod not available or version mismatch
            System.err.println("Enhanced ATM: Could not initialize BubusteinMoney integration: " + e.getMessage());
        }
    }
    
    /**
     * Check if an ItemStack is a BubusteinMoney card
     */
    public static boolean isCardItem(ItemStack itemStack) {
        if (cardItemClass == null) return false;
        
        try {
            return cardItemClass.isInstance(itemStack.getItem());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the balance from a card ItemStack
     */
    public static double getCardBalance(ItemStack cardStack) {
        if (!isCardItem(cardStack) || getMoneyMethod == null) return 0.0;
        
        try {
            Object cardItem = cardStack.getItem();
            double balance = (Double) getMoneyMethod.invoke(cardItem, cardStack);
            return roundMoney(balance); // Round to avoid precision errors
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Get the formatted balance from a card ItemStack
     */
    public static String getFormattedBalance(ItemStack cardStack) {
        if (!isCardItem(cardStack) || formatMoneyMethod == null) return "0.00";
        
        try {
            double balance = getCardBalance(cardStack);
            return (String) formatMoneyMethod.invoke(null, balance);
        } catch (Exception e) {
            return "0.00";
        }
    }
    
    /**
     * Get the currency from a card ItemStack
     */
    public static String getCardCurrency(ItemStack cardStack) {
        if (!isCardItem(cardStack) || getCurrencyMethod == null) return "EUR";
        
        try {
            Object cardItem = cardStack.getItem();
            return (String) getCurrencyMethod.invoke(cardItem, cardStack);
        } catch (Exception e) {
            return "EUR";
        }
    }
    
    /**
     * Round monetary amount using custom rounding logic:
     * - If decimal part <= 0.5: round down to previous integer
     * - If decimal part >= 0.7: round up to next integer  
     * - Between 0.5 and 0.7: round down (conservative approach)
     * This applies only to whole numbers, fractional amounts keep precision
     */
    private static double roundMoney(double amount) {
        // For precision in monetary calculations, round to 2 decimal places first
        double preciseAmount = Math.round(amount * 100.0) / 100.0;
        
        // Only apply special rounding logic if we're dealing with currency conversion
        // and the amount is greater than 1 (to preserve cent precision for small amounts)
        if (preciseAmount >= 1.0) {
            double integerPart = Math.floor(preciseAmount);
            double decimalPart = preciseAmount - integerPart;
            
            System.out.println("Enhanced ATM: Custom rounding - Amount: " + preciseAmount + 
                             ", Integer: " + integerPart + ", Decimal: " + decimalPart);
            
            if (decimalPart <= 0.5) {
                // Round down to previous integer
                System.out.println("Enhanced ATM: Rounding down (decimal <= 0.5)");
                return integerPart;
            } else if (decimalPart >= 0.7) {
                // Round up to next integer
                System.out.println("Enhanced ATM: Rounding up (decimal >= 0.7)");
                return integerPart + 1.0;
            } else {
                // Between 0.5 and 0.7: round down (conservative)
                System.out.println("Enhanced ATM: Conservative rounding down (0.5 < decimal < 0.7)");
                return integerPart;
            }
        }
        
        // For amounts less than 1, keep the precise amount to preserve cent precision
        return preciseAmount;
    }
    
    /**
     * Public method to apply the custom rounding logic for currency conversions
     * This is used by other classes that need to apply the same rounding rules
     */
    public static double applyCustomRounding(double amount) {
        return roundMoney(amount);
    }
    
    /**
     * Get exchange rate between two currencies
     */
    public static double getExchangeRate(String fromCurrency, String toCurrency) {
        if (exchangeRatesField == null) return 1.0;
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Double> exchangeRates = (Map<String, Double>) exchangeRatesField.get(null);
            
            if (fromCurrency.equals(toCurrency)) return 1.0;
            
            Double fromRate = exchangeRates.get(fromCurrency);
            Double toRate = exchangeRates.get(toCurrency);
            
            if (fromRate != null && toRate != null) {
                // Convert from -> EUR -> to
                return toRate / fromRate;
            }
            
        } catch (Exception e) {
            // Error accessing exchange rates
        }
        
        return 1.0;
    }
    
    /**
     * Execute deposit command through physical money system
     * Takes physical money from player inventory and deposits to card
     */
    public static boolean executePhysicalDeposit(ServerPlayer player, ItemStack cardStack, double amount, String currency) {
        if (!isCardItem(cardStack)) {
            return false;
        }
        
        try {
            // Calculate total available money in inventory
            double totalAvailable = calculateTotalMoney(player, currency);
            
            if (totalAvailable < amount) {
                player.sendSystemMessage(createErrorMessage(INSUFFICIENT_FUNDS_AVAILABLE, 
                    String.format("%.2f %s", totalAvailable, currency)));
                return false;
            }
            
            // Remove money from inventory and calculate change
            double totalRemoved = removeMoneyFromInventoryWithChange(player, amount, currency);
            System.out.println("Enhanced ATM: Total removed: " + totalRemoved + ", Amount to deposit: " + amount);
            
            if (totalRemoved >= amount) {
                // Add money to card using reflection
                addMoneyToCard(cardStack, amount, currency);
                
                // Calculate and give change
                double change = totalRemoved - amount;
                System.out.println("Enhanced ATM: Calculated change: " + change);
                
                if (change > 0.01) { // Allow for small floating point errors
                    System.out.println("Enhanced ATM: Giving change to player: " + change);
                    giveChangeToPlayer(player, change, currency);
                    player.sendSystemMessage(createSuccessMessage(DEPOSIT_SUCCESS_CHANGE, 
                        String.format("%.2f %s", amount, currency), 
                        String.format("%.2f %s", change, currency)));
                } else {
                    System.out.println("Enhanced ATM: No change needed");
                    player.sendSystemMessage(createSuccessMessage(DEPOSIT_SUCCESS_SIMPLE, 
                        String.format("%.2f %s", amount, currency)));
                }
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error executing physical deposit: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Execute deposit with currency conversion
     * Takes physical money from player inventory in sourceCurrency and deposits to card in targetCurrency
     */
    public static boolean executePhysicalDepositWithConversion(ServerPlayer player, ItemStack cardStack, double amount, String sourceCurrency, String targetCurrency) {
        if (!isCardItem(cardStack)) {
            return false;
        }
        
        try {
            // First, validate card compatibility
            String currentCardCurrency = getCardCurrency(cardStack);
            double currentBalance = getCardBalance(cardStack);
            
            // Check if card is compatible (has 0 balance or already has target currency)
            if (currentBalance > 0.01 && !currentCardCurrency.equals(targetCurrency)) {
                player.sendSystemMessage(createErrorMessage(CARD_CURRENCY_MISMATCH, 
                    currentCardCurrency, targetCurrency, targetCurrency));
                return false;
            }
            
            // Calculate total available money in source currency
            double totalAvailable = calculateTotalMoney(player, sourceCurrency);
            
            if (totalAvailable < amount) {
                player.sendSystemMessage(createErrorMessage(INSUFFICIENT_BALANCE_AVAILABLE, 
                    String.format("%.2f %s", totalAvailable, sourceCurrency)));
                return false;
            }
            
            // Remove money from inventory in source currency
            double totalRemoved = removeMoneyFromInventoryWithChange(player, amount, sourceCurrency);
            System.out.println("Enhanced ATM: Total removed: " + totalRemoved + " " + sourceCurrency + ", Amount to deposit: " + amount);
            
            if (totalRemoved >= amount) {
                // Convert amount to target currency and round to avoid precision errors
                double exchangeRate = getExchangeRate(sourceCurrency, targetCurrency);
                double convertedAmount = roundMoney(amount * exchangeRate);
                
                // Add converted money to card
                addMoneyToCard(cardStack, convertedAmount, targetCurrency);
                
                // Calculate and give change in source currency
                double change = totalRemoved - amount;
                System.out.println("Enhanced ATM: Calculated change: " + change + " " + sourceCurrency);
                
                if (change > 0.01) {
                    System.out.println("Enhanced ATM: Giving change to player: " + change);
                    giveChangeToPlayer(player, change, sourceCurrency);
                    player.sendSystemMessage(createSuccessMessage(DEPOSIT_SUCCESS_CONVERSION_CHANGE, 
                        String.format("%.2f %s", amount, sourceCurrency), 
                        String.format("%.2f %s", convertedAmount, targetCurrency), 
                        String.format("%.2f %s", change, sourceCurrency)));
                } else {
                    System.out.println("Enhanced ATM: No change needed");
                    player.sendSystemMessage(createSuccessMessage(DEPOSIT_SUCCESS_CONVERSION, 
                        String.format("%.2f %s", amount, sourceCurrency), 
                        String.format("%.2f %s", convertedAmount, targetCurrency)));
                }
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error executing physical deposit with conversion: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Withdraw money from card and give physical bills to player
     */
    public static boolean withdrawMoneyFromCard(ServerPlayer player, ItemStack cardStack, double amount, String currency) {
        if (!isCardItem(cardStack)) {
            return false;
        }
        
        try {
            // Get current balance
            double currentBalance = getCardBalance(cardStack, currency);
            System.out.println("Enhanced ATM: Current balance: " + currentBalance + ", Withdraw amount: " + amount);
            
            if (currentBalance < amount) {
                player.sendSystemMessage(createErrorMessage(INSUFFICIENT_BALANCE_AVAILABLE, 
                    String.format("%.2f %s", currentBalance, currency)));
                return false;
            }
            
            // Remove money from card
            double newBalance = currentBalance - amount;
            setCardBalance(cardStack, newBalance, currency);
            System.out.println("Enhanced ATM: New balance after withdrawal: " + newBalance);
            
            // Create physical money for the withdrawn amount
            createPhysicalMoneyForPlayer(player, amount, currency);
            
            player.sendSystemMessage(createSuccessMessage(WITHDRAW_SUCCESS_SIMPLE, 
                String.format("%.2f %s", amount, currency)));
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error withdrawing money from card: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Withdraw money from card with currency conversion
     * Deducts from card in sourceCurrency and gives physical bills in targetCurrency
     */
    public static boolean withdrawMoneyFromCardWithConversion(ServerPlayer player, ItemStack cardStack, double amount, String sourceCurrency, String targetCurrency) {
        if (!isCardItem(cardStack)) {
            return false;
        }
        
        try {
            // First, validate that source currency matches card currency
            String actualCardCurrency = getCardCurrency(cardStack);
            if (!actualCardCurrency.equals(sourceCurrency)) {
                player.sendSystemMessage(createErrorMessage(CARD_CURRENCY_SELECTION_MISMATCH, 
                    actualCardCurrency, sourceCurrency));
                return false;
            }
            
            // Get current balance in card currency
            double currentBalance = getCardBalance(cardStack);
            System.out.println("Enhanced ATM: Current balance: " + currentBalance + " " + sourceCurrency + ", Withdraw amount: " + amount);
            
            if (currentBalance < amount) {
                player.sendSystemMessage(createErrorMessage(INSUFFICIENT_BALANCE_AVAILABLE, 
                    String.format("%.2f %s", currentBalance, sourceCurrency)));
                return false;
            }
            
            // Remove money from card (in source currency)
            double newBalance = roundMoney(currentBalance - amount);
            setCardBalance(cardStack, newBalance, sourceCurrency);
            System.out.println("Enhanced ATM: New balance after withdrawal: " + newBalance + " " + sourceCurrency);
            
            // Convert amount to target currency for physical bills
            double exchangeRate = getExchangeRate(sourceCurrency, targetCurrency);
            double convertedAmount = roundMoney(amount * exchangeRate);
            
            // Create physical money for the converted amount
            createPhysicalMoneyForPlayer(player, convertedAmount, targetCurrency);
            
            if (sourceCurrency.equals(targetCurrency)) {
                player.sendSystemMessage(createSuccessMessage(WITHDRAW_SUCCESS_SIMPLE, 
                    String.format("%.2f %s", amount, sourceCurrency)));
            } else {
                player.sendSystemMessage(createSuccessMessage(WITHDRAW_SUCCESS_CONVERSION, 
                    String.format("%.2f %s", amount, sourceCurrency), 
                    String.format("%.2f %s", convertedAmount, targetCurrency)));
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error withdrawing money from card with conversion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Set card balance using reflection
     */
    private static void setCardBalance(ItemStack cardStack, double balance, String currency) {
        try {
            // Round balance to avoid precision errors
            double roundedBalance = roundMoney(balance);
            
            // Use reflection to call setMoney method on card item
            Method setMoneyMethod = cardItemClass.getMethod("setMoney", ItemStack.class, double.class);
            setMoneyMethod.invoke(cardStack.getItem(), cardStack, roundedBalance);
            
            System.out.println("Enhanced ATM: Successfully set card balance to: " + roundedBalance);
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error setting card balance: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get card balance using reflection
     */
    private static double getCardBalance(ItemStack cardStack, String currency) {
        try {
            // Use reflection to call getMoney method on card item
            Method getMoneyMethod = cardItemClass.getMethod("getMoney", ItemStack.class);
            double balance = (double) getMoneyMethod.invoke(cardStack.getItem(), cardStack);
            
            System.out.println("Enhanced ATM: Card balance: " + balance);
            return balance;
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error getting card balance: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }
    
    /**
     * Execute currency exchange (withdraw from one currency, deposit to another)
     */
    public static void executeCurrencyExchange(ServerPlayer player, double amount, String fromCurrency, String toCurrency) {
        try {
            CommandSourceStack commandSource = player.createCommandSourceStack();
            Commands commands = player.server.getCommands();
            
            // First withdraw in original currency
            String withdrawCommand = String.format("bubustein withdraw %.2f", amount);
            commands.performPrefixedCommand(commandSource, withdrawCommand);
            
            // Then deposit in target currency (this will convert automatically)
            double exchangeRate = getExchangeRate(fromCurrency, toCurrency);
            double rawConversion = amount * exchangeRate;
            double convertedAmount = roundMoney(rawConversion);
            
            System.out.println("Enhanced ATM: Currency Exchange - " + amount + " " + fromCurrency + " -> " + toCurrency);
            System.out.println("Enhanced ATM: Exchange rate: " + exchangeRate);
            System.out.println("Enhanced ATM: Raw conversion: " + rawConversion);
            System.out.println("Enhanced ATM: Final rounded amount: " + convertedAmount);
            
            String depositCommand = String.format("bubustein deposit %.2f %s", convertedAmount, toCurrency);
            commands.performPrefixedCommand(commandSource, depositCommand);
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error executing currency exchange: " + e.getMessage());
        }
    }
    
    /**
     * Calculate total money available in player inventory for a specific currency
     */
    private static double calculateTotalMoney(ServerPlayer player, String currency) {
        double total = 0.0;
        Inventory inventory = player.getInventory();
        
        // Calculate money from player inventory
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                double value = getItemValue(stack, currency);
                if (value > 0) {
                    total += value * stack.getCount();
                }
            }
        }
        
        // Also calculate money from ATM grid if player is using ATM
        if (player.containerMenu instanceof com.infinix.enhancedatm.common.container.EnhancedATMContainer) {
            com.infinix.enhancedatm.common.container.EnhancedATMContainer atmContainer = 
                (com.infinix.enhancedatm.common.container.EnhancedATMContainer) player.containerMenu;
            
            // Get items from denomination grid (slots 1-9)
            for (int i = 1; i <= 9; i++) {
                ItemStack stack = atmContainer.getSlot(i).getItem();
                if (!stack.isEmpty()) {
                    double value = getItemValue(stack, currency);
                    if (value > 0) {
                        total += value * stack.getCount();
                    }
                }
            }
        }
        
        return total;
    }
    
    /**
     * Remove exact amount of money from player inventory
     */
    private static boolean removeMoneyFromInventory(ServerPlayer player, double amount, String currency) {
        Inventory inventory = player.getInventory();
        
        // First pass: calculate what we have and what we need to remove
        Map<ItemStack, Integer> toRemove = new HashMap<>();
        double remaining = amount;
        
        // Sort money items by value (descending) to use larger denominations first
        List<ItemStack> moneyItems = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                double value = getItemValue(stack, currency);
                if (value > 0) {
                    moneyItems.add(stack);
                }
            }
        }
        
        moneyItems.sort((a, b) -> {
            double valueA = getItemValue(a, currency);
            double valueB = getItemValue(b, currency);
            return Double.compare(valueB, valueA);
        });
        
        // Calculate removal strategy
        for (ItemStack stack : moneyItems) {
            if (remaining <= 0.01) break; // Allow small floating point errors
            
            double value = getItemValue(stack, currency);
            if (value > 0 && value <= remaining + 0.01) { // Allow small floating point errors
                int countToRemove = Math.min(stack.getCount(), (int)Math.ceil(remaining / value));
                if (countToRemove > 0) {
                    toRemove.put(stack, countToRemove);
                    remaining -= countToRemove * value;
                }
            }
        }
        
        // If we can't make exact change, return false
        if (remaining > 0.01) { // Allow small floating point errors
            System.out.println("Enhanced ATM: Cannot make exact change. Remaining: " + remaining);
            return false;
        }
        
        // Remove the items
        for (Map.Entry<ItemStack, Integer> entry : toRemove.entrySet()) {
            System.out.println("Enhanced ATM: Removing " + entry.getValue() + " of " + entry.getKey().getDisplayName().getString());
            entry.getKey().shrink(entry.getValue());
        }
        
        return true;
    }
    
    /**
     * Remove money from inventory and return the total amount removed (for change calculation)
     */
    private static double removeMoneyFromInventoryWithChange(ServerPlayer player, double amount, String currency) {
        Inventory inventory = player.getInventory();
        
        // First pass: calculate what we have and what we need to remove
        Map<ItemStack, Integer> toRemove = new HashMap<>();
        double totalRemoved = 0.0;
        
        // Collect money items with priority: ATM grid first, then inventory
        List<ItemStack> moneyItemsFromGrid = new ArrayList<>();
        List<ItemStack> moneyItemsFromInventory = new ArrayList<>();
        
        // Priority 1: Add money items from ATM grid if player is using ATM
        if (player.containerMenu instanceof com.infinix.enhancedatm.common.container.EnhancedATMContainer) {
            com.infinix.enhancedatm.common.container.EnhancedATMContainer atmContainer = 
                (com.infinix.enhancedatm.common.container.EnhancedATMContainer) player.containerMenu;
            
            // Get items from denomination grid (slots 1-9)
            for (int i = 1; i <= 9; i++) {
                ItemStack stack = atmContainer.getSlot(i).getItem();
                if (!stack.isEmpty()) {
                    double value = getItemValue(stack, currency);
                    if (value > 0) {
                        moneyItemsFromGrid.add(stack);
                    }
                }
            }
        }
        
        // Priority 2: Add money items from player inventory (only if needed)
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                double value = getItemValue(stack, currency);
                if (value > 0) {
                    moneyItemsFromInventory.add(stack);
                }
            }
        }
        
        // Combine lists with grid items first (higher priority)
        List<ItemStack> moneyItems = new ArrayList<>();
        moneyItems.addAll(moneyItemsFromGrid);
        moneyItems.addAll(moneyItemsFromInventory);
        
        // Sort money items by value (descending) to use larger denominations first
        moneyItems.sort((a, b) -> {
            double valueA = getItemValue(a, currency);
            double valueB = getItemValue(b, currency);
            return Double.compare(valueB, valueA);
        });
        
        // Calculate removal strategy - take the smallest number of bills that cover the amount
        for (ItemStack stack : moneyItems) {
            if (totalRemoved >= amount) break; // We have enough
            
            double value = getItemValue(stack, currency);
            if (value > 0) {
                int countToRemove;
                double stillNeeded = amount - totalRemoved;
                
                if (value <= stillNeeded) {
                    // If the bill value is less than what we still need, take as many as we need
                    countToRemove = Math.min(stack.getCount(), (int)Math.ceil(stillNeeded / value));
                } else {
                    // If the bill value is greater than what we need, take just one (this will give change)
                    countToRemove = 1;
                }
                
                if (countToRemove > 0) {
                    toRemove.put(stack, countToRemove);
                    double removedValue = countToRemove * value;
                    totalRemoved += removedValue;
                    
                    System.out.println("Enhanced ATM: Planning to remove " + countToRemove + " x " + 
                        stack.getDisplayName().getString() + " (value: " + value + " each, total: " + removedValue + ")");
                }
            }
        }
        
        // Remove the items properly from their respective locations
        for (Map.Entry<ItemStack, Integer> entry : toRemove.entrySet()) {
            ItemStack stackToRemove = entry.getKey();
            int countToRemove = entry.getValue();
            
            System.out.println("Enhanced ATM: Removing " + countToRemove + " of " + stackToRemove.getDisplayName().getString());
            
            // Find and remove from player inventory first
            boolean removedFromInventory = false;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack invStack = inventory.getItem(i);
                if (invStack == stackToRemove) {
                    System.out.println("Enhanced ATM: Removing from player inventory slot " + i);
                    invStack.shrink(countToRemove);
                    removedFromInventory = true;
                    break;
                }
            }
            
            // If not found in inventory, try ATM grid
            if (!removedFromInventory && player.containerMenu instanceof com.infinix.enhancedatm.common.container.EnhancedATMContainer) {
                com.infinix.enhancedatm.common.container.EnhancedATMContainer atmContainer = 
                    (com.infinix.enhancedatm.common.container.EnhancedATMContainer) player.containerMenu;
                
                for (int i = 1; i <= 9; i++) {
                    ItemStack gridStack = atmContainer.getSlot(i).getItem();
                    if (gridStack == stackToRemove) {
                        System.out.println("Enhanced ATM: Removing from ATM grid slot " + i);
                        gridStack.shrink(countToRemove);
                        break;
                    }
                }
            }
        }
        
        return totalRemoved;
    }
    
    /**
     * Give change to player in appropriate denominations
     */
    private static void giveChangeToPlayer(ServerPlayer player, double change, String currency) {
        try {
            System.out.println("Enhanced ATM: Attempting to give change of " + change + " " + currency + " to " + player.getName().getString());
            
            // Create physical money items for change
            createPhysicalMoneyForPlayer(player, change, currency);
            
            System.out.println("Enhanced ATM: Successfully gave change of " + change + " " + currency + " to " + player.getName().getString());
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error giving change: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create physical money items and add them to player inventory
     */
    private static void createPhysicalMoneyForPlayer(ServerPlayer player, double amount, String currency) {
        try {
            if (modItemsClass == null) {
                System.err.println("Enhanced ATM: ModItems class not available");
                return;
            }
            
            double remaining = amount;
            
            // Define denominations for EUR (largest first)
            if (currency.equalsIgnoreCase("EUR")) {
                // 500€ bills
                if (remaining >= 500) {
                    int count = (int)(remaining / 500);
                    addMoneyItemToPlayer(player, "Euro500", count);
                    remaining -= count * 500;
                }
                // 200€ bills
                if (remaining >= 200) {
                    int count = (int)(remaining / 200);
                    addMoneyItemToPlayer(player, "Euro200", count);
                    remaining -= count * 200;
                }
                // 100€ bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "Euro100", count);
                    remaining -= count * 100;
                }
                // 50€ bills
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "Euro50", count);
                    remaining -= count * 50;
                }
                // 20€ bills
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "Euro20", count);
                    remaining -= count * 20;
                }
                // 10€ bills
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "Euro10", count);
                    remaining -= count * 10;
                }
                // 5€ bills
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "Euro5", count);
                    remaining -= count * 5;
                }
                // 2€ coins
                if (remaining >= 2) {
                    int count = (int)(remaining / 2);
                    addMoneyItemToPlayer(player, "Euro2", count);
                    remaining -= count * 2;
                }
                // 1€ coins
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "Euro1", count);
                    remaining -= count * 1;
                }
                // Ignore cents for simplicity
            }
            // Define denominations for USD (largest first)
            else if (currency.equalsIgnoreCase("USD")) {
                // $100 bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "Dollar100", count);
                    remaining -= count * 100;
                }
                // $50 bills
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "Dollar50", count);
                    remaining -= count * 50;
                }
                // $20 bills
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "Dollar20", count);
                    remaining -= count * 20;
                }
                // $10 bills
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "Dollar10", count);
                    remaining -= count * 10;
                }
                // $5 bills
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "Dollar5", count);
                    remaining -= count * 5;
                }
                // $1 bills
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "Dollar1", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for GBP (British Pounds)
            else if (currency.equalsIgnoreCase("GBP")) {
                // £50 notes
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "Pound50", count);
                    remaining -= count * 50;
                }
                // £20 notes
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "Pound20", count);
                    remaining -= count * 20;
                }
                // £10 notes
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "Pound10", count);
                    remaining -= count * 10;
                }
                // £5 notes
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "Pound5", count);
                    remaining -= count * 5;
                }
                // £1 coins
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "Pound1", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for CAD (Canadian Dollars)
            else if (currency.equalsIgnoreCase("CAD")) {
                // $100 bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "DollarC100", count);
                    remaining -= count * 100;
                }
                // $50 bills
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "DollarC50", count);
                    remaining -= count * 50;
                }
                // $20 bills
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "DollarC20", count);
                    remaining -= count * 20;
                }
                // $10 bills
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "DollarC10", count);
                    remaining -= count * 10;
                }
                // $5 bills
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "DollarC5", count);
                    remaining -= count * 5;
                }
                // $2 coins (Toonie)
                if (remaining >= 2) {
                    int count = (int)(remaining / 2);
                    addMoneyItemToPlayer(player, "Toonie", count);
                    remaining -= count * 2;
                }
                // $1 coins (Loonie)
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "Loonie", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for RON (Romanian Leu)
            else if (currency.equalsIgnoreCase("RON")) {
                // 500 lei bills
                if (remaining >= 500) {
                    int count = (int)(remaining / 500);
                    addMoneyItemToPlayer(player, "Lei500", count);
                    remaining -= count * 500;
                }
                // 200 lei bills
                if (remaining >= 200) {
                    int count = (int)(remaining / 200);
                    addMoneyItemToPlayer(player, "Lei200", count);
                    remaining -= count * 200;
                }
                // 100 lei bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "Lei100", count);
                    remaining -= count * 100;
                }
                // 50 lei bills
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "Lei50", count);
                    remaining -= count * 50;
                }
                // 20 lei bills
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "Lei20", count);
                    remaining -= count * 20;
                }
                // 10 lei bills
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "Lei10", count);
                    remaining -= count * 10;
                }
                // 5 lei bills
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "Lei5", count);
                    remaining -= count * 5;
                }
                // 1 leu bills
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "Leu1", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for MDL (Moldovan Leu)
            else if (currency.equalsIgnoreCase("MDL")) {
                // 1000 lei bills
                if (remaining >= 1000) {
                    int count = (int)(remaining / 1000);
                    addMoneyItemToPlayer(player, "LeiMD1000", count);
                    remaining -= count * 1000;
                }
                // 500 lei bills
                if (remaining >= 500) {
                    int count = (int)(remaining / 500);
                    addMoneyItemToPlayer(player, "LeiMD500", count);
                    remaining -= count * 500;
                }
                // 200 lei bills
                if (remaining >= 200) {
                    int count = (int)(remaining / 200);
                    addMoneyItemToPlayer(player, "LeiMD200", count);
                    remaining -= count * 200;
                }
                // 100 lei bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "LeiMD100", count);
                    remaining -= count * 100;
                }
                // 50 lei bills
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "LeiMD50", count);
                    remaining -= count * 50;
                }
                // 20 lei bills
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "LeiMD20", count);
                    remaining -= count * 20;
                }
                // 10 lei coins
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "LeiMD10", count);
                    remaining -= count * 10;
                }
                // 5 lei coins
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "LeiMD5", count);
                    remaining -= count * 5;
                }
                // 2 lei coins
                if (remaining >= 2) {
                    int count = (int)(remaining / 2);
                    addMoneyItemToPlayer(player, "LeuMD2", count);
                    remaining -= count * 2;
                }
                // 1 leu coins
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "LeuMD1", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for CHF (Swiss Francs)
            else if (currency.equalsIgnoreCase("CHF")) {
                // 1000 franc bills
                if (remaining >= 1000) {
                    int count = (int)(remaining / 1000);
                    addMoneyItemToPlayer(player, "Franc1000", count);
                    remaining -= count * 1000;
                }
                // 200 franc bills
                if (remaining >= 200) {
                    int count = (int)(remaining / 200);
                    addMoneyItemToPlayer(player, "Franc200", count);
                    remaining -= count * 200;
                }
                // 100 franc bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "Franc100", count);
                    remaining -= count * 100;
                }
                // 50 franc bills
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "Franc50", count);
                    remaining -= count * 50;
                }
                // 20 franc bills
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "Franc20", count);
                    remaining -= count * 20;
                }
                // 10 franc bills
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "Franc10", count);
                    remaining -= count * 10;
                }
                // 5 franc coins
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "Franc5", count);
                    remaining -= count * 5;
                }
                // 2 franc coins
                if (remaining >= 2) {
                    int count = (int)(remaining / 2);
                    addMoneyItemToPlayer(player, "Franc2", count);
                    remaining -= count * 2;
                }
                // 1 franc coins
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "Franc1", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for AUD (Australian Dollars)
            else if (currency.equalsIgnoreCase("AUD")) {
                // $100 bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "DollarA100", count);
                    remaining -= count * 100;
                }
                // $50 bills
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "DollarA50", count);
                    remaining -= count * 50;
                }
                // $20 bills
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "DollarA20", count);
                    remaining -= count * 20;
                }
                // $10 bills
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "DollarA10", count);
                    remaining -= count * 10;
                }
                // $5 bills
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "DollarA5", count);
                    remaining -= count * 5;
                }
                // $2 coins
                if (remaining >= 2) {
                    int count = (int)(remaining / 2);
                    addMoneyItemToPlayer(player, "DollarA2", count);
                    remaining -= count * 2;
                }
                // $1 coins
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "DollarA1", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for JPY (Japanese Yen)
            else if (currency.equalsIgnoreCase("JPY")) {
                // ¥10000 bills
                if (remaining >= 10000) {
                    int count = (int)(remaining / 10000);
                    addMoneyItemToPlayer(player, "Yen10000", count);
                    remaining -= count * 10000;
                }
                // ¥5000 bills
                if (remaining >= 5000) {
                    int count = (int)(remaining / 5000);
                    addMoneyItemToPlayer(player, "Yen5000", count);
                    remaining -= count * 5000;
                }
                // ¥1000 bills
                if (remaining >= 1000) {
                    int count = (int)(remaining / 1000);
                    addMoneyItemToPlayer(player, "Yen1000", count);
                    remaining -= count * 1000;
                }
                // ¥500 coins
                if (remaining >= 500) {
                    int count = (int)(remaining / 500);
                    addMoneyItemToPlayer(player, "Yen500", count);
                    remaining -= count * 500;
                }
                // ¥100 coins
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "Yen100", count);
                    remaining -= count * 100;
                }
                // ¥50 coins
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "Yen50", count);
                    remaining -= count * 50;
                }
                // ¥10 coins
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "Yen10", count);
                    remaining -= count * 10;
                }
                // ¥5 coins
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "Yen5", count);
                    remaining -= count * 5;
                }
                // ¥1 coins
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "Yen1", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for CZK (Czech Koruna)
            else if (currency.equalsIgnoreCase("CZK")) {
                // 5000 Kč bills
                if (remaining >= 5000) {
                    int count = (int)(remaining / 5000);
                    addMoneyItemToPlayer(player, "CZkr5000", count);
                    remaining -= count * 5000;
                }
                // 2000 Kč bills
                if (remaining >= 2000) {
                    int count = (int)(remaining / 2000);
                    addMoneyItemToPlayer(player, "CZkr2000", count);
                    remaining -= count * 2000;
                }
                // 1000 Kč bills
                if (remaining >= 1000) {
                    int count = (int)(remaining / 1000);
                    addMoneyItemToPlayer(player, "CZkr1000", count);
                    remaining -= count * 1000;
                }
                // 500 Kč bills
                if (remaining >= 500) {
                    int count = (int)(remaining / 500);
                    addMoneyItemToPlayer(player, "CZkr500", count);
                    remaining -= count * 500;
                }
                // 200 Kč bills
                if (remaining >= 200) {
                    int count = (int)(remaining / 200);
                    addMoneyItemToPlayer(player, "CZkr200", count);
                    remaining -= count * 200;
                }
                // 100 Kč bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "CZkr100", count);
                    remaining -= count * 100;
                }
                // 50 Kč coins
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "CZkr50", count);
                    remaining -= count * 50;
                }
                // 20 Kč coins
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "CZkr20", count);
                    remaining -= count * 20;
                }
                // 10 Kč coins
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "CZkr10", count);
                    remaining -= count * 10;
                }
                // 5 Kč coins
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "CZkr5", count);
                    remaining -= count * 5;
                }
                // 2 Kč coins
                if (remaining >= 2) {
                    int count = (int)(remaining / 2);
                    addMoneyItemToPlayer(player, "CZkr2", count);
                    remaining -= count * 2;
                }
                // 1 Kč coins
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "CZkr1", count);
                    remaining -= count * 1;
                }
            }
            // Define denominations for MXN (Mexican Pesos)
            else if (currency.equalsIgnoreCase("MXN")) {
                // $1000 bills
                if (remaining >= 1000) {
                    int count = (int)(remaining / 1000);
                    addMoneyItemToPlayer(player, "MXPeso1000", count);
                    remaining -= count * 1000;
                }
                // $500 bills
                if (remaining >= 500) {
                    int count = (int)(remaining / 500);
                    addMoneyItemToPlayer(player, "MXPeso500", count);
                    remaining -= count * 500;
                }
                // $200 bills
                if (remaining >= 200) {
                    int count = (int)(remaining / 200);
                    addMoneyItemToPlayer(player, "MXPeso200", count);
                    remaining -= count * 200;
                }
                // $100 bills
                if (remaining >= 100) {
                    int count = (int)(remaining / 100);
                    addMoneyItemToPlayer(player, "MXPeso100", count);
                    remaining -= count * 100;
                }
                // $50 bills
                if (remaining >= 50) {
                    int count = (int)(remaining / 50);
                    addMoneyItemToPlayer(player, "MXPeso50", count);
                    remaining -= count * 50;
                }
                // $20 bills
                if (remaining >= 20) {
                    int count = (int)(remaining / 20);
                    addMoneyItemToPlayer(player, "MXPeso20", count);
                    remaining -= count * 20;
                }
                // $10 coins
                if (remaining >= 10) {
                    int count = (int)(remaining / 10);
                    addMoneyItemToPlayer(player, "MXPeso10", count);
                    remaining -= count * 10;
                }
                // $5 coins
                if (remaining >= 5) {
                    int count = (int)(remaining / 5);
                    addMoneyItemToPlayer(player, "MXPeso5", count);
                    remaining -= count * 5;
                }
                // $2 coins
                if (remaining >= 2) {
                    int count = (int)(remaining / 2);
                    addMoneyItemToPlayer(player, "MXPeso2", count);
                    remaining -= count * 2;
                }
                // $1 coins
                if (remaining >= 1) {
                    int count = (int)(remaining / 1);
                    addMoneyItemToPlayer(player, "MXPeso1", count);
                    remaining -= count * 1;
                }
            }
            // Fallback for unsupported currencies - try to give EUR as closest alternative
            else {
                System.out.println("Enhanced ATM: Currency " + currency + " not implemented, using EUR as fallback");
                player.sendSystemMessage(createWarningMessage(CURRENCY_NOT_AVAILABLE, currency, "EUR"));
                
                // Give EUR equivalent (use createPhysicalMoneyForPlayer recursively)
                if (remaining > 0) {
                    createPhysicalMoneyForPlayer(player, remaining, "EUR");
                }
                return; // Exit to avoid the remaining amount check below
            }
            
            // Handle any remaining fractional amount by giving the smallest available denomination
            if (remaining >= 0.01) {
                System.out.println("Enhanced ATM: Remaining fractional amount: " + remaining + " " + currency + " - giving smallest denomination");
                // Give one unit of the smallest denomination to cover fractional amounts
                if (currency.equalsIgnoreCase("EUR")) {
                    addMoneyItemToPlayer(player, "Euro1", 1);
                } else if (currency.equalsIgnoreCase("USD")) {
                    addMoneyItemToPlayer(player, "Dollar1", 1);
                } else if (currency.equalsIgnoreCase("GBP")) {
                    addMoneyItemToPlayer(player, "Pound1", 1);
                } else if (currency.equalsIgnoreCase("CAD")) {
                    addMoneyItemToPlayer(player, "Loonie", 1);
                } else if (currency.equalsIgnoreCase("RON")) {
                    addMoneyItemToPlayer(player, "Leu1", 1);
                } else if (currency.equalsIgnoreCase("MDL")) {
                    addMoneyItemToPlayer(player, "LeuMD1", 1);
                } else if (currency.equalsIgnoreCase("CHF")) {
                    addMoneyItemToPlayer(player, "Franc1", 1);
                } else if (currency.equalsIgnoreCase("AUD")) {
                    addMoneyItemToPlayer(player, "DollarA1", 1);
                } else if (currency.equalsIgnoreCase("JPY")) {
                    addMoneyItemToPlayer(player, "Yen1", 1);
                } else if (currency.equalsIgnoreCase("CZK")) {
                    addMoneyItemToPlayer(player, "CZkr1", 1);
                } else if (currency.equalsIgnoreCase("MXN")) {
                    addMoneyItemToPlayer(player, "MXPeso1", 1);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error creating physical money: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Add a specific money item to player inventory
     */
    private static void addMoneyItemToPlayer(ServerPlayer player, String itemFieldName, int count) {
        try {
            Field itemField = modItemsClass.getDeclaredField(itemFieldName);
            itemField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.function.Supplier<Item> itemSupplier = (java.util.function.Supplier<Item>) itemField.get(null);
            Item moneyItem = itemSupplier.get();
            
            ItemStack moneyStack = new ItemStack(moneyItem, count);
            player.getInventory().add(moneyStack);
            
            System.out.println("Enhanced ATM: Added " + count + " of " + itemFieldName + " to player inventory");
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error adding money item " + itemFieldName + ": " + e.getMessage());
        }
    }

    /**
     * Get the value of an item stack if it's money from BubusteinMoney
     */
    private static double getItemValue(ItemStack stack, String currency) {
        if (stack.isEmpty()) return 0.0;
        
        try {
            String itemName = stack.getItem().getDescriptionId();
            
            // Debug info
            System.out.println("Enhanced ATM: Checking item: " + itemName + " for currency: " + currency);
            
            // Check if it's a BubusteinMoney item
            if (!itemName.contains("bubusteinmoneymod")) {
                return 0.0;
            }
            
            switch (currency.toUpperCase()) {
                case "EUR":
                    // Use exact matches for better performance and safety
                    switch (itemName) {
                        case "item.bubusteinmoneymod.five_hundred_euros":
                            System.out.println("Enhanced ATM: Matched 500€ for item: " + itemName);
                            return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_euros":
                            System.out.println("Enhanced ATM: Matched 200€ for item: " + itemName);
                            return 200.00;
                        case "item.bubusteinmoneymod.hundred_euros":
                            System.out.println("Enhanced ATM: Matched 100€ for item: " + itemName);
                            return 100.00;
                        case "item.bubusteinmoneymod.fifty_euros":
                            System.out.println("Enhanced ATM: Matched 50€ for item: " + itemName);
                            return 50.00;
                        case "item.bubusteinmoneymod.twenty_euros":
                            System.out.println("Enhanced ATM: Matched 20€ for item: " + itemName);
                            return 20.00;
                        case "item.bubusteinmoneymod.ten_euros":
                            System.out.println("Enhanced ATM: Matched 10€ for item: " + itemName);
                            return 10.00;
                        case "item.bubusteinmoneymod.five_euros":
                            System.out.println("Enhanced ATM: Matched 5€ for item: " + itemName);
                            return 5.00;
                        case "item.bubusteinmoneymod.two_euros":
                            System.out.println("Enhanced ATM: Matched 2€ for item: " + itemName);
                            return 2.00;
                        case "item.bubusteinmoneymod.one_euro":
                            System.out.println("Enhanced ATM: Matched 1€ for item: " + itemName);
                            return 1.00;
                        case "item.bubusteinmoneymod.fifty_ecents":
                            System.out.println("Enhanced ATM: Matched 0.50€ for item: " + itemName);
                            return 0.50;
                        case "item.bubusteinmoneymod.twenty_ecents":
                            System.out.println("Enhanced ATM: Matched 0.20€ for item: " + itemName);
                            return 0.20;
                        case "item.bubusteinmoneymod.ten_ecents":
                            System.out.println("Enhanced ATM: Matched 0.10€ for item: " + itemName);
                            return 0.10;
                        case "item.bubusteinmoneymod.five_ecents":
                            System.out.println("Enhanced ATM: Matched 0.05€ for item: " + itemName);
                            return 0.05;
                        case "item.bubusteinmoneymod.two_ecents":
                            System.out.println("Enhanced ATM: Matched 0.02€ for item: " + itemName);
                            return 0.02;
                        case "item.bubusteinmoneymod.one_ecent":
                            System.out.println("Enhanced ATM: Matched 0.01€ for item: " + itemName);
                            return 0.01;
                    }
                    break;
                    
                case "USD":
                    // Use exact matches for better performance and safety
                    switch (itemName) {
                        case "item.bubusteinmoneymod.hundred_dollars": return 100.00;
                        case "item.bubusteinmoneymod.fifty_dollars": return 50.00;
                        case "item.bubusteinmoneymod.twenty_dollars": return 20.00;
                        case "item.bubusteinmoneymod.ten_dollars": return 10.00;
                        case "item.bubusteinmoneymod.five_dollars": return 5.00;
                        case "item.bubusteinmoneymod.one_dollar": return 1.00;
                        case "item.bubusteinmoneymod.fifty_cents": return 0.50;
                        case "item.bubusteinmoneymod.twentyfive_cents": return 0.25;
                        case "item.bubusteinmoneymod.ten_cents": return 0.10;
                        case "item.bubusteinmoneymod.five_cents": return 0.05;
                        case "item.bubusteinmoneymod.one_cent": return 0.01;
                    }
                    break;
                    
                case "GBP":
                    // Use exact matches for better performance and safety
                    switch (itemName) {
                        case "item.bubusteinmoneymod.fifty_pounds": return 50.00;
                        case "item.bubusteinmoneymod.twenty_pounds": return 20.00;
                        case "item.bubusteinmoneymod.ten_pounds": return 10.00;
                        case "item.bubusteinmoneymod.five_pounds": return 5.00;
                        case "item.bubusteinmoneymod.two_pounds": return 2.00;
                        case "item.bubusteinmoneymod.one_pound": return 1.00;
                        case "item.bubusteinmoneymod.fifty_pence": return 0.50;
                        case "item.bubusteinmoneymod.twenty_pence": return 0.20;
                        case "item.bubusteinmoneymod.ten_pence": return 0.10;
                        case "item.bubusteinmoneymod.five_pence": return 0.05;
                        case "item.bubusteinmoneymod.two_pence": return 0.02;
                        case "item.bubusteinmoneymod.one_pence": return 0.01;
                    }
                    break;
                    
                case "CAD":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.hundred_cdollars": return 100.00;
                        case "item.bubusteinmoneymod.fifty_cdollars": return 50.00;
                        case "item.bubusteinmoneymod.twenty_cdollars": return 20.00;
                        case "item.bubusteinmoneymod.ten_cdollars": return 10.00;
                        case "item.bubusteinmoneymod.five_cdollars": return 5.00;
                        case "item.bubusteinmoneymod.toonie": return 2.00;
                        case "item.bubusteinmoneymod.loonie": return 1.00;
                        case "item.bubusteinmoneymod.twentyfive_ccents": return 0.25;
                        case "item.bubusteinmoneymod.ten_ccents": return 0.10;
                        case "item.bubusteinmoneymod.five_ccents": return 0.05;
                    }
                    break;
                    
                case "RON":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.cinci_sute_lei": return 500.00;
                        case "item.bubusteinmoneymod.doua_sute_lei": return 200.00;
                        case "item.bubusteinmoneymod.suta_lei": return 100.00;
                        case "item.bubusteinmoneymod.cincizeci_lei": return 50.00;
                        case "item.bubusteinmoneymod.douazeci_lei": return 20.00;
                        case "item.bubusteinmoneymod.zece_lei": return 10.00;
                        case "item.bubusteinmoneymod.cinci_lei": return 5.00;
                        case "item.bubusteinmoneymod.un_leu": return 1.00;
                        case "item.bubusteinmoneymod.cincizeci_bani": return 0.50;
                        case "item.bubusteinmoneymod.zece_bani": return 0.10;
                        case "item.bubusteinmoneymod.cinci_bani": return 0.05;
                        case "item.bubusteinmoneymod.un_ban": return 0.01;
                    }
                    break;
                    
                case "MDL":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.mie_lei_md": return 1000.00;
                        case "item.bubusteinmoneymod.cinci_sute_lei_md": return 500.00;
                        case "item.bubusteinmoneymod.doua_sute_lei_md": return 200.00;
                        case "item.bubusteinmoneymod.suta_lei_md": return 100.00;
                        case "item.bubusteinmoneymod.cincizeci_lei_md": return 50.00;
                        case "item.bubusteinmoneymod.douazeci_lei_md": return 20.00;
                        case "item.bubusteinmoneymod.zece_lei_md": return 10.00;
                        case "item.bubusteinmoneymod.cinci_lei_md": return 5.00;
                        case "item.bubusteinmoneymod.doi_lei_md": return 2.00;
                        case "item.bubusteinmoneymod.un_leu_md": return 1.00;
                        case "item.bubusteinmoneymod.cincizeci_bani_md": return 0.50;
                        case "item.bubusteinmoneymod.douazecicinci_bani_md": return 0.25;
                        case "item.bubusteinmoneymod.zece_bani_md": return 0.10;
                        case "item.bubusteinmoneymod.cinci_bani_md": return 0.05;
                    }
                    break;
                    
                case "CHF":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.thousand_francs": return 1000.00;
                        case "item.bubusteinmoneymod.two_hundred_francs": return 200.00;
                        case "item.bubusteinmoneymod.hundred_francs": return 100.00;
                        case "item.bubusteinmoneymod.fifty_francs": return 50.00;
                        case "item.bubusteinmoneymod.twenty_francs": return 20.00;
                        case "item.bubusteinmoneymod.ten_francs": return 10.00;
                        case "item.bubusteinmoneymod.five_francs": return 5.00;
                        case "item.bubusteinmoneymod.two_francs": return 2.00;
                        case "item.bubusteinmoneymod.one_franc": return 1.00;
                        case "item.bubusteinmoneymod.half_franc": return 0.50;
                        case "item.bubusteinmoneymod.twenty_centimes": return 0.20;
                        case "item.bubusteinmoneymod.ten_centimes": return 0.10;
                        case "item.bubusteinmoneymod.five_centimes": return 0.05;
                    }
                    break;
                    
                case "AUD":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.hundred_adollars": return 100.00;
                        case "item.bubusteinmoneymod.fifty_adollars": return 50.00;
                        case "item.bubusteinmoneymod.twenty_adollars": return 20.00;
                        case "item.bubusteinmoneymod.ten_adollars": return 10.00;
                        case "item.bubusteinmoneymod.five_adollars": return 5.00;
                        case "item.bubusteinmoneymod.two_adollars": return 2.00;
                        case "item.bubusteinmoneymod.one_adollar": return 1.00;
                        case "item.bubusteinmoneymod.fifty_acents": return 0.50;
                        case "item.bubusteinmoneymod.twenty_acents": return 0.20;
                        case "item.bubusteinmoneymod.ten_acents": return 0.10;
                        case "item.bubusteinmoneymod.five_acents": return 0.05;
                    }
                    break;
                    
                case "JPY":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.ten_thousand_yen": return 10000.00;
                        case "item.bubusteinmoneymod.five_thousand_yen": return 5000.00;
                        case "item.bubusteinmoneymod.thousand_yen": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_yen": return 500.00;
                        case "item.bubusteinmoneymod.hundred_yen": return 100.00;
                        case "item.bubusteinmoneymod.fifty_yen": return 50.00;
                        case "item.bubusteinmoneymod.ten_yen": return 10.00;
                        case "item.bubusteinmoneymod.five_yen": return 5.00;
                        case "item.bubusteinmoneymod.one_yen": return 1.00;
                    }
                    break;
                    
                case "CZK":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.five_thousand_cz_krone": return 5000.00;
                        case "item.bubusteinmoneymod.two_thousand_cz_krone": return 2000.00;
                        case "item.bubusteinmoneymod.thousand_cz_krone": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_cz_krone": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_cz_krone": return 200.00;
                        case "item.bubusteinmoneymod.hundred_cz_krone": return 100.00;
                        case "item.bubusteinmoneymod.fifty_cz_krone": return 50.00;
                        case "item.bubusteinmoneymod.twenty_cz_krone": return 20.00;
                        case "item.bubusteinmoneymod.ten_cz_krone": return 10.00;
                        case "item.bubusteinmoneymod.five_cz_krone": return 5.00;
                        case "item.bubusteinmoneymod.two_cz_krone": return 2.00;
                        case "item.bubusteinmoneymod.one_cz_krone": return 1.00;
                    }
                    break;
                    
                case "MXN":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.thousand_mx_pesos": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_mx_pesos": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_mx_pesos": return 200.00;
                        case "item.bubusteinmoneymod.hundred_mx_pesos": return 100.00;
                        case "item.bubusteinmoneymod.fifty_mx_pesos": return 50.00;
                        case "item.bubusteinmoneymod.twenty_mx_pesos": return 20.00;
                        case "item.bubusteinmoneymod.ten_mx_pesos": return 10.00;
                        case "item.bubusteinmoneymod.five_mx_pesos": return 5.00;
                        case "item.bubusteinmoneymod.two_mx_pesos": return 2.00;
                        case "item.bubusteinmoneymod.one_mx_peso": return 1.00;
                        case "item.bubusteinmoneymod.fifty_mx_centavos": return 0.50;
                        case "item.bubusteinmoneymod.twenty_mx_centavos": return 0.20;
                        case "item.bubusteinmoneymod.ten_mx_centavos": return 0.10;
                        case "item.bubusteinmoneymod.five_mx_centavos": return 0.05;
                    }
                    break;
                    
                case "CNY":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.hundred_cn_yuan": return 100.00;
                        case "item.bubusteinmoneymod.fifty_cn_yuan": return 50.00;
                        case "item.bubusteinmoneymod.twenty_cn_yuan": return 20.00;
                        case "item.bubusteinmoneymod.ten_cn_yuan": return 10.00;
                        case "item.bubusteinmoneymod.five_cn_yuan": return 5.00;
                        case "item.bubusteinmoneymod.one_cn_yuan": return 1.00;
                        case "item.bubusteinmoneymod.five_cn_jiao": return 0.50;
                        case "item.bubusteinmoneymod.one_cn_jiao": return 0.10;
                    }
                    break;
                    
                case "NOK":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.thousand_no_krone": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_no_krone": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_no_krone": return 200.00;
                        case "item.bubusteinmoneymod.hundred_no_krone": return 100.00;
                        case "item.bubusteinmoneymod.fifty_no_krone": return 50.00;
                        case "item.bubusteinmoneymod.twenty_no_krone": return 20.00;
                        case "item.bubusteinmoneymod.ten_no_krone": return 10.00;
                        case "item.bubusteinmoneymod.five_no_krone": return 5.00;
                        case "item.bubusteinmoneymod.one_no_krone": return 1.00;
                    }
                    break;
                    
                case "DKK":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.thousand_dk_krone": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_dk_krone": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_dk_krone": return 200.00;
                        case "item.bubusteinmoneymod.hundred_dk_krone": return 100.00;
                        case "item.bubusteinmoneymod.fifty_dk_krone": return 50.00;
                        case "item.bubusteinmoneymod.twenty_dk_krone": return 20.00;
                        case "item.bubusteinmoneymod.ten_dk_krone": return 10.00;
                        case "item.bubusteinmoneymod.five_dk_krone": return 5.00;
                        case "item.bubusteinmoneymod.two_dk_krone": return 2.00;
                        case "item.bubusteinmoneymod.one_dk_krone": return 1.00;
                        case "item.bubusteinmoneymod.fifty_aere_dk": return 0.50;
                    }
                    break;
                    
                case "SEK":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.thousand_se_krone": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_se_krone": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_se_krone": return 200.00;
                        case "item.bubusteinmoneymod.hundred_se_krone": return 100.00;
                        case "item.bubusteinmoneymod.fifty_se_krone": return 50.00;
                        case "item.bubusteinmoneymod.twenty_se_krone": return 20.00;
                        case "item.bubusteinmoneymod.ten_se_krone": return 10.00;
                        case "item.bubusteinmoneymod.five_se_krone": return 5.00;
                        case "item.bubusteinmoneymod.two_se_krone": return 2.00;
                        case "item.bubusteinmoneymod.one_se_krone": return 1.00;
                    }
                    break;
                    
                case "HUF":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.twenty_thousand_ft": return 20000.00;
                        case "item.bubusteinmoneymod.ten_thousand_ft": return 10000.00;
                        case "item.bubusteinmoneymod.five_thousand_ft": return 5000.00;
                        case "item.bubusteinmoneymod.two_thousand_ft": return 2000.00;
                        case "item.bubusteinmoneymod.thousand_ft": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_ft": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_ft": return 200.00;
                        case "item.bubusteinmoneymod.hundred_ft": return 100.00;
                        case "item.bubusteinmoneymod.fifty_ft": return 50.00;
                        case "item.bubusteinmoneymod.twenty_ft": return 20.00;
                        case "item.bubusteinmoneymod.ten_ft": return 10.00;
                        case "item.bubusteinmoneymod.five_ft": return 5.00;
                    }
                    break;
                    
                case "PLN":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.five_hundred_zloty": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_zloty": return 200.00;
                        case "item.bubusteinmoneymod.hundred_zloty": return 100.00;
                        case "item.bubusteinmoneymod.fifty_zloty": return 50.00;
                        case "item.bubusteinmoneymod.twenty_zloty": return 20.00;
                        case "item.bubusteinmoneymod.ten_zloty": return 10.00;
                        case "item.bubusteinmoneymod.five_zloty": return 5.00;
                        case "item.bubusteinmoneymod.two_zloty": return 2.00;
                        case "item.bubusteinmoneymod.one_zloty": return 1.00;
                        case "item.bubusteinmoneymod.fifty_groszy": return 0.50;
                        case "item.bubusteinmoneymod.twenty_groszy": return 0.20;
                        case "item.bubusteinmoneymod.ten_groszy": return 0.10;
                        case "item.bubusteinmoneymod.five_groszy": return 0.05;
                        case "item.bubusteinmoneymod.two_grosze": return 0.02;
                        case "item.bubusteinmoneymod.one_grosz": return 0.01;
                    }
                    break;
                    
                case "RSD":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.five_thousand_rs_dinar": return 5000.00;
                        case "item.bubusteinmoneymod.two_thousand_rs_dinar": return 2000.00;
                        case "item.bubusteinmoneymod.thousand_rs_dinar": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_rs_dinar": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_rs_dinar": return 200.00;
                        case "item.bubusteinmoneymod.hundred_rs_dinar": return 100.00;
                        case "item.bubusteinmoneymod.fifty_rs_dinar": return 50.00;
                        case "item.bubusteinmoneymod.twenty_rs_dinar": return 20.00;
                        case "item.bubusteinmoneymod.ten_rs_dinar": return 10.00;
                        case "item.bubusteinmoneymod.five_rs_dinar": return 5.00;
                        case "item.bubusteinmoneymod.two_rs_dinar": return 2.00;
                        case "item.bubusteinmoneymod.one_rs_dinar": return 1.00;
                    }
                    break;
                    
                case "ISK":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.ten_thousand_is_krone": return 10000.00;
                        case "item.bubusteinmoneymod.five_thousand_is_krone": return 5000.00;
                        case "item.bubusteinmoneymod.two_thousand_is_krone": return 2000.00;
                        case "item.bubusteinmoneymod.thousand_is_krone": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_is_krone": return 500.00;
                        case "item.bubusteinmoneymod.hundred_is_krone": return 100.00;
                        case "item.bubusteinmoneymod.fifty_is_krone": return 50.00;
                        case "item.bubusteinmoneymod.ten_is_krone": return 10.00;
                        case "item.bubusteinmoneymod.five_is_krone": return 5.00;
                        case "item.bubusteinmoneymod.one_is_krone": return 1.00;
                    }
                    break;
                    
                case "INR":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.five_hundred_in_rupees": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_in_rupees": return 200.00;
                        case "item.bubusteinmoneymod.hundred_in_rupees": return 100.00;
                        case "item.bubusteinmoneymod.fifty_in_rupees": return 50.00;
                        case "item.bubusteinmoneymod.twenty_in_rupees": return 20.00;
                        case "item.bubusteinmoneymod.ten_in_rupees": return 10.00;
                        case "item.bubusteinmoneymod.five_in_rupees": return 5.00;
                        case "item.bubusteinmoneymod.two_in_rupees": return 2.00;
                        case "item.bubusteinmoneymod.one_in_rupee": return 1.00;
                    }
                    break;
                    
                case "KRW":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.fifty_thousand_kr_won": return 50000.00;
                        case "item.bubusteinmoneymod.ten_thousand_kr_won": return 10000.00;
                        case "item.bubusteinmoneymod.five_thousand_kr_won": return 5000.00;
                        case "item.bubusteinmoneymod.thousand_kr_won": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_kr_won": return 500.00;
                        case "item.bubusteinmoneymod.hundred_kr_won": return 100.00;
                        case "item.bubusteinmoneymod.fifty_kr_won": return 50.00;
                        case "item.bubusteinmoneymod.ten_kr_won": return 10.00;
                    }
                    break;
                    
                case "BRL":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.two_hundred_br_reais": return 200.00;
                        case "item.bubusteinmoneymod.hundred_br_reais": return 100.00;
                        case "item.bubusteinmoneymod.fifty_br_reais": return 50.00;
                        case "item.bubusteinmoneymod.twenty_br_reais": return 20.00;
                        case "item.bubusteinmoneymod.ten_br_reais": return 10.00;
                        case "item.bubusteinmoneymod.five_br_reais": return 5.00;
                        case "item.bubusteinmoneymod.two_br_reais": return 2.00;
                        case "item.bubusteinmoneymod.one_br_real": return 1.00;
                        case "item.bubusteinmoneymod.fifty_br_centavos": return 0.50;
                        case "item.bubusteinmoneymod.twentyfive_br_centavos": return 0.25;
                        case "item.bubusteinmoneymod.ten_br_centavos": return 0.10;
                        case "item.bubusteinmoneymod.five_br_centavos": return 0.05;
                    }
                    break;
                    
                case "ZAR":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.two_hundred_za_rand": return 200.00;
                        case "item.bubusteinmoneymod.hundred_za_rand": return 100.00;
                        case "item.bubusteinmoneymod.fifty_za_rand": return 50.00;
                        case "item.bubusteinmoneymod.twenty_za_rand": return 20.00;
                        case "item.bubusteinmoneymod.ten_za_rand": return 10.00;
                        case "item.bubusteinmoneymod.five_za_rand": return 5.00;
                        case "item.bubusteinmoneymod.two_za_rand": return 2.00;
                        case "item.bubusteinmoneymod.one_za_rand": return 1.00;
                        case "item.bubusteinmoneymod.fifty_za_cents": return 0.50;
                        case "item.bubusteinmoneymod.twenty_za_cents": return 0.20;
                        case "item.bubusteinmoneymod.ten_za_cents": return 0.10;
                    }
                    break;
                    
                case "TRY":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.two_hundred_tr_lira": return 200.00;
                        case "item.bubusteinmoneymod.hundred_tr_lira": return 100.00;
                        case "item.bubusteinmoneymod.fifty_tr_lira": return 50.00;
                        case "item.bubusteinmoneymod.twenty_tr_lira": return 20.00;
                        case "item.bubusteinmoneymod.ten_tr_lira": return 10.00;
                        case "item.bubusteinmoneymod.five_tr_lira": return 5.00;
                        case "item.bubusteinmoneymod.one_tr_lira": return 1.00;
                        case "item.bubusteinmoneymod.fifty_kurus": return 0.50;
                        case "item.bubusteinmoneymod.twenty_five_kurus": return 0.25;
                        case "item.bubusteinmoneymod.ten_kurus": return 0.10;
                        case "item.bubusteinmoneymod.five_kurus": return 0.05;
                        case "item.bubusteinmoneymod.one_kurus": return 0.01;
                    }
                    break;
                    
                case "NZD":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.hundred_nz_dollars": return 100.00;
                        case "item.bubusteinmoneymod.fifty_nz_dollars": return 50.00;
                        case "item.bubusteinmoneymod.twenty_nz_dollars": return 20.00;
                        case "item.bubusteinmoneymod.ten_nz_dollars": return 10.00;
                        case "item.bubusteinmoneymod.five_nz_dollars": return 5.00;
                        case "item.bubusteinmoneymod.two_nz_dollars": return 2.00;
                        case "item.bubusteinmoneymod.one_nz_dollar": return 1.00;
                        case "item.bubusteinmoneymod.fifty_nz_cents": return 0.50;
                        case "item.bubusteinmoneymod.twenty_nz_cents": return 0.20;
                        case "item.bubusteinmoneymod.ten_nz_cents": return 0.10;
                    }
                    break;
                    
                case "PHP":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.thousand_ph_piso": return 1000.00;
                        case "item.bubusteinmoneymod.five_hundred_ph_piso": return 500.00;
                        case "item.bubusteinmoneymod.two_hundred_ph_piso": return 200.00;
                        case "item.bubusteinmoneymod.hundred_ph_piso": return 100.00;
                        case "item.bubusteinmoneymod.fifty_ph_piso": return 50.00;
                        case "item.bubusteinmoneymod.twenty_ph_piso": return 20.00;
                        case "item.bubusteinmoneymod.ten_ph_piso": return 10.00;
                        case "item.bubusteinmoneymod.five_ph_piso": return 5.00;
                        case "item.bubusteinmoneymod.one_ph_piso": return 1.00;
                        case "item.bubusteinmoneymod.twenty_five_ph_sentimo": return 0.25;
                        case "item.bubusteinmoneymod.five_ph_sentimo": return 0.05;
                        case "item.bubusteinmoneymod.one_ph_sentimo": return 0.01;
                    }
                    break;
                    
                case "EGP":
                    switch (itemName) {
                        case "item.bubusteinmoneymod.two_hundred_eg_pound": return 200.00;
                        case "item.bubusteinmoneymod.hundred_eg_pound": return 100.00;
                        case "item.bubusteinmoneymod.fifty_eg_pound": return 50.00;
                        case "item.bubusteinmoneymod.twenty_eg_pound": return 20.00;
                        case "item.bubusteinmoneymod.ten_eg_pound": return 10.00;
                        case "item.bubusteinmoneymod.five_eg_pound": return 5.00;
                        case "item.bubusteinmoneymod.one_eg_pound": return 1.00;
                        case "item.bubusteinmoneymod.fifty_eg_piastres": return 0.50;
                        case "item.bubusteinmoneymod.twentyfive_eg_piastres": return 0.25;
                    }
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error getting item value: " + e.getMessage());
        }
        
        return 0.0;
    }

    /**
     * Add money to credit card using reflection
     */
    private static void addMoneyToCard(ItemStack cardStack, double amount, String currency) {
        try {
            // Get current balance and round both values to avoid precision errors
            double currentBalance = roundMoney(getCardBalance(cardStack));
            double roundedAmount = roundMoney(amount);
            double newBalance = roundMoney(currentBalance + roundedAmount);
            
            System.out.println("Enhanced ATM: Current balance: " + currentBalance + ", Adding: " + roundedAmount + ", New balance: " + newBalance);
            
            // Get the CardItem instance from the ItemStack
            Item cardItem = cardStack.getItem();
            
            // Use reflection to call the setMoney method on the CardItem instance
            Method setMoneyMethod = cardItemClass.getDeclaredMethod("setMoney", ItemStack.class, double.class);
            setMoneyMethod.setAccessible(true);
            
            // Invoke the method on the CardItem instance (not null!)
            setMoneyMethod.invoke(cardItem, cardStack, newBalance);
            
            // Also set the currency of the card to match the deposit currency
            if (setCurrencyMethod != null) {
                setCurrencyMethod.setAccessible(true);
                setCurrencyMethod.invoke(cardItem, cardStack, currency);
                System.out.println("Enhanced ATM: Successfully set card currency to: " + currency);
            }
            
            System.out.println("Enhanced ATM: Successfully set card balance to: " + newBalance + " " + currency);
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error adding money to card: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Add money to credit card directly (for denomination exchange) - returns success status
     */
    private static boolean addMoneyToCardDirectly(ItemStack cardStack, double amount, String currency) {
        try {
            // Get current balance and round both values to avoid precision errors
            double currentBalance = roundMoney(getCardBalance(cardStack));
            double roundedAmount = roundMoney(amount);
            double newBalance = roundMoney(currentBalance + roundedAmount);
            
            System.out.println("Enhanced ATM: Direct card deposit - Current: " + currentBalance + ", Adding: " + roundedAmount + ", New: " + newBalance);
            
            // Get the CardItem instance from the ItemStack
            Item cardItem = cardStack.getItem();
            
            // Use reflection to call the setMoney method on the CardItem instance
            Method setMoneyMethod = cardItemClass.getDeclaredMethod("setMoney", ItemStack.class, double.class);
            setMoneyMethod.setAccessible(true);
            
            // Invoke the method on the CardItem instance
            setMoneyMethod.invoke(cardItem, cardStack, newBalance);
            
            // Also set the currency of the card to match the deposit currency
            if (setCurrencyMethod != null) {
                setCurrencyMethod.setAccessible(true);
                setCurrencyMethod.invoke(cardItem, cardStack, currency);
                System.out.println("Enhanced ATM: Successfully set card currency to: " + currency);
            }
            
            System.out.println("Enhanced ATM: Successfully deposited " + roundedAmount + " " + currency + " to card");
            return true;
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error in direct card deposit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Add a specific money item to player inventory with fallback support
     */
    private static void addMoneyItemToPlayer(Player player, String itemName, int count) {
        try {
            // Get the item field by name
            Field itemField = modItemsClass.getDeclaredField(itemName);
            itemField.setAccessible(true);
            
            // Get the item from the field
            Object itemObject = itemField.get(null);
            if (itemObject instanceof Item item) {
                ItemStack itemStack = new ItemStack(item, count);
                player.getInventory().add(itemStack);
                System.out.println("Enhanced ATM: Added " + count + "x " + itemName + " to player " + player.getName().getString());
            }
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Failed to add money item " + itemName + " to player: " + e.getMessage());
            // Try fallback strategy - give a generic equivalent
            attemptFallbackDenomination(player, itemName, count);
        }
    }
    
    /**
     * Fallback method to give alternative denominations when exact item is not available
     */
    private static void attemptFallbackDenomination(Player player, String originalItemName, int count) {
        // Map of fallback items for each currency type
        String[] fallbackItems = {};
        
        // Determine currency and fallback options based on item name
        if (originalItemName.contains("Euro") || originalItemName.contains("EUR")) {
            fallbackItems = new String[]{"Euro50", "Euro20", "Euro10", "Euro5", "Euro2", "Euro1"};
        } else if (originalItemName.contains("Dollar") && !originalItemName.contains("A") && !originalItemName.contains("C")) {
            fallbackItems = new String[]{"Dollar100", "Dollar50", "Dollar20", "Dollar10", "Dollar5", "Dollar2", "Dollar1"};
        } else if (originalItemName.contains("Pound")) {
            fallbackItems = new String[]{"Pound50", "Pound20", "Pound10", "Pound5", "Pound2", "Pound1"};
        } else if (originalItemName.contains("DollarC") || originalItemName.contains("Loonie") || originalItemName.contains("Toonie")) {
            fallbackItems = new String[]{"DollarC50", "DollarC20", "DollarC10", "DollarC5", "Toonie", "Loonie"};
        } else if (originalItemName.contains("Lei") && !originalItemName.contains("MD")) {
            fallbackItems = new String[]{"Lei100", "Lei50", "Lei20", "Lei10", "Lei5", "Leu1"};
        } else if (originalItemName.contains("LeiMD") || originalItemName.contains("LeuMD")) {
            fallbackItems = new String[]{"LeiMD100", "LeiMD50", "LeiMD20", "LeiMD10", "LeiMD5", "LeuMD2", "LeuMD1"};
        } else if (originalItemName.contains("Franc")) {
            fallbackItems = new String[]{"Franc200", "Franc100", "Franc50", "Franc20", "Franc10", "Franc5", "Franc2", "Franc1"};
        } else if (originalItemName.contains("DollarA")) {
            fallbackItems = new String[]{"DollarA50", "DollarA20", "DollarA10", "DollarA5", "DollarA2", "DollarA1"};
        } else if (originalItemName.contains("Yen")) {
            fallbackItems = new String[]{"Yen5000", "Yen1000", "Yen500", "Yen100", "Yen50", "Yen10", "Yen5", "Yen1"};
        } else if (originalItemName.contains("CZkr")) {
            fallbackItems = new String[]{"CZkr2000", "CZkr1000", "CZkr500", "CZkr200", "CZkr100", "CZkr50", "CZkr20", "CZkr10", "CZkr5", "CZkr2", "CZkr1"};
        } else if (originalItemName.contains("MXPeso")) {
            fallbackItems = new String[]{"MXPeso500", "MXPeso200", "MXPeso100", "MXPeso50", "MXPeso20", "MXPeso10", "MXPeso5", "MXPeso2", "MXPeso1"};
        }
        
        // Try each fallback item
        boolean success = false;
        for (String fallbackItem : fallbackItems) {
            try {
                Field itemField = modItemsClass.getDeclaredField(fallbackItem);
                itemField.setAccessible(true);
                Object itemObject = itemField.get(null);
                if (itemObject instanceof Item item) {
                    ItemStack itemStack = new ItemStack(item, count);
                    player.getInventory().add(itemStack);
                    System.out.println("Enhanced ATM: Fallback - Added " + count + "x " + fallbackItem + " instead of " + originalItemName);
                    player.sendSystemMessage(createWarningMessage(DENOMINATION_FALLBACK, originalItemName, fallbackItem));
                    success = true;
                    break;
                }
            } catch (Exception ignored) {
                // Continue to next fallback
            }
        }
        
        if (!success) {
            System.err.println("Enhanced ATM: All fallback attempts failed for " + originalItemName);
            player.sendSystemMessage(createCriticalErrorMessage(PHYSICAL_MONEY_ERROR, originalItemName));
        }
    }

    /**
     * Get all denomination items from player's inventory
     */
    public static List<ItemStack> getPlayerDenominations(Player player) {
        List<ItemStack> denominations = new ArrayList<>();
        Set<Item> seenItems = new HashSet<>();
        
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && isMoneyItem(stack.getItem()) && !seenItems.contains(stack.getItem())) {
                denominations.add(stack.copy());
                seenItems.add(stack.getItem());
            }
        }
        
        return denominations;
    }
    
    /**
     * Get count of specific item in player's inventory
     */
    public static int getItemCountInInventory(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Check if an item is a money denomination
     */
    public static boolean isMoneyItem(Item item) {
        if (item == null) return false;
        
        // Enhanced detection - check by item name/ID as well as reflection
        String itemName = item.toString().toLowerCase();
        String registryName = item.getDescriptionId().toLowerCase();
        
        System.out.println("Enhanced ATM: Checking if item is money - Item: " + item.toString() + ", Registry: " + registryName);
        
        // Check if it's a BubusteinMoney item by name patterns
        boolean isBubusteinItem = itemName.contains("bubusteinmoneymod") || registryName.contains("bubusteinmoneymod");
        
        // Comprehensive money pattern matching for all denominations (bills and coins)
        boolean hasMoneyPattern = 
            // General patterns
            itemName.contains("coin") || itemName.contains("bill") || itemName.contains("note") ||
            registryName.contains("coin") || registryName.contains("bill") || registryName.contains("note") ||
            
            // Currency names
            itemName.contains("dollar") || itemName.contains("euro") || itemName.contains("pound") || itemName.contains("yen") ||
            registryName.contains("dollar") || registryName.contains("euro") || registryName.contains("pound") || registryName.contains("yen") ||
            
            // Coin patterns (cents, centimes, centavos, pence, etc.)
            itemName.contains("cent") || itemName.contains("centim") || itemName.contains("centav") || itemName.contains("pence") ||
            registryName.contains("cent") || registryName.contains("centim") || registryName.contains("centav") || registryName.contains("pence") ||
            
            // Specific currency abbreviations and codes
            itemName.contains("usd") || itemName.contains("eur") || itemName.contains("gbp") || itemName.contains("jpy") ||
            registryName.contains("usd") || registryName.contains("eur") || registryName.contains("gbp") || registryName.contains("jpy") ||
            
            // Additional coin/bill types (including Romanian)
            itemName.contains("bani") || itemName.contains("franc") || itemName.contains("peso") || itemName.contains("dinar") ||
            registryName.contains("bani") || registryName.contains("franc") || registryName.contains("peso") || registryName.contains("dinar") ||
            itemName.contains("lei") || itemName.contains("leu") || itemName.contains("douazeci") ||
            registryName.contains("lei") || registryName.contains("leu") || registryName.contains("douazeci") ||
            
            // Number patterns that typically appear in denominations
            itemName.matches(".*\\b(one|two|five|ten|twenty|fifty|hundred|thousand).*") ||
            registryName.matches(".*\\b(one|two|five|ten|twenty|fifty|hundred|thousand).*");
        
        if (isBubusteinItem && hasMoneyPattern) {
            System.out.println("Enhanced ATM: Item identified as money by name pattern");
            return true;
        }
        
        // Try reflection method as backup
        if (modItemsClass != null) {
            try {
                // Check if item is one of the BubusteinMoney denomination items
                Field[] fields = modItemsClass.getDeclaredFields();
                for (Field field : fields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                        java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
                        try {
                            Object fieldValue = field.get(null);
                            if (fieldValue instanceof Item && fieldValue.equals(item)) {
                                System.out.println("Enhanced ATM: Item identified as money by reflection");
                                return true;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                System.err.println("Enhanced ATM: Error checking if item is money: " + e.getMessage());
            }
        }
        
        System.out.println("Enhanced ATM: Item NOT identified as money");
        return false;
    }
    
    /**
     * Public method to get item value for use from client side
     */
    public static double getValueFromItemStack(ItemStack stack, String currency) {
        return getItemValue(stack, currency);
    }
    
    /**
     * Get the monetary value of a denomination item
     */
    public static double getDenominationValue(ItemStack itemStack) {
        if (itemStack.isEmpty()) return 0.0;
        
        System.out.println("Enhanced ATM: Getting denomination value for: " + itemStack.getHoverName().getString());
        
        // Use the existing getItemValue function which already has all the logic implemented
        // Try with different currencies to find a match
        String[] currencies = {"EUR", "USD", "GBP", "CAD", "RON", "MDL", "CHF", "AUD", "JPY", "CZK", "MXN", "NOK", "DKK", "SEK", "HUF", "PLN", "RSD", "ISK", "CNY", "INR", "KRW", "BRL"};
        
        for (String currency : currencies) {
            double value = getItemValue(itemStack, currency);
            if (value > 0) {
                System.out.println("Enhanced ATM: Found value " + value + " " + currency + " for item");
                return value;
            }
        }
        
        // If getItemValue doesn't work, try parsing from item name directly
        String itemName = itemStack.getItem().toString().toLowerCase();
        String displayName = itemStack.getHoverName().getString().toLowerCase();
        
        System.out.println("Enhanced ATM: Trying to parse value from item name: " + itemName + ", display: " + displayName);
        
        // Try to extract value from display name or item name
        try {
            // Look for patterns like "1 dollar", "5 euro", etc.
            String[] patterns = {"dollar", "euro", "pound", "yen", "coin", "bill"};
            for (String pattern : patterns) {
                if (displayName.contains(pattern) || itemName.contains(pattern)) {
                    // Try to extract number
                    String[] words = displayName.split("\\s+");
                    for (String word : words) {
                        try {
                            double value = Double.parseDouble(word);
                            if (value > 0) {
                                System.out.println("Enhanced ATM: Parsed value from name: " + value);
                                return value;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    
                    // Try parsing from item registry name
                    String[] parts = itemName.split("_");
                    for (String part : parts) {
                        try {
                            double value = Double.parseDouble(part);
                            if (value > 0) {
                                System.out.println("Enhanced ATM: Parsed value from registry name: " + value);
                                return value;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error parsing denomination value: " + e.getMessage());
        }
        
        System.out.println("Enhanced ATM: Could not determine value for item");
        return 0.0;
    }
    
    /**
     * Execute a denomination exchange by giving physical denominations to the player
     */
    public static void executeDenominationExchange(ServerPlayer player, double totalValue, String targetCurrency) {
        try {
            System.out.println("Enhanced ATM: Starting denomination exchange - " + totalValue + " " + targetCurrency);
            
            // Generate physical denominations and give them to the player
            createPhysicalMoneyForPlayer(player, totalValue, targetCurrency);
            
            System.out.println("Enhanced ATM: Denomination exchange completed - " + totalValue + " " + targetCurrency + " added to inventory");
            player.sendSystemMessage(Component.literal(String.format("§a✓ Cambio exitoso: %.2f %s en denominaciones físicas añadido al inventario", totalValue, targetCurrency)));
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error during denomination exchange: " + e.getMessage());
            e.printStackTrace();
            player.sendSystemMessage(Component.literal("§c✗ Error al generar las denominaciones físicas"));
        }
    }
    
    /**
     * Check if BubusteinMoney mod is loaded and available
     */
    public static boolean isBubusteinMoneyAvailable() {
        return cardItemClass != null && modItemsClass != null;
    }
}