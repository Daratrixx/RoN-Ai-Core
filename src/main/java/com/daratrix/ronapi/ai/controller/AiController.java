package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.AiDependencies;
import com.daratrix.ronapi.ai.MicroUtils;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.apis.BuildingApi;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiHero;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IOrderable;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.FileLogger;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceSources;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.stream.Collectors;

public class AiController {

    public static final Map<String, AiController> controllers = new HashMap<>();

    public static void reset() {
        AiController.controllers.values().forEach(AiController::cleanup);
        AiController.controllers.clear();
    }

    public static void removeDefeatedControllers() {
        var defeated = AiController.controllers.values().stream().toList();

        for (AiController c : defeated) {
            if(c.defeated) {
                AiController.controllers.remove(c.player.getName());
            }
        }
    }

    public final FileLogger logger;
    public final IAiPlayer player;
    public final IAiLogic logic;
    public final AiArmyController armyController;
    public final AiWorkerController workerController;
    public final IAiControllerPriorities priorities;

    public boolean surrender = false;
    public boolean defeated = false;

    private IBuilding capitol = null;

    public AiController(IAiPlayer player, IAiLogic logic) {
        this.logger = new FileLogger("logs/Ron-Ai/" + player.getName() + ".ai.log");
        this.player = player;
        this.logic = logic;
        this.armyController = new AiArmyController(player, logic, this.logger);
        this.workerController = new AiWorkerController(player, logic, this.logger);
        this.priorities = new AiControllerPriorities(this.logger);
        AiController.controllers.put(player.getName(), this);
    }

    public String getWorkerListName(IUnit unit) {
        return this.workerController.getWorkerListName(unit);
    }

    public String getArmyListName(IUnit unit) {
        return this.armyController.getArmyListName(unit);
    }

    public void runAiMicro(MinecraftServer server) {
        this.armyController.refreshArmyTracker();
        if (this.armyController.army.isEmpty()) {
            return; // nothing to control
        }

        for (var u : this.armyController.army) {
            // always attempt to learn skills
            if (u instanceof ApiHero h) {
                var skillHandler = this.logic.getHeroSkillLogic(h.getTypeId());
                if (skillHandler != null) {
                    skillHandler.accept(h);
                }
            }

            // every 2 seconds, only 30%/10% chance to micro each heroes/units
            if (Math.random() >= (u.isHero() ? 0.3 : 0.1)) {
                continue;
            }

            var handler = this.logic.getMicroLogic(u.getTypeId());
            if (handler != null) {
                handler.accept(u);
                continue;
            }

            handler = MicroUtils.getMicroLogic(u.getTypeId());
            if (handler != null) {
                handler.accept(u);
                continue;
            }
        }
    }

    public void runAiArmy(MinecraftServer server) {
        this.armyController.refreshThreats();
        this.armyController.refreshArmyTracker();
        if (this.armyController.army.isEmpty()) {
            return; // nothing to control
        }

        var priorities = this.priorities.getArmyPriorities();
        this.armyController.assignArmy(priorities);

        if (priorities.defenseTarget != null) {
            this.armyController.groupAttackToward(this.armyController.defenseGroup, priorities.defenseTarget);
        } else {
            this.armyController.groupRetreatToward(this.armyController.defenseGroup, priorities.defaultGatherPoint);
        }
        if (priorities.attackTarget != null) {
            this.armyController.groupAttackToward(this.armyController.attackGroup, priorities.attackTarget);
        } else {
            this.armyController.groupRetreatToward(this.armyController.attackGroup, priorities.defaultGatherPoint);
        }

        this.armyController.groupRetreatToward(this.armyController.idleArmy, priorities.defaultGatherPoint);
    }

