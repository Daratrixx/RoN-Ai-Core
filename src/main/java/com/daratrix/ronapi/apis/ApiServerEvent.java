package com.daratrix.ronapi.apis;

import com.daratrix.ronapi.models.ApiWorld;
import com.daratrix.ronapi.registers.GameRuleRegister;
import com.daratrix.ronapi.timer.Timer;
import com.daratrix.ronapi.timer.TimerServerEvents;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ApiServerEvent {

    private static Timer worldTrackerTimer;
    private static Timer chunkScannerTimer;
    private static ApiWorld world;

    public static void CleanupTimers() {
        TimerServerEvents.destroyTimer(worldTrackerTimer);
        TimerServerEvents.destroyTimer(chunkScannerTimer);
        worldTrackerTimer = null;
        chunkScannerTimer = null;
    }

    @SubscribeEvent
    public static void onServerReady(ServerStartedEvent evt) {
        CleanupTimers();
        world = WorldApi.getSingleton();
        worldTrackerTimer = TimerServerEvents.queueTimerLooping(5, WorldApi::updateWorld);
        System.out.println("Created 5 seconds period timer for ApiWorld tracking updates");
        var scanSize = GameRuleRegister.scanSize(evt.getServer());
        WorldApi.startGridScan(scanSize);
    }

    @SubscribeEvent
    public static void onServerClosing(ServerStoppingEvent evt) {
        CleanupTimers();
        System.out.println("Destroyed timer for ApiWorld updates");
        world.reset();
    }

}
