package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.AiDependencies;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.apis.BuildingApi;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.resources.ResourceSources;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.stream.Collectors;

public class AiController {

    public static final Map<String, AiController> controllers = new HashMap<>();

    public static void reset() {
        AiController.controllers.values().forEach(AiController::cleanup);
        AiController.controllers.clear();
    }

    public final IAiPlayer player;
    public final IAiLogic logic;
    public final AiArmyController armyController;
    public final AiWorkerController workerController;
    public final IAiControllerPriorities priorities;

    private IBuilding capitol = null;

    public AiController(IAiPlayer player, IAiLogic logic) {
        controllers.put(player.getName(), this);
        this.player = player;
        this.logic = logic;
        this.armyController = new AiArmyController(player, logic);
        this.workerController = new AiWorkerController(player, logic);
        this.priorities = new AiControllerPriorities();
    }

    public String getWorkerListName(IUnit unit) {
        return this.workerController.getWorkerListName(unit);
    }

    public String getArmyListName(IUnit unit) {
        return this.armyController.getArmyListName(unit);
    }

    public void runAiMicro(float elapsed) {
        this.armyController.refreshArmyTracker();
        if (this.armyController.army.isEmpty()) {
            return; // nothing to control
        }

    }

    public void runAiArmy(float elapsed) {
        this.armyController.refreshArmyTracker();
        if (this.armyController.army.isEmpty()) {
            return; // nothing to control
        }

        var priorities = this.priorities.getArmyPriorities();
        this.armyController.assignArmy(priorities);

        if (priorities.armyTarget != null) {
            this.armyController.groupAttackToward(this.armyController.attackGroup, priorities.armyTarget);
        } else {
            this.armyController.groupRetreatToward(this.armyController.attackGroup, priorities.defaultGatherPoint);
            //} else {
            //    this.armyController.gatherToward(priorities.defaultGatherPoint);
        }
    }

    public void runAiHarvesting(float elapsed) {
        this.updateCapitol();
        if (this.capitol == null) {
            return;
        }

        if (!this.capitol.isDone()) {
            runAiPowerbuild();
            return;
        }

        var priorities = this.priorities.getHarvestingPriorities();
        if (priorities.isEmpty()) {
            runAiPowerbuild();
            return;
        }

        assert this.capitol != null && this.capitol.isDone() && !priorities.isEmpty();

        this.workerController.refreshWorkerTracker();

        var farmTypeId = this.logic.getFarmTypeId();
        var finishedFarms = this.player.getBuildings(farmTypeId).filter(IBuilding::isDone).map(IBuilding::getBuilding).collect(Collectors.toCollection(ArrayList::new));
        var busyFarms = this.workerController.farmWorkers.stream().map(u -> u.getCurrentOrderTarget() instanceof Building b ? b : null).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        var idleFarms = new ArrayList<Building>(finishedFarms);
        idleFarms.removeAll(busyFarms);
        System.out.println("- FARMS: " + finishedFarms.size() + " finishedFarms/" + busyFarms.size() + " busyFarms/" + idleFarms.size() + " idleFarms");

        var animalDetectionBox = new AABB(this.capitol.getX() - 120, this.capitol.getY() - 20, this.capitol.getZ() - 120,
                this.capitol.getX() + 120, this.capitol.getY() + 20, this.capitol.getZ() + 120);

        List<LivingEntity> animals;

        try {
            animals = Minecraft.getInstance().level.getEntitiesOfClass(LivingEntity.class, animalDetectionBox, ResourceSources::isHuntableAnimal);
        } catch (Exception e) {
            return;
        }

        animals.removeIf(e -> !GeometryUtils.isWithinDistance(this.capitol, e, 120));
        animals.sort((a, b) -> GeometryUtils.distanceComparator(a, b, this.capitol));
        var busyAnimals = this.workerController.huntWorkers.stream().map(u -> u.getCurrentOrderTarget() instanceof LivingEntity e ? e : null).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        var idleAnimals = new ArrayList<LivingEntity>(animals);
        idleAnimals.removeAll(busyAnimals);

        var resources = WorldApi.getSingleton().resources.values().stream().distinct().filter(r -> r.getAmount() > 0).collect(Collectors.toCollection(ArrayList::new));
        resources.sort((a, b) -> GeometryUtils.distanceComparator(a, b, this.capitol));

        System.out.println(this.player.getName() + " current worker assignment (total: " + this.workerController.workers.size() + " workers)");
        System.out.println("- foodWorkers: " + (this.workerController.huntWorkers.size() + this.workerController.foodBlockWorkers.size() + this.workerController.farmWorkers.size()) + "(" + this.workerController.huntWorkers.size() + " hunts/" + this.workerController.foodBlockWorkers.size() + " blocks/" + this.workerController.farmWorkers.size() + " farms)");
        System.out.println("- woodWorkers: " + this.workerController.woodWorkers.size());
        System.out.println("- oreWorkers: " + this.workerController.oreWorkers.size());
        System.out.println("- builders: " + this.workerController.builders.size());
        System.out.println("- idle: " + this.workerController.idleWorkers.size());

        // first we assign workers to task groups
        this.workerController.assignWorkers(priorities, !idleFarms.isEmpty());

        // then we ensure workers are working on their tasks
        this.workerController.orderFarmWorkers(this.workerController.farmWorkers, idleFarms);
        this.workerController.orderHuntWorkers(this.workerController.huntWorkers, idleAnimals);
        this.workerController.orderResourceWorkers(TypeIds.Resources.WoodBlock, this.workerController.woodWorkers, resources);
        this.workerController.orderResourceWorkers(TypeIds.Resources.FoodBlock, this.workerController.foodBlockWorkers, resources);
        this.workerController.orderResourceWorkers(TypeIds.Resources.OreBlock, this.workerController.oreWorkers, resources);

        if (!this.workerController.idleWorkers.isEmpty()) {
            runAiPowerbuild(this.workerController.idleWorkers);
        }
    }

