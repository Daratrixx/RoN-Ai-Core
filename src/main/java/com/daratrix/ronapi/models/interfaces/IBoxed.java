/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 *
 * @author Daratrix
 */
public interface IBoxed extends ILocated {
    public int getMinX();
    public int getMaxX();
    public int getMinY();
    public int getMaxY();
    public int getMinZ();
    public int getMaxZ();
    public BlockPos getMinPos();
    public BlockPos getMaxPos();
    public AABB getBoundingBox();
}
