package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.models.ApiResource;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.FileLogger;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.stream.Collectors;

public class AiWorkerController {
    public final IAiPlayer player;
    public final IAiLogic logic;
    public final FileLogger logger;

    public final ArrayList<IUnit> workers = new ArrayList<>();
    public final ArrayList<IUnit> huntWorkers = new ArrayList<>();
    public final ArrayList<IUnit> farmWorkers = new ArrayList<>();
    public final ArrayList<IUnit> foodBlockWorkers = new ArrayList<>();
    public final ArrayList<IUnit> woodWorkers = new ArrayList<>();
    public final ArrayList<IUnit> oreWorkers = new ArrayList<>();
    public final ArrayList<IUnit> builders = new ArrayList<>();
    public final ArrayList<IUnit> idleWorkers = new ArrayList<>();
    public final HashMap<Integer, ArrayList<IUnit>> workerTracker = new HashMap<>();

    public AiWorkerController(IAiPlayer player, IAiLogic logic, FileLogger logger) {
        this.logger = logger;
        this.player = player;
        this.logic = logic;

        this.workerTracker.put(0, this.idleWorkers);
        this.workerTracker.put(TypeIds.Resources.FoodEntity, this.huntWorkers);
        this.workerTracker.put(TypeIds.Resources.FoodFarm, this.farmWorkers);
        this.workerTracker.put(TypeIds.Resources.FoodBlock, this.foodBlockWorkers);
        this.workerTracker.put(TypeIds.Resources.WoodBlock, this.woodWorkers);
        this.workerTracker.put(TypeIds.Resources.OreBlock, this.oreWorkers);
    }

    public String getWorkerListName(IUnit unit) {
        if (this.idleWorkers.contains(unit)) return "idleWorkers";
        if (this.builders.contains(unit)) return "builders";
        if (this.oreWorkers.contains(unit)) return "oreWorkers";
        if (this.woodWorkers.contains(unit)) return "woodWorkers";
        if (this.foodBlockWorkers.contains(unit)) return "foodBlockWorkers";
        if (this.farmWorkers.contains(unit)) return "farmWorkers";
        if (this.huntWorkers.contains(unit)) return "huntWorkers";
        return null;
    }

    public void refreshWorkerTracker() {
        // reset state
        this.workers.removeIf(u -> !u.isAlive());
        this.farmWorkers.removeIf(u -> !u.isAlive());
        this.foodBlockWorkers.removeIf(u -> !u.isAlive());
        this.woodWorkers.removeIf(u -> !u.isAlive());
        this.huntWorkers.removeIf(u -> !u.isAlive());
        this.oreWorkers.removeIf(u -> !u.isAlive());
        this.builders.removeIf(u -> !u.isAlive());
        this.idleWorkers.removeIf(u -> !u.isAlive());

        // add newly tracked workers to list
        var workerStream = this.player.getUnitsFiltered(u -> u.isWorker() && u.isAlive());
        workerStream.filter(u -> !this.workers.contains(u)).forEach(u -> {
            this.workers.add(u);
            this.idleWorkers.add(u);
        });

        // detect idle builders
        for (var builder : this.builders) {
            var currentOrder = builder.getCurrentOrderId();
            if (currentOrder == TypeIds.Resources.FoodFarm) {
                this.farmWorkers.add(builder);
            } else if (currentOrder != TypeIds.Orders.Repair) {
                this.idleWorkers.add(builder);
            }
        }

        this.builders.removeAll(this.farmWorkers);
        this.builders.removeAll(this.idleWorkers);
    }

    public boolean transferWorkerAssignment(List<IUnit> source, List<IUnit> target, int reassignCount) {
        if (source == target) {
            return false; // illegal
        }

        while (reassignCount > 0 && !source.isEmpty()) {
            target.add(source.get(0));
            source.remove(0);
            --reassignCount;
        }

        return true;
    }

    private final HashMap<Integer, Integer> excessTracker = new HashMap<Integer, Integer>();

