/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.apis;

import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IResource;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.FileLogger;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.daratrix.ronapi.utils.SpiralCounter;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.research.researchItems.*;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daratrix
 */
public class BuildingApi {

    private static final Map<Integer, ArrayList<BuildingBlock>> buildingBlocks = new HashMap<>();
    private static final Map<Integer, ArrayList<BuildingBlock>> testingBlocks = new HashMap<>();
    private static final Map<Integer, ArrayList<BuildingBlock>> foundationBlocks = new HashMap<>();
    private static final Map<Integer, Boolean> isBridge = new HashMap<>();
    private static final Map<Integer, Boolean> mustCheckNetherTerrain = new HashMap<>();
    private static final ArrayList<BlockPos> farmSpiralPositions = new ArrayList<>();
    private static final ArrayList<BlockPos> mainSpiralPositions = new ArrayList<>();
    private static final ArrayList<ArrayList<Vec2>> excentricSpiralPositions = new ArrayList<>();

    public static ArrayList<BuildingBlock> getFoundationBlocks(Level level, int typeId) {
        var blocks = foundationBlocks.getOrDefault(typeId, null);
        if (blocks != null) {
            return blocks;
        }

        assert level != null;
        var testing = getTestingBlocks(level, typeId);
        var minY = testing.stream().map(x -> x.getBlockPos().getY()).min(Integer::compareTo).orElse(0);
        blocks = getTestingBlocks(level, typeId).stream().filter(b -> b.getBlockPos().getY() == minY).collect(Collectors.toCollection(ArrayList::new));
        foundationBlocks.put(typeId, blocks);

        return blocks;
    }

    public static ArrayList<BuildingBlock> getTestingBlocks(Level level, int typeId) {
        var blocks = testingBlocks.getOrDefault(typeId, null);
        if (blocks != null) {
            return blocks;
        }

        assert level != null;
        blocks = getBuildingBlocks(level, typeId).stream().map(b -> new BuildingBlock(mutable(b.getBlockPos()), b.getBlockState())).collect(Collectors.toCollection(ArrayList::new));
        testingBlocks.put(typeId, blocks);

        return blocks;
    }

    public static ArrayList<BuildingBlock> getBuildingBlocks(Level level, int typeId) {
        var blocks = buildingBlocks.getOrDefault(typeId, null);
        if (blocks != null) {
            return blocks;
        }

        var itemName = TypeIds.toStructureName(typeId);
        assert level != null;
        blocks = BuildingBlockData.getBuildingBlocksFromNbt(itemName, level);
        buildingBlocks.put(typeId, blocks);

        return blocks;
    }

    private static boolean mustCheckNeverTerrain(int typeId, String playerName) {
        if (mustCheckNetherTerrain.containsKey(typeId)) {
            return mustCheckNetherTerrain.get(typeId);
        }

        var itemName = TypeIds.toItemName(typeId);
        boolean check = true;
        if (itemName.contains("bridge"))
            check = false;
        if (!itemName.contains("buildings.piglins.") || itemName.contains("centralportal"))
            check = false;
        if (itemName.contains("portal"))
            check = false;
        if (ResearchServerEvents.playerHasResearch(playerName, ProductionItems.RESEARCH_ADVANCED_PORTALS))
            check = false;

        mustCheckNetherTerrain.put(typeId, check);
        return check;
    }

