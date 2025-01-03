/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import net.minecraft.core.BlockPos;

/**
 *
 * @author Daratrix
 */
public interface IWidget {
    // queries
    public String getName();
    public int getHealth();
    public int getMaxHealth();
    public boolean isAlive();
    public boolean isDead();
    public int getTypeId();
    public boolean is(int priority);
    public boolean isAnyOf(int... priorities);
    
    // mutations
    public void kill();

}
