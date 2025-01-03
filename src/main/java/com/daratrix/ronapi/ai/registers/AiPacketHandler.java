package com.daratrix.ronapi.ai.registers;

import com.daratrix.ronapi.RonApi;
import com.daratrix.ronapi.ai.player.AiPlayerServerboundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class AiPacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RonApi.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void init() {
        int index = 22; // hard-coded, not ideal...

        INSTANCE.messageBuilder(AiPlayerServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AiPlayerServerboundPacket::encode)
                .decoder(AiPlayerServerboundPacket::new)
                .consumer(AiPlayerServerboundPacket::handle)
                .add();
    }
}
