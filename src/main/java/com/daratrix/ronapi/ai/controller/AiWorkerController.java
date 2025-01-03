package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.AiDependencies;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.apis.BuildingApi;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiResource;
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

public class AiWorkerController {

    public static final Map<String, AiWorkerController> controllers = new HashMap<>();

    public final IAiPlayer player;
    public final IAiLogic logic;

    public final ArrayList<IUnit> workers = new ArrayList<>();
    public final ArrayList<IUnit> huntWorkers = new ArrayList<>();
    public final ArrayList<IUnit> farmWorkers = new ArrayList<>();
    public final ArrayList<IUnit> foodBlockWorkers = new ArrayList<>();
    public final ArrayList<IUnit> woodWorkers = new ArrayList<>();
    public final ArrayList<IUnit> oreWorkers = new ArrayList<>();
    public final ArrayList<IUnit> builders = new ArrayList<>();
    public final ArrayList<IUnit> idleWorkers = new ArrayList<>();
    public final HashMap<Integer, ArrayList<IUnit>> workerTracker = new HashMap<>();

    public AiWorkerController(IAiPlayer player, IAiLogic logic) {
        controllers.put(player.getName(), this);
        this.player = player;
        this.logic = logic;

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

    public void assignWorkers(AiHarvestPriorities priorities, boolean hasIdleFarms) {
        // count excess units compared to processed priorities, so we know where to pull workers from
        this.workerTracker.forEach((k, v) -> {
            this.excessTracker.put(k, v.size());
        });

        var maxFarmers = this.player.countDone(this.logic.getFarmTypeId());

        var excessPullPriority = this.farmWorkers.size() > 2
                ? new int[]{TypeIds.Resources.FoodFarm, TypeIds.Resources.WoodBlock, TypeIds.Resources.OreBlock, TypeIds.Resources.FoodBlock}
                : new int[]{TypeIds.Resources.WoodBlock, TypeIds.Resources.OreBlock, TypeIds.Resources.FoodBlock};
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

        while (it.hasNext()) {
            var p = it.next();

            var targetCount = p.count;
            if (p.typeId == TypeIds.Resources.FoodFarm && targetCount > maxFarmers) {
                targetCount = maxFarmers;
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

    public void orderFarmWorkers(List<IUnit> workers, List<Building> idleFarms) {
        if (workers.isEmpty() || idleFarms.isEmpty()) {
            return;
        }

        var workerIterator = workers.iterator();

        idleFarms.forEach(t -> {
            while (workerIterator.hasNext()) {
                var worker = workerIterator.next();
                if (worker.isCarryCapacityOver(50)) {
                    worker.issueOrder(TypeIds.Orders.Return);
                    return;
                }

                if (worker.isIdle() || worker.getCurrentOrderId() != TypeIds.Resources.FoodFarm) {
                    worker.issueHarvestOrder(t); // TypeIds.Resources.[...] are properly converted to unit actions
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

        idleAnimals.forEach(t -> {
            while (workerIterator.hasNext()) {
                var worker = workerIterator.next();
                if (worker.isCarryCapacityOver(50)) {
                    worker.issueOrder(TypeIds.Orders.Return);
                    return;
                }

                if (worker.isIdle() || worker.getCurrentOrderId() != TypeIds.Resources.FoodEntity) {
                    worker.issueAttackOrder(t); // workers attack animals to hunt
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
            System.out.println("No " + TypeIds.toItemName(taskId) + " target");
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