    public void runAiPowerbuild() {
        var workers = this.player.getUnitsFiltered(IUnit::isWorker).toList();
        runAiPowerbuild(workers);
    }

    public void runAiPowerbuild(List<IUnit> workers) {
        var constructing = this.player.getConstrutingBuildings().toList();
        if (constructing.isEmpty()) {
            return;
        }
        for (int i = 0; i < workers.size(); ++i) {
            var b = constructing.get(i % constructing.size());
            var w = workers.get(i);
            w.issueRepairOrder(b);
        }
    }

    public void runAiProduceBuildings(float elapsed) {
        this.updateCapitol();
        this.workerController.refreshWorkerTracker();
        this.player.checkCompletedBuildings();
        System.out.println("Constructing buildings: " + String.join(", ", this.player.getBuildingsFiltered(b -> b.isUnderConstruction()).map(b -> b.getName()).toList()));
        System.out.println("Completed buildings: " + String.join(", ", this.player.getBuildingsFiltered(b -> b.isDone()).map(b -> b.getName()).toList()));
        var builderLimit = this.logic.getConcurrentBuilderLimit(this.player);
        var builderCount = this.workerController.builders.size();

        if (builderCount >= builderLimit) {
            System.out.println("Skipping building priorities as builder limit is reached (" + builderCount + "/" + builderLimit + ")");
            return;
        }

        System.out.println("Processing building priorities (concurrent: " + builderCount + "/" + builderLimit + ")");

        var resourcePullPriority = player.countDone(this.logic.getFarmTypeId()) > 2
                ? new int[]{TypeIds.Resources.WoodBlock, TypeIds.Resources.FoodBlock, TypeIds.Resources.OreBlock}
                : new int[]{TypeIds.Resources.WoodBlock, TypeIds.Resources.FoodBlock, TypeIds.Resources.FoodFarm, TypeIds.Resources.OreBlock};
        var priorities = this.priorities.getBuildingPriorities();
        var it = priorities.iterator();
        while (it.hasNext()) {
            var p = it.next();
            var existingCount = this.player.count(p.typeId/*, p.location*/);
            var doneCount = this.player.countDone(p.typeId);
            var missing = p.count - doneCount;
            var toStart = p.count - existingCount;

            if (missing <= 0) {
                System.out.println("Skipping completed " + TypeIds.toItemName(p.typeId) + "(" + doneCount + "/" + p.count + ")");
                continue; // already fulfilled the priority
            }

            var constructing = this.player.getConstrutingBuildings().filter(b -> b.getTypeId() == p.typeId).toList();
            List<IUnit> availableWorkers = this.workerController.idleWorkers.stream()
                    //.filter(u -> u.getCurrentOrderId() != TypeIds.Orders.Repair) // ignore workers that are already building/repairing
                    //.sorted(this.getBuilderPriorityComparator())
                    //.limit(Math.min(p.count - existingCount, builderLimit - builderCount))
                    //.limit(p.count - existingCount)
                    .limit(Math.max(0, Math.min(p.count - existingCount, builderLimit - builderCount)))
                    .collect(Collectors.toCollection(ArrayList::new));

            System.out.println(availableWorkers.size() + " available IDLE workers for " + TypeIds.toItemName(p.typeId));

            if (toStart > 0 && !constructing.isEmpty()) {
                // priority started, make sure we are still working on the priority with at least one worker
                for (IBuilding b : constructing) {
                    var currentBuilders = b.countBuilders();
                    if (currentBuilders > 0) {
                        continue; // all good
                    }

                    // if there are not enough idle workers, we try to pull workers from other resources with excess workers according to priorities that are already processed
                    if (availableWorkers.size() < 1) {
                        for (int pullPriority : resourcePullPriority) {
                            var pulling = this.workerController.workerTracker.get(pullPriority);
                            if (!pulling.isEmpty()) {
                                System.out.println("Pulling 1 worker from " + TypeIds.toItemName(pullPriority) + " to continue building  " + TypeIds.toItemName(p.typeId));
                                this.workerController.transferWorkerAssignment(pulling, availableWorkers, 1);
                                break;
                            }
                        }
                    }

                    // find a new builder and assign it
                    var builder = availableWorkers.get(0);
                    builder.issueRepairOrder(b);
                    availableWorkers.remove(0);
                    this.workerController.idleWorkers.remove(builder);
                    this.workerController.builders.add(builder);
                    ++builderCount;

                    if (builderCount >= builderLimit) {
                        System.out.println("Skipping remaining priorities as builder limit is reached (" + builderCount + "/" + builderLimit + ")");
                        return;
                    }
                }

                return;
            }

            if (toStart <= 0) {
                //System.out.println("Skipping completed " + TypeIds.toItemName(p.typeId) + "(" + doneCount + "/" + p.count + ")");
                continue; // already fulfilled the priority
            }

            var canMake = AiDependencies.canMake(p.typeId, this.player);
            if (!canMake) {
                System.out.println("Skipping impossible " + TypeIds.toItemName(p.typeId));
                continue; // impossible to fulfill the priority
            }

            var canAfford = this.player.canAfford(p.typeId);
            if (!canAfford) {
                System.out.println("Not enough resources for " + TypeIds.toItemName(p.typeId));
                continue; // impossible to fulfill the priority
            }

            System.out.println("Processing " + TypeIds.toItemName(p.typeId) + " as " + p.typeId + " (" + doneCount + "/" + existingCount + "/" + p.count + ")");

            // if there are not enough idle workers, we try to pull workers from other resources with excess workers according to priorities that are already processed
            if (availableWorkers.size() < 1) {
                for (int pullPriority : resourcePullPriority) {
                    var pulling = this.workerController.workerTracker.get(pullPriority);
                    if (!pulling.isEmpty()) {
                        System.out.println("Pulling 1 worker from " + TypeIds.toItemName(pullPriority) + " to start building  " + TypeIds.toItemName(p.typeId));
                        this.workerController.transferWorkerAssignment(pulling, availableWorkers, 1);
                        break;
                    }
                }
            }

            for (var builder : availableWorkers) {
                System.out.println(this.capitol != null ? "Building from Capitol" : "Building from Builder");
                var lookupOrigin = this.capitol != null ? this.capitol.getPos() : new BlockPos(builder.getX(), 0, builder.getZ());
                var buildingLocation = BuildingApi.getBuildingLocation(lookupOrigin, p.typeId, player.getName());

                if (buildingLocation != null && builder.issueBuildOrder(buildingLocation, p.typeId)) {
                    ++builderCount;
                    this.workerController.builders.add(builder);
                    this.workerController.idleWorkers.remove(builder);
                    //WorldApi.queueWorldUpdate();
                    return;
                } else {
                    this.workerController.builders.remove(builder);
                    this.workerController.idleWorkers.add(builder);
                    System.err.println("Failed to issue build order");
                    return;
                }
            }
        }

        System.out.println("Completed all priorities");
    }

