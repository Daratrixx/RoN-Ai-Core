package com.daratrix.ronapi.ai;

import com.daratrix.ronapi.RonApi;
import com.daratrix.ronapi.ai.controller.AiController;
import com.daratrix.ronapi.ai.registers.AiGameRuleRegister;
import com.daratrix.ronapi.ai.scripts.MonsterScript;
import com.daratrix.ronapi.ai.scripts.PiglinScript;
import com.daratrix.ronapi.ai.scripts.VillagerScript;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiWorld;
import com.daratrix.ronapi.registers.GameRuleRegister;
import com.daratrix.ronapi.timer.Timer;
import com.daratrix.ronapi.timer.TimerServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Consumer;

public class AiServerEvent {

    private static Timer aiTickTimer;
    private static int aiTickIndex;

    public static void aiTick(MinecraftServer server) {
        var controllers = AiController.controllers.values();
        switch (aiTickIndex) {
            case 0:
                WorldApi.updateWorld(server);
                for (AiController c : controllers) {
                    if (!c.surrender && c.shouldSurrender(server)) {
                        c.sendSurrenderMessage();
                        continue;
                    }

                    if(c.surrender) {
                        c.surrender();
                        continue;
                    }

                    c.runAiUpdatePriorities(server);
                    c.runAiMicro(server);
                }

                AiController.removeDefeatedControllers();
                break;
            case 1:
                WorldApi.updateWorld(server);
                for (AiController c : controllers) {
                    c.runAiHarvesting(server);
                    c.runAiProduceUnits(server);
                    c.runAiProduceResearches(server);
                }
                break;
            case 2:
                WorldApi.updateWorld(server);
                for (AiController c : controllers) {
                    c.runAiArmy(server);
                    c.runAiMicro(server);
                }
                break;
            case 3:
                WorldApi.updateWorld(server);
                for (AiController c : controllers) {
                    c.runAiProduceBuildings(server);
                    c.logger.flush();
                }
                break;
        }

        aiTickIndex = (aiTickIndex + 1) % 4;
    }

    @SubscribeEvent
    public static void onServerReady(ServerStartedEvent evt) {
        aiTickIndex = 0;
        aiTickTimer = TimerServerEvents.queueTimerLooping(1, AiServerEvent::aiTick);
        System.out.println("Created 1 seconds period timer for aiTick callback");
        System.out.println("AI Force warpten: " + AiGameRuleRegister.forceWarpten(evt.getServer()));
        System.out.println("AI Force greedisgood: " + AiGameRuleRegister.forceGreedisgood(evt.getServer()));
        System.out.println("AI Show debug: " + AiGameRuleRegister.showDebug(evt.getServer()));
    }

    @SubscribeEvent
    public static void onServerClosing(ServerStoppingEvent evt) {
        AiController.reset();
        TimerServerEvents.destroyTimer(aiTickTimer);
        System.out.println("Removed existing Ai controllers and timer");
    }
}
