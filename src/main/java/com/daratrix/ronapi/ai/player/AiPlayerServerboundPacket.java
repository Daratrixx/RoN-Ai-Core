package com.daratrix.ronapi.ai.player;

import com.daratrix.ronapi.ai.registers.AiPacketHandler;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.player.PlayerAction;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AiPlayerServerboundPacket {
    PlayerAction action;
    public double x;
    public double y;
    public double z;
    public String aiName;

    public static void startRTSBot(Faction faction, String aiName, Double x, Double y, Double z) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null && MC.level != null) {
            BlockState bs = MC.level.getBlockState(new BlockPos(x,y,z));
            if (bs.getMaterial().isLiquid()) {
                HudClientEvents.showTemporaryMessage("Invalid starting location");
                return;
            }

            PlayerAction playerAction = switch (faction) {
                case VILLAGERS -> PlayerAction.START_RTS_VILLAGERS;
                case MONSTERS -> PlayerAction.START_RTS_MONSTERS;
                case PIGLINS -> PlayerAction.START_RTS_PIGLINS;
                case NONE -> null;
            };

            AiPacketHandler.INSTANCE.sendToServer(new AiPlayerServerboundPacket(playerAction, x, y, z, aiName));
        }

    }

    // packet-handler functions
    public AiPlayerServerboundPacket(PlayerAction action, Double x, Double y, Double z, String aiName) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.aiName = aiName;
    }

    public AiPlayerServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(PlayerAction.class);
        this.x = buffer.readDouble();
        this.y = buffer.readDouble();
        this.z = buffer.readDouble();
        this.aiName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
        buffer.writeUtf(this.aiName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        AtomicBoolean success = new AtomicBoolean(false);
        //this.aiName = this.action.name();
        ((NetworkEvent.Context)ctx.get()).enqueueWork(() -> {
            switch (this.action) {
                case START_RTS_VILLAGERS -> AiPlayerServerEvents.startRTSBot(this.aiName, new Vec3(this.x, this.y, this.z), Faction.VILLAGERS);
                case START_RTS_MONSTERS -> AiPlayerServerEvents.startRTSBot(this.aiName, new Vec3(this.x, this.y, this.z), Faction.MONSTERS);
                case START_RTS_PIGLINS -> AiPlayerServerEvents.startRTSBot(this.aiName, new Vec3(this.x, this.y, this.z), Faction.PIGLINS);
            }

            success.set(true);
        });
        ((NetworkEvent.Context)ctx.get()).setPacketHandled(true);
        return success.get();
    }
}
