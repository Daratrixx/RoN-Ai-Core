package com.daratrix.ronapi.models;

import com.daratrix.ronapi.ai.player.AiPlayerServerEvents;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSource;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiWorld {

    private static final Minecraft MC = Minecraft.getInstance();

    public final Map<String, ApiPlayer> players = new HashMap<>();
    public final Map<Unit, ApiUnit> units = new HashMap<>();
    public final Map<Building, ApiBuilding> buildings = new HashMap<>();
    public final Map<BlockPos, ApiResource> resources = new HashMap<>();

    public void reset() {
        for (ApiPlayer p : players.values()) {
            p.reset();
        }

        players.clear();
        units.clear();
        buildings.clear();
        //resources.clear();
    }


    public void updateTracking(MinecraftServer server) {
        if (MC.level == null) {
            return; // wait for the server to be ready...
        }

        synchronized (this) {
            for (var p : PlayerServerEvents.rtsPlayers) {
                track(server, p);
            }

            var allUnits = UnitServerEvents.getAllUnits();
            for (var u : allUnits) {
                if (u.isAlive() && u instanceof Unit unit) {
                    track(server, unit);
                }
            }

            var allBuildings = BuildingServerEvents.getBuildings();
            for (var b : allBuildings) {
                if (b.getHealth() > 0) {
                    track(server, b);
                }
            }

            // cleanup tracked units/buildings
            this.cleanupDead(server);
        }
    }

    private static final ArrayList<Map.Entry<Unit, ApiUnit>> tempUnits = new ArrayList<>(1000);
    private static final ArrayList<Map.Entry<Building, ApiBuilding>> tempBuildings = new ArrayList<>(1000);

    private void cleanupDead(MinecraftServer server) {
        var deadUnits = this.units.entrySet();
        for (Map.Entry<Unit, ApiUnit> x : deadUnits) {
            if (x.getValue().isDead()) {
                tempUnits.add(x);
            }
        }
        tempUnits.forEach(x -> this.untrack(server, x.getKey()));
        tempUnits.clear();

        var deadBuildings = this.buildings.entrySet();
        for (Map.Entry<Building, ApiBuilding> x : deadBuildings) {
            if (x.getValue().isDead()) {
                tempBuildings.add(x);
            }
        }
        tempBuildings.forEach(x -> this.untrack(server, x.getKey()));
        tempBuildings.clear();

        var level = server.overworld();

        try {
            // make sure the resources amount are accurate, and remove depleted resource nodes
            var depletedResourceBlocks = this.resources.keySet().stream().distinct().filter(b -> ResourceSources.getFromBlockPos(b, level) == null).collect(Collectors.toCollection(ArrayList::new));
            depletedResourceBlocks.forEach(this::untrack);
        } catch (Exception e) {
            // no clue how to not run code when the level is not ready to be scanned...
        }
    }

    public ApiPlayer track(MinecraftServer server, String playerName) {
        var apiPlayer = players.getOrDefault(playerName, null);
        if (apiPlayer != null) {
            return apiPlayer; // already tracked
        }

        var rtsPlayer = PlayerServerEvents.rtsPlayers.stream().filter(x -> x.name == playerName).findFirst();
        return rtsPlayer.map(p -> this.track(server, p)).orElse(null);
    }

    private ApiPlayer track(MinecraftServer server, RTSPlayer rtsPlayer) {
        var apiPlayer = players.getOrDefault(rtsPlayer.name, null);
        if (apiPlayer != null) {
            return apiPlayer;
        }

        if (rtsPlayer.isBot()) {
            apiPlayer = AiPlayerServerEvents.restoreAiPlayer(server, rtsPlayer);
            players.put(rtsPlayer.name, apiPlayer);
            return apiPlayer;
        }

        apiPlayer = new ApiPlayer(rtsPlayer);
        players.put(rtsPlayer.name, apiPlayer);

        return apiPlayer;
    }

    public ApiPlayer track(MinecraftServer server, ApiPlayer apiPlayer) {
        players.put(apiPlayer.name, apiPlayer);

        return apiPlayer;
    }

    public ApiUnit track(MinecraftServer server, Unit unit) {
        var apiUnit = units.getOrDefault(unit, null);
        if (apiUnit != null) {
            return apiUnit; // already tracked
        }

        apiUnit = new ApiUnit(unit);
        units.put(unit, apiUnit);
        //System.out.println("Tracking unit " + apiUnit.getName() + " (" + apiUnit.getOwnerName() + ")");

        var apiPlayer = track(server, unit.getOwnerName());
        if (apiPlayer != null) {
            apiPlayer.track(apiUnit);
        }

        return apiUnit;
    }

    public ApiBuilding track(MinecraftServer server, Building building) {
        var apiBuilding = buildings.getOrDefault(building, null);
        if (apiBuilding != null) {
            return apiBuilding; // already tracked
        }

        apiBuilding = new ApiBuilding(building);
        buildings.put(building, apiBuilding);
        //System.out.println("Tracking building " + apiBuilding.getName() + " (" + apiBuilding.getOwnerName() + ")");

        var apiPlayer = track(server, building.ownerName);
        if (apiPlayer != null) {
            apiPlayer.track(apiBuilding);
        }

        return apiBuilding;
    }

    private void untrack(MinecraftServer server, Unit unit) {
        var tracked = units.getOrDefault(unit, null);
        if (tracked == null) {
            return; // not tracked
        }

        ApiPlayer player = track(server, unit.getOwnerName());
        if (player != null) {
            player.untrack(tracked);
        }

        units.remove(unit);
    }

    private void untrack(MinecraftServer server, Building building) {
        var tracked = buildings.getOrDefault(building, null);
        if (tracked == null) {
            return; // not tracked
        }

        ApiPlayer player = track(server, building.ownerName);
        if (player != null) {
            player.untrack(tracked);
        }

        buildings.remove(building);
    }

    private void untrack(BlockPos pos) {
        var tracked = resources.getOrDefault(pos, null);
        if (tracked == null) {
            return; // not tracked
        }

        tracked.untrack(MC.level, pos);
        resources.remove(pos);
    }

    private ApiResource track(BlockPos pos, ResourceName resource) {
        synchronized (resources) {
            if (resource == ResourceName.NONE) {
                return null; // nothing to track
            }

            if (BuildingUtils.isPosInsideAnyBuilding(false, pos)) {
                return null;
            }

            var tracked = resources.getOrDefault(pos, null);
            if (tracked != null) {
                //System.out.println("Updated tracked resource");
                //tracked.updateValueAndBoundingBox(MC.level);
                return tracked; // already tracked
            }

            var output = new ApiResource(resource);
            output.track(MC.level, pos);
            resources.put(pos, output);
            trackAdjacentTo(pos, output);
            return output;
        }
    }

    private void trackAdjacentTo(BlockPos pos, ApiResource resource) {
        var adjacentPositions = Stream.of(pos.above(), pos.below(), pos.east(), pos.west(), pos.south(), pos.north()).toList();
        for (BlockPos adjPosition : adjacentPositions) {
            trackAdjacent(adjPosition, resource);
        }
    }

    private void trackAdjacent(BlockPos pos, ApiResource resource) {
        var existing = resources.getOrDefault(pos, null);
        if (existing != null) {
            return;
        }

        ResourceSource resourceSource = ResourceSources.getFromBlockPos(pos, MC.level);
        if (resourceSource == null) {
            return;
        }

        var resourceName = resourceSource.resourceName;
        if (resourceName != resource.getResourceType()) {
            return;
        }

        resource.track(MC.level, pos);
        resources.put(pos, resource);
        trackAdjacentTo(pos, resource);
    }

    public boolean scanChunk(MinecraftServer server, int x, int z) {
        try {
            assert server != null;
            var overworld = server.overworld();
            var chunk = server.overworld().getChunk(x, z);

            //var status = chunk.getStatus();
            if (chunk.isEmpty()) {
                overworld.setChunkForced(x, z, true);
                return false; // stop scanning for this tick
            }

            if (!overworld.hasChunk(x, z)) {
                return false; // stop scanning for this tick
            }

            for (int i = 0; i < 16 * 16; ++i) {
                var pos = new BlockPos.MutableBlockPos();
                var x0 = i % 16;
                var z0 = i / 16;
                //pos.setX(x0);
                //pos.setZ(z0);
                //var y = getTerrainHeight(chunk, pos);
                pos.setX(x0 + x * 16);
                pos.setZ(z0 + z * 16);
                var y = getTerrainHeight(server, pos, true);
                var topBlocKState = overworld.getBlockState(pos);
                if (y > -64 && topBlocKState.isAir()) {
                    return false; // no loaded
                }

                ResourceSource resourceSource;
                while ((resourceSource = ResourceSources.getFromBlockPos(pos, overworld)) != null) {
                    var resourceName = resourceSource.resourceName;
                    track(pos.immutable(), resourceName);

                    // we repeat the process downward to find entire stacks of resources, not just the tip
                    pos.setY(--y);
                }
            }

            overworld.setChunkForced(x, z, false);

            return true;
        } catch (Exception e) {
            //System.err.println(e.getMessage());
            //e.printStackTrace(System.err);
            return false;
        }
    }

    public int getTerrainHeight(MinecraftServer server, BlockPos.MutableBlockPos pos, boolean allowLeaves) {
        var type = allowLeaves ? Heightmap.Types.MOTION_BLOCKING : Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        var y = server.overworld().getHeight(type, pos.getX(), pos.getZ()) - 1;
        pos.setY(y);
        return y;
    }

    public int getTerrainHeight(ChunkAccess chunk, BlockPos.MutableBlockPos pos) {
        int y = 96;

        while (y >= 64) {
            pos.setY(y);
            var block = chunk.getBlockState(pos);
            if (!block.isAir() && (block.getMaterial().isSolidBlocking() || block.getBlock() instanceof LeavesBlock)) {
                //if (!block.is(Blocks.SAND) && !block.is(Blocks.GRASS_BLOCK)) {
                //System.out.println(block.getBlock().getName().toString() + " at " + (pos.getX()) + ", " + (y) + ", " + (pos.getZ()));
                //}
                break;
            }
            --y;
        }

        return y;
    }
}
