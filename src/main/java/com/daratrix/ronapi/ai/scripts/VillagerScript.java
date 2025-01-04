package com.daratrix.ronapi.ai.scripts;

import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.solegendary.reignofnether.util.Faction;

import java.util.stream.Stream;

public class VillagerScript implements IAiLogic {

    public static void register() {
        AiLogics.registerAiLogic(new VillagerScript());
    }

    public static final String name = "VillagerScript";

    private void setHarvestPriorities(IAiPlayer player, AiHarvestPriorities harvestingPriorities) {
        var farmCount = player.countDone(this.getFarmTypeId());

        harvestingPriorities.addPriority(TypeIds.Resources.FoodEntity, 2);
        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 4);
        harvestingPriorities.addPriority(TypeIds.Resources.FoodBlock, 3);

        for (int i = 0; i < farmCount; ++i) {
            harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, i + 1); // each farm need a wood worker
            harvestingPriorities.addPriority(TypeIds.Resources.FoodFarm, i + 1); // each farm need a farmer
            harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, i / 3); // for every 3 farms, add an ore worker
        }

        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 14);
        harvestingPriorities.addPriority(TypeIds.Resources.FoodFarm, 10);
        harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, 5);

        // in case of excessive workers, we assign to non-food resources
        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 20);
        harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, 10);
    }

    public void setBuildingPriorities(IAiPlayer player, AiProductionPriorities priorities) {

        // until the Town Centre is done, it should be the only priority
        if (player.countDone(TypeIds.Villagers.TownCentre) == 0) {
            priorities.addPriority(TypeIds.Villagers.TownCentre, 1);
            return;
        }

        // dynamically add houses any time we have less than 5 pop space to spare
        int productionPower = 3 // TC buffer
                + player.countDone(TypeIds.Villagers.Barracks) * 3
                + player.countDone(TypeIds.Villagers.Blacksmith) * 4
                + player.countDone(TypeIds.Villagers.ArcaneTower) * 3
                + player.countDone(TypeIds.Villagers.Castle) * 6;
        var popCap = player.getPopCap();
        var popUsed = player.getPopUsed();
        System.out.println("productionPower: " + productionPower + "(" + popUsed + "/" + popCap + ")");
        if (popCap < player.getMaxPop() && popCap - popUsed < productionPower) {
            var existing = player.countDone(TypeIds.Villagers.House);
            var target = 1 + (int) (productionPower / 10) + existing;
            System.out.println("more houses: " + existing + "=>" + target);
            priorities.addPriority(TypeIds.Villagers.House, target);
            if (popCap - popUsed < 3) {
                return;
            }
        }

        // dynamically create stockpiles every time nearest resource distance becomes too high
        //var townCentre = player.getUnitsFiltered(u -> u.getTypeId() == TypeIds.Villagers.TownCentre);
        //var stockpiles = player.getUnitsFiltered(u -> u.getTypeId() == TypeIds.Villagers.Stockpile);
        //var dropoffPoints = Stream.concat(townCentre, stockpiles);
        //if (dropoffPoints.noneMatch(u -> WorldApi.hasResourcesWithinDistanceOfType(u, 32, TypeIds.Resources.WoodBlock))) {
        //
        //}
        //if (dropoffPoints.noneMatch(u -> WorldApi.hasResourcesWithinDistanceOfType(u, 32, TypeIds.Resources.OreBlock))) {
        //
        //}

        //priorities.addPriority(TypeIds.Villagers.Stockpile, 1);
        priorities.addPriority(TypeIds.Villagers.House, 1); // at least one house before farm
        priorities.addPriority(TypeIds.Villagers.Farm, 3);
        //priorities.addPriority(TypeIds.Villagers.House, 2);
        priorities.addPriority(TypeIds.Villagers.Barracks, 1);
        priorities.addPriority(TypeIds.Villagers.Farm, 6);
        priorities.addPriority(TypeIds.Villagers.Barracks, 2);
        priorities.addPriority(TypeIds.Villagers.Blacksmith, 1);
        priorities.addPriority(TypeIds.Villagers.Barracks, 3);
        priorities.addPriority(TypeIds.Villagers.ArcaneTower, 1);
        priorities.addPriority(TypeIds.Villagers.Barracks, 4);
        priorities.addPriority(TypeIds.Villagers.Castle, 1);
        priorities.addPriority(TypeIds.Villagers.Barracks, 5);
        priorities.addPriority(TypeIds.Villagers.Library, 1);
    }

    public void setUnitPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Villagers.Villager, 8);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 2);
        priorities.addPriority(TypeIds.Villagers.Pillager, 2);
        priorities.addPriority(TypeIds.Villagers.Villager, 16);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 6);
        priorities.addPriority(TypeIds.Villagers.Pillager, 6);
        priorities.addPriority(TypeIds.Villagers.Villager, 24);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 12);
        priorities.addPriority(TypeIds.Villagers.Pillager, 12);
        priorities.addPriority(TypeIds.Villagers.Villager, 32);
        priorities.addPriority(TypeIds.Villagers.IronGolem, 3);
        priorities.addPriority(TypeIds.Villagers.Evoker, 3);
        priorities.addPriority(TypeIds.Villagers.Ravager, 3);
    }

    private void setArmyPriorities(IAiPlayer player, AiArmyPriorities armyPriorities) {
        armyPriorities.setDefaultGatherPoint(player);
        armyPriorities.setAttackTarget(player);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Faction getFaction() {
        return Faction.VILLAGERS;
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