    public void runAiProduceUnits(float elapsed) {
        var priorities = this.priorities.getUnitPriorities();
        var it = priorities.iterator();
        while (it.hasNext()) {
            var p = it.next();
            var doneCount = this.player.countDone(p.typeId);
            var existingCount = this.player.count(p.typeId);
            var needed = p.count - doneCount;

            if (needed <= 0) {
                System.out.println("Skipping completed " + TypeIds.toItemName(p.typeId));
                continue; // already fulfilled the priority
            }

            var canMake = AiDependencies.canMake(p.typeId, this.player);
            if (!canMake) {
                System.out.println("Missing requirement for " + TypeIds.toItemName(p.typeId));
                continue; // impossible to fulfill the priority
            }

            var canAfford = this.player.canAfford(p.typeId);
            if (!canAfford) {
                System.out.println("Not enough resources for " + TypeIds.toItemName(p.typeId));
                continue; // impossible to fulfill the priority
            }

            System.out.println("Processing " + TypeIds.toItemName(p.typeId) + "(" + doneCount + "/" + existingCount + "/" + p.count + ")");

            //System.out.println("Looking for producers: " + String.join(",", AiDependencies.getSourceTypeIds(p.typeId).stream().map(x -> TypeIds.toItemName(x)).toList()));

            var producers = this.player.getIdleBuildings(AiDependencies.getSourceTypeIds(p.typeId)).toList();
            System.out.println(producers.size() + " producers for " + TypeIds.toItemName(p.typeId));
            for (int i = 0; i < needed && i < producers.size(); ++i) {
                var producer = producers.get(i);
                if (producer.issueTrainOrder(p.typeId)) {
                    WorldApi.queueWorldUpdate();
                } else {
                    System.err.println("Failed to issue train order for " + TypeIds.toItemName(p.typeId));
                }
            }
        }

    }

    public void runAiProduceResearches(float elapsed) {

    }

    public void runAiUpdatePriorities(float elapsed) {
        this.player.checkCompletedBuildings();
        this.priorities.reset();
        this.logic.setPriorities(this.player, this.priorities);
    }

    public void updateCapitol() {
        if (this.capitol != null && !this.capitol.isAlive()) {
            this.capitol = null;
        }

        if (this.capitol == null) {
            this.capitol = this.player.getBuildings(this.logic.getCapitolTypeId()).findFirst().orElse(null);
        }
    }

    public void cleanup() {
        this.capitol = null;
    }
}
