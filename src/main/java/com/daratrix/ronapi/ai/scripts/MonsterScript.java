package com.daratrix.ronapi.ai.scripts;

import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.solegendary.reignofnether.util.Faction;

public class MonsterScript implements IAiLogic {

    public static void register() {
        AiLogics.registerAiLogic(new MonsterScript());
    }

    public static final String name = "MonsterScript";

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
                + player.countDone(TypeIds.Monsters.Dungeon) * 3
                + player.countDone(TypeIds.Monsters.SpiderLair) * 3
                + player.countDone(TypeIds.Monsters.Stronghold) * 4;
        if (player.getPopCap() < player.getMaxPop() && player.getPopCap() - player.getPopUsed() < productionPower) {
            priorities.addPriority(TypeIds.Monsters.House, 1 + (int) (productionPower / 10) + player.countDone(TypeIds.Monsters.House));
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
        priorities.addPriority(TypeIds.Monsters.Farm, 3);
        //priorities.addPriority(TypeIds.Monsters.House, 2);
        priorities.addPriority(TypeIds.Monsters.Graveyard, 1);
        priorities.addPriority(TypeIds.Monsters.Farm, 6);
        priorities.addPriority(TypeIds.Monsters.Graveyard, 2);
        priorities.addPriority(TypeIds.Monsters.Laboratory, 1);
        priorities.addPriority(TypeIds.Monsters.Graveyard, 3);
        priorities.addPriority(TypeIds.Monsters.Dungeon, 1);
        priorities.addPriority(TypeIds.Monsters.Graveyard, 4);
        priorities.addPriority(TypeIds.Monsters.SpiderLair, 1);
        priorities.addPriority(TypeIds.Monsters.Graveyard, 5);
        priorities.addPriority(TypeIds.Monsters.Stronghold, 1);
        priorities.addPriority(TypeIds.Monsters.Graveyard, 6);
        priorities.addPriority(TypeIds.Monsters.Dungeon, 2);
        priorities.addPriority(TypeIds.Monsters.Graveyard, 7);
    }

    public void setUnitPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        var zombie = player.countDone(TypeIds.Monsters.HusksUpgrade) > 0
                ? TypeIds.Monsters.Husk
                : TypeIds.Monsters.Zombie;
        var skeleton = player.countDone(TypeIds.Monsters.StrayUpgrade) > 0
                ? TypeIds.Monsters.Stray
                : TypeIds.Monsters.Skeleton;
        priorities.addPriority(TypeIds.Monsters.Villager, 8);
        priorities.addPriority(zombie, 4);
        priorities.addPriority(skeleton, 4);
        priorities.addPriority(TypeIds.Monsters.Villager, 16);
        priorities.addPriority(zombie, 10);
        priorities.addPriority(skeleton, 8);
        priorities.addPriority(TypeIds.Monsters.Villager, 24);
        priorities.addPriority(zombie, 20);
        priorities.addPriority(skeleton, 12);
        priorities.addPriority(TypeIds.Monsters.Villager, 28);
        priorities.addPriority(TypeIds.Monsters.Creeper, 2);
        priorities.addPriority(TypeIds.Monsters.Warden, 2);
        priorities.addPriority(zombie, 40);
        priorities.addPriority(skeleton, 20);
        priorities.addPriority(TypeIds.Monsters.Creeper, 4);
        priorities.addPriority(TypeIds.Monsters.Warden, 5);
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
        return Faction.MONSTERS;
    }

    @Override
    public void setPriorities(IAiPlayer player, IAiControllerPriorities priorities) {
        this.setBuildingPriorities(player, priorities.getBuildingPriorities());
        this.setUnitPriorities(player, priorities.getUnitPriorities());
        this.setArmyPriorities(player, priorities.getArmyPriorities());
    }
}
