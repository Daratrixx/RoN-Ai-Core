package com.daratrix.ronapi.ai.controller.interfaces;

import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.solegendary.reignofnether.util.Faction;

public interface IAiLogic {
    public String getName();

    public Faction getFaction();

    public void setPriorities(IAiPlayer player, IAiControllerPriorities priorities);

    default int getWorkerTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Villager;
            case MONSTERS -> TypeIds.Monsters.Villager;
            case PIGLINS -> TypeIds.Piglins.Grunt;
            default -> 0;
        };
    }

    default int getCapitolTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.TownCentre;
            case MONSTERS -> TypeIds.Monsters.Mausoleum;
            case PIGLINS -> TypeIds.Piglins.MainPortal;
            default -> 0;
        };
    }

    default int getFarmTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Farm;
            case MONSTERS -> TypeIds.Monsters.Farm;
            case PIGLINS -> TypeIds.Piglins.Farm;
            default -> 0;
        };
    }

    default int getTowerTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Watchtower;
            case MONSTERS -> TypeIds.Monsters.Watchtower;
            case PIGLINS -> TypeIds.Piglins.Bastion;
            default -> 0;
        };
    }

    default int getCastleTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Castle;
            case MONSTERS -> TypeIds.Monsters.Stronghold;
            case PIGLINS -> TypeIds.Piglins.Fortress;
            default -> 0;
        };
    }

    // indicate how many structures the AI can build at the same time
    default int getConcurrentBuildingLimit(IAiPlayer player) {
        return (int) 1 + player.countDone(this.getWorkerTypeId()) / 8;
    }

    // indicate how many workers can be pulled away from tasks to build new structures
    default int getConcurrentBuilderLimit(IAiPlayer player) {
        if (player.countDone(this.getCapitolTypeId()) == 0) {
            return player.getMaxPop(); // allow all available workers to build the TC
        }

        return (int) 1 + player.countDone(this.getWorkerTypeId()) / 8;
    }

    default boolean useWarpten() {
        return true;
    }

    default boolean useGreedisgood() {
        return true;
    }
}
