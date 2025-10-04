package com.infinix.enhancedatm.client.screen;

import com.infinix.enhancedatm.EnhancedATMMod;
import com.infinix.enhancedatm.common.container.EnhancedATMContainer;
import com.infinix.enhancedatm.common.network.NetworkHandler;
import com.infinix.enhancedatm.common.network.packets.CurrencyExchangePacket;
import com.infinix.enhancedatm.common.network.packets.DepositPacket;
import com.infinix.enhancedatm.common.network.packets.DetectGridMoneyPacket;
import com.infinix.enhancedatm.common.network.packets.WithdrawPacket;
import com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration;
import com.infinix.enhancedatm.common.config.ATMGuiConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced ATM Screen - Modern ATM interface with currency exchange
 */
public class EnhancedATMScreen extends AbstractContainerScreen<EnhancedATMContainer> {
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(EnhancedATMMod.MODID, "textures/gui/enhanced_atm.png");
    
    // Reflection cache - Para evitar congelamientos en reload
    private static java.lang.reflect.Field cachedXField = null;
    private static java.lang.reflect.Field cachedYField = null;
    private static boolean reflectionInitialized = false;
    
    // Throttling para evitar spam de actualizaciones
    private static long lastUpdateTime = 0;
    private static final long UPDATE_COOLDOWN = 1000; // 1 segundo entre actualizaciones
    private static int updateCount = 0;
    private static final int MAX_UPDATES_PER_SESSION = 10; // Máximo 10 actualizaciones por sesión
    
    // Estado para evitar actualizaciones innecesarias
    private static String lastConfigHash = "";
    private static boolean isUpdating = false; // Prevenir actualizaciones simultáneas
    
    // GUI Components
    private EditBox amountField;
    private Button depositButton, withdrawButton;
    private Button sourceCurrencyButton, targetCurrencyButton;
    private Button detectMoneyButton;
    private List<String> availableCurrencies;
    private int selectedSourceCurrencyIndex = 0;
    private int selectedTargetCurrencyIndex = 0; // Default to EUR
    
    // Denomination Exchange Grid
    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;
    private static final int SLOT_SIZE = 18; // Minecraft slot size
    private DenominationSlot[][] denominationGrid;
    private List<ItemStack> playerDenominations;
    private List<ItemStack> selectedDenominations;
    
    // Display information
    private String cardBalance = "0.00";
    private String cardCurrency = "EUR";
    private double exchangeRate = 1.0;
    
    // Update optimization
    private int updateCounter = 0;
    
    // Grid currency tracking
    private String detectedGridCurrency = "";
    private boolean isSourceCurrencyLocked = false;
    
    // Grid total conversion tracking
    private double detectedGridTotal = 0.0;
    private String detectedGridTotalCurrency = "";
    private double gridConversionTotal = 0.0;
    
    // Configurable inventory positions
    private int inventoryLabelX = 8;
    private int inventoryLabelY = 166;
    
    // Card currency tracking
    private String lastKnownCardCurrency = "";
    private boolean isCardCurrencyLocked = false;
    private long lastCardChangeTime = 0;
    private long lastGridChangeTime = 0;
    private boolean hasCardWithBalance = false;
    
