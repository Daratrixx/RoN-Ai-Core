/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import net.minecraft.core.BlockPos;

import java.util.Collection;

/**
 *
 * @author Daratrix
 */
public interface IWidget extends ILocated {
    // queries
    public String getName();
    public int getHealth();
    public int getMaxHealth();
    public boolean isAlive();
    public boolean isDead();
    public int getTypeId();
    public boolean is(int typeId);
    default boolean isAnyOf(int... typeIds) {
        for (int typeId : typeIds) {
            if (this.is(typeId)) {
                return true;
            }
        }

        return false;
    }

    default boolean isAnyOf(Iterable<Integer> typeIds) {
        for (int typeId : typeIds) {
            if (this.is(typeId)) {
                return true;
            }
        }

        return false;
    }

    default boolean isMeleeAttacker() {
        return false;
    }

    default boolean isRangedAttacker() {
        return false;
    }

    // mutations
    public void kill();

}
