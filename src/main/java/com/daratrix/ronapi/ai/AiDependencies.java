package com.daratrix.ronapi.ai;

import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;

import java.util.*;

public class AiDependencies {
    private final static Map<Integer, HashSet<Integer>> PriorityRequirements = new HashMap<>();
    private final static Map<Integer, HashSet<Integer>> PrioritySources = new HashMap<>();

    private final static Map<Integer, Integer> PriorityUpgradesFrom = new HashMap<>();

    public static void setRequirements(int priority, Integer... requirements) {
        var l = PriorityRequirements.getOrDefault(priority, null);
        if (l == null) {
            l = new HashSet<Integer>(requirements.length);
            PriorityRequirements.put(priority, l);
        }

        l.addAll(Arrays.asList(requirements));
    }

    public static void setSources(int priority, Integer... sources) {
        var l = PrioritySources.getOrDefault(priority, null);
        if (l == null) {
            l = new HashSet<Integer>(sources.length);
            PrioritySources.put(priority, l);
        }

        l.addAll(Arrays.asList(sources));
    }

    public static void setProduction(int priority, Integer... produced) {
        for (Integer p : produced) {
            setSources(p, priority);
        }
    }

    public static void setUpgradesInto(int priority, int tranformsInto) {
        PriorityUpgradesFrom.put(tranformsInto, priority);
        setSources(priority, tranformsInto);
    }

    public static int lastMissingrequirement = 0; // for logging

    public static boolean canMake(int priority, IAiPlayer aiPlayer) {
        HashSet<Integer> requirements = PriorityRequirements.getOrDefault(priority, null);
        if (requirements == null) {
            return true;
        }

        for (Integer requirement : requirements) {
            if (aiPlayer.countDone(requirement) == 0) {
                lastMissingrequirement = requirement; // for logging
                return false;
            }
        }

        return true;
    }

    public static HashSet<Integer> getSourceTypeIds(int priority) {
        return PrioritySources.getOrDefault(priority, null);
    }

    public static int getUpgradeSourceTypeId(int priority) {
        return PriorityUpgradesFrom.getOrDefault(priority, 0);
    }

