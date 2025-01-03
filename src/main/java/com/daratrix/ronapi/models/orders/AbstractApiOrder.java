/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.orders;

import com.daratrix.ronapi.models.interfaces.*;

/**
 *
 * @author Daratrix
 */
public abstract class AbstractApiOrder implements IOrder {

    protected IPlayerWidget sourceWidget;
    protected int id;

    @Override
    public IPlayerWidget getSourceWidget() {
        return this.sourceWidget;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public IWidget getTargetWidget() {
        return null;
    }

    @Override
    public IUnit getTargetUnit() {
        return null;
    }

    @Override
    public IBuilding getTargetBuilding() {
        return null;
    }

    @Override
    public IResource getTargetResource() {
        return null;
    }

    @Override
    public int getTargetX() {
        return 0;
    }

    @Override
    public int getTargetY() {
        return 0;
    }

    @Override
    public int getTargetZ() {
        return 0;
    }
}
