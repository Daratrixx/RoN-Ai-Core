/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import net.minecraft.core.BlockPos;
import com.solegendary.reignofnether.building.Building;

import java.util.List;

/**
 *
 * @author Daratrix
 */
public interface IBuilding extends IPlayerWidget, IStructure {
    // queries
    Building getBuilding();
    public List<Integer> getProductionQueue();
    public float getProductionProgress();
    public BlockPos getPos();
    int getPopSupplied();

    public boolean isDone();
    public boolean isUnderConstruction();

    public int countBuilders();

    boolean isCapitol();
    boolean canBeGarrisoned();
    boolean hasUpgrade(int typeId);
    boolean isUpgraded();

    // mutations

    boolean issueTrainOrder(int typeId);

    boolean issueResearchOrder(int typeId);

    boolean issueUpgradeOrder(int typeId);
}
