package com.infinix.enhancedatm.common.network.packets;

import com.infinix.enhancedatm.common.config.ATMGuiConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet to sync GUI configuration from server to client
 */
public class SyncGuiConfigPacket {
    
    private final String configJson;
    
    public SyncGuiConfigPacket(String configJson) {
        this.configJson = configJson;
    }
    
    public SyncGuiConfigPacket(FriendlyByteBuf buf) {
        this.configJson = buf.readUtf();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.configJson);
    }
    
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                // This runs on the client side
                ATMGuiConfig.updateFromJson(this.configJson);
                System.out.println("Enhanced ATM: Client received and applied GUI configuration from server");
                
                // Update any open ATM screens
                updateOpenATMScreens();
            });
        }
        context.setPacketHandled(true);
    }
    
    /**
     * Update any currently open ATM screens with new configuration
     */
    private void updateOpenATMScreens() {
        try {
            System.out.println("Enhanced ATM: Checking for open ATM screens to update...");
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            System.out.println("Enhanced ATM: Current screen: " + (minecraft.screen != null ? minecraft.screen.getClass().getSimpleName() : "null"));
            
            if (minecraft.screen instanceof com.infinix.enhancedatm.client.screen.EnhancedATMScreen atmScreen) {
                System.out.println("Enhanced ATM: Found open ATM screen, updating configuration...");
                
                // SOLO actualizar configuración - sin cerrar pantalla
                atmScreen.updateGuiConfiguration();
                System.out.println("Enhanced ATM: Configuration sync completed");
                
                // ELIMINADO: No cerrar ni reabrir pantalla - causa bucles infinitos
                // ELIMINADO: minecraft.setScreen(null);
                // ELIMINADO: minecraft.execute(() -> {...});
                
            } else {
                System.out.println("Enhanced ATM: No ATM screen open, skipping update");
            }
        } catch (Exception e) {
            System.err.println("Enhanced ATM: Error updating open ATM screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}