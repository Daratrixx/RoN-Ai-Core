package com.daratrix.ronapi.ai.player;

import com.daratrix.ronapi.ai.controller.AiController;
import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.timer.TimerServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;

import static com.solegendary.reignofnether.player.PlayerServerEvents.players;

public class AiPlayerServerEvents {

    public static void startRTSBot(String name, Vec3 pos, Faction faction) {
        var id = getNextBotId();
        var aiPlayerName = name + id;
        PlayerServerEvents.startRTSBot(aiPlayerName, pos, faction);
        var rtsPlayer = PlayerServerEvents.rtsPlayers.stream().filter(p -> p.id == id).findFirst().get();
        System.out.println("Creating new AI agent " + rtsPlayer.name);
        var controller = createAiPlayer(rtsPlayer, name);
        WorldApi.getSingleton().track((AiPlayer) controller.player);

        if (controller.logic.useGreedisgood()) {
            // extra resources for faster development
            ResourcesServerEvents.addSubtractResources(new Resources(rtsPlayer.name, 25000, 25000, 25000));
        }
    }

    public static AiPlayer restoreAiPlayer(RTSPlayer rtsPlayer) {
        System.out.println("Restoring AI agent " + rtsPlayer.name);
        var logicName = rtsPlayer.name.substring(0, rtsPlayer.name.lastIndexOf("-"));
        return (AiPlayer) createAiPlayer(rtsPlayer, logicName).player;
    }

    public static AiController createAiPlayer(RTSPlayer rtsPlayer, String logicName) {
        var aiPlayer = new AiPlayer(rtsPlayer);
        var controller = startAiController(aiPlayer, logicName);

        if (controller.logic.useWarpten()) {
            // warpten for faster development
            ResearchServerEvents.addCheat(rtsPlayer.name, "warpten");
            ResearchClientboundPacket.addCheat(rtsPlayer.name, "warpten");
        }

        return controller;
    }

    public static AiController startAiController(IAiPlayer aiPlayer, String logicName) {
        System.out.println("Creating AI controller using logic " + logicName);
        var logic = AiLogics.get(logicName);
        return new AiController(aiPlayer, logic);
    }

    public static int getNextBotId() {
        int minId = 0;
        if (!PlayerServerEvents.rtsPlayers.isEmpty()) {
            minId = (Integer) Collections.min(PlayerServerEvents.rtsPlayers.stream().map((r) -> {
                return r.id;
            }).toList());
        }

        if (minId >= 0) {
            return -1;
        } else {
            return minId - 1;
        }
    }

    public static void sendMessageToAllPlayers(String msg) {
        sendMessageToAllPlayers(msg, false);
    }

    public static void sendMessageToAllPlayers(String msg, boolean bold) {
        for (ServerPlayer player : players) {
            player.sendSystemMessage(Component.literal(""));
            if (bold)
                player.sendSystemMessage(Component.literal(msg).withStyle(Style.EMPTY.withBold(true)));
            else
                player.sendSystemMessage(Component.literal(msg));
            player.sendSystemMessage(Component.literal(""));
        }
    }
}