    public EnhancedATMScreen(EnhancedATMContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        
        this.imageWidth = 256;  // Ajustado para textura 256x259
        this.imageHeight = 259; // Ajustado para textura 256x259
        
        // Initialize available currencies - ALL SUPPORTED CURRENCIES
        this.availableCurrencies = new ArrayList<>();
        this.availableCurrencies.addAll(List.of(
            "EUR",  // Euro
            "USD",  // US Dollar
            "GBP",  // British Pound
            "CAD",  // Canadian Dollar
            "RON",  // Romanian Leu
            "MDL",  // Moldovan Leu
            "CHF",  // Swiss Franc
            "AUD",  // Australian Dollar
            "JPY",  // Japanese Yen
            "CZK",  // Czech Koruna
            "MXN",  // Mexican Peso
            "NOK",  // Norwegian Krone
            "DKK",  // Danish Krone
            "SEK",  // Swedish Krone
            "HUF",  // Hungarian Forint
            "PLN",  // Polish Zloty
            "RSD",  // Serbian Dinar
            "ISK",  // Icelandic Krone
            "CNY",  // Chinese Yuan
            "INR",  // Indian Rupee
            "KRW",  // South Korean Won
            "BRL",  // Brazilian Real
            "ZAR",  // South African Rand
            "TRY",  // Turkish Lira
            "NZD",  // New Zealand Dollar
            "PHP",  // Philippine Peso
            "EGP"   // Egyptian Pound
        ));
        
        // Initialize denomination grid and lists
        this.denominationGrid = new DenominationSlot[GRID_HEIGHT][GRID_WIDTH];
        this.playerDenominations = new ArrayList<>();
        this.selectedDenominations = new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int guiLeft = this.leftPos;
        int guiTop = this.topPos;
        
        // Get configuration instance
        ATMGuiConfig config = ATMGuiConfig.getInstance();
        
        // Update container slot positions based on configuration
        updateContainerSlotPositions();
        
        // Adjust player inventory slot positions based on configuration
        adjustPlayerInventoryPositions(config);
        
        // Amount input field - configurable position and size
        this.amountField = new EditBox(this.font, 
            config.amountField.getX(guiLeft), 
            config.amountField.getY(guiTop), 
            config.amountField.width, 
            config.amountField.height, 
            Component.translatable("gui.enhancedatm.amount_field"));
        this.amountField.setMaxLength(10);
        this.amountField.setValue("0.00");
        this.addWidget(this.amountField);
        
        // Source currency selector button (FROM) - configurable
        this.sourceCurrencyButton = Button.builder(
            Component.literal(availableCurrencies.get(selectedSourceCurrencyIndex)),
            button -> cycleSourceCurrency()
        ).bounds(
            config.sourceCurrencyButton.getX(guiLeft), 
            config.sourceCurrencyButton.getY(guiTop), 
            config.sourceCurrencyButton.width, 
            config.sourceCurrencyButton.height
        ).build();
        this.addRenderableWidget(this.sourceCurrencyButton);
        
        // Target currency selector button (TO) - configurable
        this.targetCurrencyButton = Button.builder(
            Component.literal(availableCurrencies.get(selectedTargetCurrencyIndex)),
            button -> cycleTargetCurrency()
        ).bounds(
            config.targetCurrencyButton.getX(guiLeft), 
            config.targetCurrencyButton.getY(guiTop), 
            config.targetCurrencyButton.width, 
            config.targetCurrencyButton.height
        ).build();
        this.addRenderableWidget(this.targetCurrencyButton);
        
        // Deposit button - configurable
        this.depositButton = Button.builder(
            Component.translatable("gui.enhancedatm.deposit"),
            button -> performDeposit()
        ).bounds(
            config.depositButton.getX(guiLeft), 
            config.depositButton.getY(guiTop), 
            config.depositButton.width, 
            config.depositButton.height
        ).build();
        this.addRenderableWidget(this.depositButton);
        
        // Withdraw button - configurable
        this.withdrawButton = Button.builder(
            Component.translatable("gui.enhancedatm.withdraw"),
            button -> performWithdraw()
        ).bounds(
            config.withdrawButton.getX(guiLeft), 
            config.withdrawButton.getY(guiTop), 
            config.withdrawButton.width, 
            config.withdrawButton.height
        ).build();
        this.addRenderableWidget(this.withdrawButton);
        
        // Detect money button - configurable
        this.detectMoneyButton = Button.builder(
            Component.translatable("gui.enhancedatm.detect_money_button"),
            button -> detectGridMoney()
        ).bounds(
            config.detectMoneyButton.getX(guiLeft), 
            config.detectMoneyButton.getY(guiTop), 
            config.detectMoneyButton.width, 
            config.detectMoneyButton.height
        ).build();
        this.addRenderableWidget(this.detectMoneyButton);
        
        // Exchange button removed - not used
        // Denomination exchange button moved to top of GUI
        
        // Denomination slots are handled by the container
        
        // Update card information and preserve lock states
        updateCardInfo();
        
        // Initialize grid currency detection and preserve lock states
        updateGridCurrency();
        
        // Restore proper button appearance after resize
        updateSourceCurrencyButtonAppearance();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Update card info every frame to reflect changes
        updateCardInfo();
        
        // Update grid currency detection every frame
        updateGridCurrency();
        
        // Denomination slots are handled automatically by the container system
        
        // Render amount field (necesario para que se vea)
        this.amountField.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    

    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        
        // Prevent blurriness by disabling blend
        RenderSystem.disableBlend();
        
        int guiLeft = this.leftPos;
        int guiTop = this.topPos;
        
        // Draw main GUI background - offset down to leave space for controls above
        int textureOffsetY = 0; // Sin offset para aprovechar toda la textura 256x256
        guiGraphics.blit(TEXTURE, guiLeft, guiTop + textureOffsetY, 0, 0, this.imageWidth, this.imageHeight); // Usa toda la textura 256x256
        
        // Draw information ABOVE the texture (not overlapping)
        
        // Get configuration instance
        ATMGuiConfig config = ATMGuiConfig.getInstance();
        
        // Draw title above the texture  
        if (config.showTitleLabel) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.enhancedatm.atm_title"), 
                guiLeft + this.imageWidth / 2, config.titleLabel.getY(guiTop), config.titleLabelColor);
        }
        
        // Draw exchange rate for source > target conversion
        String sourceCurrency = availableCurrencies.get(selectedSourceCurrencyIndex);
        String targetCurrency = availableCurrencies.get(selectedTargetCurrencyIndex);
        if (!sourceCurrency.equals(targetCurrency) && config.showExchangeRateLabel) {
            guiGraphics.drawString(this.font, Component.translatable("gui.enhancedatm.exchange_rate_label", sourceCurrency, String.format("%.4f", exchangeRate), targetCurrency), 
                config.exchangeRateLabel.getX(guiLeft), config.exchangeRateLabel.getY(guiTop), config.exchangeRateLabelColor);
        }
        
        // Draw grid total (without "Total:" prefix)
        if (detectedGridTotal > 0.0 && !detectedGridTotalCurrency.isEmpty() && config.showGridTotalLabel) {
            String targetCurr = availableCurrencies.get(selectedTargetCurrencyIndex);
            if (!detectedGridTotalCurrency.equals(targetCurr)) {
                // Show conversion: "5.00 EUR → 5.80 USD"
                guiGraphics.drawString(this.font, Component.literal(String.format("%.2f %s → %.2f %s", detectedGridTotal, detectedGridTotalCurrency, gridConversionTotal, targetCurr)), 
                    config.gridTotalLabel.getX(guiLeft), config.gridTotalLabel.getY(guiTop), config.gridTotalLabelColor);
            } else {
                // Show just the amount: "5.00 EUR"
                guiGraphics.drawString(this.font, Component.literal(String.format("%.2f %s", detectedGridTotal, detectedGridTotalCurrency)), 
                    config.gridTotalLabel.getX(guiLeft), config.gridTotalLabel.getY(guiTop), config.gridTotalLabelColor);
            }
        }
        
        // Draw amount label above amount field
        if (config.showAmountLabel) {
            guiGraphics.drawString(this.font, Component.translatable("gui.enhancedatm.amount_label"), 
                config.amountLabel.getX(guiLeft), config.amountLabel.getY(guiTop), config.amountLabelColor);
        }
        
        // Draw conversion arrow between currency buttons - configurable position
        guiGraphics.drawString(this.font, Component.literal(">"), 
            config.conversionArrow.getX(guiLeft), config.conversionArrow.getY(guiTop), 0xFFFFFF);
        
        // Draw card balance below the card slot (positioned under the slot) - only when there's a card
        ItemStack cardInSlot = this.menu.getCardInSlot();
        if (config.showCardBalanceLabel && BubusteinMoneyIntegration.isCardItem(cardInSlot)) {
            guiGraphics.drawString(this.font, Component.translatable("gui.enhancedatm.card_balance_label", cardBalance, cardCurrency), 
                config.cardBalanceLabel.getX(guiLeft), config.cardBalanceLabel.getY(guiTop), config.cardBalanceLabelColor);
        }
        
        // Denomination slots are now rendered automatically by the container system
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle amount field input
        if (this.amountField.isFocused()) {
            return this.amountField.keyPressed(keyCode, scanCode, modifiers);
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle amount field clicks
        this.amountField.mouseClicked(mouseX, mouseY, button);
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public void resize(net.minecraft.client.Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        // Recalculate leftPos/topPos and reinvoke init() to reposition widgets
        this.init(mc, width, height);
    }
    
    private int lastGridHash = 0; // Track changes in grid content
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        // Check if grid content has changed (more efficient than updating every tick)
        int currentGridHash = calculateGridHash();
        if (currentGridHash != lastGridHash) {
            lastGridHash = currentGridHash;
            updateAmountFieldFromGrid();
        }
    }
    
    /**
     * Calculate a simple hash of the grid content to detect changes
     */
    private int calculateGridHash() {
        int hash = 0;
        for (int i = 1; i <= 9; i++) {
            ItemStack stack = this.menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                hash += stack.getItem().hashCode() + stack.getCount();
            }
        }
        return hash;
    }
    
    /**
     * Update the amount input field with the total value from grid
     * This method is called when grid content changes
     */
    public void updateAmountFieldFromGrid() {
        if (hasItemsInGrid()) {
            String gridCurrency = getGridCurrency();
            if (gridCurrency != null) {
                double gridTotal = getGridTotalValue(gridCurrency);
                if (gridTotal > 0) {
                    // Format the amount nicely (remove unnecessary decimals)
                    String formattedAmount = gridTotal == (long) gridTotal ? 
                        String.valueOf((long) gridTotal) : 
                        String.valueOf(gridTotal);
                    amountField.setValue(formattedAmount);
                }
            }
        } else {
            // Grid is empty, clear the field if it was set by grid
            // Only clear if the field contains a value that matches previous grid totals
            amountField.setValue("");
        }
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.amountField.isFocused()) {
            return this.amountField.charTyped(codePoint, modifiers);
        }
        
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    protected void slotClicked(net.minecraft.world.inventory.Slot slot, int slotId, int mouseButton, net.minecraft.world.inventory.ClickType clickType) {
        // Handle regular slot interactions (like dragging from inventory)
        super.slotClicked(slot, slotId, mouseButton, clickType);
        
        // Check if a denomination slot was clicked (slots 1-9)
        if (slotId >= 1 && slotId <= 9) {
            // Schedule update for next tick to ensure slot content is updated
            minecraft.tell(() -> updateAmountFieldFromGrid());
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // AQUÍ ES DONDE SE BLOQUEA EL TEXTO "INVENTARIO"
        // Al dejar este método vacío, impedimos que la clase padre (AbstractContainerScreen)
        // dibuje automáticamente el texto "Inventario" sobre el inventario del jugador
        
        // Si quisieras restaurar el texto "Inventario", descomenta la siguiente línea:
        // super.renderLabels(guiGraphics, mouseX, mouseY);
    }
    
    private void cycleSourceCurrency() {
        // Don't allow cycling if the source currency is locked due to grid contents or card
        if (isSourceCurrencyLocked || isCardCurrencyLocked) {
            return; // Block the currency change
        }
        
        selectedSourceCurrencyIndex = (selectedSourceCurrencyIndex + 1) % availableCurrencies.size();
        this.sourceCurrencyButton.setMessage(Component.literal(availableCurrencies.get(selectedSourceCurrencyIndex)));
        updateExchangeRate();
    }
    
    private void cycleTargetCurrency() {
        selectedTargetCurrencyIndex = (selectedTargetCurrencyIndex + 1) % availableCurrencies.size();
        this.targetCurrencyButton.setMessage(Component.literal(availableCurrencies.get(selectedTargetCurrencyIndex)));
        updateExchangeRate();
        
        // Recalculate grid conversion total when target currency changes
        if (detectedGridTotal > 0 && !detectedGridTotalCurrency.isEmpty()) {
            String targetCurrency = availableCurrencies.get(selectedTargetCurrencyIndex);
            if (!detectedGridTotalCurrency.equals(targetCurrency)) {
                double conversionRate = BubusteinMoneyIntegration.getExchangeRate(detectedGridTotalCurrency, targetCurrency);
                gridConversionTotal = detectedGridTotal * conversionRate;
            } else {
                gridConversionTotal = detectedGridTotal;
            }
        }
    }
    
    private void updateCardInfo() {
        // Get card info from the card slot in the container
        ItemStack cardInSlot = this.menu.getCardInSlot();
        if (BubusteinMoneyIntegration.isCardItem(cardInSlot)) {
            String newCardCurrency = BubusteinMoneyIntegration.getCardCurrency(cardInSlot);
            
            // Check if card currency changed (new card inserted)
            if (!newCardCurrency.equals(this.cardCurrency)) {
                lastCardChangeTime = System.currentTimeMillis();
                System.out.println("Enhanced ATM: Card currency changed to " + newCardCurrency + " at time " + lastCardChangeTime);
            }
            
            this.cardBalance = BubusteinMoneyIntegration.getFormattedBalance(cardInSlot);
            this.cardCurrency = newCardCurrency;
            
            // Check if card has balance and handle currency locking
            double balance = BubusteinMoneyIntegration.getCardBalance(cardInSlot);
            if (balance > 0.0) {
                hasCardWithBalance = true;
                lastKnownCardCurrency = this.cardCurrency;
                
                // Only lock to card currency if card was changed more recently than grid
                if (!isSourceCurrencyLocked || lastCardChangeTime > lastGridChangeTime) {
                    isCardCurrencyLocked = true;
                    isSourceCurrencyLocked = false; // Reset grid lock
                    
                    // Update source currency to match the card
                    int currencyIndex = availableCurrencies.indexOf(this.cardCurrency);
                    if (currencyIndex >= 0) {
                        selectedSourceCurrencyIndex = currencyIndex;
                        this.sourceCurrencyButton.setMessage(Component.literal(availableCurrencies.get(selectedSourceCurrencyIndex)));
                        System.out.println("Enhanced ATM: Switched to card currency: " + this.cardCurrency);
                    }
                    
                    // Update button appearance to show it's locked
                    updateSourceCurrencyButtonAppearance();
                }
            } else {
                // Card has no balance, unlock if it was locked by card
                if (isCardCurrencyLocked && !isSourceCurrencyLocked) {
                    isCardCurrencyLocked = false;
                    updateSourceCurrencyButtonAppearance();
                }
                hasCardWithBalance = false;
            }
            
            updateExchangeRate();
        } else {
            // No card in slot - reset display and unlock if locked by card
            this.cardBalance = "0.00";
            this.cardCurrency = "EUR";
            this.exchangeRate = 1.0;
            
            if (isCardCurrencyLocked && !isSourceCurrencyLocked) {
                isCardCurrencyLocked = false;
                updateSourceCurrencyButtonAppearance();
            }
            hasCardWithBalance = false;
        }
    }
    
    private void updateExchangeRate() {
        String sourceCurrency = availableCurrencies.get(selectedSourceCurrencyIndex);
        String targetCurrency = availableCurrencies.get(selectedTargetCurrencyIndex);
        this.exchangeRate = BubusteinMoneyIntegration.getExchangeRate(sourceCurrency, targetCurrency);
    }
    
    /**
     * Update source currency based on grid contents
     */
    private void updateGridCurrency() {
        // Get all denomination items from the 3x3 grid
        java.util.List<ItemStack> denominationItems = this.menu.getDenominationItems();
        
        if (denominationItems.isEmpty()) {
            // Grid is empty - unlock source currency selection only if not locked by card
            if (isSourceCurrencyLocked && !isCardCurrencyLocked) {
                isSourceCurrencyLocked = false;
                detectedGridCurrency = "";
                // Reset grid totals
                detectedGridTotal = 0.0;
                detectedGridTotalCurrency = "";
                gridConversionTotal = 0.0;
                // Update button appearance to show it's unlocked
                updateSourceCurrencyButtonAppearance();
            }
            return;
        }
        
        // Calculate total money by currency
        java.util.Map<String, Double> currencyTotals = new java.util.HashMap<>();
        String foundCurrency = "";
        
        for (ItemStack stack : denominationItems) {
            if (BubusteinMoneyIntegration.isMoneyItem(stack.getItem())) {
                String currency = extractCurrencyFromItemName(stack);
                if (!currency.isEmpty()) {
                    if (foundCurrency.isEmpty()) {
                        foundCurrency = currency;
                    }
                    
                    // Get the value and calculate total for this stack
                    double value = BubusteinMoneyIntegration.getDenominationValue(stack);
                    if (value > 0) {
                        double totalValue = value * stack.getCount();
                        currencyTotals.merge(currency, totalValue, Double::sum);
                    }
                }
            }
        }
        
        // Update grid totals
        if (!currencyTotals.isEmpty()) {
            // Use the most common currency or first found
            detectedGridTotalCurrency = foundCurrency;
            detectedGridTotal = currencyTotals.getOrDefault(foundCurrency, 0.0);
            
            // Calculate conversion to target currency
            String targetCurrency = availableCurrencies.get(selectedTargetCurrencyIndex);
            if (!detectedGridTotalCurrency.equals(targetCurrency)) {
                double conversionRate = BubusteinMoneyIntegration.getExchangeRate(detectedGridTotalCurrency, targetCurrency);
                gridConversionTotal = detectedGridTotal * conversionRate;
            } else {
                gridConversionTotal = detectedGridTotal;
            }
        }
        
        // If we found a currency, check if it's different (new item added to grid)
        if (!foundCurrency.isEmpty() && (!foundCurrency.equals(detectedGridCurrency) || !isSourceCurrencyLocked)) {
            // Register grid change time
            if (!foundCurrency.equals(detectedGridCurrency)) {
                lastGridChangeTime = System.currentTimeMillis();
                System.out.println("Enhanced ATM: Grid currency changed to " + foundCurrency + " at time " + lastGridChangeTime);
            }
            
            detectedGridCurrency = foundCurrency;
            
            // Only lock to grid currency if grid was changed more recently than card
            if (!isCardCurrencyLocked || lastGridChangeTime > lastCardChangeTime) {
                isSourceCurrencyLocked = true;
                isCardCurrencyLocked = false; // Reset card lock
                
                // Update source currency to match the grid
                int currencyIndex = availableCurrencies.indexOf(foundCurrency);
                if (currencyIndex >= 0) {
                    selectedSourceCurrencyIndex = currencyIndex;
                    this.sourceCurrencyButton.setMessage(Component.literal(availableCurrencies.get(selectedSourceCurrencyIndex)));
                    updateExchangeRate();
                    System.out.println("Enhanced ATM: Switched to grid currency: " + foundCurrency);
                }
                
                // Update button appearance to show it's locked
                updateSourceCurrencyButtonAppearance();
            }
        }
    }
    
    /**
     * Update the appearance of the source currency button based on lock status
     */
    private void updateSourceCurrencyButtonAppearance() {
        String currentCurrency = availableCurrencies.get(selectedSourceCurrencyIndex);
        
        if (isCardCurrencyLocked) {
            // Show currency with a card lock indicator
            this.sourceCurrencyButton.setMessage(Component.literal("💳" + currentCurrency));
        } else if (isSourceCurrencyLocked) {
            // Show currency with a grid lock indicator
            this.sourceCurrencyButton.setMessage(Component.literal("🔒" + currentCurrency));
        } else {
            // Show normal currency
            this.sourceCurrencyButton.setMessage(Component.literal(currentCurrency));
        }
    }
    
    /**
     * Extract currency code from item name based on actual mod item names
     */
    private String extractCurrencyFromItemName(ItemStack stack) {
        String itemName = stack.getItem().toString().toLowerCase();
        String displayName = stack.getHoverName().getString().toLowerCase();
        
        // Map of currency identifiers based on actual mod item names (most specific first)
        
        // EUR - Euro items
        if (itemName.contains("euro") || itemName.contains("ecent") || displayName.contains("euro")) return "EUR";
        
        // USD - US Dollar items (contains "dollar" but not "cdollar", "adollar", etc)
        if ((itemName.contains("dollar") && !itemName.contains("cdollar") && !itemName.contains("adollar") && !itemName.contains("nz_dollar")) || 
            itemName.contains("cents") && !itemName.contains("ccents") && !itemName.contains("acents") && !itemName.contains("br_centavos") && !itemName.contains("mx_centavos") && !itemName.contains("za_cents") && !itemName.contains("nz_cents") && !itemName.contains("ph_sentimo") ||
            displayName.contains("dollar") && !displayName.contains("canadian") && !displayName.contains("australian") && !displayName.contains("new zealand")) return "USD";
        
        // GBP - British Pound items
        if (itemName.contains("pound") && !itemName.contains("eg_pound") || itemName.contains("pence") || displayName.contains("pound") && !displayName.contains("egyptian")) return "GBP";
        
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
     * Check if the denomination grid has any items
     */
    private boolean hasItemsInGrid() {
        for (int i = 1; i <= 9; i++) { // Denomination slots are indices 1-9
            ItemStack stack = this.menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the currency type of items in the grid
     * Returns null if grid is empty or has mixed currencies
     */
    private String getGridCurrency() {
        String gridCurrency = null;
        
        for (int i = 1; i <= 9; i++) { // Denomination slots are indices 1-9
            ItemStack stack = this.menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                String itemCurrency = extractCurrencyFromItemName(stack);
                
                if (gridCurrency == null) {
                    gridCurrency = itemCurrency;
                } else if (!gridCurrency.equals(itemCurrency)) {
                    // Mixed currencies found - not allowed
                    return null;
                }
            }
        }
        
        return gridCurrency;
    }
    
    /**
     * Calculate total value of items in the grid
     */
    private double getGridTotalValue(String currency) {
        double total = 0.0;
        
        for (int i = 1; i <= 9; i++) { // Denomination slots are indices 1-9
            ItemStack stack = this.menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                try {
                    // First try to use BubusteinMoneyIntegration proper method
                    double value = com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration.getValueFromItemStack(stack, currency);
                    
                    if (value > 0) {
                        total += value * stack.getCount();
                        System.out.println("Enhanced ATM: Grid item " + stack.getDisplayName().getString() + 
                                         " has value " + value + " x " + stack.getCount() + " = " + (value * stack.getCount()));
                    } else {
                        // Fallback to parsing from item name
                        String itemName = stack.getItem().toString().toLowerCase();
                        value = parseValueFromItemName(itemName);
                        total += value * stack.getCount();
                        System.out.println("Enhanced ATM: Fallback parsing for " + stack.getDisplayName().getString() + 
                                         " gave value " + value + " x " + stack.getCount() + " = " + (value * stack.getCount()));
                    }
                } catch (Exception e) {
                    System.out.println("Enhanced ATM: Error calculating item value: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("Enhanced ATM: Total grid value calculated: " + total + " " + currency);
        return total;
    }
    
    /**
     * Parse monetary value from item name (fallback method)
     * Based on ACTUAL BubusteinMoney items found in the mod
     */
    private double parseValueFromItemName(String itemName) {
        System.out.println("Enhanced ATM: Parsing value from item name: '" + itemName + "'");
        
        String lowerName = itemName.toLowerCase();
        
        // EXACT MATCHES for BubusteinMoney items (most reliable)
        // Check larger values first to avoid partial matches
        
        // Very high denominations (10,000 - 1,000)
        if (lowerName.contains("ten_thousand_yen")) return 10000.0;
        if (lowerName.contains("five_thousand_yen") || lowerName.contains("five_thousand_cz_krone")) return 5000.0;
        if (lowerName.contains("two_thousand_cz_krone")) return 2000.0;
        if (lowerName.contains("thousand_francs") || lowerName.contains("thousand_yen") || 
            lowerName.contains("thousand_cz_krone") || lowerName.contains("thousand_mx_pesos") || 
            lowerName.contains("mie_lei_md")) return 1000.0;
        
        // High denominations (500 - 200)
        if (lowerName.contains("five_hundred_euros") || lowerName.contains("five_hundred_yen") || 
            lowerName.contains("five_hundred_cz_krone") || lowerName.contains("five_hundred_mx_pesos") ||
            lowerName.contains("cinci_sute_lei") || lowerName.contains("cinci_sute_lei_md")) return 500.0;
        if (lowerName.contains("two_hundred_euros") || lowerName.contains("two_hundred_francs") ||
            lowerName.contains("two_hundred_cz_krone") || lowerName.contains("two_hundred_mx_pesos") ||
            lowerName.contains("doua_sute_lei") || lowerName.contains("doua_sute_lei_md")) return 200.0;
        
        // Medium denominations (100 - 50)
        if (lowerName.contains("hundred_euros") || lowerName.contains("hundred_dollars") ||
            lowerName.contains("hundred_cdollars") || lowerName.contains("hundred_francs") ||
            lowerName.contains("hundred_adollars") || lowerName.contains("hundred_yen") ||
            lowerName.contains("hundred_cz_krone") || lowerName.contains("hundred_mx_pesos") ||
            lowerName.contains("suta_lei") || lowerName.contains("suta_lei_md")) return 100.0;
        if (lowerName.contains("fifty_euros") || lowerName.contains("fifty_dollars") ||
            lowerName.contains("fifty_cdollars") || lowerName.contains("fifty_pounds") ||
            lowerName.contains("fifty_francs") || lowerName.contains("fifty_adollars") ||
            lowerName.contains("fifty_yen") || lowerName.contains("fifty_cz_krone") ||
            lowerName.contains("fifty_mx_pesos") || lowerName.contains("cincizeci_lei") ||
            lowerName.contains("cincizeci_lei_md")) return 50.0;
        
        // Small bills (20 - 5)
        if (lowerName.contains("twenty_euros") || lowerName.contains("twenty_dollars") ||
            lowerName.contains("twenty_cdollars") || lowerName.contains("twenty_pounds") ||
            lowerName.contains("twenty_francs") || lowerName.contains("twenty_adollars") ||
            lowerName.contains("twenty_cz_krone") || lowerName.contains("twenty_mx_pesos") ||
            lowerName.contains("douazeci_lei") || lowerName.contains("douazeci_lei_md")) return 20.0;
        if (lowerName.contains("ten_euros") || lowerName.contains("ten_dollars") ||
            lowerName.contains("ten_cdollars") || lowerName.contains("ten_pounds") ||
            lowerName.contains("ten_francs") || lowerName.contains("ten_adollars") ||
            lowerName.contains("ten_yen") || lowerName.contains("ten_cz_krone") ||
            lowerName.contains("ten_mx_pesos") || lowerName.contains("zece_lei") ||
            lowerName.contains("zece_lei_md")) return 10.0;
        if (lowerName.contains("five_euros") || lowerName.contains("five_dollars") ||
            lowerName.contains("five_cdollars") || lowerName.contains("five_pounds") ||
            lowerName.contains("five_francs") || lowerName.contains("five_adollars") ||
            lowerName.contains("five_yen") || lowerName.contains("five_cz_krone") ||
            lowerName.contains("five_mx_pesos") || lowerName.contains("cinci_lei") ||
            lowerName.contains("cinci_lei_md")) return 5.0;
        
        // Coins (1 - 2)
        if (lowerName.contains("two_euros") || lowerName.contains("two_pounds") ||
            lowerName.contains("two_francs") || lowerName.contains("two_adollars") ||
            lowerName.contains("two_cz_krone") || lowerName.contains("two_mx_pesos") ||
            lowerName.contains("toonie") || lowerName.contains("doi_lei_md")) return 2.0;
        if (lowerName.contains("one_euro") || lowerName.contains("one_dollar") ||
            lowerName.contains("one_pound") || lowerName.contains("one_franc") ||
            lowerName.contains("one_adollar") || lowerName.contains("one_yen") ||
            lowerName.contains("one_cz_krone") || lowerName.contains("one_mx_peso") ||
            lowerName.contains("loonie") || lowerName.contains("un_leu") ||
            lowerName.contains("un_leu_md")) return 1.0;
        
        // Fractional coins (0.01 - 0.50)
        if (lowerName.contains("fifty_ecents") || lowerName.contains("fifty_cents") ||
            lowerName.contains("fifty_pence") || lowerName.contains("fifty_acents") ||
            lowerName.contains("fifty_mx_centavos") || lowerName.contains("half_franc") ||
            lowerName.contains("cincizeci_bani") || lowerName.contains("cincizeci_bani_md")) return 0.50;
        if (lowerName.contains("twentyfive_cents") || lowerName.contains("twentyfive_ccents") ||
            lowerName.contains("douazecicinci_bani_md")) return 0.25;
        if (lowerName.contains("twenty_ecents") || lowerName.contains("twenty_pence") ||
            lowerName.contains("twenty_centimes") || lowerName.contains("twenty_acents") ||
            lowerName.contains("twenty_mx_centavos")) return 0.20;
        if (lowerName.contains("ten_ecents") || lowerName.contains("ten_cents") ||
            lowerName.contains("ten_ccents") || lowerName.contains("ten_pence") ||
            lowerName.contains("ten_centimes") || lowerName.contains("ten_acents") ||
            lowerName.contains("ten_mx_centavos") || lowerName.contains("zece_bani") ||
            lowerName.contains("zece_bani_md")) return 0.10;
        if (lowerName.contains("five_ecents") || lowerName.contains("five_cents") ||
            lowerName.contains("five_ccents") || lowerName.contains("five_pence") ||
            lowerName.contains("five_centimes") || lowerName.contains("five_acents") ||
            lowerName.contains("five_mx_centavos") || lowerName.contains("cinci_bani") ||
            lowerName.contains("cinci_bani_md")) return 0.05;
        if (lowerName.contains("two_ecents") || lowerName.contains("two_pence")) return 0.02;
        if (lowerName.contains("one_ecent") || lowerName.contains("one_cent") ||
            lowerName.contains("one_pence") || lowerName.contains("un_ban")) return 0.01;
        
        // Fallback: try numeric extraction for cases like "bubusteinmoneymod:item_500"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(itemName);
        
        // Find all numbers and take the largest one (most likely the denomination)
        double maxValue = 0.0;
        while (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                if (value > maxValue && value <= 10000) { // Extended range for JPY
                    maxValue = value;
                }
                System.out.println("Enhanced ATM: Found number: " + value + " in item name");
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        }
        
        if (maxValue > 0) {
            System.out.println("Enhanced ATM: Using max value from numbers: " + maxValue);
            return maxValue;
        }
        
        System.out.println("Enhanced ATM: No value found, using default: 1.0");
        return 1.0; // Default value
    }
    
    private void performDeposit() {
        try {
            double amount = Double.parseDouble(amountField.getValue());
            String sourceCurrency = availableCurrencies.get(selectedSourceCurrencyIndex);
            String targetCurrency = availableCurrencies.get(selectedTargetCurrencyIndex);
            
            if (amount > 0) {
                // First check if there's a valid card in the slot
                ItemStack cardInSlot = this.menu.getCardInSlot();
                if (!com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration.isCardItem(cardInSlot)) {
                    // No card - show error message
                    minecraft.player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("message.enhancedatm.card_required")
                    );
                    return;
                }
                
                // Check if there are mixed currencies in grid (validation)
                if (hasItemsInGrid()) {
                    String gridCurrency = getGridCurrency();
                    if (gridCurrency == null) {
                        // Mixed currencies in grid - show error message
                        minecraft.player.sendSystemMessage(
                            net.minecraft.network.chat.Component.translatable("message.enhancedatm.mixed_currencies_error")
                        );
                        return;
                    }
                }
                
                // Send deposit packet - server will handle both inventory and grid items
                NetworkHandler.INSTANCE.sendToServer(new DepositPacket(amount, sourceCurrency, targetCurrency));
                
                // Clear the grid after successful deposit (if it had items)
                if (hasItemsInGrid()) {
                    clearDenominationGrid();
                    // Clear the amount field since we deposited from grid
                    amountField.setValue("");
                }
                
                // Don't close GUI - let player see the result
            }
        } catch (NumberFormatException e) {
            // Invalid amount - show error or ignore
        }
    }
    
    /**
     * Clear all items from the denomination grid
     */
    private void clearDenominationGrid() {
        for (int i = 1; i <= 9; i++) { // Denomination slots are indices 1-9
            this.menu.getSlot(i).set(ItemStack.EMPTY);
        }
    }
    
    private void performWithdraw() {
        try {
            double amount = Double.parseDouble(amountField.getValue());
            String sourceCurrency = availableCurrencies.get(selectedSourceCurrencyIndex);
            String targetCurrency = availableCurrencies.get(selectedTargetCurrencyIndex);
            
            if (amount > 0) {
                // Check if there's a valid card in the slot
                ItemStack cardInSlot = this.menu.getCardInSlot();
                if (!com.infinix.enhancedatm.common.utils.BubusteinMoneyIntegration.isCardItem(cardInSlot)) {
                    // No card - show error message
                    minecraft.player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("message.enhancedatm.card_required")
                    );
                    return;
                }
                
                NetworkHandler.INSTANCE.sendToServer(new WithdrawPacket(amount, sourceCurrency, targetCurrency));
                // Don't close GUI - let player see the result
            }
        } catch (NumberFormatException e) {
            // Invalid amount - show error or ignore
        }
    }
    
    private void detectGridMoney() {
        // Send packet to server to detect and report money in the 3x3 grid
        // Include the target currency for automatic exchange
        String targetCurrency = availableCurrencies.get(selectedTargetCurrencyIndex);
        NetworkHandler.INSTANCE.sendToServer(new DetectGridMoneyPacket(targetCurrency));
    }
    
    private void performExchange() {
        try {
            double amount = Double.parseDouble(amountField.getValue());
            String fromCurrency = cardCurrency;
            String toCurrency = availableCurrencies.get(selectedTargetCurrencyIndex);
            
            if (amount > 0 && !fromCurrency.equals(toCurrency)) {
                NetworkHandler.INSTANCE.sendToServer(new CurrencyExchangePacket(amount, fromCurrency, toCurrency));
                // Don't close GUI - let player see the result
            }
        } catch (NumberFormatException e) {
            // Invalid amount - show error or ignore
        }
    }
    
    /**
     * Adjust player inventory slot positions based on configuration
     * Note: Player inventory positioning is handled by the container.
     * This method is kept for future implementation.
     */
    private void adjustPlayerInventoryPositions(ATMGuiConfig config) {
        // For now, this functionality is prepared but not fully implemented
        // as it requires more complex container modifications.
        // The configuration structure is ready for when it's implemented.
        this.inventoryLabelX = config.playerInventory.offsetX;
        this.inventoryLabelY = config.playerInventory.offsetY;
    }
    
    /**
     * MÁXIMA PROTECCIÓN: Update GUI configuration con todas las protecciones anti-congelamiento
     * Called when configuration is reloaded via command or sync
     */
    public void updateGuiConfiguration() {
        System.out.println("Enhanced ATM: updateGuiConfiguration called (optimized version)");
        if (this.leftPos != 0 && this.topPos != 0) {
            System.out.println("Enhanced ATM: Proceeding with OPTIMIZED GUI configuration update");
            
            // OPTIMIZACIÓN: Solo actualizar slots (con caché de reflexión)
            updateContainerSlotPositions();
            
            // OPTIMIZACIÓN: Solo actualizar posiciones de widgets (sin recrear)
            rebuildButtons();
            
            // OPTIMIZACIÓN: NO llamar a this.init() - evita recrear toda la GUI
            System.out.println("Enhanced ATM: Configuration updated without full re-initialization");
        } else {
            System.out.println("Enhanced ATM: Skipping GUI update - position not initialized");
        }
    }
    
    /**
     * Generar hash simple de la configuración para detectar cambios
     */
    private String generateConfigHash(ATMGuiConfig config) {
        return String.valueOf(
            config.cardSlot.offsetX + config.cardSlot.offsetY + 
            config.playerInventory.offsetX + config.playerInventory.offsetY +
            config.denominationGrid.offsetX + config.denominationGrid.offsetY +
            config.amountField.offsetX + config.amountField.offsetY
        );
    }
    
    /**
     * Reset update counters when GUI is closed
     */
    @Override
    public void onClose() {
        // Reset counters when GUI closes
        updateCount = 0;
        lastUpdateTime = 0;
        System.out.println("Enhanced ATM: GUI closed, reset update counters");
        super.onClose();
    }
    
    /**
     * Update the positions of container slots based on configuration
     */
    private void updateContainerSlotPositions() {
        ATMGuiConfig config = ATMGuiConfig.getInstance();
        System.out.println("Enhanced ATM: Updating container slot positions...");
        
        // Update card slot (slot 0 in the container)
        net.minecraft.world.inventory.Slot cardSlotContainer = this.menu.getSlot(0);
        System.out.println("Enhanced ATM: Card slot BEFORE update - X: " + cardSlotContainer.x + ", Y: " + cardSlotContainer.y);
        System.out.println("Enhanced ATM: Card slot TARGET position - X: " + config.cardSlot.offsetX + ", Y: " + config.cardSlot.offsetY);
        
        updateSlotPosition(cardSlotContainer, config.cardSlot.offsetX, config.cardSlot.offsetY);
        
        System.out.println("Enhanced ATM: Card slot AFTER update - X: " + cardSlotContainer.x + ", Y: " + cardSlotContainer.y);
        System.out.println("Enhanced ATM: GUI size is " + this.imageWidth + "x" + this.imageHeight + " - Slot should be visible if within these bounds");
        
        // Update denomination slots (slots 1-9 in the container)
        int denomStartX = config.denominationGrid.offsetX;
        int denomStartY = config.denominationGrid.offsetY;
        System.out.println("Enhanced ATM: Denomination grid config - X: " + denomStartX + ", Y: " + denomStartY);
        
        for (int i = 1; i <= 9; i++) { // Denomination slots are indices 1-9
            net.minecraft.world.inventory.Slot slot = this.menu.getSlot(i);
            int slotIndex = i - 1; // 0-8
            int row = slotIndex / 3;
            int col = slotIndex % 3;
            
            // Update slot position using reflection
            updateSlotPosition(slot, denomStartX + (col * 18), denomStartY + (row * 18));
        }
        
        // Update player inventory slots (slots 10-45)
        int invStartX = config.playerInventory.offsetX;
        int invStartY = config.playerInventory.offsetY;
        
        // Player inventory slots (27 slots)
        for (int i = 10; i < 37; i++) {
            net.minecraft.world.inventory.Slot slot = this.menu.getSlot(i);
            int slotIndex = i - 10; // 0-26
            int row = slotIndex / 9;
            int col = slotIndex % 9;
            
            updateSlotPosition(slot, invStartX + (col * 18), invStartY + (row * 18));
        }
        
        // Player hotbar slots (9 slots)
        for (int i = 37; i < 46; i++) {
            net.minecraft.world.inventory.Slot slot = this.menu.getSlot(i);
            int slotIndex = i - 37; // 0-8
            
            updateSlotPosition(slot, invStartX + (slotIndex * 18), invStartY + 58); // 58px below inventory
        }
    }
    
    /**
     * Inicializar caché de reflexión una sola vez
     */
    private static void initializeReflectionCache() {
        if (reflectionInitialized) return; // Ya inicializado
        
        try {
            // Try common field names SOLO UNA VEZ
            String[] possibleXNames = {"x", "field_75223_e", "xPos"};
            String[] possibleYNames = {"y", "field_75221_f", "yPos"};
            
            // Find X field
            for (String fieldName : possibleXNames) {
                try {
                    cachedXField = net.minecraft.world.inventory.Slot.class.getDeclaredField(fieldName);
                    cachedXField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException e) {
                    // Try next name
                }
            }
            
            // Find Y field
            for (String fieldName : possibleYNames) {
                try {
                    cachedYField = net.minecraft.world.inventory.Slot.class.getDeclaredField(fieldName);
                    cachedYField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException e) {
                    // Try next name
                }
            }
            
            if (cachedXField != null && cachedYField != null) {
                reflectionInitialized = true;
                System.out.println("Enhanced ATM: Reflection cache initialized successfully");
            } else {
                System.err.println("Enhanced ATM: Failed to initialize reflection cache");
            }
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error initializing reflection cache: " + e.getMessage());
        }
    }
    
    /**
     * Update a slot's position using CACHED reflection (muy rápido)
     */
    private void updateSlotPosition(net.minecraft.world.inventory.Slot slot, int newX, int newY) {
        try {
            // Inicializar caché si es necesario (solo la primera vez)
            if (!reflectionInitialized) {
                initializeReflectionCache();
            }
            
            // Usar campos cacheados (sin búsqueda repetitiva)
            if (cachedXField != null && cachedYField != null) {
                cachedXField.set(slot, newX);
                cachedYField.set(slot, newY);
                // System.out.println("Enhanced ATM: Slot updated to (" + newX + ", " + newY + ")"); // Comentado para reducir spam
            } else {
                System.err.println("Enhanced ATM: Reflection cache not available, skipping slot update");
            }
            
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Failed to update slot position: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Optimizado: Solo actualizar posiciones sin recrear widgets
     */
    private void rebuildButtons() {
        // OPTIMIZACIÓN: No limpiar widgets, solo actualizar posiciones
        ATMGuiConfig config = ATMGuiConfig.getInstance();
        int guiLeft = this.leftPos;
        int guiTop = this.topPos;
        
        // Actualizar posiciones de widgets existentes (mucho más rápido)
        if (this.amountField != null) {
            updateWidgetPosition(this.amountField, config.amountField.getX(guiLeft), config.amountField.getY(guiTop));
        }
        
        if (this.sourceCurrencyButton != null) {
            updateWidgetPosition(this.sourceCurrencyButton, config.sourceCurrencyButton.getX(guiLeft), config.sourceCurrencyButton.getY(guiTop));
        }
        
        if (this.targetCurrencyButton != null) {
            updateWidgetPosition(this.targetCurrencyButton, config.targetCurrencyButton.getX(guiLeft), config.targetCurrencyButton.getY(guiTop));
        }
        
        if (this.depositButton != null) {
            updateWidgetPosition(this.depositButton, config.depositButton.getX(guiLeft), config.depositButton.getY(guiTop));
        }
        
        if (this.withdrawButton != null) {
            updateWidgetPosition(this.withdrawButton, config.withdrawButton.getX(guiLeft), config.withdrawButton.getY(guiTop));
        }
        
        if (this.detectMoneyButton != null) {
            updateWidgetPosition(this.detectMoneyButton, config.detectMoneyButton.getX(guiLeft), config.detectMoneyButton.getY(guiTop));
        }
    }
    
    /**
     * Actualizar posición de un widget sin recrearlo
     */
    private void updateWidgetPosition(net.minecraft.client.gui.components.AbstractWidget widget, int newX, int newY) {
        try {
            // Usar reflexión cacheada para actualizar posición de widgets
            java.lang.reflect.Field xField = widget.getClass().getField("x");
            java.lang.reflect.Field yField = widget.getClass().getField("y");
            xField.setAccessible(true);
            yField.setAccessible(true);
            xField.set(widget, newX);
            yField.set(widget, newY);
        } catch (Exception e) {
            // Si falla reflexión, recrear solo este widget
            System.err.println("Enhanced ATM: Failed to update widget position, widget may need recreation");
        }
    }
    
    /**
     * Create all buttons with configurable positions
     */
    private void createButtons(ATMGuiConfig config, int guiLeft, int guiTop) {
        // Source currency selector button (FROM) - configurable
        this.sourceCurrencyButton = Button.builder(
            Component.literal(availableCurrencies.get(selectedSourceCurrencyIndex)),
            button -> cycleSourceCurrency()
        ).bounds(
            config.sourceCurrencyButton.getX(guiLeft), 
            config.sourceCurrencyButton.getY(guiTop), 
            config.sourceCurrencyButton.width, 
            config.sourceCurrencyButton.height
        ).build();
        this.addRenderableWidget(this.sourceCurrencyButton);
        
        // Target currency selector button (TO) - configurable
        this.targetCurrencyButton = Button.builder(
            Component.literal(availableCurrencies.get(selectedTargetCurrencyIndex)),
            button -> cycleTargetCurrency()
        ).bounds(
            config.targetCurrencyButton.getX(guiLeft), 
            config.targetCurrencyButton.getY(guiTop), 
            config.targetCurrencyButton.width, 
            config.targetCurrencyButton.height
        ).build();
        this.addRenderableWidget(this.targetCurrencyButton);
        
        // Deposit button - configurable
        this.depositButton = Button.builder(
            Component.translatable("gui.enhancedatm.deposit"),
            button -> performDeposit()
        ).bounds(
            config.depositButton.getX(guiLeft), 
            config.depositButton.getY(guiTop), 
            config.depositButton.width, 
            config.depositButton.height
        ).build();
        this.addRenderableWidget(this.depositButton);
        
        // Withdraw button - configurable
        this.withdrawButton = Button.builder(
            Component.translatable("gui.enhancedatm.withdraw"),
            button -> performWithdraw()
        ).bounds(
            config.withdrawButton.getX(guiLeft), 
            config.withdrawButton.getY(guiTop), 
            config.withdrawButton.width, 
            config.withdrawButton.height
        ).build();
        this.addRenderableWidget(this.withdrawButton);
        
        // Detect money button - configurable
        this.detectMoneyButton = Button.builder(
            Component.literal("💰"),
            button -> detectGridMoney()
        ).bounds(
            config.detectMoneyButton.getX(guiLeft), 
            config.detectMoneyButton.getY(guiTop), 
            config.detectMoneyButton.width, 
            config.detectMoneyButton.height
        ).build();
        this.addRenderableWidget(this.detectMoneyButton);
    }
    
    /**
     * Initialize the denomination grid positions
     */
    private void initializeDenominationGrid(int guiLeft, int guiTop) {
        ATMGuiConfig config = ATMGuiConfig.getInstance();
        int startX = config.denominationGrid.getX(guiLeft);
        int startY = config.denominationGrid.getY(guiTop);
        
        for (int row = 0; row < GRID_HEIGHT; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                int x = startX + (col * SLOT_SIZE); // No spacing between slots like Minecraft
                int y = startY + (row * SLOT_SIZE); // No spacing between slots like Minecraft
                denominationGrid[row][col] = new DenominationSlot(x, y);
            }
        }
        
        // Initial population will be handled by the render loop
    }
    
    /**
     * Initialize empty grid for drag and drop operations
     */
    private void updateDenominationGrid() {
        // Keep grid as is - only clear when explicitly needed
        // Grid starts empty and players drag items into it
    }
    

    
    /**
     * Render the denomination grid with Minecraft-style slots
     */
    private void renderDenominationGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int row = 0; row < GRID_HEIGHT; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                DenominationSlot slot = denominationGrid[row][col];
                
                // Draw Minecraft-style slot background (dark with lighter borders)
                // Dark background
                guiGraphics.fill(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE, 0xFF8B8B8B);
                
                // Light top and left borders (3D effect)
                guiGraphics.fill(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + 1, 0xFFC6C6C6); // Top
                guiGraphics.fill(slot.x, slot.y, slot.x + 1, slot.y + SLOT_SIZE, 0xFFC6C6C6); // Left
                
                // Dark bottom and right borders (3D effect)
                guiGraphics.fill(slot.x, slot.y + SLOT_SIZE - 1, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE, 0xFF373737); // Bottom
                guiGraphics.fill(slot.x + SLOT_SIZE - 1, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE, 0xFF373737); // Right
                
                // Inner slot area (slightly darker)
                guiGraphics.fill(slot.x + 1, slot.y + 1, slot.x + SLOT_SIZE - 1, slot.y + SLOT_SIZE - 1, 0xFF555555);
                
                // Selection highlight (red tint if selected)
                if (slot.selected) {
                    guiGraphics.fill(slot.x + 1, slot.y + 1, slot.x + SLOT_SIZE - 1, slot.y + SLOT_SIZE - 1, 0x88FF4444);
                }
                
                // Draw item if present
                if (!slot.isEmpty()) {
                    guiGraphics.renderItem(slot.itemStack, slot.x + 1, slot.y + 1);
                    
                    // Draw count if > 1
                    if (slot.count > 1) {
                        String countText = String.valueOf(slot.count);
                        int textWidth = font.width(countText);
                        int textX = slot.x + SLOT_SIZE - textWidth - 2;
                        int textY = slot.y + SLOT_SIZE - font.lineHeight - 1;
                        
                        // Draw shadow for better readability
                        guiGraphics.drawString(font, countText, textX + 1, textY + 1, 0x000000);
                        guiGraphics.drawString(font, countText, textX, textY, 0xFFFFFF);
                    }
                }
                
                // Hover highlight (white overlay)
                if (slot.contains(mouseX, mouseY)) {
                    guiGraphics.fill(slot.x + 1, slot.y + 1, slot.x + SLOT_SIZE - 1, slot.y + SLOT_SIZE - 1, 0x40FFFFFF);
                }
            }
        }
    }
    
    /**
     * Inner class to represent a denomination slot in the grid
     */
    private static class DenominationSlot {
        public final int x, y;
        public ItemStack itemStack;
        public boolean selected;
        public int count;
        
        public DenominationSlot(int x, int y) {
            this.x = x;
            this.y = y;
            this.itemStack = ItemStack.EMPTY;
            this.selected = false;
            this.count = 0;
        }
        
        public boolean isEmpty() {
            return itemStack.isEmpty() || count <= 0;
        }
        
        public void setItem(ItemStack stack, int count) {
            this.itemStack = stack.copy();
            this.itemStack.setCount(1); // Display only 1 item for visual clarity
            this.count = count;
        }
        
        public void clear() {
            this.itemStack = ItemStack.EMPTY;
            this.selected = false;
            this.count = 0;
        }
        
        public boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + SLOT_SIZE && 
                   mouseY >= y && mouseY < y + SLOT_SIZE;
        }
    }
}