/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.ai.player.interfaces;

import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IPlayer;
import com.daratrix.ronapi.models.interfaces.IUnit;

import java.util.Collection;
import java.util.stream.Stream;

/**
 *
 * @author Daratrix
 */
public interface IAiPlayer extends IPlayer {
    public int count(int priority);
    public int count(int priority, AiProductionPriorities.Location location);
    public int countDone(int priority);

    int countDone(int priority, AiProductionPriorities.Location location);

    public Stream<IUnit> getIdleUnits(int priority);
    public Stream<IUnit> getIdleUnits(Collection<Integer> priorities);
    public Stream<IUnit> getUnits(int priority);
    public Stream<IUnit> getUnits(Collection<Integer> priorities);
    public Stream<IBuilding> getIdleBuildings(int priority);
    public Stream<IBuilding> getIdleBuildings(Collection<Integer> priorities);
    public Stream<IBuilding> getBuildings(int priority);
    public Stream<IBuilding> getBuildings(Collection<Integer> priorities);

    Stream<IBuilding> getConstrutingBuildings();

    void checkCompletedBuildings();
}
