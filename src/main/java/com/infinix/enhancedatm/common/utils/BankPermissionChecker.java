package com.infinix.enhancedatm.common.utils;

import com.infinix.enhancedatm.common.blocks.ModBlocks;
import com.infinix.enhancedatm.common.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Utility class to check for Bank Permission Block near ATMs
 * Provides range-based verification for ATM access control
 * 
 * @author InfinixMC
 * @version 1.0.0
 */
public class BankPermissionChecker {
    
    /**
     * Check if there is a Bank Permission Block within range of the given position
     * 
     * @param level The world/level to check in
     * @param atmPos The position of the ATM
     * @return true if a Bank Permission Block is found within range, false otherwise
     */
    public static boolean hasPermissionNearby(Level level, BlockPos atmPos) {
        // If the system is disabled, always allow access
        if (!Config.ENABLE_BANK_PERMISSION_SYSTEM.get()) {
            return true;
        }
        
        int range = Config.BANK_PERMISSION_RANGE.get();
        Block permissionBlock = ModBlocks.BANK_PERMISSION_BLOCK.get();
        
        // Check all blocks in a cube around the ATM
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = atmPos.offset(x, y, z);
                    
                    // Check if this position contains a Bank Permission Block
                    if (level.getBlockState(checkPos).getBlock() == permissionBlock) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if there is a Bank Permission Block within range using spherical distance
     * This is more efficient than cubic check for large ranges
     * 
     * @param level The world/level to check in
     * @param atmPos The position of the ATM
     * @return true if a Bank Permission Block is found within range, false otherwise
     */
    public static boolean hasPermissionNearbySphere(Level level, BlockPos atmPos) {
        // If the system is disabled, always allow access
        if (!Config.ENABLE_BANK_PERMISSION_SYSTEM.get()) {
            return true;
        }
        
        int range = Config.BANK_PERMISSION_RANGE.get();
        int rangeSq = range * range; // Square of the range for distance comparison
        Block permissionBlock = ModBlocks.BANK_PERMISSION_BLOCK.get();
        
        // Check all blocks in a cube around the ATM, but only accept those within spherical distance
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    // Calculate squared distance (avoids sqrt calculation)
                    int distSq = x * x + y * y + z * z;
                    
                    // Only check blocks within spherical range
                    if (distSq <= rangeSq) {
                        BlockPos checkPos = atmPos.offset(x, y, z);
                        
                        // Check if this position contains a Bank Permission Block
                        if (level.getBlockState(checkPos).getBlock() == permissionBlock) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get the nearest Bank Permission Block position within range
     * 
     * @param level The world/level to check in
     * @param atmPos The position of the ATM
     * @return The position of the nearest Bank Permission Block, or null if none found
     */
    public static BlockPos getNearestPermissionBlock(Level level, BlockPos atmPos) {
        if (!Config.ENABLE_BANK_PERMISSION_SYSTEM.get()) {
            return null;
        }
        
        int range = Config.BANK_PERMISSION_RANGE.get();
        Block permissionBlock = ModBlocks.BANK_PERMISSION_BLOCK.get();
        
        BlockPos nearestPos = null;
        double nearestDistSq = Double.MAX_VALUE;
        
        // Check all blocks in a cube around the ATM
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = atmPos.offset(x, y, z);
                    
                    // Check if this position contains a Bank Permission Block
                    if (level.getBlockState(checkPos).getBlock() == permissionBlock) {
                        double distSq = atmPos.distSqr(checkPos);
                        
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearestPos = checkPos;
                        }
                    }
                }
            }
        }
        
        return nearestPos;
    }
    
    /**
     * Check if the Bank Permission System is enabled
     * 
     * @return true if the system is enabled, false otherwise
     */
    public static boolean isSystemEnabled() {
        return Config.ENABLE_BANK_PERMISSION_SYSTEM.get();
    }
    
    /**
     * Get the configured range for Bank Permission Block checking
     * 
     * @return The range in blocks
     */
    public static int getPermissionRange() {
        return Config.BANK_PERMISSION_RANGE.get();
    }
}
