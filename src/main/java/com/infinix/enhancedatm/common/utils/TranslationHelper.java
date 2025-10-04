package com.infinix.enhancedatm.common.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * Utility class for handling translatable messages in Enhanced ATM
 */
public class TranslationHelper {
    
    public static final String MOD_ID = "enhancedatm";
    
    // Message key constants
    public static final String INSUFFICIENT_FUNDS_AVAILABLE = "message." + MOD_ID + ".insufficient_funds_available";
    public static final String DEPOSIT_SUCCESS_SIMPLE = "message." + MOD_ID + ".deposit_success_simple";
    public static final String DEPOSIT_SUCCESS_CONVERSION = "message." + MOD_ID + ".deposit_success_conversion";
    public static final String DEPOSIT_SUCCESS_CONVERSION_CHANGE = "message." + MOD_ID + ".deposit_success_conversion_change";
    public static final String DEPOSIT_SUCCESS_CHANGE = "message." + MOD_ID + ".deposit_success_change";
    public static final String CARD_CURRENCY_MISMATCH = "message." + MOD_ID + ".card_currency_mismatch";
    public static final String INSUFFICIENT_BALANCE_AVAILABLE = "message." + MOD_ID + ".insufficient_balance_available";
    public static final String WITHDRAW_SUCCESS_SIMPLE = "message." + MOD_ID + ".withdraw_success_simple";
    public static final String WITHDRAW_SUCCESS_CONVERSION = "message." + MOD_ID + ".withdraw_success_conversion";
    public static final String CARD_CURRENCY_SELECTION_MISMATCH = "message." + MOD_ID + ".card_currency_selection_mismatch";
    public static final String CURRENCY_NOT_AVAILABLE = "message." + MOD_ID + ".currency_not_available";
    public static final String DENOMINATION_FALLBACK = "message." + MOD_ID + ".denomination_fallback";
    public static final String PHYSICAL_MONEY_ERROR = "message." + MOD_ID + ".physical_money_error";
    public static final String BUBUSTEIN_MONEY_REQUIRED = "message." + MOD_ID + ".bubustein_money_required";
    public static final String CARD_REQUIRED = "message." + MOD_ID + ".card_required";
    public static final String MISSING_KEY = "message." + MOD_ID + ".missing_key";
    public static final String USE_KEY_IN_HAND = "message." + MOD_ID + ".use_key_in_hand";
    
    /**
     * Create a translatable component with green color formatting
     */
    public static Component createSuccessMessage(String translationKey, Object... args) {
        return Component.translatable(translationKey, args).withStyle(ChatFormatting.GREEN);
    }
    
    /**
     * Create a translatable component with red color formatting
     */
    public static Component createErrorMessage(String translationKey, Object... args) {
        return Component.translatable(translationKey, args).withStyle(ChatFormatting.RED);
    }
    
    /**
     * Create a translatable component with gold color formatting
     */
    public static Component createWarningMessage(String translationKey, Object... args) {
        return Component.translatable(translationKey, args).withStyle(ChatFormatting.GOLD);
    }
    
    /**
     * Create a translatable component with red color and bold formatting
     */
    public static Component createCriticalErrorMessage(String translationKey, Object... args) {
        return Component.translatable(translationKey, args).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }
    
    /**
     * Create a basic translatable component
     */
    public static Component createTranslatableMessage(String translationKey, Object... args) {
        return Component.translatable(translationKey, args);
    }
}