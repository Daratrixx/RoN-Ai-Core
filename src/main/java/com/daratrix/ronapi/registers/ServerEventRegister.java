package com.daratrix.ronapi.registers;

import com.daratrix.ronapi.RonApi;
import com.daratrix.ronapi.ai.AiServerEvent;
import com.daratrix.ronapi.ai.player.AiPlayerServerEvents;
import com.daratrix.ronapi.apis.ApiServerEvent;
import com.daratrix.ronapi.timer.TimerServerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public class ServerEventRegister {

    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;

    public ServerEventRegister() { }

    public void registerServerEvents() {
        System.out.println(RonApi.MOD_ID + ">ServerEventRegister.registerServerEvents");
        vanillaEventBus.register(ApiServerEvent.class);
        vanillaEventBus.register(TimerServerEvents.class);
        vanillaEventBus.register(AiServerEvent.class);
    }
}
