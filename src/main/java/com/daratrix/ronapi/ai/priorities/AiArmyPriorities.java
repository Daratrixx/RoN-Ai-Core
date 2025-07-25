package com.daratrix.ronapi.ai.priorities;

import com.daratrix.ronapi.ai.controller.AiArmyController;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiPlayerBase;
import com.daratrix.ronapi.models.interfaces.IPlayer;
import com.daratrix.ronapi.models.interfaces.IPlayerWidget;
import com.daratrix.ronapi.utils.FileLogger;
import com.daratrix.ronapi.utils.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public class AiArmyPriorities extends AiAbstractPriorities<AiArmyPriorities.AiArmyPriority> {

    public final FileLogger logger;
    public IPlayerWidget defenseTarget = null;
    public BlockPos.MutableBlockPos defaultGatherPoint = new BlockPos.MutableBlockPos();

    public IPlayerWidget attackTarget = null;
    public BlockPos.MutableBlockPos attackGatherPoint = new BlockPos.MutableBlockPos();
    public IPlayerWidget harassTarget = null;
    public BlockPos.MutableBlockPos harassGatherPoint = new BlockPos.MutableBlockPos();

    public AiArmyPriorities(FileLogger logger) {
        this.logger = logger;
    }


    @Override
    public AiArmyPriority addPriority(int typeId, int count) {
        var priority = new AiArmyPriority(typeId, count);
        this.priorities.add(priority);
        return priority;
    }

    public boolean pickDefenseTarget(IPlayer player) {
        if (this.defenseTarget != null && this.defenseTarget.isAlive() && WorldApi.isNearPlayerBases(this.defenseTarget, player, 8)) {
            return true;
        }

        var threatenedBase = player.getBasesFiltered(ApiPlayerBase::hasThreats).findFirst().orElse(null);
        if (threatenedBase == null) {
            this.defenseTarget = null;
            return false;
        }

        this.defenseTarget = threatenedBase.getThreats().findFirst().orElse(null);
        return this.defenseTarget != null;
    }

    /**
     * Set where units will gather while they are not used
     *
     * @param player
     */
    public void pickDefaultGatherPoint(Level level, IPlayer player) {
        // we try to find a base, either a base with a Capitol, or any base
        var base = player.getBasesFiltered(ApiPlayerBase::hasCapitol).findFirst().orElse(null);

        if (base == null) {
            base = player.getBasesFiltered(b -> true).findFirst().orElse(null);
        }

        if (base == null) {
            // if we can't find a base, fallback to the center of the map
            this.defaultGatherPoint.set(0, 0, 0);
            WorldApi.getTerrainHeight(level, this.defaultGatherPoint);
            return;
        }

        // if we have a base, choose which side of the base to gather at
        var zeroOverlapX = GeometryUtils.isOverlapping(base.getMinX(), base.getMaxX(), 0, 0);
        var zeroOverlapZ = GeometryUtils.isOverlapping(base.getMinZ(), base.getMaxZ(), 0, 0);

        if (zeroOverlapX) {
            this.defaultGatherPoint.setX(0);
        } else {
            var x = base.getX();
            if (x > 0) {
                this.defaultGatherPoint.setX(base.getMinX() + 15);
            } else {
                this.defaultGatherPoint.setX(base.getMaxX() - 15);
            }
        }

        if (zeroOverlapZ) {
            this.defaultGatherPoint.setZ(0);
        } else {
            var z = base.getZ();
            if (z > 0) {
                this.defaultGatherPoint.setZ(base.getMinZ() + 15);
            } else {
                this.defaultGatherPoint.setZ(base.getMaxZ() - 15);
            }
        }

        // adjust to terrain height
        WorldApi.getTerrainHeight(level, this.defaultGatherPoint);
    }

    /**
     * Set where the units will group up before launching the assault.
     * Once all attacking units are close enough to this point, they will attack-move toward the target.
     * This should be closer to the target than the default gather point.
     *
     * @param player
     */
    public void setAttackGatherPoint(IPlayer player) {
    }

    public void pickAttackTarget(IPlayer player) {
        var armyPop = player.getArmyPop();
        var workerPop = player.getWorkerPop();
        if (armyPop * 2 < workerPop) {
            this.attackTarget = null;
            return; // army is too small, shouldn't try to attack
        }

        if (this.attackTarget != null && this.attackTarget.isAlive()) {
            var currentTargetPlayer = WorldApi.getPlayer(this.attackTarget.getOwnerName());
            if (currentTargetPlayer == null || currentTargetPlayer.getArmyPop() * AiArmyController.maxPunchUpThreshold > armyPop ) {
                this.attackTarget = null; // shouldn't keep fighting the target
            }

            return; // keep attacking the same target / return to base
        }

        var enemies = WorldApi.getPlayerEnemies(player)
                .filter(p -> p.getArmyPop() * AiArmyController.maxPunchUpThreshold <= armyPop) // make sure the target is not too strong
                .toList();
        var enemyBases = enemies.stream()
                .flatMap(p -> p.getBasesFiltered(b -> !b.buildings.isEmpty()))
                .sorted((a, b) -> GeometryUtils.distanceComparator(a, b, this.defaultGatherPoint))
                .toList();
        if (!enemyBases.isEmpty()) {
            this.attackTarget = enemyBases.get(0).buildings.get(0);
            return;
        }

        var enemyUnits = enemies.stream()
                .flatMap(p -> p.getUnitsFiltered(u -> u.isAlive()))
                .sorted((a, b) -> GeometryUtils.distanceComparator(a, b, this.defaultGatherPoint))
                .toList();
        if (!enemyUnits.isEmpty()) {
            this.attackTarget = enemyUnits.get(0);
            return;
        }

        this.attackTarget = null;
    }

    /**
     * Set where the units will group up before starting to harass.
     * Once all harassing units are close enough to this point, they will start harassing the target (attack/retreat as needed).
     * This should be closer to the target than the default gather point, and not too close to the attackGatherPoint.
     *
     * @param player
     */
    public void setHarassGatherPoint(IPlayer player) {

    }

    public void setHarassTarget(IPlayer player) {

    }

    public static class AiArmyPriority extends AiAbstractPriorities.AiAbstractPriority {
        public AiArmyPriority(int typeId, int count) {
            super(typeId, count);
        }
    }
}
