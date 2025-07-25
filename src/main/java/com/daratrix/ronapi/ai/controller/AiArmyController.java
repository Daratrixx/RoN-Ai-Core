package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiPlayerBase;
import com.daratrix.ronapi.models.interfaces.ILocated;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.FileLogger;
import com.daratrix.ronapi.utils.GeometryUtils;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;

public class AiArmyController {
    public final IAiPlayer player;
    public final IAiLogic logic;
    public final FileLogger logger;

    public final ArrayList<IUnit> army = new ArrayList<>();
    public final ArrayList<IUnit> idleArmy = new ArrayList<>();

    public final ArrayList<IUnit> defenseGroup = new ArrayList<>();
    public final ArrayList<IUnit> attackGroup = new ArrayList<>();
    public final ArrayList<IUnit> harassGroup = new ArrayList<>();
    private int totalThreatPower;

    public static final float maxPunchUpThreshold = 0.85f; // don't attack a player when population is comparatively too small

    public AiArmyController(IAiPlayer player, IAiLogic logic, FileLogger logger) {
        this.player = player;
        this.logic = logic;
        this.logger = logger;
    }

    public String getArmyListName(IUnit unit) {
        if (this.idleArmy.contains(unit)) return "idleArmy";
        if (this.defenseGroup.contains(unit)) return "defenseGroup";
        if (this.attackGroup.contains(unit)) return "attackGroup";
        if (this.harassGroup.contains(unit)) return "harassGroup";
        return null;
    }

    public void refreshArmyTracker() {
        // reset state
        this.army.removeIf(u -> !u.isAlive());
        this.idleArmy.removeIf(u -> !u.isAlive());
        this.defenseGroup.removeIf(u -> !u.isAlive());
        this.attackGroup.removeIf(u -> !u.isAlive());
        this.harassGroup.removeIf(u -> !u.isAlive());

        // add newly tracked soldiers to list
        this.player.getUnitsFiltered(u -> !u.isWorker() && u.isAlive())
                .filter(u -> !this.army.contains(u)).forEach(u -> {
                    this.army.add(u);
                    this.idleArmy.add(u);
                });
    }

    public void refreshThreats() {
        this.totalThreatPower = this.player.getBasesFiltered(b -> true).map(ApiPlayerBase::updateThreats).reduce(0, Integer::sum);
    }

    public void assignArmy(AiArmyPriorities priorities) {
        if (priorities.defenseTarget != null) {
            // use all idle army for defence
            this.defenseGroup.addAll(this.idleArmy);
            this.idleArmy.clear();
            var defensePower = this.defenseGroup.stream().map(IUnit::getPopUsed).reduce(0, Integer::sum);

            // check if we need to pull defenders from attacking army as well
            if (defensePower < this.totalThreatPower) {
                this.attackGroup.sort((a, b) -> GeometryUtils.distanceComparator(a, b, priorities.defenseTarget));
                while (!this.attackGroup.isEmpty() && defensePower < this.totalThreatPower) {
                    var defender = this.attackGroup.get(0);
                    this.defenseGroup.add(defender);
                    defensePower += defender.getPopUsed();
                }
            }
        } else {
            this.idleArmy.addAll(this.defenseGroup);
            this.defenseGroup.clear();
        }

        if (priorities.attackTarget != null) {
            this.attackGroup.addAll(this.idleArmy);
            this.idleArmy.clear();
        } else {
            this.idleArmy.addAll(this.attackGroup);
            this.attackGroup.clear();
        }
    }

    public void groupAttackToward(Collection<IUnit> group, ILocated target) {
        if (target == null || group.isEmpty()) {
            return;
        }

        float x = target.getX();
        float y = target.getY();
        float z = target.getZ();

        var unitCount = group.size();
        BlockPos.MutableBlockPos center = new BlockPos.MutableBlockPos();

        for (IUnit u : group) {
            var pos = u.getPos();
            center.setX(center.getX() + pos.getX());
            center.setY(center.getY() + pos.getY());
            center.setZ(center.getZ() + pos.getZ());
        }
        center.setX(center.getX() / unitCount);
        center.setY(center.getY() / unitCount);
        center.setZ(center.getZ() / unitCount);

        ArrayList<Float> groupDistance = new ArrayList<>(group.size());
        for (IUnit u : group) {
            groupDistance.add(GeometryUtils.distanceSquared(u, center));
        }
        groupDistance.sort(Float::compare);

        var median = groupDistance.get(unitCount / 2);
        var edgeCase = groupDistance.get(3 * unitCount / 4);
        if (median > 10 * 10 || edgeCase > 20 * 20) {
            this.logger.log("Army is too spread out, regrouping...");
            for (IUnit u : group) {
                u.issueAttackOrder(center.getX(), center.getY(), center.getZ()); // TODO: fix Y
            }

            return;
        }

        for (IUnit u : group) {
            u.issueAttackOrder(x, y, z);
        }
    }

    public void groupRetreatToward(Collection<IUnit> group, BlockPos target) {
        if (target == null) {
            return;
        }

        float x = target.getX();
        float y = target.getY();
        float z = target.getZ();
        int strictDistance = 16 * 16; // squared
        int softDistance = 8 * 8; // squared

        for (IUnit u : group) {
            if (u.getCurrentOrderId() == TypeIds.Orders.Return) {
                continue; // keep returning
            }
            var d = GeometryUtils.distanceSquared(u, target);
            // if very far force move, if kinda close attack move, if close do nothing
            if (u.isCarryingItems()) {
                u.issueOrder(TypeIds.Orders.Return); // while idling with resources, return them
            } else if (d > strictDistance) {
                u.issueMoveOrder(x, y, z);
            } else if (d > softDistance) {
                u.issueAttackOrder(x, y, z);
            }
        }
    }
}
