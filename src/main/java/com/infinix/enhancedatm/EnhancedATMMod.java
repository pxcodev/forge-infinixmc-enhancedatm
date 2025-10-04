package com.infinix.enhancedatm;

import com.infinix.enhancedatm.client.screen.EnhancedATMScreen;
import com.infinix.enhancedatm.common.blocks.ModBlocks;
import com.infinix.enhancedatm.common.container.EnhancedATMContainer;
import com.infinix.enhancedatm.common.network.NetworkHandler;
import com.infinix.enhancedatm.common.config.Config;
import com.infinix.enhancedatm.common.commands.ReloadATMConfigCommand;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Enhanced ATM Mod - Adds functional ATM interface and currency exchange to BubusteinMoney mod
 * 
 * Features:
 * - Functional ATM interface for deposits/withdrawals
 * - Real-time currency exchange
 * - Transaction history
 * - Configurable fees and limits
 * - Bank permission system for ATM access control
 * 
 * @author InfinixMC
 * @version 1.0.0
 */
@Mod(EnhancedATMMod.MODID)
public class EnhancedATMMod {
    public static final String MODID = "enhancedatm";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Container registration
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    
    public static final RegistryObject<MenuType<EnhancedATMContainer>> ENHANCED_ATM_CONTAINER = CONTAINERS.register(
        "enhanced_atm", 
        () -> IForgeMenuType.create(EnhancedATMContainer::new)
    );

    public EnhancedATMMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register mod contents
        CONTAINERS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);

        // Register lifecycle events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // Register config in custom directory
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "enhancedatm/enhancedatm-common.toml");

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new com.infinix.enhancedatm.common.events.ATMInteractionHandler());

        LOGGER.info("Enhanced ATM mod loading...");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Enhanced ATM common setup");
        
        // Initialize network handler
        event.enqueueWork(() -> {
            NetworkHandler.init();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Enhanced ATM client setup");
        
        // Register screens
        event.enqueueWork(() -> {
            MenuScreens.register(ENHANCED_ATM_CONTAINER.get(), EnhancedATMScreen::new);
        });
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Enhanced ATM server starting - Currency exchange system ready!");
        
        // Initialize server-side configuration
        initializeServerConfig();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ReloadATMConfigCommand.register(event.getDispatcher());
        com.infinix.enhancedatm.common.commands.GiveBankPermissionCommand.register(event.getDispatcher());
    }
    
    /**
     * Initialize server-side configuration
     */
    private void initializeServerConfig() {
        try {
            LOGGER.info("Enhanced ATM: Starting server configuration initialization...");
            
            // Force check the configuration on server startup - this will always check for updates
            com.infinix.enhancedatm.common.config.ATMGuiConfig config = 
                com.infinix.enhancedatm.common.config.ATMGuiConfig.getInstanceAndForceCheck();
            
            LOGGER.info("Enhanced ATM: Server configuration initialized successfully");
            
            // Log configuration version for debugging
            LOGGER.info("Enhanced ATM: Configuration version: " + config.configVersion);
            
            // Check if cardSlot is properly initialized
            if (config.cardSlot != null) {
                LOGGER.info("Enhanced ATM: CardSlot position loaded: (" + config.cardSlot.offsetX + ", " + config.cardSlot.offsetY + ")");
            } else {
                LOGGER.warn("Enhanced ATM: CardSlot position is null after initialization - forcing update");
                com.infinix.enhancedatm.common.config.ATMGuiConfig.forceUpdate();
            }
            
        } catch (Exception e) {
            LOGGER.error("Enhanced ATM: Failed to initialize server configuration", e);
            // Try to create a basic configuration as a fallback
            try {
                LOGGER.info("Enhanced ATM: Attempting to create basic configuration as fallback...");
                com.infinix.enhancedatm.common.config.ATMGuiConfig.getInstance().saveConfig();
                LOGGER.info("Enhanced ATM: Basic configuration created successfully");
            } catch (Exception fallbackException) {
                LOGGER.error("Enhanced ATM: Failed to create fallback configuration", fallbackException);
            }
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Enhanced ATM client setup complete");
        }
    }
}