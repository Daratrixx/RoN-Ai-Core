package com.daratrix.ronapi.commands;

import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.registers.GameRuleRegister;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ScanResourceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scanResources")
                .executes(ScanResourceCommand::scanResources));
    }

    private static int scanResources(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        var server = commandSourceStackCommandContext.getSource().getServer();
        var scanSize = GameRuleRegister.scanSize(server);
        WorldApi.startGridScan(scanSize);
        return Command.SINGLE_SUCCESS;
    }
}
