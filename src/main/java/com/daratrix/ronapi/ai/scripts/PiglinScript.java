package com.daratrix.ronapi.ai.scripts;

import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.solegendary.reignofnether.util.Faction;

public class PiglinScript extends IAiLogic.AbstractAiLogic {

    public static void register() {
        AiLogics.registerAiLogic(new PiglinScript());
    }

    public static final String name = "PiglinScript";

    private void setHarvestPriorities(IAiPlayer player, AiHarvestPriorities harvestingPriorities) {
        var farmCount = player.countDone(this.getFarmTypeId());
        var villagerCount = player.countDone(this.getWorkerTypeId());

        if (player.getFood() < 300) {
            harvestingPriorities.addPriority(TypeIds.Resources.FoodEntity, 2);
        }
        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 4);
        if (player.getFood() < 200) {
            harvestingPriorities.addPriority(TypeIds.Resources.FoodBlock, 3);
        }

        for (int i = 0; i < farmCount; ++i) {
            harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, i + 1); // each farm need a wood worker
            harvestingPriorities.addPriority(TypeIds.Resources.FoodFarm, i + 1); // each farm need a farmer
            harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, i / 3); // for every 3 farms, add an ore worker
        }

        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 18);
        harvestingPriorities.addPriority(TypeIds.Resources.FoodFarm, 12);
        harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, 6);

        // in case of excessive workers, we assign to non-food resources
        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 24);
        harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, 12);
    }

    public void setBuildingPriorities(IAiPlayer player, AiProductionPriorities priorities) {

        priorities.setDefaultLocation(AiProductionPriorities.Location.MAIN);

        // until the Town Centre is done, it should be the only priority
        if (player.countDone(TypeIds.Piglins.MainPortal) == 0) {
            priorities.addPriority(TypeIds.Piglins.MainPortal, 1);
            return;
        }

        if (player.count(TypeIds.Piglins.Grunt) < 7) {
            return; // wait to have a few Piglins before making the first house/building
        }

        // dynamically add houses any time we have less than 5 pop space to spare
        int productionPower = 4 // TC buffer
                + player.count(TypeIds.Piglins.MilitaryPortal) * 3;
        var popCap = player.getPopCap();
        var popUsed = player.getPopUsed();
        var popFree = popCap - popUsed;
        priorities.logger.log("productionPower: " + productionPower + "(" + popUsed + "/" + popCap + ")");
        if (popCap < this.getMaxPopulation() && popFree < productionPower) {
            var existing = player.countDone(TypeIds.Piglins.House);
            var target = 1 + (int) (productionPower / 10) + existing;
            priorities.logger.log("more houses: " + existing + "=>" + target);
            priorities.addPriority(TypeIds.Piglins.House, target);
            if (popCap - popUsed < 3) {
                return;
            }
        }

        // dynamically create stockpiles every time nearest resource distance becomes too high
        //var townCentre = player.getUnitsFiltered(u -> u.getTypeId() == TypeIds.Piglins.TownCentre);
        //var stockpiles = player.getUnitsFiltered(u -> u.getTypeId() == TypeIds.Piglins.Stockpile);
        //var dropoffPoints = Stream.concat(townCentre, stockpiles);
        //if (dropoffPoints.noneMatch(u -> WorldApi.hasResourcesWithinDistanceOfType(u, 32, TypeIds.Resources.WoodBlock))) {
        //
        //}
        //if (dropoffPoints.noneMatch(u -> WorldApi.hasResourcesWithinDistanceOfType(u, 32, TypeIds.Resources.OreBlock))) {
        //
        //}

        //priorities.addPriority(TypeIds.Piglins.Stockpile, 1);
        priorities.addPriority(TypeIds.Piglins.Portal, 1); // at least one house before farm
        priorities.addPriority(TypeIds.Piglins.CivilianPortal, 1); // at least one house before farm
        priorities.addPriority(TypeIds.Piglins.Farm, 1, AiProductionPriorities.Location.FARM);
        //priorities.addPriority(TypeIds.Piglins.House, 2);
        priorities.addPriority(TypeIds.Piglins.Portal, 2);
        priorities.addPriority(TypeIds.Piglins.MilitaryPortal, 1);
        priorities.addPriority(TypeIds.Piglins.Farm, 3, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Piglins.Portal, 3);
        priorities.addPriority(TypeIds.Piglins.MilitaryPortal, 2);
        priorities.addPriority(TypeIds.Piglins.Farm, 6, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Piglins.Bastion, 1);
        priorities.addPriority(TypeIds.Piglins.Farm, 9, AiProductionPriorities.Location.FARM);
        /*priorities.addPriority(TypeIds.Piglins.ArcaneTower, 1);
        priorities.addPriority(TypeIds.Piglins.Library, 1);
        priorities.addPriority(TypeIds.Piglins.Barracks, 4);
        priorities.addPriority(TypeIds.Piglins.Farm, 12, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Piglins.GrandLibrary, 1);
        priorities.addPriority(TypeIds.Piglins.Castle, 1);
        priorities.addPriority(TypeIds.Piglins.Barracks, 5);
        priorities.addPriority(TypeIds.Piglins.OfficersQuarters, 1);*/
    }

    public void setUnitPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Piglins.Grunt, 8);
        priorities.addPriority(TypeIds.Piglins.Brute, 3);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 4);
        priorities.addPriority(TypeIds.Piglins.Grunt, 16);
        priorities.addPriority(TypeIds.Piglins.Brute, 10);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 12);
        priorities.addPriority(TypeIds.Piglins.Grunt, 30);
        priorities.addPriority(TypeIds.Piglins.Brute, 20);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 30);
        priorities.addPriority(TypeIds.Piglins.Grunt, 40);
    }

    private void setArmyPriorities(IAiPlayer player, AiArmyPriorities armyPriorities) {
        armyPriorities.pickDefaultGatherPoint(player);
        if (armyPriorities.pickDefenseTarget(player)) {
            armyPriorities.attackTarget = null; // don't attack while bases are threatened
        } else {
            armyPriorities.pickAttackTarget(player);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Faction getFaction() {
        return Faction.PIGLINS;
    }

    @Override
    public void setPriorities(IAiPlayer player, IAiControllerPriorities priorities) {
        this.setHarvestPriorities(player, priorities.getHarvestingPriorities());
        this.setBuildingPriorities(player, priorities.getBuildingPriorities());
        this.setUnitPriorities(player, priorities.getUnitPriorities());
        this.setArmyPriorities(player, priorities.getArmyPriorities());
    }

    @Override
    public boolean useWarpten() {
        return true;
    }

    @Override
    public boolean useGreedisgood() {
        return true;
    }
}
