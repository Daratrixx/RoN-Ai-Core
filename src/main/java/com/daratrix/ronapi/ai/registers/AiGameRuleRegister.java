package com.daratrix.ronapi.ai.registers;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.Level;

public class AiGameRuleRegister {
    public static GameRules.Key<BooleanValue> FORCE_WARPTEN;
    public static GameRules.Key<BooleanValue> FORCE_GREEDISGOOD;
    public static GameRules.Key<BooleanValue> SHOW_DEBUG;

    public static void init() {
        FORCE_WARPTEN = GameRules.register("ronAiForceWarpten", Category.MISC, BooleanValue.create(false));
        FORCE_GREEDISGOOD = GameRules.register("ronAiForceGreedisgood", Category.MISC, BooleanValue.create(false));
        SHOW_DEBUG = GameRules.register("ronAiShowDebug", Category.MISC, BooleanValue.create(false));
    }

    public static <T extends GameRules.Value<T>> T getRule(MinecraftServer server, GameRules.Key<T> rule) {
        return server != null ? server.getGameRules().getRule(rule) : null;
    }

    public static <T extends GameRules.Value<T>> T getRule(Level level, GameRules.Key<T> rule) {
        return level != null ? level.getGameRules().getRule(rule) : null;
    }

    public static boolean getValue(BooleanValue rule) {
        return rule != null ? rule.get() : false;
    }

    public static int getValue(IntegerValue rule) {
        return rule != null ? rule.get() : 0;
    }

    public static boolean showDebug(MinecraftServer server) {
        return getValue(getRule(server, SHOW_DEBUG));
    }

    public static boolean showDebug(Level level) {
        return getValue(getRule(level, SHOW_DEBUG));
    }

    public static boolean forceWarpten(MinecraftServer server) {
        return getValue(getRule(server, FORCE_WARPTEN));
    }

    public static boolean forceWarpten(Level level) {
        return getValue(getRule(level, FORCE_WARPTEN));
    }

    public static boolean forceGreedisgood(MinecraftServer server) {
        return getValue(getRule(server, FORCE_GREEDISGOOD));
    }

    public static boolean forceGreedisgood(Level level) {
        return getValue(getRule(level, FORCE_GREEDISGOOD));
    }


}
