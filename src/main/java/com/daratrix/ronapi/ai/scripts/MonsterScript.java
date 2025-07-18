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
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.world.level.Level;

public class MonsterScript extends IAiLogic.AbstractAiLogic {

    public static void register() {
        AiLogics.registerAiLogic(new MonsterScript());
    }

    public static final String name = "MonsterScript";

    public MonsterScript() {
        super();

        this.heroSkillLogics.put(TypeIds.Monsters.HeroNecromancer, MonsterScript::NecromancerSkills);
    }

    private static void NecromancerSkills(IHero hero) {
        hero.upgradeSkill(RaiseDead.class, 1);
        hero.upgradeSkill(InsomniaCurse.class, 1);
        hero.upgradeSkill(SoulSiphonPassive.class, 1);
        hero.upgradeSkill(RaiseDead.class, 2);
        hero.upgradeSkill(InsomniaCurse.class, 2);
        hero.upgradeSkill(BloodMoon.class, 1);
        hero.upgradeSkill(SoulSiphonPassive.class, 2);
        hero.upgradeSkill(RaiseDead.class, 3);
        hero.upgradeSkill(InsomniaCurse.class, 3);
        hero.upgradeSkill(SoulSiphonPassive.class, 3);
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

        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 16);
        harvestingPriorities.addPriority(TypeIds.Resources.FoodFarm, 12);
        harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, 6);

        // in case of excessive workers, we assign to non-food resources
        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 20);
        harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, 12);
    }

    public void setBuildingPriorities(IAiPlayer player, AiProductionPriorities priorities) {

        // until the Town Centre is done, it should be the only priority
        if (player.countDone(TypeIds.Monsters.Mausoleum) == 0) {
            priorities.addPriority(TypeIds.Monsters.Mausoleum, 1);
            return;
        }

        // dynamically add houses any time we have less than 5 pop space to spare
        int productionPower = 3 // TC buffer
                + player.countDone(TypeIds.Monsters.Mausoleum) * 3
                + player.countDone(TypeIds.Monsters.Graveyard) * 2
                + player.countDone(TypeIds.Monsters.Dungeon) * 2
                + player.countDone(TypeIds.Monsters.SpiderLair) * 2
                + player.countDone(TypeIds.Monsters.Stronghold) * 4;
        if (player.getPopCap() < this.getMaxPopulation() && player.getPopCap() - player.getPopUsed() < productionPower) {
            priorities.addPriority(TypeIds.Monsters.House, 1 + (int) (productionPower / 10) + player.countDone(TypeIds.Monsters.House)).atSide();
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

        //priorities.addPriority(TypeIds.Monsters.Stockpile, 1);
        //priorities.addPriority(TypeIds.Monsters.House, 1);
        priorities.addPriority(TypeIds.Monsters.Farm, 3).atFarm();
        //priorities.addPriority(TypeIds.Monsters.House, 2);
        priorities.addPriority(TypeIds.Monsters.Graveyard, 1).atFront();
        priorities.addPriority(TypeIds.Monsters.Farm, 6).atFarm();
        priorities.addPriority(TypeIds.Monsters.Altar, 1).atBack();
        priorities.addPriority(TypeIds.Monsters.Graveyard, 2).atFront();
        priorities.addPriority(TypeIds.Monsters.Laboratory, 1).atBack();
        priorities.addPriority(TypeIds.Monsters.Graveyard, 3).atFront();
        priorities.addPriority(TypeIds.Monsters.Dungeon, 1).atFront();
        priorities.addPriority(TypeIds.Monsters.Graveyard, 4).atFront();
        priorities.addPriority(TypeIds.Monsters.SpiderLair, 1).atBack();
        priorities.addPriority(TypeIds.Monsters.Graveyard, 5).atFront();
        priorities.addPriority(TypeIds.Monsters.Farm, 9).atFarm();
        priorities.addPriority(TypeIds.Monsters.Stronghold, 1).atFront();
        priorities.addPriority(TypeIds.Monsters.Graveyard, 6).atFront();
        priorities.addPriority(TypeIds.Monsters.Dungeon, 2).atFront();
        priorities.addPriority(TypeIds.Monsters.Graveyard, 7).atFront();
        priorities.addPriority(TypeIds.Monsters.Farm, 12).atFarm();
    }

    public void setUnitPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        var zombie = player.hasResearch(TypeIds.Monsters.HusksUpgrade)
                ? TypeIds.Monsters.Husk
                : TypeIds.Monsters.Zombie;
        var skeleton = player.hasResearch(TypeIds.Monsters.StrayUpgrade)
                ? TypeIds.Monsters.Stray
                : TypeIds.Monsters.Skeleton;
        var spider = player.hasResearch(TypeIds.Monsters.SpiderUpgrade)
                ? TypeIds.Monsters.PoisonSpider
                : TypeIds.Monsters.Spider;
        priorities.addPriority(TypeIds.Monsters.Villager, 8);
        priorities.addPriority(zombie, 4);
        priorities.addPriority(skeleton, 4);
        priorities.addPriority(TypeIds.Monsters.Villager, 16);
        priorities.addPriority(zombie, 10);
        priorities.addPriority(skeleton, 8);
        priorities.addPriority(TypeIds.Monsters.HeroNecromancer, 1);
        priorities.addPriority(TypeIds.Monsters.Villager, 24);
        priorities.addPriority(zombie, 20);
        priorities.addPriority(skeleton, 12);
        priorities.addPriority(spider, 3);
        priorities.addPriority(TypeIds.Monsters.Villager, 28);
        priorities.addPriority(TypeIds.Monsters.Creeper, 2);
        priorities.addPriority(TypeIds.Monsters.Warden, 2);
        priorities.addPriority(zombie, 30);
        priorities.addPriority(skeleton, 20);
        priorities.addPriority(spider, 5);
        priorities.addPriority(TypeIds.Monsters.Creeper, 4);
        priorities.addPriority(TypeIds.Monsters.Villager, 38);
        priorities.addPriority(TypeIds.Monsters.Warden, 5);
    }

    public void setResearchPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Monsters.HusksUpgrade, 1);
        priorities.addPriority(TypeIds.Monsters.SpiderWebs, 1);
        priorities.addPriority(TypeIds.Monsters.StrayUpgrade, 1);
        priorities.addPriority(TypeIds.Monsters.SpiderUpgrade, 1);
        priorities.addPriority(TypeIds.Monsters.SculkAmplifiers, 1);
        priorities.addPriority(TypeIds.Monsters.Silverfish, 1);
    }

    private void setArmyPriorities(Level level, IAiPlayer player, AiArmyPriorities armyPriorities) {
        armyPriorities.pickDefaultGatherPoint(level, player);
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
        return Faction.MONSTERS;
    }

    @Override
    public void setPriorities(Level level, IAiPlayer player, IAiControllerPriorities priorities) {
        this.setHarvestPriorities(player, priorities.getHarvestingPriorities());
        this.setBuildingPriorities(player, priorities.getBuildingPriorities());
        this.setUnitPriorities(player, priorities.getUnitPriorities());
        this.setResearchPriorities(player, priorities.getResearchPriorities());
        this.setArmyPriorities(level, player, priorities.getArmyPriorities());
    }
}
