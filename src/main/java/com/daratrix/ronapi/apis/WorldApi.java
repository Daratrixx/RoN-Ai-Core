package com.daratrix.ronapi.apis;

import com.daratrix.ronapi.models.ApiWorld;
import com.daratrix.ronapi.models.interfaces.IPlayer;
import com.daratrix.ronapi.models.interfaces.IPlayerWidget;
import com.daratrix.ronapi.models.interfaces.IResource;
import com.daratrix.ronapi.models.interfaces.IStructure;
import com.daratrix.ronapi.timer.Timer;
import com.daratrix.ronapi.timer.TimerServerEvents;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.alliance.AllianceSystem;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldApi {

    private static final Minecraft MC = Minecraft.getInstance();
    private static ApiWorld singleton = null;

    public static ApiWorld getSingleton() {
        if (singleton == null) {
            System.out.println("Created ApiWorld singleton");
            singleton = new ApiWorld();
        }
        return singleton;
    }

    private static final Timer gridScanTimer = TimerServerEvents.queueTimerLooping(0, WorldApi::gridScanStep).pause();
    private static boolean gridScanRunning = false;
    private static int gridScanSize;
    private static int gridScanProgress;
    private static int gridScanTarget;

    public static void startGridScan(int size) {
        synchronized (gridScanTimer) {
            if (gridScanRunning) {
                System.err.println("Grid scan is already running!");
                return;
            }

            getSingleton().resources.clear();
            gridScanRunning = true;
            gridScanSize = size;
            gridScanTarget = (gridScanSize * 2) * (gridScanSize * 2);
            gridScanProgress = 0;
            System.out.println("Starting grid scan with size: " + gridScanSize + "(" + gridScanTarget + " chunks to scan)");
            gridScanTimer.unpause();
        }
    }

    public static void gridScanStep(float elasped) {
        if (gridScanProgress == gridScanTarget) {
            System.out.println("Grid scan completed! (size: " + gridScanSize + ")");
            var server = MC.getSingleplayerServer();
            if (server != null) {
                server.getPlayerList().broadcastSystemMessage(Component.literal("Grid scan completed! (size: " + gridScanSize + ")"), false);
            }
            gridScanRunning = false;
            gridScanTimer.pause();
            return;
        }

        if (gridScanProgress % 10 == 0) {
            System.out.printf("%d %% scanned...\r\n", (100 * gridScanProgress / gridScanTarget));
        }

        var x = gridScanProgress % (gridScanSize * 2);
        var z = gridScanProgress / (gridScanSize * 2);
        x -= gridScanSize;
        z -= gridScanSize;

        getSingleton().scanChunk(x, z);
        ++gridScanProgress;
    }

    public static void scanChunks(float elaspedTime) {
        //System.out.println("scanChunks (s" + elaspedTime + ")");
        getSingleton().scanNextChunk();
    }

    private static Timer queuedWorldUpdate = null;

    public static void queueWorldUpdate() {
        if (queuedWorldUpdate == null) {
            queuedWorldUpdate = TimerServerEvents.queueTimer(0, WorldApi::updateWorld); // queue a world update ASAP
        }
    }

    public static void updateWorld(float elaspedTime) {
        //System.out.println("updateWorld (s" + elaspedTime + ")");
        getSingleton().updateTracking();
        TimerServerEvents.destroyTimer(queuedWorldUpdate);
        queuedWorldUpdate = null;
    }

    public static boolean hasResourcesWithinDistance(IStructure w, float distance) {
        return getSingleton().resources.values().stream().anyMatch(r -> GeometryUtils.isWithinDistance(r, w, distance));
    }

    public static boolean hasResourcesWithinDistanceOfType(IStructure w, float distance, int type) {
        return getSingleton().resources.values().stream().filter(r -> type == r.getTypeId()).anyMatch(r -> GeometryUtils.isWithinDistance(r, w, distance));
    }

    public static boolean hasResourcesWithinDistanceOfType(IStructure w, float distance, int... types) {
        return getSingleton().resources.values().stream().filter(r -> Arrays.stream(types).anyMatch(t -> t == r.getTypeId())).anyMatch(r -> GeometryUtils.isWithinDistance(r, w, distance));
    }

    public static IResource getResourceAt(BlockPos pos) {
        return getSingleton().resources.getOrDefault(pos, null);
    }

    public static int getTerrainHeight(BlockPos.MutableBlockPos pos) {
        if (MC.level == null) {
            return 0;
        }

        return getTerrainHeight(MC.level.getChunkAt(pos), pos);
    }

    public static int getTerrainHeight(ChunkAccess chunk, BlockPos.MutableBlockPos pos) {
        int y = 96;

        while (y >= 64) {
            pos.setY(y);
            var block = chunk.getBlockState(pos);
            if (!block.isAir() && block.getMaterial().isSolidBlocking()) {
                if (!block.is(Blocks.SAND) && !block.is(Blocks.GRASS_BLOCK)) {
                    //System.out.println(block.getBlock().getName().toString() + " at " + (pos.getX()) + ", " + (y) + ", " + (pos.getZ()));
                }
                break;
            }
            --y;
        }

        return y;
    }

    public static boolean playerCanAfford(String ownerName, ResourceCost itemCost) {
        IPlayer player = getSingleton().players.getOrDefault(ownerName, null);
        if (player == null) {
            return false;
        }

        return player.canAfford(itemCost);
    }

    public static Stream<IPlayer> getPlayerEnemies(IPlayer p) {
        return singleton.players.values().stream().filter(x -> x != p && !AllianceSystem.isAllied(x.getName(), p.getName())).map(x -> (IPlayer) x);
    }

    public static Stream<IPlayer> getPlayerEnemies(String playerName) {
        return singleton.players.values().stream().filter(x -> x.getName() != playerName && !AllianceSystem.isAllied(x.getName(), playerName)).map(x -> (IPlayer) x);
    }

    private static String getThreatsPlayerName = null;

    private static boolean getThreatsPredicate(LivingEntity e) {
        if (e instanceof Unit u) {
            var owner = u.getOwnerName();
            return !Objects.equals(owner, getThreatsPlayerName) && !AllianceSystem.isAllied(getThreatsPlayerName, u.getOwnerName());
        }
        return false;
    }

    public static void findThreats(AABB boundingBox, String playerName, Collection<IPlayerWidget> outputThreats) {
        var enemies = getPlayerEnemies(playerName);
        enemies.forEach(p -> {
            p.getUnitsFiltered(u -> !u.isWorker()).filter(u -> GeometryUtils.contains(boundingBox, u.getPos())).collect(Collectors.toCollection(() -> outputThreats));
        });
    }

    public static boolean isNearPlayerBases(IPlayerWidget defenseTarget, IPlayer player, float distance) {
        return player.getUnitsFiltered(b -> GeometryUtils.isWithinDistance(b, defenseTarget, distance)).findAny().isPresent();
    }
}
