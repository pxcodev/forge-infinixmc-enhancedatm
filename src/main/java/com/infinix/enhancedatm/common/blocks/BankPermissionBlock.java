package com.infinix.enhancedatm.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bank Permission Block - Special block that enables ATM functionality within range
 * Can only be obtained via admin commands
 * ATMs will only work if this block is within configured range
 * 
 * @author InfinixMC
 * @version 1.0.0
 */
public class BankPermissionBlock extends Block {
    
    // Block shape - slightly smaller than full block for visual distinction
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    
    public BankPermissionBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)
            .strength(50.0F, 1200.0F) // Very strong, hard to break
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel((state) -> 10) // Emits light level 10
        );
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    /**
     * Get the display name for this block
     */
    public static Component getDisplayName() {
        return Component.translatable("block.enhancedatm.bank_permission_block");
    }
}
