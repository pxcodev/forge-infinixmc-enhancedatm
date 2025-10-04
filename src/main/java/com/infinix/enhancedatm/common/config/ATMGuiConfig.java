package com.infinix.enhancedatm.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Configuration class for Enhanced ATM GUI positions and settings
 */
public class ATMGuiConfig {
    
    private static ATMGuiConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "gui_config.json";
    private static final String CONFIG_VERSION = "1.2.0"; // Version with robust null detection for boxed types
    
    // Configuration version (for auto-update detection)
    public String configVersion = CONFIG_VERSION;
    
    // Label positions (relative to GUI top-left) - Updated default configuration
    public LabelPosition exchangeRateLabel = new LabelPosition(227, 5);
    public LabelPosition gridTotalLabel = new LabelPosition(97, 25);
    public LabelPosition amountLabel = new LabelPosition(193, 5);
    public LabelPosition cardBalanceLabel = new LabelPosition(72, 65);
    public LabelPosition titleLabel = new LabelPosition(10, 88);
    
    // Button positions (relative to GUI top-left) - Updated default configuration
    public ButtonPosition amountField = new ButtonPosition(5, 202, 80, 20);
    public ButtonPosition sourceCurrencyButton = new ButtonPosition(98, 202, 35, 20);
    public ButtonPosition targetCurrencyButton = new ButtonPosition(148, 202, 35, 20);
    public ButtonPosition depositButton = new ButtonPosition(190, 108, 50, 20);
    public ButtonPosition withdrawButton = new ButtonPosition(190, 132, 50, 20);
    public ButtonPosition detectMoneyButton = new ButtonPosition(190, 157, 50, 20);
    
    // Arrow symbol position
    public LabelPosition conversionArrow = new LabelPosition(207, 137);
    
    // Grid positions - Updated default configuration
    public GridPosition denominationGrid = new GridPosition(5, 39);
    public GridPosition playerInventory = new GridPosition(6, 108);
    public GridPosition cardSlot = new GridPosition(86, 54);
    
    // Label colors (in hex format)
    public int exchangeRateLabelColor = 0xFFFFFF; // White
    public int gridTotalLabelColor = 0x00FF00;   // Green
    public int amountLabelColor = 0xFFFFFF;      // White
    public int cardBalanceLabelColor = 0xFFFFFF; // White
    public int titleLabelColor = 0xFFFFFF;       // White
    
    // Label visibility toggles
    public boolean showExchangeRateLabel = true;
    public boolean showGridTotalLabel = true;
    public boolean showAmountLabel = true;
    public boolean showCardBalanceLabel = true;
    public boolean showTitleLabel = true;
    
    /**
     * Get the singleton instance
     */
    public static ATMGuiConfig getInstance() {
        if (instance == null) {
            instance = new ATMGuiConfig();
            System.out.println("Enhanced ATM: Creating new configuration instance");
            instance.loadConfig();
        }
        return instance;
    }
    
    /**
     * Force reload configuration - useful for server startup checks
     */
    public static ATMGuiConfig getInstanceAndForceCheck() {
        if (instance == null) {
            instance = new ATMGuiConfig();
            System.out.println("Enhanced ATM: Creating new configuration instance with force check");
        } else {
            System.out.println("Enhanced ATM: Force checking existing configuration instance");
        }
        
        try {
            System.out.println("Enhanced ATM: *** CALLING loadConfig() FROM getInstanceAndForceCheck() ***");
            instance.loadConfig(); // Always reload to check for updates
            System.out.println("Enhanced ATM: *** loadConfig() COMPLETED SUCCESSFULLY ***");
        } catch (Exception e) {
            System.err.println("Enhanced ATM: *** CRITICAL ERROR IN getInstanceAndForceCheck() ***");
            System.err.println("Enhanced ATM: Error details: " + e.getMessage());
            e.printStackTrace();
            
            // Try to create a basic instance with defaults as fallback
            try {
                System.out.println("Enhanced ATM: Attempting fallback configuration creation...");
                instance.configVersion = CONFIG_VERSION;
                instance.saveConfig();
                System.out.println("Enhanced ATM: Fallback configuration created successfully");
            } catch (Exception fallbackException) {
                System.err.println("Enhanced ATM: Fallback also failed: " + fallbackException.getMessage());
                fallbackException.printStackTrace();
            }
        }
        
        return instance;
    }
    
