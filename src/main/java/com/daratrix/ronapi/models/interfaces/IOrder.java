/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

/**
 *
 * @author Daratrix
 */
public interface IOrder {
    public IPlayerWidget getSourceWidget();
    public int getId();
    public IWidget getTargetWidget();
    public IUnit getTargetUnit();
    public IBuilding getTargetBuilding();
    public IResource getTargetResource();
    public int getTargetX();
    public int getTargetY();
    public int getTargetZ();
}
