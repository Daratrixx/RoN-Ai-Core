/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.stream.Stream;

/**
 *
 * @author Daratrix
 */
public interface IStructure extends IBoxed {
    // queries
    public int getBlockCount();
    public Stream<BlockPos> getBlocks();
    
    // mutations
    public void destroy();
}
