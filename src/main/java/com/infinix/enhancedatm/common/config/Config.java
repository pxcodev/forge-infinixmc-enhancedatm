package com.infinix.enhancedatm.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for Enhanced ATM mod
 */
public class Config {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Bank Permission System configuration
    public static final ForgeConfigSpec.BooleanValue ENABLE_BANK_PERMISSION_SYSTEM;
    public static final ForgeConfigSpec.IntValue BANK_PERMISSION_RANGE;
    
    static {
        BUILDER.push("Bank Permission System");
        BUILDER.comment("Configuration for the Bank Permission System that controls ATM access");
        
        ENABLE_BANK_PERMISSION_SYSTEM = BUILDER
            .comment("Enable the Bank Permission System (requires Bank Permission Block near ATMs)")
            .define("enable_bank_permission_system", false);
        
        BANK_PERMISSION_RANGE = BUILDER
            .comment("Range in blocks to search for Bank Permission Block around ATMs")
            .defineInRange("bank_permission_range", 10, 1, 64);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}