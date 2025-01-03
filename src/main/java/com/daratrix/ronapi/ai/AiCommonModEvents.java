package com.daratrix.ronapi.ai;

import com.daratrix.ronapi.RonApi;
import com.daratrix.ronapi.ai.registers.AiPacketHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = RonApi.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AiCommonModEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        System.out.println("ronapi AiCommonModEvents commonSetup ");
        event.enqueueWork(AiPacketHandler::init);
    }
}
