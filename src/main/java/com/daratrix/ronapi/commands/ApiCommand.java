package com.daratrix.ronapi.commands;

import com.daratrix.ronapi.apis.WorldApi;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ApiCommand {

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent evt) {
        var dispatcher = evt.getDispatcher();
        dispatcher.register(Commands.literal("scan")
                .then(Commands.argument("size", IntegerArgumentType.integer(1, 32))
                        .executes(ApiCommand::scan)));
    }

    public static int scan(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int size = IntegerArgumentType.getInteger(context, "size");

        WorldApi.startGridScan(size);

        return Command.SINGLE_SUCCESS;
    }

}