    static {
        // setup Villager sources
        setProduction(TypeIds.Villagers.Villager,
                TypeIds.Villagers.TownCentre,
                TypeIds.Villagers.Stockpile,
                TypeIds.Villagers.House,
                TypeIds.Villagers.Farm,
                TypeIds.Villagers.Barracks,
                TypeIds.Villagers.Watchtower,
                TypeIds.Villagers.Blacksmith,
                TypeIds.Villagers.ArcaneTower,
                TypeIds.Villagers.Castle,
                TypeIds.Villagers.Library);

        setProduction(TypeIds.Villagers.Barracks,
                TypeIds.Villagers.Vindicator,
                TypeIds.Villagers.Pillager);

        setProduction(TypeIds.Villagers.TownCentre,
                TypeIds.Villagers.Villager);

        setProduction(TypeIds.Villagers.Blacksmith,
                TypeIds.Villagers.IronGolem,
                TypeIds.Villagers.GolemSmithing);

        setProduction(TypeIds.Villagers.Library,
                TypeIds.Villagers.LingeringPotions,
                TypeIds.Villagers.WaterPotions,
                TypeIds.Villagers.HealingPotions,
                TypeIds.Villagers.Vexes,
                TypeIds.Villagers.EnchantMaiming,
                TypeIds.Villagers.EnchantSharpness,
                TypeIds.Villagers.EnchantQuickCharge,
                TypeIds.Villagers.EnchantMultiShot,
                TypeIds.Villagers.EnchantVigor);

        setProduction(TypeIds.Villagers.ArcaneTower,
                TypeIds.Villagers.Witch,
                TypeIds.Villagers.Evoker);

        setProduction(TypeIds.Villagers.Castle,
                TypeIds.Villagers.Ravager,
                TypeIds.Villagers.RavagerCavalry,
                TypeIds.Villagers.OfficersQuarters);

        // setup Villager requirements
        setRequirements(TypeIds.Villagers.Barracks, TypeIds.Villagers.TownCentre);
        setRequirements(TypeIds.Villagers.House, TypeIds.Villagers.TownCentre);
        setRequirements(TypeIds.Villagers.Farm, TypeIds.Villagers.TownCentre);
        setRequirements(TypeIds.Villagers.Stockpile, TypeIds.Villagers.TownCentre);
        setRequirements(TypeIds.Villagers.Blacksmith, TypeIds.Villagers.Barracks);
        setRequirements(TypeIds.Villagers.ArcaneTower, TypeIds.Villagers.Barracks);
        setRequirements(TypeIds.Villagers.Library, TypeIds.Villagers.ArcaneTower);
        setRequirements(TypeIds.Villagers.Castle, TypeIds.Villagers.Library, TypeIds.Villagers.Blacksmith, TypeIds.Villagers.Barracks);

        setRequirements(TypeIds.Villagers.EnchantSharpness, TypeIds.Villagers.GrandLibrary);
        setRequirements(TypeIds.Villagers.EnchantMultiShot, TypeIds.Villagers.GrandLibrary);
        setRequirements(TypeIds.Villagers.EnchantVigor, TypeIds.Villagers.GrandLibrary);

        setUpgradesInto(TypeIds.Villagers.Library, TypeIds.Villagers.GrandLibrary);
        setUpgradesInto(TypeIds.Villagers.Castle, TypeIds.Villagers.OfficersQuarters);

        // setup Monster sources
        setProduction(TypeIds.Monsters.Villager,
                TypeIds.Monsters.Mausoleum,
                TypeIds.Monsters.Stockpile,
                TypeIds.Monsters.House,
                TypeIds.Monsters.Farm,
                TypeIds.Monsters.SculkCatalyst,
                TypeIds.Monsters.Graveyard,
                TypeIds.Monsters.Watchtower,
                TypeIds.Monsters.Laboratory,
                TypeIds.Monsters.Dungeon,
                TypeIds.Monsters.SpiderLair,
                TypeIds.Monsters.Stronghold);

        setProduction(TypeIds.Monsters.Graveyard,
                TypeIds.Monsters.Zombie,
                TypeIds.Monsters.Husk,
                TypeIds.Monsters.Drowned,
                TypeIds.Monsters.Skeleton,
                TypeIds.Monsters.Stray);

        setProduction(TypeIds.Monsters.Mausoleum,
                TypeIds.Monsters.Villager);

        setProduction(TypeIds.Monsters.Laboratory,
                TypeIds.Monsters.HusksUpgrade,
                TypeIds.Monsters.StrayUpgrade);

        setProduction(TypeIds.Monsters.Dungeon,
                TypeIds.Monsters.Creeper);

        setProduction(TypeIds.Monsters.SpiderLair,
                TypeIds.Monsters.Spider,
                TypeIds.Monsters.PoisonSpider);

        setProduction(TypeIds.Monsters.Stronghold,
                TypeIds.Monsters.Warden);

        // setup Monster requirements
        setRequirements(TypeIds.Monsters.Graveyard, TypeIds.Monsters.Mausoleum);
        setRequirements(TypeIds.Monsters.House, TypeIds.Monsters.Mausoleum);
        setRequirements(TypeIds.Monsters.Farm, TypeIds.Monsters.Mausoleum);
        setRequirements(TypeIds.Monsters.Stockpile, TypeIds.Monsters.Mausoleum);
        setRequirements(TypeIds.Monsters.SculkCatalyst, TypeIds.Monsters.Mausoleum);
        setRequirements(TypeIds.Monsters.Laboratory, TypeIds.Monsters.Mausoleum, TypeIds.Monsters.Graveyard);
        setRequirements(TypeIds.Monsters.SpiderLair, TypeIds.Monsters.Laboratory);
        setRequirements(TypeIds.Monsters.Dungeon, TypeIds.Monsters.Laboratory);
        setRequirements(TypeIds.Monsters.Stronghold, TypeIds.Monsters.SpiderLair, TypeIds.Monsters.Dungeon, TypeIds.Monsters.Graveyard);

        setUpgradesInto(TypeIds.Monsters.Laboratory, TypeIds.Monsters.LightningRod);

        // setup Piglin sources
        /*setProduction(TypeIds.Piglins.Villager,
                TypeIds.Piglins.TownCentre,
                TypeIds.Piglins.Stockpile,
                TypeIds.Piglins.House,
                TypeIds.Piglins.Farm,
                TypeIds.Piglins.Barracks,
                TypeIds.Piglins.Blacksmith,
                TypeIds.Piglins.ArcaneTower,
                TypeIds.Piglins.Castle,
                TypeIds.Piglins.Library);

        setProduction(TypeIds.Piglins.Barracks,
                TypeIds.Piglins.Vindicator,
                TypeIds.Piglins.Pillager);

        setProduction(TypeIds.Piglins.TownCentre,
                TypeIds.Piglins.Villager);

        setProduction(TypeIds.Piglins.Blacksmith,
                TypeIds.Piglins.IronGolem,
                TypeIds.Piglins.DiamondAxes,
                TypeIds.Piglins.MultishotCrossbows);

        setProduction(TypeIds.Villagers.ArcaneTower,
                TypeIds.Piglins.Witch,
                TypeIds.Piglins.Evoker);

        setProduction(TypeIds.Piglins.Castle,
                TypeIds.Piglins.Ravager);

        // setup Piglin requirements
        setRequirements(TypeIds.Piglins.Barracks, TypeIds.Piglins.TownCentre);
        setRequirements(TypeIds.Piglins.House, TypeIds.Piglins.TownCentre);
        setRequirements(TypeIds.Piglins.Farm, TypeIds.Piglins.TownCentre);
        setRequirements(TypeIds.Piglins.Blacksmith, TypeIds.Piglins.Barracks);
        setRequirements(TypeIds.Piglins.ArcaneTower, TypeIds.Piglins.Barracks);
        setRequirements(TypeIds.Piglins.Library, TypeIds.Piglins.ArcaneTower);
        setRequirements(TypeIds.Piglins.Castle, TypeIds.Piglins.Library, TypeIds.Piglins.Blacksmith, TypeIds.Piglins.Barracks);
        setUpgradesInto(TypeIds.Piglins.Portal, TypeIds.Piglins.CivilianPortal);
        setUpgradesInto(TypeIds.Piglins.Portal, TypeIds.Piglins.MilitaryPortal);
        setUpgradesInto(TypeIds.Piglins.Portal, TypeIds.Piglins.TransportPortal);
        */
    }
}