    public void assignWorkers(AiHarvestPriorities priorities, int availableAnimals) {
        // count excess units compared to processed priorities, so we know where to pull workers from
        this.workerTracker.forEach((k, v) -> {
            this.excessTracker.put(k, v.size());
        });

        var maxFarmers = this.player.countDone(this.logic.getFarmTypeId());
        var maxHunters = availableAnimals;

        var excessPullPriority = maxFarmers > 3
                ? new int[]{0, TypeIds.Resources.FoodBlock, TypeIds.Resources.FoodEntity, TypeIds.Resources.FoodFarm, TypeIds.Resources.WoodBlock, TypeIds.Resources.OreBlock}
                : new int[]{0, TypeIds.Resources.FoodBlock, TypeIds.Resources.FoodEntity, TypeIds.Resources.WoodBlock, TypeIds.Resources.OreBlock};
        var it = priorities.iterator();
        if (this.player.getWood() == 0) {
            // if we ran out of wood, send all farmers to wood and don't allow reassigning farm workers
            this.transferWorkerAssignment(this.farmWorkers, this.woodWorkers, this.farmWorkers.size());
            maxFarmers = 0;
        }

        var excessFarmers = this.farmWorkers.size() - maxFarmers;
        if (excessFarmers > 0) {
            // if we have more farmers assigned than we have farms, we set them as idle so we can reassign them
            this.transferWorkerAssignment(this.farmWorkers, this.idleWorkers, excessFarmers);
        }

        var excessHunters = this.huntWorkers.size() - maxHunters;
        if (excessHunters > 0) {
            this.transferWorkerAssignment(this.huntWorkers, this.idleWorkers, excessHunters);
        }

        while (it.hasNext()) {
            var p = it.next();

            var targetCount = p.count;
            if (p.typeId == TypeIds.Resources.FoodFarm && targetCount > maxFarmers) {
                targetCount = maxFarmers;
            }

            if (p.typeId == TypeIds.Resources.FoodEntity && targetCount > maxHunters) {
                targetCount = maxHunters;
            }

            var taskWorkers = this.workerTracker.get(p.typeId);
            var needed = targetCount - taskWorkers.size();

            if (needed <= 0) {
                this.excessTracker.put(p.typeId, taskWorkers.size() - targetCount); // adjust excess workers
                continue; // fulfilled
            }

            var replacementWorkers = this.idleWorkers.stream().limit(needed).collect(Collectors.toCollection(ArrayList::new));
            this.idleWorkers.removeAll(replacementWorkers);

            // if there are not enough idle workers, we try to pull workers from other resources with excess workers according to priorities that are already processed
            if (replacementWorkers.size() < needed) {
                for (int pullPriority : excessPullPriority) {
                    if (pullPriority == p.typeId) {
                        continue; // can't reassign from the same task
                    }

                    var missing = needed - replacementWorkers.size();
                    if (missing <= 0) {
                        break; // we pulled all the workers we needed from other resources
                    }

                    var pullPriorityExcess = this.excessTracker.get(pullPriority);
                    if (pullPriorityExcess > 0) {
                        transferWorkerAssignment(this.workerTracker.get(pullPriority), replacementWorkers, missing);
                    }
                }
            }

            // commit worker reassignment
            taskWorkers.addAll(replacementWorkers);

            this.excessTracker.put(p.typeId, taskWorkers.size() - targetCount); // adjust excess workers
        }
    }

    private ArrayList<BuildingPlacement> assignedFarms = new ArrayList<>();

    public void spreadFarmWorkers(List<IUnit> workers, List<BuildingPlacement> idleFarms) {
        this.assignedFarms.clear();
        for (IUnit worker : workers) {
            var targetFarm = worker.getWorkerUnit().getGatherResourceGoal().data.targetFarm;
            if (targetFarm != null) {
                if (this.assignedFarms.contains(targetFarm)) {
                    if (!idleFarms.isEmpty()) {
                        worker.issueHarvestOrder(idleFarms.remove(0)); // TypeIds.Resources.[...] are properly converted to unit actions
                    } else {
                        worker.issueOrder(TypeIds.Orders.Stop);
                    }
                } else {
                    this.assignedFarms.add(targetFarm);
                }
            }
        }
    }

    public void orderFarmWorkers(List<IUnit> workers, List<BuildingPlacement> idleFarms) {
        if (workers.isEmpty() || idleFarms.isEmpty()) {
            return;
        }

        this.spreadFarmWorkers(workers, idleFarms);

        var workerIterator = workers.iterator();

        idleFarms.forEach(targetFarm -> {
            while (workerIterator.hasNext()) {
                var worker = workerIterator.next();
                if (worker.isCarryCapacityOver(50)) {
                    worker.issueOrder(TypeIds.Orders.Return);
                    return;
                }

                if (worker.isIdle() || worker.getCurrentOrderId() != TypeIds.Resources.FoodFarm) {
                    worker.issueHarvestOrder(targetFarm); // TypeIds.Resources.[...] are properly converted to unit actions
                    return;
                }
            }
        });
    }

    public void orderHuntWorkers(List<IUnit> workers, List<LivingEntity> idleAnimals) {
        if (workers.isEmpty() || idleAnimals.isEmpty()) {
            return;
        }

        var workerIterator = workers.iterator();

        idleAnimals.forEach(targetAnimal -> {
            while (workerIterator.hasNext()) {
                var worker = workerIterator.next();
                if (worker.isCarryCapacityOver(50)) {
                    worker.issueOrder(TypeIds.Orders.Return);
                    return;
                }

                if (worker.isIdle() || worker.getCurrentOrderId() != TypeIds.Resources.FoodEntity) {
                    worker.issueAttackOrder(targetAnimal); // workers attack animals to hunt
                    return;
                }
            }
        });
    }

    public void orderResourceWorkers(int taskId, List<IUnit> workers, List<ApiResource> resources) {
        if (workers.isEmpty() || resources.isEmpty()) {
            return;
        }

        var targetResource = resources.stream().filter(r -> r.getTypeId() == taskId).findFirst().orElse(null);
        if (targetResource == null) {
            this.logger.log("No " + TypeIds.toItemName(taskId) + " target");
            return;
        }

        for (IUnit worker : workers) {
            if (worker.isCarryCapacityOver(50)) {
                worker.issueOrder(TypeIds.Orders.Return);
                return;
            }

            if (worker.isIdle() || worker.getCurrentOrderId() != taskId) {
                worker.issueHarvestOrder(targetResource); // TypeIds.Resources.[...] are properly converted to unit actions
                return;
            }
        }
    }
}
