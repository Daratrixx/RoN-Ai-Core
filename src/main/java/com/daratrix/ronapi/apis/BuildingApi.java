/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.apis;

import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IResource;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.daratrix.ronapi.utils.SpiralCounter;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.research.researchItems.*;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Daratrix
 */
public class BuildingApi {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final Map<Integer, ArrayList<BuildingBlock>> buildingBlocks = new HashMap<>();
    private static final Map<Integer, ArrayList<BuildingBlock>> testingBlocks = new HashMap<>();
    private static final Map<Integer, ArrayList<BuildingBlock>> foundationBlocks = new HashMap<>();
    private static final Map<Integer, Boolean> isBridge = new HashMap<>();
    private static final Map<Integer, Boolean> mustCheckNetherTerrain = new HashMap<>();

    public static ProductionItem toProductionItem(int typeId, ProductionBuilding building) {
        var itemName = TypeIds.toItemName(typeId);
        return toProductionItem(itemName, building);
    }

    public static ProductionItem toProductionItem(String itemName, ProductionBuilding building) {
        return switch (itemName) {
            case CreeperProd.itemName -> new CreeperProd(building);
            case SkeletonProd.itemName -> new SkeletonProd(building);
            case ZombieProd.itemName -> new ZombieProd(building);
            case StrayProd.itemName -> new StrayProd(building);
            case HuskProd.itemName -> new HuskProd(building);
            case DrownedProd.itemName -> new DrownedProd(building);
            case SpiderProd.itemName -> new SpiderProd(building);
            case PoisonSpiderProd.itemName -> new PoisonSpiderProd(building);
            case VillagerProd.itemName -> new VillagerProd(building);
            case ZombieVillagerProd.itemName -> new ZombieVillagerProd(building);
            case VindicatorProd.itemName -> new VindicatorProd(building);
            case PillagerProd.itemName -> new PillagerProd(building);
            case IronGolemProd.itemName -> new IronGolemProd(building);
            case WitchProd.itemName -> new WitchProd(building);
            case EvokerProd.itemName -> new EvokerProd(building);
            case WardenProd.itemName -> new WardenProd(building);
            case RavagerProd.itemName -> new RavagerProd(building);

            case GruntProd.itemName -> new GruntProd(building);
            case BruteProd.itemName -> new BruteProd(building);
            case HeadhunterProd.itemName -> new HeadhunterProd(building);
            case HoglinProd.itemName -> new HoglinProd(building);
            case BlazeProd.itemName -> new BlazeProd(building);
            case WitherSkeletonProd.itemName -> new WitherSkeletonProd(building);
            case GhastProd.itemName -> new GhastProd(building);

            case ResearchVindicatorAxes.itemName -> new ResearchVindicatorAxes(building);
            case ResearchPillagerCrossbows.itemName -> new ResearchPillagerCrossbows(building);
            case ResearchLabLightningRod.itemName -> new ResearchLabLightningRod(building);
            case ResearchResourceCapacity.itemName -> new ResearchResourceCapacity(building);
            case ResearchSpiderJockeys.itemName -> new ResearchSpiderJockeys(building);
            case ResearchPoisonSpiders.itemName -> new ResearchPoisonSpiders(building);
            case ResearchHusks.itemName -> new ResearchHusks(building);
            case ResearchDrowned.itemName -> new ResearchDrowned(building);
            case ResearchStrays.itemName -> new ResearchStrays(building);
            case ResearchLingeringPotions.itemName -> new ResearchLingeringPotions(building);
            case ResearchEvokerVexes.itemName -> new ResearchEvokerVexes(building);
            case ResearchGolemSmithing.itemName -> new ResearchGolemSmithing(building);
            case ResearchSilverfish.itemName -> new ResearchSilverfish(building);
            case ResearchSculkAmplifiers.itemName -> new ResearchSculkAmplifiers(building);
            case ResearchCastleFlag.itemName -> new ResearchCastleFlag(building);
            case ResearchRavagerCavalry.itemName -> new ResearchRavagerCavalry(building);
            case ResearchBruteShields.itemName -> new ResearchBruteShields(building);
            case ResearchHoglinCavalry.itemName -> new ResearchHoglinCavalry(building);
            case ResearchHeavyTridents.itemName -> new ResearchHeavyTridents(building);
            case ResearchBlazeFirewall.itemName -> new ResearchBlazeFirewall(building);
            case ResearchWitherClouds.itemName -> new ResearchWitherClouds(building);
            case ResearchAdvancedPortals.itemName -> new ResearchAdvancedPortals(building);
            case ResearchFireResistance.itemName -> new ResearchFireResistance(building);

            case ResearchPortalForCivilian.itemName -> new ResearchPortalForCivilian(building);
            case ResearchPortalForMilitary.itemName -> new ResearchPortalForMilitary(building);
            case ResearchPortalForTransport.itemName -> new ResearchPortalForTransport(building);
            default -> null;
        };
    }

