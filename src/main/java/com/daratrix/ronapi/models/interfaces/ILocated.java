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
public interface ILocated {
    public float getX();
    public float getY();
    public float getZ();
    public BlockPos getPos();
}
