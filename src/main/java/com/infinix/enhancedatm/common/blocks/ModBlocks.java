package com.infinix.enhancedatm.common.blocks;

import com.infinix.enhancedatm.EnhancedATMMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for all custom blocks in Enhanced ATM mod
 */
public class ModBlocks {
    
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(ForgeRegistries.BLOCKS, EnhancedATMMod.MODID);
    
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, EnhancedATMMod.MODID);
    
    // Bank Permission Block - Special admin-only block that enables ATM functionality
    public static final RegistryObject<Block> BANK_PERMISSION_BLOCK = BLOCKS.register("bank_permission_block",
        BankPermissionBlock::new);
    
    // Block Item for Bank Permission Block
    public static final RegistryObject<Item> BANK_PERMISSION_BLOCK_ITEM = ITEMS.register("bank_permission_block",
        () -> new BlockItem(BANK_PERMISSION_BLOCK.get(), new Item.Properties()));
}
