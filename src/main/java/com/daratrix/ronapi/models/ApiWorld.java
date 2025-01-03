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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.chunk.ChunkAccess;

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
    private int lastChunkX = 0;
    private int lastChunkZ = 0;
    private int scannedChunks = 0;

    public int getScannedChunksCount() {
        return this.scannedChunks;
    }

    public int getLastChunkX() {
        return this.lastChunkX;
    }

    public int getLastChunkZ() {
        return this.lastChunkZ;
    }

    public void reset() {
        this.lastChunkX = 0;
        this.lastChunkZ = 0;
        this.scannedChunks = 0;

        for (ApiPlayer p : players.values()) {
            p.reset();
        }

        players.clear();
        units.clear();
        buildings.clear();
        resources.clear();
    }


    public void updateTracking() {
        synchronized (this) {
            for (var p : PlayerServerEvents.rtsPlayers) {
                track(p);
            }

            var allUnits = UnitServerEvents.getAllUnits();
            for (var u : allUnits) {
                if (u.isAlive() && u instanceof Unit unit) {
                    var apiUnit = track(unit);
                    //if (apiUnit != null && apiUnit.isDead()) {
                    //    untrack(unit);
                    //}
                }
            }

            var allBuildings = BuildingServerEvents.getBuildings();
            for (var b : allBuildings) {
                if (b.getHealth() > 0) {
                    var apiBuilding = track(b);
                    //if (apiBuilding != null && apiBuilding.isDead()) {
                    //    untrack(b);
                    //}
                }
            }

            // cleanup tracked units/buildings
            this.cleanupDead();
        }
    }

    private static final ArrayList<Map.Entry<Unit, ApiUnit>> tempUnits = new ArrayList<>(1000);
    private static final ArrayList<Map.Entry<Building, ApiBuilding>> tempBuildings = new ArrayList<>(1000);

    private void cleanupDead() {
        var deadUnits = this.units.entrySet();
        for (Map.Entry<Unit, ApiUnit> x : deadUnits) {
            if (x.getValue().isDead()) {
                tempUnits.add(x);
            }
        }
        tempUnits.forEach(x -> this.untrack(x.getKey()));

        var deadBuildings = this.buildings.entrySet();
        for (Map.Entry<Building, ApiBuilding> x : deadBuildings) {
            if (x.getValue().isDead()) {
                tempBuildings.add(x);
            }
        }
        tempBuildings.forEach(x -> this.untrack(x.getKey()));

        if (MC.level == null) {
            return;
        }

        try {
            // make sure the resources amount are accurate, and remove depleted resource nodes
            var depletedResourceBlocks = this.resources.keySet().stream().distinct().filter(b -> ResourceSources.getFromBlockPos(b, MC.level) == null).collect(Collectors.toCollection(ArrayList::new));
            depletedResourceBlocks.forEach(this::untrack);
        } catch (Exception e) {
            // no clue how to not run code when the level is not ready to be scanned...
        }
    }

    public ApiPlayer track(String playerName) {
        var apiPlayer = players.getOrDefault(playerName, null);
        if (apiPlayer != null) {
            return apiPlayer; // already tracked
        }

        var rtsPlayer = PlayerServerEvents.rtsPlayers.stream().filter(x -> x.name == playerName).findFirst();
        return rtsPlayer.map(this::track).orElse(null);
    }

    private ApiPlayer track(RTSPlayer rtsPlayer) {
        var apiPlayer = players.getOrDefault(rtsPlayer.name, null);
        if (apiPlayer != null) {
            return apiPlayer;
        }

        if (rtsPlayer.isBot()) {
            apiPlayer = AiPlayerServerEvents.restoreAiPlayer(rtsPlayer);
            players.put(rtsPlayer.name, apiPlayer);
            return apiPlayer;
        }

        apiPlayer = new ApiPlayer(rtsPlayer);
        players.put(rtsPlayer.name, apiPlayer);

        return apiPlayer;
    }

    public ApiPlayer track(ApiPlayer apiPlayer) {
        players.put(apiPlayer.name, apiPlayer);

        return apiPlayer;
    }

    public ApiUnit track(Unit unit) {
        var apiUnit = units.getOrDefault(unit, null);
        if (apiUnit != null) {
            return apiUnit; // already tracked
        }

        apiUnit = new ApiUnit(unit);
        units.put(unit, apiUnit);
        //System.out.println("Tracking unit " + apiUnit.getName() + " (" + apiUnit.getOwnerName() + ")");

        var apiPlayer = track(unit.getOwnerName());
        if (apiPlayer != null) {
            apiPlayer.track(apiUnit);
        }

        return apiUnit;
    }

    public ApiBuilding track(Building building) {
        var apiBuilding = buildings.getOrDefault(building, null);
        if (apiBuilding != null) {
            return apiBuilding; // already tracked
        }

        apiBuilding = new ApiBuilding(building);
        buildings.put(building, apiBuilding);
        //System.out.println("Tracking building " + apiBuilding.getName() + " (" + apiBuilding.getOwnerName() + ")");

        var apiPlayer = track(building.ownerName);
        if (apiPlayer != null) {
            apiPlayer.track(apiBuilding);
        }

        return apiBuilding;
    }

    private void untrack(Unit unit) {
        var tracked = units.getOrDefault(unit, null);
        if (tracked == null) {
            return; // not tracked
        }

        ApiPlayer player = track(unit.getOwnerName());
        if (player != null) {
            player.untrack(tracked);
        }

        units.remove(unit);
    }

    private void untrack(Building building) {
        var tracked = buildings.getOrDefault(building, null);
        if (tracked == null) {
            return; // not tracked
        }

        ApiPlayer player = track(building.ownerName);
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

            /*var adjacentPos = Stream.of(pos.above(), pos.below(), pos.east(), pos.west(), pos.south(), pos.north()).toList();
            var adjacentResources = resources.entrySet().stream()
                    .filter(x -> x.getValue().getResourceType() == resource)
                    .filter(x -> adjacentPos.stream().anyMatch(y -> y.equals(x.getKey())))
                    .map(Map.Entry::getValue)
                    .distinct()
                    .toList();
            if (!adjacentResources.isEmpty()) {
                //System.out.println("Added to existing resource");
                var existing = adjacentResources.get(0);
                existing.track(MC.level, pos);
                resources.put(pos, existing);

                for (var merging : adjacentResources) {
                    if (merging != existing) {
                        //System.out.println("Merging adjacent resources");
                        var blocksToMerge = merging.getBlocks().toList();
                        for (var mergingPos : blocksToMerge) {
                            existing.track(MC.level, mergingPos);
                            resources.put(mergingPos, existing);
                        }
                    }
                }

                return existing;
            }
            */
            //System.out.println("Tracking new resource");
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

    public boolean scanNextChunk() {
        if (this.scanChunk(this.scannedChunks)) {
            this.scannedChunks++;
            //System.out.println("scanNextChunk success, next chunk is " + scannedChunks);
            return true;
        }

        return false;
    }

    public boolean scanChunk(int n) {
        if (n < this.scannedChunks) {
            //System.out.println("scanChunk skipped, already scanned " + scannedChunks);
            return false; // already scanned
        }

        if (n == 0) {
            return this.scanChunk(0, 0);
        }

        int s = getChunkS(n);
        n = getChunkN(n, s);
        return scanChunk(getChunkX(n, s), getChunkZ(n, s));
    }

    public static int getChunkS(int n) {
        int s = 1;
        while (n >= s * s) {
            s += 2;
        }

        return s;
    }

    public static int getChunkN(int n, int s) {
        return n - (s - 2) * (s - 2);
    }

    public static int getChunkX(int n, int s) {
        var s2 = s / 2;
        int side = n / (s - 1);
        return switch (side) {
            case 0 -> (n % (s - 1)) - s2;
            case 1 -> s2;
            case 2 -> s2 - (n % (s - 1));
            default -> -s2;
        };
    }

    public static int getChunkZ(int n, int s) {
        var s2 = s / 2;
        int side = n / (s - 1);
        return switch (side) {
            case 0 -> s2;
            case 1 -> s2 - (n % (s - 1));
            case 2 -> -s2;
            default -> (n % (s - 1)) - s2;
        };
    }

    public boolean scanChunk(int x, int z) {
        try {
            this.lastChunkX = x;
            this.lastChunkZ = z;
            //System.out.println("scanChunk " + x + ", " + z + "(" + this.scannedChunks + ")");
            assert MC.level != null;
            var chunk = MC.level.getChunk(x, z);

            if (chunk.isEmpty()) {
                return false;
            }

            for (int i = 0; i < 16 * 16; ++i) {
                var pos = new BlockPos.MutableBlockPos();
                var x0 = i % 16;
                var z0 = i / 16;
                pos.setX(x0);
                pos.setZ(z0);
                var y = getTerrainHeight(chunk, pos);

                // for resource tracking, account for chunk offset, otherwise we always read in the 0,0 chunk
                pos.setX(x0 + x * 16);
                pos.setZ(z0 + z * 16);

                ResourceSource resourceSource;
                while ((resourceSource = ResourceSources.getFromBlockPos(pos, MC.level)) != null) {
                    var resourceName = resourceSource.resourceName;
                    //if(resourceSource.resourceValue == 1) {
                    //    System.out.println("found " + resourceName.name() + " resource at " + (pos.getX()) + ", " + (y) + ", " + (pos.getZ()));
                    //}
                    track(pos.immutable(), resourceName);

                    // we repeat the process downward to find entire stacks of resources, not just the tip
                    pos.setY(--y);
                }
            }

            return true;
        } catch (Exception e) {
            //System.err.println(e.getMessage());
            //e.printStackTrace(System.err);
            return false;
        }
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
