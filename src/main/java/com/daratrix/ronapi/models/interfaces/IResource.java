/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import com.solegendary.reignofnether.resources.ResourceName;

/**
 *
 * @author Daratrix
 */
public interface IResource extends IStructure {
    public int getAmount();
    public ResourceName getResourceType();

    int getTypeId();
}
