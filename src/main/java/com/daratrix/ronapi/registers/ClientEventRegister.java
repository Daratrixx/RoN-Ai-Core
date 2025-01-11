package com.daratrix.ronapi.registers;


import com.daratrix.ronapi.RonApi;
import com.daratrix.ronapi.ai.AiServerEvent;
import com.daratrix.ronapi.ai.cursor.AiCursorClientEvents;
import com.daratrix.ronapi.ai.player.AiPlayerServerEvents;
import com.daratrix.ronapi.apis.ApiServerEvent;
import com.daratrix.ronapi.cursor.CursorClientEvents;
import com.daratrix.ronapi.ai.hud.AiHudClientEvents;
import com.daratrix.ronapi.timer.TimerServerEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public class ClientEventRegister {

    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;

    public ClientEventRegister() { }

    /**
     * Register client only events. This method must only be called when it is certain that the mod
     * is executing code on the client side and not the dedicated server.
     */

    public void registerClientEvents() {
        System.out.println(RonApi.MOD_ID + ">ClientEventRegister.registerClientEvents CLIENT");
        vanillaEventBus.register(CursorClientEvents.class);
        vanillaEventBus.register(AiCursorClientEvents.class);
        vanillaEventBus.register(AiHudClientEvents.class);


        System.out.println(RonApi.MOD_ID + ">ClientEventRegister.registerClientEvents SERVER");
        vanillaEventBus.register(ApiServerEvent.class);
        vanillaEventBus.register(TimerServerEvents.class);
        vanillaEventBus.register(AiServerEvent.class);
    }
}