    public static ArrayList<BuildingBlock> getFoundationBlocks(int typeId) {
        var blocks = foundationBlocks.getOrDefault(typeId, null);
        if (blocks != null) {
            return blocks;
        }

        assert MC.level != null;
        var testing = getTestingBlocks(typeId);
        var minY = testing.stream().map(x -> x.getBlockPos().getY()).min(Integer::compareTo).orElse(0);
        blocks = getTestingBlocks(typeId).stream().filter(b -> b.getBlockPos().getY() == minY).collect(Collectors.toCollection(ArrayList::new));
        foundationBlocks.put(typeId, blocks);

        return blocks;
    }

    public static ArrayList<BuildingBlock> getTestingBlocks(int typeId) {
        var blocks = testingBlocks.getOrDefault(typeId, null);
        if (blocks != null) {
            return blocks;
        }

        assert MC.level != null;
        blocks = getBuildingBlocks(typeId).stream().map(b -> new BuildingBlock(mutable(b.getBlockPos()), b.getBlockState())).collect(Collectors.toCollection(ArrayList::new));
        testingBlocks.put(typeId, blocks);

        return blocks;
    }

    public static ArrayList<BuildingBlock> getBuildingBlocks(int typeId) {
        var blocks = buildingBlocks.getOrDefault(typeId, null);
        if (blocks != null) {
            return blocks;
        }

        var itemName = TypeIds.toStructureName(typeId);
        assert MC.level != null;
        blocks = BuildingBlockData.getBuildingBlocks(itemName, MC.level);
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
        if (ResearchServerEvents.playerHasResearch(playerName, ResearchAdvancedPortals.itemName))
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

    public static BlockPos getBuildingLocation(BlockPos lookupOrigin, int typeId, String playerName) {
        return getBuildingLocation(lookupOrigin, typeId, playerName, 0, 10, 10, 2);
    }

    public static BlockPos getBuildingLocation(BlockPos lookupOrigin, int typeId, String playerName, int spiralMin, int spiralMax, int boxOffset, int offset) {
        spiral.reset();
        if (spiralMin > 0) {
            spiral.skipS(spiralMin);
        }

        var buildingBlocks = getBuildingBlocks(typeId);
        var checkNeverTerrain = mustCheckNeverTerrain(typeId, playerName);
        var testingBlocks = getTestingBlocks(typeId);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos();
        GeometryUtils.setMinMaxFromBlocks(min, max, testingBlocks.stream().map(BuildingBlock::getBlockPos));
        max.move(1, 1, 1);
        var w = Math.abs(max.getX() - min.getX());
        var l = Math.abs(max.getZ() - min.getZ());
        var foundationBlocks = getFoundationBlocks(typeId);
        while (spiral.getS() < spiralMax * 2) {
            var spiralX = spiral.getX();
            var signX = Math.signum(spiralX);
            var spiralZ = spiral.getZ();
            var signZ = Math.signum(spiralZ);
            pos.set(lookupOrigin.getX() + boxOffset * signX + (spiralX * offset) + (spiralX * w / 2.),
                    0,
                    lookupOrigin.getZ() + boxOffset * signZ + (spiralZ * offset) + (spiralZ * l / 2.));
            WorldApi.getTerrainHeight(pos); // also updates pos.setY()
            // offset buildingBlocks by pos and store the results in testingBlocks
            offsetBuildingBlocks(buildingBlocks, pos, testingBlocks);
            GeometryUtils.setMinMaxFromBlocks(min, max, testingBlocks.stream().map(BuildingBlock::getBlockPos));
            max.move(1, 1, 1);
            var boundingBox = GeometryUtils.getBoundingBox(min, max, offset);

            if (isBuildingPlacementWithinWorldBorder(boundingBox)
                    && !isOverlappingResources(boundingBox)
                    && !isOverlappingAnyOtherBuilding(boundingBox)
                    && !isBuildingPlacementInvalid(testingBlocks, foundationBlocks, checkNeverTerrain)) {
                //System.out.println("Found location for " + TypeIds.toItemName(typeId));
                return pos;
            }

            spiral.next();
        }

        //System.err.println("Failed to find location for " + TypeIds.toItemName(typeId));
        return null;
    }

    // minimum % of blocks below a building that need to be supported by a solid block for it to be placeable
    // 1 means you can't have any gaps at all, 0 means you can place buildings in mid-air
    private static final float MIN_SUPPORTED_BLOCKS_PERCENT = 0.6f;

    // piglin buildings must be build on at least 80% nether blocks
    private static final float MIN_NETHER_BLOCKS_PERCENT = 0.8f;

    private static boolean isBuildingPlacementInvalid(ArrayList<BuildingBlock> blocksToDraw, ArrayList<BuildingBlock> foundation, boolean checkNeverTerrain) {
        if (MC.level == null)
            return true;

        //System.out.println("isBuildingPlacementInvalid " + foundation.size() + "/" + blocksToDraw.size());

        int solidBlocksBelow = 0;
        int blocksBelow = 0;

        int netherBlocksBelow = 0;
        int nbBlocksBelow = 0;

        for (BuildingBlock block : blocksToDraw) {
            BlockPos bp = block.getBlockPos().above();
            BlockState bs = block.getBlockState(); // building block
            BlockState bsWorld = MC.level.getBlockState(bp);

            if ((bsWorld.isSolid() || !bsWorld.getFluidState().isEmpty()) && (bs.isSolid() || !bs.getFluidState().isEmpty())) {
                //System.out.println("Skipping location due to clipping at " + bp.toShortString());
                return true; // clipping
            }

            if (MC.level != null && foundation.contains(block)) {
                BlockState bsBelow = MC.level.getBlockState(bp.below()); // world block
                if (bs.isSolid() && !(bsBelow.getBlock() instanceof IceBlock)) {
                    blocksBelow += 1;
                    if (bsBelow.isSolid() && !(bsBelow.getBlock() instanceof LeavesBlock)) {
                        solidBlocksBelow += 1;
                    }
                }
            }

            if (checkNeverTerrain && MC.level != null && foundation.contains(block) && bs.isSolid()) {
                nbBlocksBelow += 1;
                if (NetherBlocks.isNetherBlock(MC.level, bp.below())) {
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
        resources.clear();
        WorldApi.getSingleton().resources.values().stream().distinct().collect(Collectors.toCollection(() -> resources));
        for (IResource r : resources) {
            if (GeometryUtils.colliding(boundingBox, r.getBoundingBox())) {
                //System.out.println("Skipping location due to colliding with resources");
                return true; // clipping at least one building
            }
        }

        return false;
    }

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

    private static boolean isBuildingPlacementWithinWorldBorder(AABB boundingBox) {
        var aabbCenter = boundingBox.getCenter();
        var aabbSize = boundingBox.getSize();
        if (MC.level.getWorldBorder().getDistanceToBorder(aabbCenter.x, aabbCenter.z) < aabbSize) {
            //System.out.println("Skipping location due to being out of bounds");
            return false;
        }

        return true;
    }
}
