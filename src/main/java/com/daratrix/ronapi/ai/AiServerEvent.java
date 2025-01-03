package com.daratrix.ronapi.ai;

import com.daratrix.ronapi.RonApi;
import com.daratrix.ronapi.ai.controller.AiController;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiWorld;
import com.daratrix.ronapi.timer.Timer;
import com.daratrix.ronapi.timer.TimerServerEvents;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AiServerEvent {

    private static Timer aiTickTimer;
    private static int aiTickIndex;

    public static void aiTick(float t) {
        System.out.println(RonApi.MOD_ID + ">AiServerEvent.aiTick " + aiTickIndex);
        var controllers = AiController.controllers.values();
        switch (aiTickIndex) {
            case 0:
                WorldApi.updateWorld(t);
                for (AiController c : controllers) {
                    c.runAiUpdatePriorities(t);
                    c.runAiMicro(t);
                }
                break;
            case 1:
                WorldApi.updateWorld(t);
                for (AiController c : controllers) {
                    c.runAiHarvesting(t);
                    c.runAiProduceUnits(t);
                    c.runAiProduceResearches(t);
                }
                break;
            case 2:
                WorldApi.updateWorld(t);
                for (AiController c : controllers) {
                    c.runAiArmy(t);
                    c.runAiMicro(t);
                }
                break;
            case 3:
                WorldApi.updateWorld(t);
                for (AiController c : controllers) {
                    c.runAiProduceBuildings(t);
                }
                break;
        }
        aiTickIndex = (aiTickIndex + 1) % 4;
    }

    public static void cleanupControllers() {
        AiController.controllers.values().forEach(AiController::cleanup);
        AiController.controllers.clear();
        TimerServerEvents.destroyTimer(aiTickTimer);
    }

    @SubscribeEvent
    public static void onServerReady(ServerStartedEvent evt) {
        aiTickIndex = 0;
        aiTickTimer = TimerServerEvents.queueTimerLooping(1, AiServerEvent::aiTick);
        System.out.println("Created 1 seconds period timer for aiTick callback");
    }

    @SubscribeEvent
    public static void onServerClosing(ServerStoppingEvent evt) {
        cleanupControllers();
        System.out.println("Removed existing Ai controllers and timer");
    }

}