    private static BlockPos.MutableBlockPos mutable(BlockPos pos) {
        return new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ()); // tf forge no copy constructor
    }

    private static void offsetBuildingBlocks(ArrayList<BuildingBlock> source, BlockPos offset, ArrayList<BuildingBlock> target) {
        for (int i = 0; i < source.size(); ++i) {
            var start = source.get(i).getBlockPos();
            var end = (BlockPos.MutableBlockPos) target.get(i).getBlockPos();
            end.setX(start.getX() + offset.getX());
            end.setY(start.getY() + offset.getY()); // we check above ground
            end.setZ(start.getZ() + offset.getZ());
        }
    }

    private static final SpiralCounter spiral = new SpiralCounter();

    // minimum % of blocks below a building that need to be supported by a solid block for it to be placeable
    // 1 means you can't have any gaps at all, 0 means you can place buildings in mid-air
    private static final float MIN_SUPPORTED_BLOCKS_PERCENT = 0.6f;

    // piglin buildings must be build on at least 80% nether blocks
    private static final float MIN_NETHER_BLOCKS_PERCENT = 0.8f;

    private static boolean isBuildingPlacementInvalid(Level level, ArrayList<BuildingBlock> blocksToDraw, ArrayList<BuildingBlock> foundation, boolean checkNeverTerrain) {
        if (level == null)
            return true;

        //System.out.println("isBuildingPlacementInvalid " + foundation.size() + "/" + blocksToDraw.size());

        int solidBlocksBelow = 0;
        int blocksBelow = 0;

        int netherBlocksBelow = 0;
        int nbBlocksBelow = 0;

        for (BuildingBlock block : blocksToDraw) {
            BlockPos bp = block.getBlockPos().above();
            BlockState bs = block.getBlockState(); // building block
            BlockState bsWorld = level.getBlockState(bp);

            if ((bsWorld.isSolid() || !bsWorld.getFluidState().isEmpty()) && (bs.isSolid() || !bs.getFluidState().isEmpty())) {
                //System.out.println("Skipping location due to clipping at " + bp.toShortString());
                return true; // clipping
            }

            if (level != null && foundation.contains(block)) {
                BlockState bsBelow = level.getBlockState(bp.below()); // world block
                if (bs.isSolid() && !(bsBelow.getBlock() instanceof IceBlock)) {
                    blocksBelow += 1;
                    if (bsBelow.isSolid() && !(bsBelow.getBlock() instanceof LeavesBlock)) {
                        solidBlocksBelow += 1;
                    }
                }
            }

            if (checkNeverTerrain && level != null && foundation.contains(block) && bs.isSolid()) {
                nbBlocksBelow += 1;
                if (NetherBlocks.isNetherBlock(level, bp.below())) {
                    netherBlocksBelow += 1;
                }
            }
        }

        if (blocksBelow == 0 || ((float) solidBlocksBelow / (float) blocksBelow) < MIN_SUPPORTED_BLOCKS_PERCENT) {
            //System.out.println("Skipping location due to being too uneven (" + solidBlocksBelow + "/" + blocksBelow + ")");
            return true; // too many empty blocks
        }

        if (checkNeverTerrain && (nbBlocksBelow == 0 || ((float) netherBlocksBelow / (float) blocksBelow) < MIN_NETHER_BLOCKS_PERCENT)) {
            //System.out.println("Skipping location due to not being nether terrain (" + netherBlocksBelow + "/" + blocksBelow + ")");
            return true; // too many non-nether blocks
        }

        return false;
    }

    private static final ArrayList<IResource> resources = new ArrayList<>();

    // don't allow building inside/on top of resources
    private static boolean isOverlappingResources(AABB boundingBox) {
        for (IResource r : resources) {
            if (GeometryUtils.colliding(boundingBox, r.getBoundingBox())) {
                //System.out.println("Skipping location due to colliding with resources");
                return true; // clipping at least one building
            }
        }

        return false;
    }

    private static final ArrayList<IBuilding> buildings = new ArrayList<>();

    // disallow the building borders from overlapping any other's, even if they don't collide physical blocks
    // also allow for a 1 block gap between buildings so units can spawn and stairs don't have their blockstates messed up
    private static boolean isOverlappingAnyOtherBuilding(AABB boundingBox) {
        var buildings = WorldApi.getSingleton().buildings.values();
        for (IBuilding building : buildings) {
            if (boundingBox.intersects(building.getBoundingBox())) {
                //System.out.println("Skipping location due to colliding with another building");
                return true; // clipping at least one building
            }
        }

        return false;
    }

    private static boolean isBuildingPlacementWithinWorldBorder(Level level, AABB boundingBox) {
        var aabbCenter = boundingBox.getCenter();
        var aabbSize = boundingBox.getSize();
        if (level.getWorldBorder().getDistanceToBorder(aabbCenter.x, aabbCenter.z) < aabbSize) {
            //System.out.println("Skipping location due to being out of bounds");
            return false;
        }

        return true;
    }


    public static BlockPos getBuildingLocation(Level level, BlockPos lookupOrigin, int typeId, String playerName, AiProductionPriorities.Location location, AiProductionPriorities.Proximity proximity) {
        var boxOffset = 5;
        var offset = 1;
        if (location == AiProductionPriorities.Location.FARM || location == AiProductionPriorities.Location.CAPITOL) {
            offset = 0;
        }

        var buildingBlocks = getBuildingBlocks(level, typeId);
        var checkNeverTerrain = mustCheckNeverTerrain(typeId, playerName);
        var testingBlocks = getTestingBlocks(level, typeId);

        var potentialPositions = getPotentialPositions(lookupOrigin, location, proximity);

        resources.clear();
        WorldApi.getSingleton().resources.values().stream().distinct().collect(Collectors.toCollection(() -> resources));
        buildings.clear();
        WorldApi.getSingleton().buildings.values().stream().distinct().collect(Collectors.toCollection(() -> buildings));

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos();
        GeometryUtils.setMinMaxFromBlocks(min, max, testingBlocks.stream().map(BuildingBlock::getBlockPos));
        max.move(1, 1, 1);
        var w = Math.abs(max.getX() - min.getX());
        var l = Math.abs(max.getZ() - min.getZ());
        var foundationBlocks = getFoundationBlocks(level, typeId);
        for (Vec2 potentialPosition : potentialPositions) {
            var x = potentialPosition.x;
            var signX = Math.signum(x);
            var z = potentialPosition.y;
            var signZ = Math.signum(z);
            pos.set(lookupOrigin.getX() + boxOffset * signX + x * (offset + w / 2.),
                    0,
                    lookupOrigin.getZ() + boxOffset * signZ + z * (offset + l / 2.));
            WorldApi.getTerrainHeight(level, pos); // also updates pos.setY()

            // offset buildingBlocks by pos and store the results in testingBlocks
            offsetBuildingBlocks(buildingBlocks, pos, testingBlocks);
            GeometryUtils.setMinMaxFromBlocks(min, max, testingBlocks.stream().map(BuildingBlock::getBlockPos));
            max.move(1, 1, 1);
            var boundingBox = GeometryUtils.getBoundingBox(min, max, offset);

            if (isBuildingPlacementWithinWorldBorder(level, boundingBox)
                    && !isOverlappingResources(boundingBox)
                    && !isOverlappingAnyOtherBuilding(boundingBox)
                    && !isBuildingPlacementInvalid(level, testingBlocks, foundationBlocks, checkNeverTerrain)) {
                return pos;
            }

            spiral.next();
        }

        //System.err.println("Failed to find location for " + TypeIds.toItemName(typeId));
        return null;
    }

    private static ArrayList<Vec2> getPotentialPositionsOutput;

    private static ArrayList<Vec2> getPotentialPositions(BlockPos origin, AiProductionPriorities.Location location, AiProductionPriorities.Proximity proximity) {
        getPotentialPositionsOutput.clear();
        var toCenter = new Vec2(-origin.getX(), -origin.getZ());
        switch (proximity) {
            case RANDOM -> sortRandom(toCenter);
            case FRONT -> sortFront(toCenter);
            case BACK -> sortBack(toCenter);
            case SIDE -> sortSide(toCenter);
        }

        int shift = -1;
        switch (location) {
            case ANY -> shift = 4; // start building at the edge of the farm area
            case CAPITOL -> shift = 0; // start building at the center
            case MAIN -> shift = 6;  // start building outside the farm area
            case FARM -> shift = 0;  // start building at the center
        }

        for (int i = 0; i < excentricSpiralPositions.size(); ++i) {
            getPotentialPositionsOutput.addAll(excentricSpiralPositions.get((i + shift) % excentricSpiralPositions.size()));
        }

        return getPotentialPositionsOutput;
    }

    private static void sortRandom(Vec2 toCenter) {
        var comparator = compareRandom(toCenter);
        sortPositions(comparator);
    }

    private static void sortFront(Vec2 toCenter) {
        var comparator = compareFront(toCenter);
        sortPositions(comparator);
    }

    private static void sortBack(Vec2 toCenter) {
        var comparator = compareBack(toCenter);
        sortPositions(comparator);
    }

    private static void sortSide(Vec2 toCenter) {
        var comparator = compareSide(toCenter);
        sortPositions(comparator);
    }

    private static void sortPositions(Comparator<Vec2> comparator) {
        for (ArrayList<Vec2> positions : excentricSpiralPositions) {
            positions.sort(comparator);
        }
    }

    private static Comparator<Vec2> compareRandom(Vec2 toCenter) {
        return (Vec2 a, Vec2 b) -> {
            return Integer.compare(toCenter.hashCode() * a.hashCode(), toCenter.hashCode() * b.hashCode());
        };
    }

    private static Comparator<Vec2> compareFront(Vec2 toCenter) {
        return (Vec2 a, Vec2 b) -> {
            var simA = GeometryUtils.similarity(a, toCenter);
            var simB = GeometryUtils.similarity(b, toCenter);
            return Double.compare(simA, simB);
        };
    }

    private static Comparator<Vec2> compareBack(Vec2 toCenter) {
        return (Vec2 a, Vec2 b) -> {
            var simA = -GeometryUtils.similarity(a, toCenter);
            var simB = -GeometryUtils.similarity(b, toCenter);
            return Double.compare(simA, simB);
        };
    }

    private static Comparator<Vec2> compareSide(Vec2 toCenter) {
        return (Vec2 a, Vec2 b) -> {
            var simA = -Math.abs(GeometryUtils.similarity(a, toCenter));
            var simB = -Math.abs(GeometryUtils.similarity(b, toCenter));
            return Double.compare(simA, simB);
        };
    }

    static {
        spiral.reset();
        for (int i = 0; i < 12; ++i) {
            ArrayList<Vec2> ring = new ArrayList<>();
            excentricSpiralPositions.add(ring);

            while (true) {
                var x = spiral.getX();
                var z = spiral.getZ();

                var pos = new Vec2(x, z);
                ring.add(pos);

                if (spiral.next()) {
                    break;
                }
            }
        }

        getPotentialPositionsOutput = new ArrayList<Vec2>(excentricSpiralPositions.stream().map(ArrayList::size).reduce(Integer::sum).get());
    }
}
