package com.infinix.enhancedatm.common.events;

import com.infinix.enhancedatm.EnhancedATMMod;
import com.infinix.enhancedatm.common.container.EnhancedATMContainer;
import com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration;
import com.infinix.enhancedatm.common.utils.BankPermissionChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

/**
 * Handles ATM block interactions and integrates with BubusteinMoney mod
 */
public class ATMInteractionHandler {
    
    private static final String BUBUSTEIN_ATM_CLASS = "tk.bubustein.money.block.custom.ATM";
    private static final String BUBUSTEIN_KEY_ITEM_CLASS = "tk.bubustein.money.item.KeyItem";
    
    @SubscribeEvent
    public void onPlayerInteractWithBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        InteractionHand hand = event.getHand();
        ItemStack heldItem = player.getItemInHand(hand);
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        
        // Check if the block is a BubusteinMoney ATM
        if (isATMBlock(block)) {
            System.out.println("Enhanced ATM: ATM block detected, checking for key...");
            
            // First check if the Bank Permission System is enabled and if permission exists
            // Only check on server side to avoid duplicate messages
            if (BankPermissionChecker.isSystemEnabled()) {
                boolean hasPermission = BankPermissionChecker.hasPermissionNearbySphere(level, pos);
                
                if (!hasPermission) {
                    // No Bank Permission Block nearby - deny access
                    if (!level.isClientSide) {
                        player.sendSystemMessage(
                            Component.translatable("message.enhancedatm.bank_permission_required",
                                BankPermissionChecker.getPermissionRange())
                                .withStyle(net.minecraft.ChatFormatting.RED)
                        );
                    }
                    
                    event.setResult(Event.Result.DENY);
                    event.setCanceled(true);
                    return;
                }
            }
            
            // Check if player has a key item in hand (try both methods)
            boolean hasKey = isKeyItem(heldItem) || isKeyItemByName(heldItem);
            if (hasKey) {
                System.out.println("Enhanced ATM: Key detected in hand: " + heldItem.getItem().getClass().getName());
                if (!level.isClientSide) {
                    // Damage the key (same as original mod)
                    heldItem.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                    
                    // Open our enhanced ATM interface
                    openEnhancedATMInterface((ServerPlayer) player, pos);
                }
                
                // Cancel the original interaction to prevent conflicts
                event.setResult(Event.Result.ALLOW);
                event.setCanceled(true);
            } else {
                // Show message using our translation system
                if (!level.isClientSide) {
                    // First check if player has a key in any inventory slot
                    boolean hasKeyInInventory = hasKeyInInventory(player);
                    if (hasKeyInInventory) {
                        player.sendSystemMessage(Component.translatable("message.enhancedatm.use_key_in_hand")
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.enhancedatm.missing_key")
                            .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
                    }
                }
                event.setResult(Event.Result.DENY);
                event.setCanceled(true);
            }
        }
    }
    
    /**
     * Check if the block is a BubusteinMoney ATM using reflection
     */
    private boolean isATMBlock(Block block) {
        try {
            Class<?> atmClass = Class.forName(BUBUSTEIN_ATM_CLASS);
            boolean isATM = atmClass.isInstance(block);
            if (isATM) {
                System.out.println("Enhanced ATM: ATM block confirmed: " + block.getClass().getName());
            }
            return isATM;
        } catch (ClassNotFoundException e) {
            // BubusteinMoney mod not loaded - check by name as fallback
            String blockName = block.getClass().getName();
            boolean isATMByName = blockName.contains("ATM") && blockName.contains("bubustein");
            if (isATMByName) {
                System.out.println("Enhanced ATM: ATM detected by name: " + blockName);
            }
            return isATMByName;
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error checking ATM block: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if the item is a BubusteinMoney key using reflection
     */
    private boolean isKeyItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        
        try {
            Class<?> keyItemClass = Class.forName(BUBUSTEIN_KEY_ITEM_CLASS);
            boolean isKey = keyItemClass.isInstance(itemStack.getItem());
            System.out.println("Enhanced ATM: Checking item " + itemStack.getItem().getClass().getName() + " - Is key: " + isKey);
            return isKey;
        } catch (ClassNotFoundException e) {
            System.err.println("Enhanced ATM: BubusteinMoney KeyItem class not found - mod not loaded?");
            return false;
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error checking key item: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if player has a key item anywhere in their inventory
     */
    private boolean hasKeyInInventory(Player player) {
        try {
            Class<?> keyItemClass = Class.forName(BUBUSTEIN_KEY_ITEM_CLASS);
            for (ItemStack stack : player.getInventory().items) {
                if (keyItemClass.isInstance(stack.getItem())) {
                    return true;
                }
            }
            return false;
        } catch (ClassNotFoundException e) {
            // Fallback: check by class name
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty()) {
                    String itemName = stack.getItem().getClass().getName();
                    if (itemName.contains("KeyItem") && itemName.contains("bubustein")) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    /**
     * Alternative key detection using class name (fallback method)
     */
    private boolean isKeyItemByName(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        String itemName = itemStack.getItem().getClass().getName();
        boolean isKey = itemName.contains("KeyItem") && itemName.contains("bubustein");
        System.out.println("Enhanced ATM: Checking item by name " + itemName + " - Is key: " + isKey);
        return isKey;
    }
    
    /**
     * Open the enhanced ATM interface
     */
    private void openEnhancedATMInterface(ServerPlayer player, BlockPos pos) {
        MenuProvider containerProvider = new SimpleMenuProvider(
            (windowId, playerInventory, playerEntity) -> new EnhancedATMContainer(
                windowId, 
                playerInventory, 
                ContainerLevelAccess.create(player.level(), pos)
            ),
            Component.translatable("container.enhancedatm.enhanced_atm")
        );
        
        NetworkHooks.openScreen(player, containerProvider, pos);
    }
}