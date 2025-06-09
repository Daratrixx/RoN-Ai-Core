/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * @author Daratrix
 */
public interface IOrderable {
    // queries
    public int getCurrentOrderId();
    public Object getCurrentOrderTarget();
    public List<IOrder> getOrderQueue();
    public default boolean isIdle() {
        return this.getCurrentOrderId() == 0;
    }

    // mutations
    public default boolean issuePointOrder(int x, int y, int z, int orderId) {
        return this.issuePointOrder(new BlockPos(x, y, z), orderId);
    }

    public boolean issuePointOrder(BlockPos pos, int orderId);
    public boolean issueWidgetOrder(IWidget target, int orderId);
    public boolean issueWidgetOrder(LivingEntity target, int orderId);
    public boolean issueWidgetOrder(BuildingPlacement target, int orderId);
    public boolean issueOrder(int typeId);
    public boolean clearOrder(boolean clearQueue);

    //public boolean queuePointOrder(int x, int y, int z, int orderId);
    //public boolean queueWidgetOrder(IWidget target, int orderId);
}
