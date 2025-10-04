package com.infinix.enhancedatm.common.commands;

import com.infinix.enhancedatm.common.config.ATMGuiConfig;
import com.infinix.enhancedatm.common.network.NetworkHandler;
import com.infinix.enhancedatm.common.network.packets.SyncGuiConfigPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Command to reload Enhanced ATM GUI configuration
 */
public class ReloadATMConfigCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enhancedatm")
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2)) // Require OP level 2
                .executes(ReloadATMConfigCommand::execute)
            )
            .then(Commands.literal("forceupdate")
                .requires(source -> source.hasPermission(2)) // Require OP level 2
                .executes(ReloadATMConfigCommand::executeForceUpdate)
            )
            .then(Commands.literal("regenerate")
                .requires(source -> source.hasPermission(2)) // Require OP level 2
                .executes(ReloadATMConfigCommand::executeRegenerate)
            )
        );
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            // Reload the server-side configuration
            ATMGuiConfig.reload();
            
            // Sync configuration with all connected clients
            String configJson = ATMGuiConfig.getConfigAsJson();
            SyncGuiConfigPacket syncPacket = new SyncGuiConfigPacket(configJson);
            
            // Send to all players
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), syncPacket);
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§a[Enhanced ATM] §fConfiguración recargada y sincronizada con " + 
                    context.getSource().getServer().getPlayerList().getPlayerCount() + " jugadores"), 
                true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c[Enhanced ATM] §fError al recargar configuración: " + e.getMessage())
            );
            return 0;
        }
    }

    private static int executeForceUpdate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            // Force update the server-side configuration
            ATMGuiConfig.forceUpdate();
            
            // Sync configuration with all connected clients
            String configJson = ATMGuiConfig.getConfigAsJson();
            SyncGuiConfigPacket syncPacket = new SyncGuiConfigPacket(configJson);
            
            // Send to all players
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), syncPacket);
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§a[Enhanced ATM] §fConfiguración actualizada forzosamente y sincronizada con " + 
                    context.getSource().getServer().getPlayerList().getPlayerCount() + " jugadores"), 
                true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c[Enhanced ATM] §fError al actualizar configuración: " + e.getMessage())
            );
            return 0;
        }
    }

    private static int executeRegenerate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            // Regenerate the server-side configuration with all defaults
            ATMGuiConfig.getInstance().saveConfig();
            
            // Sync configuration with all connected clients
            String configJson = ATMGuiConfig.getConfigAsJson();
            SyncGuiConfigPacket syncPacket = new SyncGuiConfigPacket(configJson);
            
            // Send to all players
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), syncPacket);
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§a[Enhanced ATM] §fConfiguración regenerada completamente y sincronizada con " + 
                    context.getSource().getServer().getPlayerList().getPlayerCount() + " jugadores. " +
                    "§eIncluye todas las nuevas propiedades como cardSlot."), 
                true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c[Enhanced ATM] §fError al regenerar configuración: " + e.getMessage())
            );
            return 0;
        }
    }
}