/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models;

import com.daratrix.ronapi.apis.BuildingApi;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IOrder;
import com.daratrix.ronapi.models.interfaces.IWidget;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Daratrix
 */
public class ApiBuilding implements IBuilding {

    protected final Building building;
    protected final ProductionBuilding production;
    protected final int typeId;
    private final AABB boundingBox;
    protected final boolean isCapitol;

    public ApiBuilding(Building building) {
        this.building = building;
        this.production = building instanceof ProductionBuilding p ? p : null;
        this.typeId = TypeIds.get(building);
        this.boundingBox = GeometryUtils.getBoundingBox(building.minCorner, building.maxCorner.offset(1, 1, 1));
        this.isCapitol = building.isCapitol;
    }

    @Override
    public List<Integer> getProductionQueue() {
        return List.of();
    }

    @Override
    public float getProductionProgress() {
        return 0;
    }

    @Override
    public BlockPos getPos() {
        return this.building.originPos;
    }

    @Override
    public int getPopSupplied() {
        return this.building.popSupply;
    }

    @Override
    public boolean isDone() {
        return this.building.isBuilt;
    }

    @Override
    public boolean isUnderConstruction() {
        return !this.building.isBuilt;
    }

    @Override
    public String getOwnerName() {
        return building.ownerName;
    }

    @Override
    public void setOwnerByName(String ownerName) {
        building.ownerName = ownerName;
    }

    @Override
    public int getCurrentOrderId() {
        return 0;
    }

    @Override
    public Object getCurrentOrderTarget() {
        return null;
    }

    @Override
    public int countBuilders() {
        return this.building.getBuilders(Minecraft.getInstance().level).size();
    }

    @Override
    public boolean isCapitol() {
        return this.isCapitol;
    }

    @Override
    public List<IOrder> getOrderQueue() {
        return List.of();
    }

    @Override
    public boolean isIdle() {
        return this.production == null || this.production.productionQueue.isEmpty();
    }

    @Override
    public boolean issuePointOrder(int x, int y, int z, int orderId) {
        return false;
    }

    @Override
    public boolean issuePointOrder(BlockPos pos, int orderId) {
        return false;
    }

    @Override
    public boolean issueWidgetOrder(IWidget target, int orderId) {
        return false;
    }

    @Override
    public boolean issueWidgetOrder(LivingEntity target, int orderId) {
        return false;
    }

    @Override
    public boolean issueWidgetOrder(Building target, int orderId) {
        return false;
    }

    @Override
    public boolean issueOrder(int typeId) {
        return false;
    }

    @Override
    public boolean issueTrainOrder(int typeId) {
        if (this.production == null) {
            return false;
        }

        var production = BuildingApi.toProductionItem(typeId, this.production);
        if (production == null || !production.canAfford(this.getOwnerName())) {
            return false;
        }

        this.production.productionQueue.add(production);
        ResourcesServerEvents.addSubtractResources(new Resources(
                building.ownerName,
                -production.foodCost,
                -production.woodCost,
                -production.oreCost
        ));

        return true;
    }

    @Override
    public boolean issueResearchOrder(int typeId) {
        return this.issueTrainOrder(typeId);
    }

    @Override
    public boolean issueUpgradeOrder(int typeId) {
        return this.issueTrainOrder(typeId);
    }

    @Override
    public Building getBuilding() {
        return this.building;
    }

    @Override
    public boolean clearOrder(boolean clearQueue) {
        return false;
    }

    @Override
    public int getMinX() {
        return this.building.minCorner.getX();
    }

    @Override
    public int getMaxX() {
        return this.building.maxCorner.getX();
    }

    @Override
    public int getMinY() {
        return this.building.minCorner.getY();
    }

    @Override
    public int getMaxY() {

        return this.building.maxCorner.getY();
    }

    @Override
    public int getMinZ() {
        return this.building.minCorner.getZ();
    }

    @Override
    public int getMaxZ() {
        return this.building.maxCorner.getZ();
    }

    @Override
    public BlockPos getMinPos() {
        return this.building.minCorner;
    }

    @Override
    public BlockPos getMaxPos() {
        return this.building.maxCorner;
    }

    @Override
    public int getBlockCount() {
        return 0;
    }

    @Override
    public Stream<BlockPos> getBlocks() {
        return Stream.empty();
    }

    @Override
    public AABB getBoundingBox() {
        return this.boundingBox;
    }

    @Override
    public void destroy() {
        //this.building.destroy(this.building.getLevel());
    }

    @Override
    public String getName() {
        return this.building.name;
    }

    @Override
    public int getHealth() {
        return this.building.getHealth();
    }

    @Override
    public int getMaxHealth() {
        return this.building.getMaxHealth();
    }

    @Override
    public boolean isAlive() {
        return this.building.getHealth() > 0;
    }

    @Override
    public boolean isDead() {
        return this.building.getHealth() <= 0;
    }

    @Override
    public float getX() {
        return this.building.centrePos.getX();
    }

    @Override
    public float getY() {
        return this.building.centrePos.getY();
    }

    @Override
    public float getZ() {
        return this.building.centrePos.getZ();
    }

    @Override
    public void kill() {

    }

    @Override
    public int getTypeId() {
        return this.typeId;
    }

    @Override
    public boolean is(int priority) {
        return this.typeId == priority;
    }

    @Override
    public boolean isAnyOf(int... priorities) {
        for (int priority : priorities) {
            if (this.typeId == priority) {
                return true;
            }
        }

        return false;
    }
}