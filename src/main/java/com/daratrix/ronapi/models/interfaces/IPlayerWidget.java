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
public interface IPlayerWidget extends IWidget, IOrderable {
    // queries
    public String getOwnerName();
    
    // mutations
    public void setOwnerByName(String ownerName);
}