    /**
     * Reload the configuration from file
     */
    public static void reload() {
        if (instance != null) {
            instance.loadConfig();
            System.out.println("Enhanced ATM: GUI configuration reloaded");
        }
    }
    
    /**
     * Force update the configuration file with current defaults
     * Useful for manually updating old configuration files
     */
    public static void forceUpdate() {
        if (instance == null) {
            instance = new ATMGuiConfig();
        }
        instance.configVersion = CONFIG_VERSION;
        instance.saveConfig();
        System.out.println("Enhanced ATM: Configuration forcefully updated to version " + CONFIG_VERSION);
    }
    

    

    
    /**
     * Update configuration from JSON string (used for client sync)
     */
    public static void updateFromJson(String json) {
        try {
            ATMGuiConfig loaded = GSON.fromJson(json, ATMGuiConfig.class);
            if (loaded != null) {
                // Ensure instance exists
                if (instance == null) {
                    instance = new ATMGuiConfig();
                }
                // Copy loaded values to current instance
                instance.exchangeRateLabel = loaded.exchangeRateLabel != null ? loaded.exchangeRateLabel : new LabelPosition(227, 5);
                instance.gridTotalLabel = loaded.gridTotalLabel != null ? loaded.gridTotalLabel : new LabelPosition(97, 5);
                instance.amountLabel = loaded.amountLabel != null ? loaded.amountLabel : new LabelPosition(193, 5);
                instance.cardBalanceLabel = loaded.cardBalanceLabel != null ? loaded.cardBalanceLabel : new LabelPosition(72, 65);
                instance.titleLabel = loaded.titleLabel != null ? loaded.titleLabel : new LabelPosition(27, 88);
                
                // Button positions
                instance.amountField = loaded.amountField != null ? loaded.amountField : new ButtonPosition(5, 202, 80, 20);
                instance.sourceCurrencyButton = loaded.sourceCurrencyButton != null ? loaded.sourceCurrencyButton : new ButtonPosition(98, 202, 35, 20);
                instance.targetCurrencyButton = loaded.targetCurrencyButton != null ? loaded.targetCurrencyButton : new ButtonPosition(148, 202, 35, 20);
                instance.depositButton = loaded.depositButton != null ? loaded.depositButton : new ButtonPosition(190, 108, 50, 20);
                instance.withdrawButton = loaded.withdrawButton != null ? loaded.withdrawButton : new ButtonPosition(190, 132, 50, 20);
                instance.detectMoneyButton = loaded.detectMoneyButton != null ? loaded.detectMoneyButton : new ButtonPosition(190, 157, 50, 20);
                
                // Arrow position
                instance.conversionArrow = loaded.conversionArrow != null ? loaded.conversionArrow : new LabelPosition(207, 137);
                
                // Grid positions
                instance.denominationGrid = loaded.denominationGrid != null ? loaded.denominationGrid : new GridPosition(6, 40);
                instance.playerInventory = loaded.playerInventory != null ? loaded.playerInventory : new GridPosition(6, 110);
                instance.cardSlot = loaded.cardSlot != null ? loaded.cardSlot : new GridPosition(86, 54);
                
                instance.exchangeRateLabelColor = loaded.exchangeRateLabelColor;
                instance.gridTotalLabelColor = loaded.gridTotalLabelColor;
                instance.amountLabelColor = loaded.amountLabelColor;
                instance.cardBalanceLabelColor = loaded.cardBalanceLabelColor;
                instance.titleLabelColor = loaded.titleLabelColor;
                
                instance.showExchangeRateLabel = loaded.showExchangeRateLabel;
                instance.showGridTotalLabel = loaded.showGridTotalLabel;
                instance.showAmountLabel = loaded.showAmountLabel;
                instance.showCardBalanceLabel = loaded.showCardBalanceLabel;
                instance.showTitleLabel = loaded.showTitleLabel;
                
                // Update version to current
                instance.configVersion = CONFIG_VERSION;
                
                System.out.println("Enhanced ATM: Configuration synced from server");
            }
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Failed to sync configuration from server: " + e.getMessage());
        }
    }
    
