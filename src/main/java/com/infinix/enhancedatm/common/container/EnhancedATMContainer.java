package com.infinix.enhancedatm.common.container;

import com.infinix.enhancedatm.EnhancedATMMod;
import com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/**
 * Container for the Enhanced ATM interface
 * Handles the server-side logic for the ATM GUI
 */
public class EnhancedATMContainer extends AbstractContainerMenu {
    
    private final ContainerLevelAccess levelAccess;
    private final BlockPos atmPos;
    private final Container cardSlot;
    private final Container denominationSlots;
    
    // Client-side constructor
    public EnhancedATMContainer(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory, ContainerLevelAccess.NULL);
    }
    
    // Server-side constructor  
    public EnhancedATMContainer(int windowId, Inventory playerInventory, ContainerLevelAccess levelAccess) {
        super(EnhancedATMMod.ENHANCED_ATM_CONTAINER.get(), windowId);
        
        this.levelAccess = levelAccess;
        this.atmPos = levelAccess != ContainerLevelAccess.NULL ? 
            levelAccess.evaluate((level, pos) -> pos).orElse(BlockPos.ZERO) : BlockPos.ZERO;
        
        // Create container for the credit card slot
        this.cardSlot = new SimpleContainer(1);
        
        // Create container for denomination exchange slots (3x3 = 9 slots)
        this.denominationSlots = new SimpleContainer(9);
        
        // Add card slot with configurable position
        int cardX = 124; // Default position
        int cardY = 115; // Default position
        try {
            com.infinix.enhancedatm.common.config.ATMGuiConfig config = com.infinix.enhancedatm.common.config.ATMGuiConfig.getInstance();
            cardX = config.cardSlot.offsetX;
            cardY = config.cardSlot.offsetY;
            System.out.println("Enhanced ATM: Using configured card slot position: (" + cardX + ", " + cardY + ")");
        } catch (Exception e) {
            System.out.println("Enhanced ATM: Failed to load config, using default card slot position: (" + cardX + ", " + cardY + ")");
        }
        this.addSlot(new CreditCardSlot(cardSlot, 0, cardX, cardY));
        
        // Add denomination exchange slots (3x3 grid)
        addDenominationSlots();
        
        // Add player inventory slots
        addPlayerInventorySlots(playerInventory);
    }
    
    /**
     * Add denomination exchange slots to the container (3x3 grid)
     */
    private void addDenominationSlots() {
        // Try to get configuration, fallback to default if not available
        int startX = 30; // Default position
        int startY = 97; // Default position
        
        try {
            com.infinix.enhancedatm.common.config.ATMGuiConfig config = com.infinix.enhancedatm.common.config.ATMGuiConfig.getInstance();
            startX = config.denominationGrid.offsetX;
            startY = config.denominationGrid.offsetY;
            System.out.println("Enhanced ATM: Using configured denomination grid position: (" + startX + ", " + startY + ")");
        } catch (Exception e) {
            System.out.println("Enhanced ATM: Failed to load config, using default denomination grid position: (" + startX + ", " + startY + ")");
        }
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int x = startX + (col * 18);
                int y = startY + (row * 18);
                this.addSlot(new DenominationSlot(denominationSlots, slotIndex, x, y));
            }
        }
    }
    
    /**
     * Add player inventory and hotbar slots to the container
     */
    private void addPlayerInventorySlots(Inventory playerInventory) {
        // Try to get configuration, fallback to default if not available
        int invStartX = 8;   // Default position
        int invStartY = 166; // Default position
        
        try {
            com.infinix.enhancedatm.common.config.ATMGuiConfig config = com.infinix.enhancedatm.common.config.ATMGuiConfig.getInstance();
            invStartX = config.playerInventory.offsetX;
            invStartY = config.playerInventory.offsetY;
            System.out.println("Enhanced ATM: Using configured player inventory position: (" + invStartX + ", " + invStartY + ")");
        } catch (Exception e) {
            System.out.println("Enhanced ATM: Failed to load config, using default player inventory position: (" + invStartX + ", " + invStartY + ")");
        }
        
        // Player inventory (3x9 = 27 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invStartX + col * 18, invStartY + row * 18));
            }
        }
        
        // Player hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, invStartX + col * 18, invStartY + 58)); // 58px below inventory
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            if (slotIndex == 0) {
                // Moving from card slot to player inventory
                if (!this.moveItemStackTo(slotStack, 10, this.slots.size(), true)) { // Start after denomination slots (0=card, 1-9=denomination)
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= 1 && slotIndex <= 9) {
                // Moving from denomination slots to player inventory
                if (!this.moveItemStackTo(slotStack, 10, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory
                if (BubusteinMoneyIntegration.isCardItem(slotStack)) {
                    // Try to move card to card slot
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (BubusteinMoneyIntegration.isMoneyItem(slotStack.getItem())) {
                    // Try to move money to denomination slots
                    if (!this.moveItemStackTo(slotStack, 1, 10, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemstack;
    }
    
    @Override
    public boolean stillValid(Player player) {
        // Check if player is still near the ATM and ATM block is still present
        return levelAccess.evaluate((level, pos) -> {
            if (level.getBlockState(pos).getBlock().getClass().getSimpleName().equals("ATM")) {
                return player.distanceToSqr((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) <= 64.0D;
            }
            return false;
        }, true);
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        // Return items to player when container is closed
        if (!player.level().isClientSide) {
            // Return card to player
            ItemStack cardStack = cardSlot.getItem(0);
            if (!cardStack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(cardStack);
                cardSlot.setItem(0, ItemStack.EMPTY);
            }
            
            // Return denomination items to player
            for (int i = 0; i < denominationSlots.getContainerSize(); i++) {
                ItemStack denominationStack = denominationSlots.getItem(i);
                if (!denominationStack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(denominationStack);
                    denominationSlots.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }
    
    /**
     * Get the position of the ATM block
     */
    public BlockPos getATMPosition() {
        return atmPos;
    }
    
    /**
     * Get the credit card currently in the card slot
     */
    public ItemStack getCardInSlot() {
        return cardSlot.getItem(0);
    }
    
    /**
     * Get all denomination items from the denomination slots
     */
    public java.util.List<ItemStack> getDenominationItems() {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        System.out.println("DEBUG: getDenominationItems - Container size: " + denominationSlots.getContainerSize());
        
        for (int i = 0; i < denominationSlots.getContainerSize(); i++) {
            ItemStack stack = denominationSlots.getItem(i);
            System.out.println("DEBUG: Slot " + i + " contains: " + (stack.isEmpty() ? "EMPTY" : stack.getHoverName().getString() + " x" + stack.getCount()));
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        
        System.out.println("DEBUG: Returning " + items.size() + " denomination items");
        return items;
    }
    
    /**
     * Clear all denomination items from the grid
     */
    public void clearDenominationGrid() {
        for (int i = 0; i < denominationSlots.getContainerSize(); i++) {
            denominationSlots.setItem(i, ItemStack.EMPTY);
        }
        System.out.println("DEBUG: Cleared all denomination slots");
    }
    
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        
        // If the denomination slots container changed, notify client to update amount field
        if (container == this.denominationSlots) {
            // Trigger client-side update via the screen
            this.broadcastChanges();
        }
    }
    
    /**
     * Custom slot that only accepts credit cards from BubusteinMoney mod
     */
    public static class CreditCardSlot extends Slot {
        public CreditCardSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        
        @Override
        public boolean mayPlace(ItemStack stack) {
            // Only allow BubusteinMoney credit cards
            return BubusteinMoneyIntegration.isCardItem(stack);
        }
        
        @Override
        public int getMaxStackSize() {
            return 1; // Only one card at a time
        }
    }
    
    /**
     * Custom slot that only accepts money items (coins and bills) from BubusteinMoney mod
     */
    public static class DenominationSlot extends Slot {
        public DenominationSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            System.out.println("DEBUG: Created DenominationSlot at position (" + x + ", " + y + ") with index " + slot);
        }
        
        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty()) {
                System.out.println("DEBUG: mayPlace called with empty stack");
                return false;
            }
            
            boolean isMoneyItem = BubusteinMoneyIntegration.isMoneyItem(stack.getItem());
            System.out.println("DEBUG: mayPlace called with item: " + stack.getItem() + ", isMoneyItem: " + isMoneyItem);
            
            // Enhanced currency item detection for all bills and coins
            String itemName = stack.getItem().toString().toLowerCase();
            String registryName = stack.getItem().getDescriptionId().toLowerCase();
            
            boolean isCurrencyItem = 
                // BubusteinMoney mod items
                itemName.contains("bubusteinmoneymod") || registryName.contains("bubusteinmoneymod") ||
                
                // General money patterns
                itemName.contains("coin") || itemName.contains("bill") || itemName.contains("note") ||
                registryName.contains("coin") || registryName.contains("bill") || registryName.contains("note") ||
                
                // Currency names and codes
                itemName.contains("euro") || itemName.contains("dollar") || itemName.contains("pound") || itemName.contains("yen") ||
                itemName.contains("usd") || itemName.contains("eur") || itemName.contains("gbp") || itemName.contains("jpy") ||
                registryName.contains("euro") || registryName.contains("dollar") || registryName.contains("pound") || registryName.contains("yen") ||
                registryName.contains("usd") || registryName.contains("eur") || registryName.contains("gbp") || registryName.contains("jpy") ||
                
                // Coin-specific patterns (cents, centimes, centavos, pence, etc.)
                itemName.contains("cent") || itemName.contains("centim") || itemName.contains("centav") || itemName.contains("pence") ||
                registryName.contains("cent") || registryName.contains("centim") || registryName.contains("centav") || registryName.contains("pence") ||
                
            // Additional patterns (including Romanian denominations)
            itemName.contains("bani") || itemName.contains("franc") || itemName.contains("peso") || itemName.contains("dinar") ||
            registryName.contains("bani") || registryName.contains("franc") || registryName.contains("peso") || registryName.contains("dinar") ||
            
            // Romanian currency specific patterns
            itemName.contains("lei") || itemName.contains("leu") || itemName.contains("douazeci") ||
            registryName.contains("lei") || registryName.contains("leu") || registryName.contains("douazeci");            System.out.println("DEBUG: isCurrencyItem: " + isCurrencyItem + " for item: " + itemName);
            
            // First check if it's a valid money item
            if (!isMoneyItem && !isCurrencyItem) {
                return false;
            }
            
            // Check if grid already has items and enforce single denomination rule
            return checkSingleDenominationRule(stack);
        }
        
        /**
         * Check if the stack can be placed according to single denomination rule
         */
        private boolean checkSingleDenominationRule(ItemStack stackToPlace) {
            // Get the currency of the item to place
            String currencyToPlace = extractCurrencyFromItemName(stackToPlace.getItem().toString());
            
            // Check all denomination slots for existing items
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack existingStack = container.getItem(i);
                if (!existingStack.isEmpty()) {
                    String existingCurrency = extractCurrencyFromItemName(existingStack.getItem().toString());
                    
                    // If currencies don't match, deny placement
                    if (!currencyToPlace.equals(existingCurrency)) {
                        System.out.println("DEBUG: Blocking placement - existing currency: " + existingCurrency + 
                                         ", new currency: " + currencyToPlace);
                        return false;
                    }
                }
            }
            
            return true; // Allow if grid is empty or same currency
        }
        
        /**
         * Extract currency from item name (similar to screen method)
         */
        private String extractCurrencyFromItemName(String itemName) {
            String lowerName = itemName.toLowerCase();
            
            if (lowerName.contains("euro") || lowerName.contains("eur")) {
                return "EUR";
            } else if (lowerName.contains("dollar") || lowerName.contains("usd")) {
                return "USD";
            } else if (lowerName.contains("coin") || lowerName.contains("bill") || lowerName.contains("note")) {
                // Default currency for generic money items
                return "USD";
            }
            
            return "UNKNOWN";
        }
        
        @Override
        public void set(ItemStack stack) {
            System.out.println("DEBUG: DenominationSlot.set called with: " + stack);
            super.set(stack);
            
            // Mark container as changed so client will be notified
            container.setChanged();
        }
    }
}