    public void runAiHarvesting(MinecraftServer server) {
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
        var busyFarms = this.workerController.farmWorkers.stream()
                .map(u -> u.getCurrentOrderTarget() instanceof BuildingPlacement b ? b : null)
                .filter(Objects::nonNull)
                .distinct().collect(Collectors.toCollection(ArrayList::new));
        var idleFarms = new ArrayList<BuildingPlacement>(finishedFarms);
        idleFarms.removeAll(busyFarms);
        this.logger.log("- FARMS: " + finishedFarms.size() + " finishedFarms/" + busyFarms.size() + " busyFarms/" + idleFarms.size() + " idleFarms");

        var animalDetectionBox = new AABB(this.capitol.getX() - 120, this.capitol.getY() - 20, this.capitol.getZ() - 120,
                this.capitol.getX() + 120, this.capitol.getY() + 20, this.capitol.getZ() + 120);

        List<LivingEntity> animals;

        try {
            animals = server.overworld().getEntitiesOfClass(LivingEntity.class, animalDetectionBox, ResourceSources::isHuntableAnimal);
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

        this.logger.log(this.player.getName() + " current worker assignment (total: " + this.workerController.workers.size() + " workers)");
        this.logger.log("- foodWorkers: " + (this.workerController.huntWorkers.size() + this.workerController.foodBlockWorkers.size() + this.workerController.farmWorkers.size()) + "(" + this.workerController.huntWorkers.size() + " hunts/" + this.workerController.foodBlockWorkers.size() + " blocks/" + this.workerController.farmWorkers.size() + " farms)");
        this.logger.log("- woodWorkers: " + this.workerController.woodWorkers.size());
        this.logger.log("- oreWorkers: " + this.workerController.oreWorkers.size());
        this.logger.log("- builders: " + this.workerController.builders.size());
        this.logger.log("- idle: " + this.workerController.idleWorkers.size());

        // first we assign workers to task groups
        this.workerController.assignWorkers(priorities, animals.size());

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

    public boolean runAiUpgradeBuildings(MinecraftServer server, AiProductionPriorities.AiProductionPriority p) {
        var upgradeSource = AiDependencies.getUpgradeSourceTypeId(p.typeId);
        var existing = this.player.getBuildingsFiltered(b -> b.isDone() && b.is(upgradeSource) && (b.hasUpgrade(p.typeId) || !b.isUpgraded())).toList();
        var upgraded = existing.stream().filter(b -> b.hasUpgrade(p.typeId)).toList();
        var upgrading = existing.stream().filter(b -> b.getCurrentOrderId() == p.typeId).toList();
        var idleSources = existing.stream().filter(b -> !upgraded.contains(b) && !upgrading.contains(b) && b.isIdle()).toList();
        var existingCount = upgrading.size() + upgraded.size();
        var doneCount = upgraded.size();
        var missing = p.count() - doneCount;
        var toStart = p.count() - existingCount;

        if (missing <= 0) {
            this.logger.log("Skipping completed " + TypeIds.toItemName(p.typeId) + "(" + doneCount + "/" + p.count() + ")");
            return true; // already fulfilled the priority
        }

        if (idleSources.isEmpty()) {
            this.logger.log("Skipping upgrade " + TypeIds.toItemName(p.typeId) + " because there are no " + TypeIds.toItemName(upgradeSource) + " available " + "(" + doneCount + "/" + p.count() + ")");
            return false; // can't fulfil the priority at this time
        }

        this.logger.log("Attempting to upgrade " + TypeIds.toItemName(p.typeId) + "(" + toStart + " to start, " + doneCount + "/" + p.count() + " done)");

        for (var source : idleSources) {
            if (toStart <= 0) {
                return true;
            }

            if (!source.issueUpgradeOrder(p.typeId)) {
                return false; // could not start - skip remaining iterations
            }

            this.logger.log("Started upgrade: " + TypeIds.toItemName(p.typeId));
            toStart--;
        }

        return false;
    }

    public void runAiProduceBuildings(MinecraftServer server) {
        this.workerController.refreshWorkerTracker();

        if (this.workerController.workers.isEmpty()) {
            this.logger.log("No workers alive to build, skipping building priorities");
            return;
        }

        this.updateCapitol();
        this.player.checkCompletedBuildings();
        this.logger.log("Constructing buildings: " + String.join(", ", this.player.getBuildingsFiltered(b -> b.isUnderConstruction()).map(b -> b.getName()).toList()));
        this.logger.log("Completed buildings: " + String.join(", ", this.player.getBuildingsFiltered(b -> b.isDone()).map(b -> b.getName()).toList()));
        var builderLimit = this.logic.getConcurrentBuilderLimit(this.player);
        var builderCount = this.workerController.builders.size();

        if (builderCount >= builderLimit) {
            this.logger.log("Skipping building priorities as builder limit is reached (" + builderCount + "/" + builderLimit + ")");
            return;
        }

        this.logger.log("Processing building priorities (concurrent: " + builderCount + "/" + builderLimit + ")");

        var resourcePullPriority = player.countDone(this.logic.getFarmTypeId()) > 2
                ? new int[]{0, TypeIds.Resources.WoodBlock, TypeIds.Resources.FoodBlock, TypeIds.Resources.OreBlock}
                : new int[]{0, TypeIds.Resources.WoodBlock, TypeIds.Resources.FoodBlock, TypeIds.Resources.FoodFarm, TypeIds.Resources.OreBlock};
        var priorities = this.priorities.getBuildingPriorities();
        var it = priorities.iterator();

        ArrayList<IUnit> availableWorkers = new ArrayList<>();

        mainLoop:
        while (it.hasNext()) {
            var p = it.next();
            var upgradeSource = AiDependencies.getUpgradeSourceTypeId(p.typeId);
            if (upgradeSource != 0) {
                if (!runAiUpgradeBuildings(server, p) && p.isRequired()) {
                    break mainLoop;
                }
                continue;
            }

            var existingCount = this.player.count(p.typeId/*, p.location*/);
            var doneCount = this.player.countDone(p.typeId);
            var missing = p.count() - doneCount;
            var toStart = p.count() - existingCount;
            var toFinish = existingCount - doneCount;

            if (missing <= 0) {
                this.logger.log("Skipping completed " + TypeIds.toItemName(p.typeId) + "(" + doneCount + "/" + p.count() + ")");
                continue; // already fulfilled the priority
            }

            var constructing = this.player.getConstrutingBuildings().filter(b -> b.getTypeId() == p.typeId).toList();
            availableWorkers.removeAll(this.workerController.builders);
            this.workerController.idleWorkers.addAll(availableWorkers);
            availableWorkers.clear();

            //this.logger.log(availableWorkers.size() + " available IDLE workers for " + TypeIds.toItemName(p.typeId));

            if (toFinish > 0 && !constructing.isEmpty()) {
                var buildersPerStructure = Math.max(1, p.builders() / toFinish);
                // priority started, make sure we are still working on the priority with at least one worker
                for (IBuilding b : constructing) {
                    var currentBuilders = b.countBuilders();
                    if (currentBuilders >= buildersPerStructure) {
                        continue; // all good
                    }

                    var needed = Math.min(buildersPerStructure - currentBuilders, builderLimit - builderCount);
                    if (needed == 0) {
                        break;
                    }

                    // we pull workers from other resources with excess workers according to priorities that are already processed
                    for (int pullPriority : resourcePullPriority) {
                        if (needed <= 0) {
                            break; // pulled enough!
                        }

                        var pulling = this.workerController.workerTracker.get(pullPriority);
                        if (!pulling.isEmpty()) {
                            this.logger.log("Pulling 1 worker from " + TypeIds.toItemName(pullPriority) + " to continue building  " + TypeIds.toItemName(p.typeId));
                            needed -= this.workerController.transferWorkerAssignment(pulling, availableWorkers, needed);
                        }
                    }

                    if (availableWorkers.isEmpty()) {
                        this.logger.log("Skipping remaining priorities as no new builders can be found.");
                        break mainLoop;
                    }

                    // find a new builder and assign it
                    for (IUnit builder : availableWorkers) {
                        builder.issueRepairOrder(b);
                        this.workerController.builders.add(builder);
                        ++builderCount;
                    }

                    if (builderCount >= builderLimit) {
                        this.logger.log("Skipping remaining priorities as builder limit is reached (" + builderCount + "/" + builderLimit + ")");
                        break mainLoop;
                    }
                }
            }

            if (toStart <= 0) {
                //this.logger.log("Skipping completed " + TypeIds.toItemName(p.typeId) + "(" + doneCount + "/" + p.count() + ")");
                continue; // already fulfilled the priority
            }

            var canMake = AiDependencies.canMake(p.typeId, this.player);
            if (!canMake) {
                this.logger.log("Skipping impossible " + TypeIds.toItemName(p.typeId) + " due to missing requirement:" + TypeIds.toItemName(AiDependencies.lastMissingrequirement));
                if (p.isRequired()) {
                    break mainLoop;
                }
                continue; // impossible to fulfill the priority
            }

            var canAfford = this.player.canAfford(p.typeId);
            if (!canAfford) {
                this.logger.log("Not enough resources for " + TypeIds.toItemName(p.typeId));
                if (p.isRequired()) {
                    break mainLoop;
                }
                continue; // impossible to fulfill the priority
            }

            this.logger.log("Processing " + TypeIds.toItemName(p.typeId) + " as " + p.typeId + " (" + doneCount + "/" + existingCount + "/" + p.count() + ")");

            // we pull workers from other resources with excess workers according to priorities that are already processed
            for (int pullPriority : resourcePullPriority) {
                if (!availableWorkers.isEmpty()) {
                    break;
                }

                var pulling = this.workerController.workerTracker.get(pullPriority);
                if (!pulling.isEmpty()) {
                    this.logger.log("Pulling 1 worker from " + TypeIds.toItemName(pullPriority) + " to start building " + TypeIds.toItemName(p.typeId));
                    this.workerController.transferWorkerAssignment(pulling, availableWorkers, 1);
                    break;
                }
            }

            for (var builder : availableWorkers) {
                var buildingLocation = this.getBuildingLocation(server.overworld(), builder, p.typeId, p.location, p.proximity);

                if (buildingLocation != null && builder.issueBuildOrder(buildingLocation, p.typeId)) {
                    ++builderCount;
                    this.workerController.builders.add(builder);
                    break mainLoop;
                } else {
                    this.workerController.builders.remove(builder);
                    this.workerController.idleWorkers.add(builder);
                    this.logger.log(buildingLocation == null ? "Failed to find a build location" : "Failed to issue build order");
                    break mainLoop;
                }
            }
        }

        availableWorkers.removeAll(workerController.builders);
        this.workerController.idleWorkers.addAll(availableWorkers);
        this.logger.log("Completed all priorities");
    }

    public BlockPos getBuildingLocation(Level level, IUnit builder, int typeId, AiProductionPriorities.Location location, AiProductionPriorities.Proximity proximity) {
        if (this.capitol == null) {
            this.logger.log("Building around Builder at CAPITOL-RANDOM");
            return BuildingApi.getBuildingLocation(level, builder.getPos(), typeId, player.getName(), AiProductionPriorities.Location.CAPITOL, AiProductionPriorities.Proximity.RANDOM);
        }

        return BuildingApi.getBuildingLocation(level, this.capitol.getPos(), typeId, player.getName(), location, proximity);
    }

    public void runAiProduceHero(MinecraftServer server, int typeId) {
        boolean reviving = this.player.hasHero(typeId);
        var cost = reviving
                ? TypeIds.toReviveItem(typeId)
                : TypeIds.toProductionItem(typeId);
        var canAfford = cost.canAfford(server.overworld(), player.getName());
        if (!canAfford) {
            this.logger.log("Not enough resources for " + (reviving ? "reviving" : "training") + " " + TypeIds.toItemName(typeId));
            return; // impossible to fulfill the priority
        }

        this.logger.log("Processing " + TypeIds.toItemName(typeId));

        var producers = this.player.getIdleBuildings(AiDependencies.getSourceTypeIds(typeId)).toList();
        this.logger.log(producers.size() + " producers for " + TypeIds.toItemName(typeId));
        for (int i = 0; i < producers.size(); ++i) {
            var producer = producers.get(i);
            if (reviving ? producer.issueReviveOrder(typeId) : producer.issueTrainOrder(typeId)) {
                WorldApi.queueWorldUpdate();
            } else {
                this.logger.log("Failed to issue train order for " + TypeIds.toItemName(typeId));
            }

            return;
        }
    }

    public void runAiProduceUnits(MinecraftServer server) {
        var priorities = this.priorities.getUnitPriorities();
        var it = priorities.iterator();

        mainLoop:
        while (it.hasNext()) {
            var p = it.next();
            var doneCount = this.player.countDone(p.typeId);
            var existingCount = this.player.count(p.typeId);
            var needed = p.count() - doneCount;

            if (needed <= 0) {
                this.logger.log("Skipping completed " + TypeIds.toItemName(p.typeId));
                continue; // already fulfilled the priority
            }

            var canMake = AiDependencies.canMake(p.typeId, this.player);
            if (!canMake) {
                this.logger.log("Skipping impossible " + TypeIds.toItemName(p.typeId) + " due to missing " + TypeIds.toItemName(AiDependencies.lastMissingrequirement));
                if (p.isRequired()) {
                    break mainLoop;
                }
                continue; // impossible to fulfill the priority
            }

            if (TypeIds.isHero(p.typeId)) {
                this.runAiProduceHero(server, p.typeId);
                continue;
            }

            var canAfford = this.player.canAfford(p.typeId);
            if (!canAfford) {
                this.logger.log("Not enough resources for " + TypeIds.toItemName(p.typeId));
                if (p.isRequired()) {
                    break mainLoop;
                }
                continue; // impossible to fulfill the priority
            }

            this.logger.log("Processing " + TypeIds.toItemName(p.typeId) + "(" + doneCount + "/" + existingCount + "/" + p.count() + ")");

            //this.logger.log("Looking for producers: " + String.join(",", AiDependencies.getSourceTypeIds(p.typeId).stream().map(x -> TypeIds.toItemName(x)).toList()));

            var producers = this.player.getIdleBuildings(AiDependencies.getSourceTypeIds(p.typeId)).toList();
            this.logger.log(producers.size() + " producers for " + TypeIds.toItemName(p.typeId));
            for (int i = 0; i < needed && i < producers.size(); ++i) {
                var producer = producers.get(i);
                if (producer.issueTrainOrder(p.typeId)) {
                    WorldApi.queueWorldUpdate();
                } else {
                    this.logger.log("Failed to issue train order for " + TypeIds.toItemName(p.typeId));
                    if (p.isRequired()) {
                        break mainLoop;
                    }
                }
            }
        }

    }

    public void runAiProduceResearches(MinecraftServer server) {
        var priorities = this.priorities.getResearchPriorities();
        var it = priorities.iterator();
        while (it.hasNext()) {
            var p = it.next();
            if (this.player.hasResearch(p.typeId)) {
                this.logger.log("Skipping completed " + TypeIds.toItemName(p.typeId));
                continue;
            }

            var inProgress = this.player.count(p.typeId);
            if (inProgress > 0) {
                this.logger.log("Skipping ongoing " + TypeIds.toItemName(p.typeId));
                continue;
            }

            var canMake = AiDependencies.canMake(p.typeId, this.player);
            if (!canMake) {
                this.logger.log("Skipping impossible " + TypeIds.toItemName(p.typeId) + " due to missing " + TypeIds.toItemName(AiDependencies.lastMissingrequirement));
                continue; // impossible to fulfill the priority
            }

            var canAfford = this.player.canAfford(p.typeId);
            if (!canAfford) {
                this.logger.log("Not enough resources for " + TypeIds.toItemName(p.typeId));
                continue; // impossible to fulfill the priority
            }

            this.logger.log("Processing " + TypeIds.toItemName(p.typeId));

            var producers = this.player.getIdleBuildings(AiDependencies.getSourceTypeIds(p.typeId)).toList();
            this.logger.log(producers.size() + " producers for " + TypeIds.toItemName(p.typeId));
            if (producers.isEmpty()) {
                continue;
            }

            var producer = producers.get(0);
            if (producer.issueResearchOrder(p.typeId)) {
                WorldApi.queueWorldUpdate();
            } else {
                this.logger.log("Failed to issue train order for " + TypeIds.toItemName(p.typeId));
            }
        }
    }

    public void runAiUpdatePriorities(MinecraftServer server) {
        //this.player.checkCompletedBuildings(); // done before checking for surrender
        this.priorities.reset();
        this.logic.setPriorities(server.overworld(), this.player, this.priorities);
    }

    public boolean shouldSurrender(MinecraftServer server) {
        this.player.checkCompletedBuildings();
        this.surrender = this.logic.shouldSurrender(server.overworld(), this.player, this.priorities);
        return this.surrender;
    }

    public void sendSurrenderMessage() {
        PlayerServerEvents.sendMessageToAllPlayers(this.player.getName() + ": I cannot win anymore, I must abdicate.");
    }

    public void surrender() {
        PlayerServerEvents.defeat(this.player.getName(), Component.translatable("server.reignofnether.surrendered").getString());
        this.defeated = true;
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
        this.logger.flush();
        this.capitol = null;
    }
}
