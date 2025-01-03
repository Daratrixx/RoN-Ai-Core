package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.models.interfaces.ILocated;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.GeometryUtils;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;

public class AiArmyController {

    public final IAiPlayer player;
    public final IAiLogic logic;

    public final ArrayList<IUnit> army = new ArrayList<>();
    public final ArrayList<IUnit> idleArmy = new ArrayList<>();

    public final ArrayList<IUnit> attackGroup = new ArrayList<>();
    public final ArrayList<IUnit> harassGroup = new ArrayList<>();

    public AiArmyController(IAiPlayer player, IAiLogic logic) {
        this.player = player;
        this.logic = logic;
    }

    public String getArmyListName(IUnit unit) {
        if (this.idleArmy.contains(unit)) return "idleArmy";
        if (this.attackGroup.contains(unit)) return "attackGroup";
        if (this.harassGroup.contains(unit)) return "harassGroup";
        return null;
    }

    public void refreshArmyTracker() {
        // reset state
        this.army.removeIf(u -> !u.isAlive());
        this.idleArmy.removeIf(u -> !u.isAlive());
        this.attackGroup.removeIf(u -> !u.isAlive());
        this.harassGroup.removeIf(u -> !u.isAlive());

        // add newly tracked soldiers to list
        this.player.getUnitsFiltered(u -> !u.isWorker() && u.isAlive())
                .filter(u -> !this.army.contains(u)).forEach(u -> {
                    this.army.add(u);
                    this.idleArmy.add(u);
                });
    }

    public void runAiAttack() {

    }

    public void runAiDefence() {

    }

    public void runAiHarass() {

    }

    public void assignArmy(AiArmyPriorities priorities) {
        // for now use every single units to attack
        this.attackGroup.clear();
        this.attackGroup.addAll(this.army);
        this.idleArmy.clear();
    }

    public void groupAttackToward(Collection<IUnit> group, ILocated target) {
        if (target == null) {
            return;
        }

        for (IUnit u : group) {
            u.issueAttackOrder(target.getX(), target.getY(), target.getZ());
        }
    }

    public void groupRetreatToward(Collection<IUnit> group, BlockPos target) {
        if (target == null) {
            return;
        }

        for (IUnit u : group) {
            if (GeometryUtils.isWithinDistance(u, target, 10)) {
                u.issueAttackOrder(target.getX(), target.getY(), target.getZ());
            } else {
                u.issueMoveOrder(target.getX(), target.getY(), target.getZ());
            }
        }
    }
}
