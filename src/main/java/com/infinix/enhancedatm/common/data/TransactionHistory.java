package com.infinix.enhancedatm.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Manages transaction history for Enhanced ATM
 */
public class TransactionHistory {
    
    private static final String NBT_TRANSACTIONS = "atmTransactions";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Transaction record
     */
    public static class Transaction {
        public final String type;        // "deposit", "withdraw", "exchange"
        public final double amount;
        public final String currency;
        public final String timestamp;
        public final String details;     // Additional information
        
        public Transaction(String type, double amount, String currency, String details) {
            this.type = type;
            this.amount = amount;
            this.currency = currency;
            this.timestamp = DATE_FORMAT.format(new Date());
            this.details = details != null ? details : "";
        }
        
        public Transaction(CompoundTag tag) {
            this.type = tag.getString("type");
            this.amount = tag.getDouble("amount");
            this.currency = tag.getString("currency");
            this.timestamp = tag.getString("timestamp");
            this.details = tag.getString("details");
        }
        
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type);
            tag.putDouble("amount", amount);
            tag.putString("currency", currency);
            tag.putString("timestamp", timestamp);
            tag.putString("details", details);
            return tag;
        }
    }
    
    /**
     * Add a transaction to player's history
     */
    public static void addTransaction(Player player, String type, double amount, String currency, String details) {
        CompoundTag playerData = player.getPersistentData();
        
        // Get existing transactions
        List<Transaction> transactions = getTransactions(player);
        
        // Add new transaction
        transactions.add(new Transaction(type, amount, currency, details));
        
        // Limit history size to 10 entries
        int maxEntries = 10;
        while (transactions.size() > maxEntries) {
            transactions.remove(0); // Remove oldest
        }
        
        // Save back to NBT
        ListTag transactionList = new ListTag();
        for (Transaction transaction : transactions) {
            transactionList.add(transaction.toNBT());
        }
        
        playerData.put(NBT_TRANSACTIONS, transactionList);
    }
    
    /**
     * Get player's transaction history
     */
    public static List<Transaction> getTransactions(Player player) {
        CompoundTag playerData = player.getPersistentData();
        List<Transaction> transactions = new ArrayList<>();
        
        if (playerData.contains(NBT_TRANSACTIONS)) {
            ListTag transactionList = playerData.getList(NBT_TRANSACTIONS, Tag.TAG_COMPOUND);
            
            for (int i = 0; i < transactionList.size(); i++) {
                CompoundTag transactionTag = transactionList.getCompound(i);
                transactions.add(new Transaction(transactionTag));
            }
        }
        
        return transactions;
    }
    
    /**
     * Clear player's transaction history
     */
    public static void clearHistory(Player player) {
        CompoundTag playerData = player.getPersistentData();
        playerData.remove(NBT_TRANSACTIONS);
    }
    
    /**
     * Get formatted transaction string for display
     */
    public static String formatTransaction(Transaction transaction) {
        String symbol = transaction.type.equals("withdraw") ? "-" : "+";
        return String.format("[%s] %s %s%.2f %s %s", 
            transaction.timestamp.substring(11), // Show only time
            transaction.type.toUpperCase(),
            symbol,
            transaction.amount,
            transaction.currency,
            transaction.details.isEmpty() ? "" : "(" + transaction.details + ")"
        ).trim();
    }
}