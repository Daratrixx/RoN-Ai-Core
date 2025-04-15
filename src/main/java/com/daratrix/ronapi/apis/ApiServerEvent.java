package com.daratrix.ronapi.apis;

import com.daratrix.ronapi.commands.ScanResourceCommand;
import com.daratrix.ronapi.models.ApiWorld;
import com.daratrix.ronapi.models.savedata.WorldApiResourceSaveData;
import com.daratrix.ronapi.registers.GameRuleRegister;
import com.daratrix.ronapi.timer.Timer;
import com.daratrix.ronapi.timer.TimerServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.stream.Collectors;

public class ApiServerEvent {

    private static Timer worldTrackerTimer;

    public static void cleanupTimers() {
        TimerServerEvents.destroyTimer(worldTrackerTimer);
        worldTrackerTimer = null;
    }

    @SubscribeEvent
    public static void onServerReady(ServerStartedEvent evt) {
        cleanupTimers();
        WorldApi.getSingleton().reset();
        worldTrackerTimer = TimerServerEvents.queueTimerLooping(5, WorldApi::updateWorld);
        System.out.println("Created 5 seconds period timer for ApiWorld tracking updates");

        var server = evt.getServer();
        var level = server.overworld();
        LoadApiResources(level, server);
        System.out.println("API Scan size: " + GameRuleRegister.scanSize(server));
        System.out.println("API Show debug: " + GameRuleRegister.showDebug(server));
    }

    private static void LoadApiResources(ServerLevel level, MinecraftServer server) {
        var savedResources = WorldApiResourceSaveData.getInstance(level);
        if (savedResources.resources.isEmpty()) {
            // run the map scan to build the list of resources from scratch
            //var scanSize = GameRuleRegister.scanSize(server);
            //WorldApi.startGridScan(scanSize);
            return;
        }

        // use the loaded list of resources
        var resources = WorldApi.getSingleton().resources;
        resources.clear();

        for (var resource : savedResources.resources) {
            resource.getBlocks().forEach((b) -> resources.put(b, resource));
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        cleanupTimers();
        System.out.println("Destroyed timer for ApiWorld updates");
        var level = evt.getServer().overworld();
        SaveApiResources(level);
    }

    private static void SaveApiResources(ServerLevel level) {
        var savedResources = WorldApiResourceSaveData.getInstance(level);
        savedResources.resources.clear();
        WorldApi.getSingleton().resources.values().stream().distinct().collect(Collectors.toCollection(() -> savedResources.resources));
        savedResources.save();
        level.getDataStorage().save();
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {
        ScanResourceCommand.register(evt.getDispatcher());
    }
}
