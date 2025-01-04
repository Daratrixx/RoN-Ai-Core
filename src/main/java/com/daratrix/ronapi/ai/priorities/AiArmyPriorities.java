package com.daratrix.ronapi.ai.priorities;

import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.interfaces.ILocated;
import com.daratrix.ronapi.models.interfaces.IPlayer;
import com.daratrix.ronapi.utils.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public class AiArmyPriorities extends AiAbstractPriorities<AiArmyPriorities.AiArmyPriority> {

    public BlockPos.MutableBlockPos defaultGatherPoint = new BlockPos.MutableBlockPos();

    public ILocated armyTarget = null;
    public BlockPos.MutableBlockPos armyGatherPoint = new BlockPos.MutableBlockPos();
    public ILocated harassTarget = null;
    public BlockPos.MutableBlockPos harassGatherPoint = new BlockPos.MutableBlockPos();


    @Override
    public void addPriority(int typeId, int count) {
        this.priorities.add(new AiArmyPriority(typeId, count));
    }

    /**
     * Set where units will gather while they are not used
     *
     * @param player
     */
    public void setDefaultGatherPoint(IPlayer player) {
        // we try to find a base, either a base with a Capitol, or any base
        var base = player.getBasesFiltered(b -> b.hasCapitol()).findFirst().orElse(null);

        if (base == null) {
            base = player.getBasesFiltered(b -> true).findFirst().orElse(null);
        }

        if (base == null) {
            // if we can't find a base, fallback to the center of the map
            this.defaultGatherPoint.set(0, 0, 0);
            WorldApi.getTerrainHeight(this.defaultGatherPoint);
            return;
        }

        // if we have a base, choose which side of the base to gather at
        var dX = GeometryUtils.distanceX(base, Vec3i.ZERO);
        var dZ = GeometryUtils.distanceZ(base, Vec3i.ZERO);

        if (dX > 0) {
            var x = base.getX();
            if (x > 0) {
                this.defaultGatherPoint.setX(base.getMinX() + 15);
            } else {
                this.defaultGatherPoint.setX(base.getMaxX() - 15);
            }
        } else {
            this.defaultGatherPoint.setX(Math.min(Math.max(0, base.getMinX() + 15), base.getMaxX() - 15));
        }

        if (dZ > 0) {
            var z = base.getZ();
            if (z > 0) {
                this.defaultGatherPoint.setZ(base.getMinZ() + 15);
            } else {
                this.defaultGatherPoint.setZ(base.getMaxZ() - 15);
            }
        } else {
            this.defaultGatherPoint.setZ(Math.min(Math.max(0, base.getMinZ() + 15), base.getMaxZ() - 15));
        }

        // adjust to terrain height
        WorldApi.getTerrainHeight(this.defaultGatherPoint);
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

    public void setAttackTarget(IPlayer player) {
        var armyPop = player.getArmyPop();
        var workerPop = player.getWorkerPop();
        if (armyPop * 2 < workerPop) {
            this.armyTarget = null;
            return; // army is too small, shouldn't try to attack
        }

        var enemies = WorldApi.getPlayerEnemies(player).toList();
        var enemyBases = enemies.stream()
                .flatMap(p -> p.getBasesFiltered(b -> !b.buildings.isEmpty()))
                .sorted((a, b) -> GeometryUtils.distanceComparator(a, b, this.defaultGatherPoint))
                .toList();
        if (!enemyBases.isEmpty()) {
            this.armyTarget = enemyBases.get(0).buildings.get(0);
            return;
        }

        var enemyUnits = enemies.stream()
                .flatMap(p -> p.getUnitsFiltered(u -> u.isAlive()))
                .sorted((a, b) -> GeometryUtils.distanceComparator(a, b, this.defaultGatherPoint))
                .toList();
        if (!enemyUnits.isEmpty()) {
            this.armyTarget = enemyUnits.get(0);
            return;
        }

        this.armyTarget = null;
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
