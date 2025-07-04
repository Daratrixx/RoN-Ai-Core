package com.daratrix.ronapi.ai.scripts;

import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.models.interfaces.IHero;
import com.solegendary.reignofnether.ability.heroAbilities.monster.BloodMoon;
import com.solegendary.reignofnether.ability.heroAbilities.monster.InsomniaCurse;
import com.solegendary.reignofnether.ability.heroAbilities.monster.RaiseDead;
import com.solegendary.reignofnether.ability.heroAbilities.monster.SoulSiphonPassive;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.FancyFeast;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.GreedIsGoodPassive;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.LootExplosion;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.ThrowTNT;
import com.solegendary.reignofnether.util.Faction;

public class PiglinScript extends IAiLogic.AbstractAiLogic {

    public static void register() {
        AiLogics.registerAiLogic(new PiglinScript());
    }

    public static final String name = "PiglinScript";

    public PiglinScript() {
        super();

        this.heroSkillLogics.put(TypeIds.Piglins.HeroMerchant, PiglinScript::MerchantSkills);
    }

    private static void MerchantSkills(IHero hero) {
        hero.upgradeSkill(ThrowTNT.class, 1);
        hero.upgradeSkill(FancyFeast.class, 1);
        hero.upgradeSkill(GreedIsGoodPassive.class, 1);
        hero.upgradeSkill(ThrowTNT.class, 2);
        hero.upgradeSkill(FancyFeast.class, 2);
        hero.upgradeSkill(LootExplosion.class, 1);
        hero.upgradeSkill(GreedIsGoodPassive.class, 2);
        hero.upgradeSkill(ThrowTNT.class, 3);
        hero.upgradeSkill(FancyFeast.class, 3);
        hero.upgradeSkill(GreedIsGoodPassive.class, 3);
    }

    protected int portalCount = 0;
    protected int civilianPortalCount = 0;
    protected int militaryPortalCount = 0;
    protected int advancedPortalCount = 0;

    protected void BuildPortal(AiProductionPriorities priorities, int count) {
        if (count < portalCount) {
            return;
        }

        priorities.addPriority(TypeIds.Piglins.Portal, count);
        this.portalCount = count;
    }

    protected void BuildCivilianPortal(AiProductionPriorities priorities, int count) {
        if (count < civilianPortalCount) {
            return;
        }

        this.BuildPortal(priorities, count + militaryPortalCount + advancedPortalCount);
        priorities.addPriority(TypeIds.Piglins.CivilianPortal, count);
        this.civilianPortalCount = count;
    }

    protected void BuildMilitaryPortal(AiProductionPriorities priorities, int count) {
        if (count < militaryPortalCount) {
            return;
        }

        this.BuildPortal(priorities, civilianPortalCount + count + advancedPortalCount);
        priorities.addPriority(TypeIds.Piglins.MilitaryPortal, count);
        this.militaryPortalCount = count;
    }

    protected void BuildAdvancedPortal(AiProductionPriorities priorities, int count) {
        if (count < advancedPortalCount) {
            return;
        }

        this.BuildPortal(priorities, civilianPortalCount + militaryPortalCount + count);
        priorities.addPriority(TypeIds.Piglins.AdvancedPortals, count);
        this.advancedPortalCount = count;
    }

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

        if (player.count(TypeIds.Piglins.Grunt) < 6) {
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
            var existing = player.countDone(TypeIds.Piglins.CivilianPortal);
            var target = 1 + (int) (productionPower / 15) + existing;
            priorities.logger.log("more houses: " + existing + "=>" + target);
            this.BuildCivilianPortal(priorities, target);
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
        this.BuildCivilianPortal(priorities, 1); // at least one house before farm
        priorities.addPriority(TypeIds.Piglins.Farm, 1, AiProductionPriorities.Location.FARM);
        this.BuildMilitaryPortal(priorities, 1);
        priorities.addPriority(TypeIds.Piglins.Farm, 3, AiProductionPriorities.Location.FARM);
        this.BuildMilitaryPortal(priorities,  2);
        priorities.addPriority(TypeIds.Piglins.Altar, 1);
        priorities.addPriority(TypeIds.Piglins.Farm, 6, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Piglins.Bastion, 1);
        priorities.addPriority(TypeIds.Piglins.Farm, 9, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Piglins.HoglinStables, 1);
        priorities.addPriority(TypeIds.Piglins.FlameSanctuary, 1);
        this.BuildMilitaryPortal(priorities,  4);
        priorities.addPriority(TypeIds.Piglins.Farm, 12, AiProductionPriorities.Location.FARM);
        priorities.addPriority(TypeIds.Piglins.WitherShrine, 1);
        priorities.addPriority(TypeIds.Piglins.Fortress, 1);
        this.BuildMilitaryPortal(priorities,  5);
    }

    public void setUnitPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Piglins.Grunt, 8);
        priorities.addPriority(TypeIds.Piglins.Brute, 3);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 4);
        priorities.addPriority(TypeIds.Piglins.Grunt, 16);
        priorities.addPriority(TypeIds.Piglins.Brute, 10);
        priorities.addPriority(TypeIds.Piglins.HeroMerchant, 1);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 12);
        priorities.addPriority(TypeIds.Piglins.Grunt, 30);
        priorities.addPriority(TypeIds.Piglins.Brute, 14);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 14);
        priorities.addPriority(TypeIds.Piglins.Blaze, 5);
        priorities.addPriority(TypeIds.Piglins.WitherSkeleton, 6);
        priorities.addPriority(TypeIds.Piglins.Ghast, 3);
    }

    public void setResearchPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Piglins.HeadhunterTrident, 1);
        priorities.addPriority(TypeIds.Piglins.BruteShield, 1);
        priorities.addPriority(TypeIds.Piglins.HoglinCavalry, 1);
        priorities.addPriority(TypeIds.Piglins.FireResistance, 1);
        priorities.addPriority(TypeIds.Piglins.BlazeFirewall, 1);
        priorities.addPriority(TypeIds.Piglins.WitherCloud, 1);
        priorities.addPriority(TypeIds.Piglins.AdvancedPortals, 1);
        priorities.addPriority(TypeIds.Piglins.SoulFireballs, 1);
        priorities.addPriority(TypeIds.Piglins.Bloodlust, 1);
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
        this.portalCount = 0;
        this.civilianPortalCount = 0;
        this.militaryPortalCount = 0;
        this.advancedPortalCount = 0;
        this.setHarvestPriorities(player, priorities.getHarvestingPriorities());
        this.setBuildingPriorities(player, priorities.getBuildingPriorities());
        this.setUnitPriorities(player, priorities.getUnitPriorities());
        this.setResearchPriorities(player, priorities.getResearchPriorities());
        this.setArmyPriorities(player, priorities.getArmyPriorities());
    }
}