    /**
     * Get configuration as JSON string (for server sync)
     */
    public static String getConfigAsJson() {
        if (instance != null) {
            return GSON.toJson(instance);
        }
        return "{}";
    }
    
    /**
     * Load configuration from file - simple version, no auto-update
     */
    private void loadConfig() {
        try {
            System.out.println("Enhanced ATM: Loading GUI configuration...");
            
            File configFile = getConfigFile();
            System.out.println("Enhanced ATM: Configuration file path: " + configFile.getAbsolutePath());
        
            if (!configFile.exists()) {
                // Create default config file only once
                System.out.println("Enhanced ATM: Configuration file does not exist, creating default configuration...");
                saveConfig();
                System.out.println("Enhanced ATM: Created default GUI configuration file at: " + configFile.getAbsolutePath());
                return;
            }
            
            // Check file permissions
            if (!configFile.canRead()) {
                System.err.println("Enhanced ATM: Cannot read configuration file: " + configFile.getAbsolutePath());
                return;
            }
        
            try (FileReader reader = new FileReader(configFile)) {
                System.out.println("Enhanced ATM: Loading configuration from file...");
                ATMGuiConfig loaded = GSON.fromJson(reader, ATMGuiConfig.class);
                if (loaded != null) {
                    System.out.println("Enhanced ATM: Configuration loaded successfully");
                    
                    // Simply copy loaded values, using defaults for null values
                    this.exchangeRateLabel = loaded.exchangeRateLabel != null ? loaded.exchangeRateLabel : new LabelPosition(227, 5);
                    this.gridTotalLabel = loaded.gridTotalLabel != null ? loaded.gridTotalLabel : new LabelPosition(97, 25);
                    this.amountLabel = loaded.amountLabel != null ? loaded.amountLabel : new LabelPosition(193, 5);
                    this.cardBalanceLabel = loaded.cardBalanceLabel != null ? loaded.cardBalanceLabel : new LabelPosition(72, 65);
                    this.titleLabel = loaded.titleLabel != null ? loaded.titleLabel : new LabelPosition(27, 88);
                    
                    // Button positions
                    this.amountField = loaded.amountField != null ? loaded.amountField : new ButtonPosition(5, 202, 80, 20);
                    this.sourceCurrencyButton = loaded.sourceCurrencyButton != null ? loaded.sourceCurrencyButton : new ButtonPosition(98, 202, 35, 20);
                    this.targetCurrencyButton = loaded.targetCurrencyButton != null ? loaded.targetCurrencyButton : new ButtonPosition(148, 202, 35, 20);
                    this.depositButton = loaded.depositButton != null ? loaded.depositButton : new ButtonPosition(190, 108, 50, 20);
                    this.withdrawButton = loaded.withdrawButton != null ? loaded.withdrawButton : new ButtonPosition(190, 132, 50, 20);
                    this.detectMoneyButton = loaded.detectMoneyButton != null ? loaded.detectMoneyButton : new ButtonPosition(190, 157, 50, 20);
                    
                    // Arrow position
                    this.conversionArrow = loaded.conversionArrow != null ? loaded.conversionArrow : new LabelPosition(207, 137);
                    
                    // Grid positions
                    this.denominationGrid = loaded.denominationGrid != null ? loaded.denominationGrid : new GridPosition(5, 39);
                    this.playerInventory = loaded.playerInventory != null ? loaded.playerInventory : new GridPosition(6, 108);
                    this.cardSlot = loaded.cardSlot != null ? loaded.cardSlot : new GridPosition(86, 54);
                    
                    // Colors - just copy loaded values (GSON will use 0 if missing)
                    this.exchangeRateLabelColor = loaded.exchangeRateLabelColor;
                    this.gridTotalLabelColor = loaded.gridTotalLabelColor;
                    this.amountLabelColor = loaded.amountLabelColor;
                    this.cardBalanceLabelColor = loaded.cardBalanceLabelColor;
                    this.titleLabelColor = loaded.titleLabelColor;
                    
                    // Show flags - just copy loaded values (GSON will use false if missing)
                    this.showExchangeRateLabel = loaded.showExchangeRateLabel;
                    this.showGridTotalLabel = loaded.showGridTotalLabel;
                    this.showAmountLabel = loaded.showAmountLabel;
                    this.showCardBalanceLabel = loaded.showCardBalanceLabel;
                    this.showTitleLabel = loaded.showTitleLabel;
                    
                    // Keep the loaded version (no auto-update)
                    this.configVersion = loaded.configVersion != null ? loaded.configVersion : CONFIG_VERSION;
                    
                    System.out.println("Enhanced ATM: GUI configuration loaded successfully (version: " + this.configVersion + ")");
                } else {
                    System.err.println("Enhanced ATM: Failed to parse configuration file - using defaults");
                }
            } catch (IOException e) {
                System.err.println("Enhanced ATM: Failed to load GUI configuration: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error loading configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save current configuration to file - simple version
     */
    public void saveConfig() {
        File configFile = getConfigFile();
        
        try {
            // Ensure directory exists
            File parentDir = configFile.getParentFile();
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                System.out.println("Enhanced ATM: Config directory created: " + created + " at " + parentDir.getAbsolutePath());
            }
            
            // Ensure the configVersion is always set to current version before saving
            this.configVersion = CONFIG_VERSION;
            
            // Write configuration directly to file
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
                writer.flush();
            }
            
            System.out.println("Enhanced ATM: GUI configuration saved successfully to: " + configFile.getAbsolutePath());
            System.out.println("Enhanced ATM: Configuration version saved: " + this.configVersion);
            
        } catch (IOException e) {
            System.err.println("Enhanced ATM: Failed to save GUI configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the configuration file
     */
    private File getConfigFile() {
        // Create Enhanced ATM config directory in server config folder
        File configDir = FMLPaths.CONFIGDIR.get().toFile();
        File enhancedAtmDir = new File(configDir, "enhancedatm");
        
        // Ensure the directory exists
        if (!enhancedAtmDir.exists()) {
            enhancedAtmDir.mkdirs();
            System.out.println("Enhanced ATM: Created config directory: " + enhancedAtmDir.getAbsolutePath());
        }
        
        return new File(enhancedAtmDir, CONFIG_FILE_NAME);
    }
    
    /**
     * Inner class to represent label positions
     */
    public static class LabelPosition {
        public int offsetY;
        public int offsetX;
        
        public LabelPosition() {
            this(0, 0);
        }
        
        public LabelPosition(int offsetY, int offsetX) {
            this.offsetY = offsetY;
            this.offsetX = offsetX;
        }
        
        /**
         * Get absolute X position
         */
        public int getX(int guiLeft) {
            return guiLeft + offsetX;
        }
        
        /**
         * Get absolute Y position
         */
        public int getY(int guiTop) {
            return guiTop + offsetY;
        }
    }
    
    /**
     * Inner class to represent button positions and sizes
     */
    public static class ButtonPosition {
        public int offsetX;
        public int offsetY;
        public int width;
        public int height;
        
        public ButtonPosition() {
            this(0, 0, 100, 20);
        }
        
        public ButtonPosition(int offsetX, int offsetY, int width, int height) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.width = width;
            this.height = height;
        }
        
        /**
         * Get absolute X position
         */
        public int getX(int guiLeft) {
            return guiLeft + offsetX;
        }
        
        /**
         * Get absolute Y position
         */
        public int getY(int guiTop) {
            return guiTop + offsetY;
        }
    }
    
    /**
     * Inner class to represent grid positions (for inventory grids)
     */
    public static class GridPosition {
        public int offsetX;
        public int offsetY;
        
        public GridPosition() {
            this(0, 0);
        }
        
        public GridPosition(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
        
        /**
         * Get absolute X position
         */
        public int getX(int guiLeft) {
            return guiLeft + offsetX;
        }
        
        /**
         * Get absolute Y position
         */
        public int getY(int guiTop) {
            return guiTop + offsetY;
        }
    }
}