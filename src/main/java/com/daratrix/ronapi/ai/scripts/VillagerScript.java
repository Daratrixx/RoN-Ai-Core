package com.daratrix.ronapi.ai.scripts;

import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.solegendary.reignofnether.util.Faction;

public class VillagerScript extends IAiLogic.AbstractAiLogic {

    public static void register() {
        AiLogics.registerAiLogic(new VillagerScript());
    }

    public static final String name = "VillagerScript";

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
        if (player.countDone(TypeIds.Villagers.TownCentre) == 0) {
            priorities.addPriority(TypeIds.Villagers.TownCentre, 1);
            return;
        }

        if (player.count(TypeIds.Villagers.Villager) < 7) {
            return; // wait to have a few villagers before making the first house/building
        }

        // dynamically add houses any time we have less than 5 pop space to spare
        int productionPower = 4 // TC buffer
                + player.count(TypeIds.Villagers.Barracks) * 3
                + player.count(TypeIds.Villagers.Blacksmith) * 4
                + player.count(TypeIds.Villagers.ArcaneTower) * 3
                + player.count(TypeIds.Villagers.Castle) * 6;
        var popCap = player.getPopCap();
        var popUsed = player.getPopUsed();
        var popFree = popCap - popUsed;
        priorities.logger.log("productionPower: " + productionPower + "(" + popUsed + "/" + popCap + ")");
        if (popCap < this.getMaxPopulation() && popFree < productionPower) {
            var existing = player.countDone(TypeIds.Villagers.House);
            var target = 1 + (int) (productionPower / 10) + existing;
            priorities.logger.log("more houses: " + existing + "=>" + target);
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
        priorities.addPriority(TypeIds.Villagers.Farm, 1, AiProductionPriorities.Location.FARM);
        //priorities.addPriority(TypeIds.Villagers.House, 2);
        priorities.addPriority(TypeIds.Villagers.Barracks, 1);
        priorities.addPriority(TypeIds.Villagers.Farm, 3, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Villagers.Barracks, 2);
        priorities.addPriority(TypeIds.Villagers.Altar, 1);
        priorities.addPriority(TypeIds.Villagers.Farm, 6, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Villagers.Blacksmith, 1);
        priorities.addPriority(TypeIds.Villagers.Barracks, 3);
        priorities.addPriority(TypeIds.Villagers.Farm, 9, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Villagers.ArcaneTower, 1);
        priorities.addPriority(TypeIds.Villagers.Library, 1);
        priorities.addPriority(TypeIds.Villagers.Barracks, 4);
        priorities.addPriority(TypeIds.Villagers.Farm, 12, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Villagers.GrandLibrary, 1);
        priorities.addPriority(TypeIds.Villagers.Castle, 1);
        priorities.addPriority(TypeIds.Villagers.Barracks, 5);
        priorities.addPriority(TypeIds.Villagers.OfficersQuarters, 1);
    }

    public void setUnitPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Villagers.Villager, 8);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 2);
        priorities.addPriority(TypeIds.Villagers.Pillager, 2);
        priorities.addPriority(TypeIds.Villagers.Villager, 16);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 6);
        priorities.addPriority(TypeIds.Villagers.Pillager, 6);
        priorities.addPriority(TypeIds.Villagers.HeroRoyalGuard, 1);
        priorities.addPriority(TypeIds.Villagers.Villager, 24);
        priorities.addPriority(TypeIds.Villagers.Witch, 3);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 12);
        priorities.addPriority(TypeIds.Villagers.Pillager, 9);
        priorities.addPriority(TypeIds.Villagers.Villager, 32);
        priorities.addPriority(TypeIds.Villagers.IronGolem, 3);
        priorities.addPriority(TypeIds.Villagers.Evoker, 3);
        priorities.addPriority(TypeIds.Villagers.Ravager, 3);
    }

    public void setResearchPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Villagers.LingeringPotions, 1);
        priorities.addPriority(TypeIds.Villagers.HealingPotions, 1);
        priorities.addPriority(TypeIds.Villagers.WaterPotions, 1);
        priorities.addPriority(TypeIds.Villagers.Vexes, 1);
        priorities.addPriority(TypeIds.Villagers.RavagerCavalry, 1);
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
        return Faction.VILLAGERS;
    }

    @Override
    public void setPriorities(IAiPlayer player, IAiControllerPriorities priorities) {
        this.setHarvestPriorities(player, priorities.getHarvestingPriorities());
        this.setBuildingPriorities(player, priorities.getBuildingPriorities());
        this.setUnitPriorities(player, priorities.getUnitPriorities());
        this.setResearchPriorities(player, priorities.getResearchPriorities());
        this.setArmyPriorities(player, priorities.getArmyPriorities());
    }

    @Override
    public boolean useWarpten() {
        return false;
    }

    @Override
    public boolean useGreedisgood() {
        return false;
    }
}
