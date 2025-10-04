package com.infinix.enhancedatm.common.commands;

import com.infinix.enhancedatm.common.blocks.ModBlocks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Command to give Bank Permission Block to administrators
 * Only players with permission level 2 (OP) can use this command
 */
public class GiveBankPermissionCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("enhancedatm")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.literal("givebankpermission")
                    .executes(context -> giveBankPermission(context, 1))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(context -> giveBankPermission(
                            context, 
                            IntegerArgumentType.getInteger(context, "amount")
                        ))
                    )
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> giveBankPermissionToPlayer(
                            context, 
                            EntityArgument.getPlayer(context, "player"),
                            1
                        ))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(context -> giveBankPermissionToPlayer(
                                context,
                                EntityArgument.getPlayer(context, "player"),
                                IntegerArgumentType.getInteger(context, "amount")
                            ))
                        )
                    )
                )
        );
    }
    
    private static int giveBankPermission(CommandContext<CommandSourceStack> context, int amount) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return giveBlockToPlayer(context.getSource(), player, amount);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("This command can only be executed by a player!"));
            return 0;
        }
    }
    
    private static int giveBankPermissionToPlayer(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, int amount) {
        return giveBlockToPlayer(context.getSource(), targetPlayer, amount);
    }
    
    private static int giveBlockToPlayer(CommandSourceStack source, ServerPlayer player, int amount) {
        ItemStack blockStack = new ItemStack(ModBlocks.BANK_PERMISSION_BLOCK_ITEM.get(), amount);
        
        boolean success = player.getInventory().add(blockStack);
        
        if (success) {
            source.sendSuccess(
                () -> Component.literal("§aGave " + amount + " Bank Permission Block(s) to " + player.getName().getString()),
                true
            );
            
            player.sendSystemMessage(
                Component.literal("§6You have received " + amount + " Bank Permission Block(s)! §ePlace them near ATMs to enable their use.")
            );
            
            return 1;
        } else {
            source.sendFailure(
                Component.literal("§cCould not give Bank Permission Block - player's inventory is full!")
            );
            return 0;
        }
    }
}
