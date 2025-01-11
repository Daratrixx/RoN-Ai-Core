package com.daratrix.ronapi.registers;

import com.daratrix.ronapi.apis.WorldApi;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.solegendary.reignofnether.gamemode.GameModeClientboundPacket;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GameRuleRegister {
    public static GameRules.Key<GameRules.IntegerValue> SCAN_SIZE; // in number of chunks from 0,0 in all X/Z directions
    public static GameRules.Key<GameRules.BooleanValue> SHOW_DEBUG;

    public static void init() {
        SCAN_SIZE = GameRules.register("ronApiScanSize", Category.MISC, IntegerValue.create(0));
        SHOW_DEBUG = GameRules.register("ronApiShowDebug", Category.MISC, GameRules.BooleanValue.create(false));
    }

    public static <T extends GameRules.Value<T>> T getRule(MinecraftServer server, GameRules.Key<T> rule) {
        return server != null ? server.getGameRules().getRule(rule) : null;
    }

    public static <T extends GameRules.Value<T>> T getRule(Level level, GameRules.Key<T> rule) {
        return level != null ? level.getGameRules().getRule(rule) : null;
    }

    public static boolean getValue(GameRules.BooleanValue rule) {
        return rule != null ? rule.get() : false;
    }

    public static int getValue(GameRules.IntegerValue rule) {
        return rule != null ? rule.get() : 0;
    }

    public static boolean showDebug(MinecraftServer server) {
        return getValue(getRule(server, SHOW_DEBUG));
    }

    public static boolean showDebug(Level level) {
        return getValue(getRule(level, SHOW_DEBUG));
    }

    public static int scanSize(MinecraftServer server) {
        return getValue(getRule(server, SCAN_SIZE));
    }

    public static int scanSize(Level level) {
        return getValue(getRule(level, SCAN_SIZE));
    }

    @SubscribeEvent
    public static void onCommandUsed(CommandEvent evt) {
        List<ParsedCommandNode<CommandSourceStack>> nodes = evt.getParseResults().getContext().getNodes();
        if (nodes.size() > 2) {
            if (((ParsedCommandNode) nodes.get(0)).getNode().getName().equals("gamerule")) {
                var args = evt.getParseResults().getContext().getArguments();
                if (((ParsedCommandNode) nodes.get(1)).getNode().getName().equals("ronApiScanSize")) {
                    var size = (Integer) ((ParsedArgument) args.get("value")).getResult();
                    WorldApi.startGridScan(size);
                }
            }
        }
    }
}
