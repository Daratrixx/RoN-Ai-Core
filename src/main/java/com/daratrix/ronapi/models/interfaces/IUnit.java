/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import com.daratrix.ronapi.apis.TypeIds;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * @author Daratrix
 */
public interface IUnit extends IPlayerWidget, ILocated {

    // queries
    public int getPopUsed();
    public boolean isWorker();
    public boolean isHero();
    public int getId();

    public boolean isMaxCarryCapacityReached();
    public boolean isCarryCapacityOver(int capacity);
    boolean isCarryingItems();

    Unit getUnit();
    WorkerUnit getWorkerUnit();

    // mutations
    boolean issueBuildOrder(BlockPos pos, int typeId);

    default boolean issueBuildOrder(int x, int y, int z, int typeId) {
        return issueBuildOrder(new BlockPos(x, y, z), typeId);
    }

    public default boolean issueRepairOrder(IBuilding building) {
        return this.issueWidgetOrder(building, TypeIds.Orders.Repair);
    }

    public default boolean issueAttackOrder(IBuilding building) {
        return this.issueWidgetOrder(building, TypeIds.Orders.AttackBuilding);
    }

    public default boolean issueHarvestOrder(IBuilding building) {
        return this.issueWidgetOrder(building, TypeIds.Orders.HarvestFarm);
    }

    public default boolean issueHarvestOrder(BuildingPlacement building) {
        return this.issueWidgetOrder(building, TypeIds.Orders.HarvestFarm);
    }

    public default boolean issueHarvestOrder(IResource resource) {
        return this.issuePointOrder(resource.getNearestBlock(this), TypeIds.Orders.HarvestBlock);
    }

    public default boolean issueAttackOrder(IUnit unit) {
        return this.issueWidgetOrder(unit, TypeIds.Orders.AttackUnit);
    }

    public default boolean issueAttackOrder(LivingEntity entity) {
        return this.issueWidgetOrder(entity, TypeIds.Orders.AttackUnit);
    }

    public default boolean issueAttackOrder(BlockPos pos) {
        return this.issuePointOrder(pos, TypeIds.Orders.AttackMove);
    }

    public default boolean issueAttackOrder(int x, int y, int z) {
        return this.issuePointOrder(x, y, z, TypeIds.Orders.AttackMove);
    }

    public default boolean issueAttackOrder(float x, float y, float z) {
        return this.issuePointOrder((int)x, (int)y, (int)z, TypeIds.Orders.AttackMove);
    }

    public default boolean issueAttackGroundOrder(BlockPos pos) {
        return this.issuePointOrder(pos, TypeIds.Orders.AttackGround);
    }

    public default boolean issueAttackGroundOrder(int x, int y, int z) {
        return this.issuePointOrder(x, y, z, TypeIds.Orders.AttackGround);
    }

    public default boolean issueMoveOrder(BlockPos pos) {
        return this.issuePointOrder(pos, TypeIds.Orders.Move);
    }

    public default boolean issueMoveOrder(int x, int y, int z) {
        return this.issuePointOrder(x, y, z, TypeIds.Orders.Move);
    }

    public default boolean issueMoveOrder(float x, float y, float z) {
        return this.issuePointOrder((int)x, (int)y, (int)z, TypeIds.Orders.Move);
    }
}
