package com.daratrix.ronapi.apis;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.monsters.PumpkinFarm;
import com.solegendary.reignofnether.building.buildings.monsters.SpruceStockpile;
import com.solegendary.reignofnether.building.buildings.piglins.NetherwartFarm;
import com.solegendary.reignofnether.building.buildings.piglins.PortalBasic;
import com.solegendary.reignofnether.building.buildings.villagers.OakStockpile;
import com.solegendary.reignofnether.building.buildings.villagers.WheatFarm;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.ReviveHeroProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.researchItems.*;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TypeIds {

    private static int TypeIdCount = 0;
    private static Map<String, Integer> NameToTypeId = new HashMap<>();
    private static Map<Integer, String> TypeIdToName = new HashMap<>();
    private static Map<Integer, String> TypeIdToHeroName = new HashMap<>();
    private static Map<Integer, String> TypeIdToStructureName = new HashMap<>();
    private static Map<Integer, ResourceCost> TypeIdToCost = new HashMap<>();
    //private static Map<Integer, ResourceCost> TypeIdToReviveCost = new HashMap<>();
    private static Map<Integer, ProductionItem> TypeIdToProductionItem = new HashMap<>();
    private static Map<Integer, ReviveHeroProductionItem> TypeIdToReviveItem = new HashMap<>();
    private static Map<Integer, Building> TypeIdToBuildingData = new HashMap<>();

    public static int add(String itemName) {
        if (NameToTypeId.containsKey(itemName)) {
            return NameToTypeId.get(itemName); // don't overwrite existing values
        }

        ++TypeIdCount;
        NameToTypeId.put(itemName, TypeIdCount);
        TypeIdToName.put(TypeIdCount, itemName);
        return TypeIdCount;
    }

    public static int add(String itemName, ResourceCost cost) {
        var typeId = add(itemName);
        if (cost != null) {
            TypeIdToCost.put(typeId, cost);
        }

        return typeId;
    }

    public static int add(ProductionItem prod, ResourceCost cost) {
        return add(prod.getItemName(), cost);
    }

    public static int add(ResourceName resourceName) {
        return add(resourceName.name());
    }

    public static int add(UnitAction action) {
        return add(action.name());
    }

    public static int add(UnitAction action, ResourceCost cost) {
        return add(action.name(), cost);
    }

    public static <T extends Building> int addB(T b) {
        try {
            var name = b.name;
            return addB(b, name);
        } catch (Exception e) {
            return 0;
        }
    }

    public static <T extends Building> int addB(T b, String name) {
        try {
            var cost = b.cost;
            var typeId = add(name, cost);
            var structrueName = b.structureName;
            NameToTypeId.put(structrueName, typeId);
            TypeIdToStructureName.put(typeId, structrueName);
            TypeIdToBuildingData.put(typeId, b);
            return typeId;
        } catch (Exception e) {
            return 0;
        }
    }

    public static <T extends ProductionItem> int addP(T p) {
        try {
            var name = p.getItemName();
            return addP(p, name);
        } catch (Exception e) {
            return 0;
        }
    }

    public static <T extends ProductionItem> int addP(T p, String name) {
        try {
            var cost = p.defaultCost;
            var typeId = add(name, cost);
            TypeIdToProductionItem.put(typeId, p);
            return typeId;
        } catch (Exception e) {
            return 0;
        }
    }

    public static <T extends ProductionItem, R extends ReviveHeroProductionItem> int addH(T p, R r, String heroId) {
        try {
            var name = p.getItemName();
            var cost = p.defaultCost;
            var typeId = add(name, cost);
            TypeIdToProductionItem.put(typeId, p);
            TypeIdToReviveItem.put(typeId, r);
            //TypeIdToReviveCost.put(typeId, r.defaultCost);
            TypeIdToHeroName.put(typeId, heroId);
            return typeId;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int get(String itemName) {
        return NameToTypeId.getOrDefault(itemName, 0);
    }

    public static int get(ResourceName resourceName) {
        return switch (resourceName) {
            case FOOD -> Resources.FoodBlock;
            case WOOD -> Resources.WoodBlock;
            case ORE -> Resources.OreBlock;
            default -> 0;
        };
    }

    public static int get(ProductionItem prod) {
        return NameToTypeId.getOrDefault(prod.getItemName(), 0);
    }

    public static int get(Unit unit) {
        if (unit instanceof com.solegendary.reignofnether.unit.units.villagers.RoyalGuardUnit)
            return Villagers.HeroRoyalGuard;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit)
            return Monsters.HeroNecromancer;
        if (unit instanceof com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit)
            return Piglins.HeroMerchant;

        if (unit instanceof com.solegendary.reignofnether.unit.units.villagers.VillagerUnit) return Villagers.Villager;
        if (unit instanceof com.solegendary.reignofnether.unit.units.villagers.VindicatorUnit)
            return Villagers.Vindicator;
        if (unit instanceof com.solegendary.reignofnether.unit.units.villagers.PillagerUnit) return Villagers.Pillager;
        if (unit instanceof com.solegendary.reignofnether.unit.units.villagers.IronGolemUnit)
            return Villagers.IronGolem;
        if (unit instanceof com.solegendary.reignofnether.unit.units.villagers.WitchUnit) return Villagers.Witch;
        if (unit instanceof com.solegendary.reignofnether.unit.units.villagers.EvokerUnit) return Villagers.Evoker;
        if (unit instanceof com.solegendary.reignofnether.unit.units.villagers.RavagerUnit) return Villagers.Ravager;

        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerUnit)
            return Monsters.Villager;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.ZombieUnit) return Monsters.Zombie;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.HuskUnit) return Monsters.Husk;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.DrownedUnit) return Monsters.Drowned;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit) return Monsters.Skeleton;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.StrayUnit) return Monsters.Stray;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.CreeperUnit) return Monsters.Creeper;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.SpiderUnit) return Monsters.Spider;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.PoisonSpiderUnit) return Monsters.PoisonSpider;
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.WardenUnit) return Monsters.Warden;

        if (unit instanceof com.solegendary.reignofnether.unit.units.piglins.GruntUnit) return Piglins.Grunt;
        if (unit instanceof com.solegendary.reignofnether.unit.units.piglins.BruteUnit) return Piglins.Brute;
        if (unit instanceof com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit) return Piglins.Headhunter;
        if (unit instanceof com.solegendary.reignofnether.unit.units.piglins.WitherSkeletonUnit)
            return Piglins.WitherSkeleton;
        if (unit instanceof com.solegendary.reignofnether.unit.units.piglins.HoglinUnit) return Piglins.Hoglin;
        if (unit instanceof com.solegendary.reignofnether.unit.units.piglins.BlazeUnit) return Piglins.Blaze;
        if (unit instanceof com.solegendary.reignofnether.unit.units.piglins.GhastUnit) return Piglins.Ghast;

        return NameToTypeId.getOrDefault(unit.getClass().getTypeName().replace("Unit", ""), 0);
    }

    public static int get(Building building) {
        return NameToTypeId.getOrDefault(building.name, 0);
    }

    public static class Resources {
        public final static int FoodBlock = add(ResourceName.FOOD);
        public final static int WoodBlock = add(ResourceName.WOOD);
        public final static int OreBlock = add(ResourceName.ORE);
        public final static int FoodEntity = add("HUNT");
        public final static int FoodFarm = add("FARM");
    }

    public static class Villagers {
        public final static int HeroRoyalGuard = addH(ProductionItems.ROYAL_GUARD, ProductionItems.ROYAL_GUARD_REVIVE, "entity.reignofnether.royal_guard_unit");
        public final static int ShrineOfProperity = addB(Buildings.SHRINE_OF_PROSPERITY);
        public final static int Altar = ShrineOfProperity;

        // Villager structures
        public final static int TownCentre = addB(Buildings.TOWN_CENTRE);
        public final static int Stockpile = addB(Buildings.OAK_STOCKPILE, OakStockpile.buildingName);
        public final static int House = addB(Buildings.VILLAGER_HOUSE);
        public final static int Farm = addB(Buildings.WHEAT_FARM, WheatFarm.buildingName);
        public final static int Barracks = addB(Buildings.BARRACKS);
        public final static int Blacksmith = addB(Buildings.BLACKSMITH);
        public final static int Watchtower = addB(Buildings.WATCHTOWER);
        public final static int ArcaneTower = addB(Buildings.ARCANE_TOWER);
        public final static int Library = addB(Buildings.LIBRARY);
        public final static int Castle = addB(Buildings.CASTLE);

        // Villager units
        public final static int Villager = addP(ProductionItems.VILLAGER);
        public final static int Vindicator = addP(ProductionItems.VINDICATOR);
        public final static int Pillager = addP(ProductionItems.PILLAGER);
        public final static int IronGolem = addP(ProductionItems.IRON_GOLEM);
        public final static int Witch = addP(ProductionItems.WITCH);
        public final static int Evoker = addP(ProductionItems.EVOKER);
        public final static int Ravager = addP(ProductionItems.RAVAGER);

        // Villager researches
        public final static int GolemSmithing = addP(ProductionItems.RESEARCH_GOLEM_SMITHING);
        public final static int LingeringPotions = addP(ProductionItems.RESEARCH_LINGERING_POTIONS);
        public final static int WaterPotions = addP(ProductionItems.RESEARCH_WATER_POTIONS, ResearchWaterPotions.itemName);
        public final static int HealingPotions = addP(ProductionItems.RESEARCH_HEALING_POTIONS, ResearchHealingPotions.itemName);
        public final static int Vexes = addP(ProductionItems.RESEARCH_EVOKER_VEXES);
        public final static int GrandLibrary = addP(ProductionItems.RESEARCH_GRAND_LIBRARY);
        public final static int RavagerCavalry = addP(ProductionItems.RESEARCH_RAVAGER_CAVALRY);
        public final static int OfficersQuarters = addP(ProductionItems.RESEARCH_CASTLE_FLAG);
        // deprecated
        public final static int DiamondAxes = addP(ProductionItems.RESEARCH_VINDICATOR_AXES);
        public final static int MultishotCrossbows = addP(ProductionItems.RESEARCH_PILLAGER_CROSSBOWS);

        // Villager enchants
        public final static int EnchantMaiming = add(UnitAction.ENCHANT_MAIMING, ResourceCosts.ENCHANT_MAIMING);
        public final static int EnchantSharpness = add(UnitAction.ENCHANT_SHARPNESS, ResourceCosts.ENCHANT_SHARPNESS);
        public final static int EnchantQuickCharge = add(UnitAction.ENCHANT_QUICKCHARGE, ResourceCosts.ENCHANT_QUICK_CHARGE);
        public final static int EnchantMultiShot = add(UnitAction.ENCHANT_MULTISHOT, ResourceCosts.ENCHANT_MULTISHOT);
        public final static int EnchantVigor = add(UnitAction.ENCHANT_VIGOR, ResourceCosts.ENCHANT_VIGOR);
    }

    public static class Monsters {
        public final static int HeroNecromancer = addH(ProductionItems.NECROMANCER, ProductionItems.NECROMANCER_REVIVE, "entity.reignofnether.necromancer_unit");
        public final static int AltarOfDarkness = addB(Buildings.ALTAR_OF_DARKNESS);
        public final static int Altar = AltarOfDarkness;

        // Monster structures
        public final static int Mausoleum = addB(Buildings.MAUSOLEUM);
        public final static int Stockpile = addB(Buildings.SPRUCE_STOCKPILE, SpruceStockpile.buildingName);
        public final static int House = addB(Buildings.HAUNTED_HOUSE);
        public final static int Farm = addB(Buildings.PUMPKIN_FARM, PumpkinFarm.buildingName);
        public final static int Graveyard = addB(Buildings.GRAVEYARD);
        public final static int Laboratory = addB(Buildings.LABORATORY);
        public final static int Watchtower = addB(Buildings.DARK_WATCHTOWER);
        public final static int SpiderLair = addB(Buildings.SPIDER_LAIR);
        public final static int Dungeon = addB(Buildings.DUNGEON);
        public final static int Stronghold = addB(Buildings.STRONGHOLD);
        public final static int SculkCatalyst = addB(Buildings.SCULK_CATALYST);

        // Monster units
        public final static int Villager = addP(ProductionItems.ZOMBIE_VILLAGER);
        public final static int Zombie = addP(ProductionItems.ZOMBIE);
        public final static int Husk = addP(ProductionItems.HUSK);
        public final static int Drowned = addP(ProductionItems.DROWNED);
        public final static int Skeleton = addP(ProductionItems.SKELETON);
        public final static int Stray = addP(ProductionItems.STRAY);
        public final static int Spider = addP(ProductionItems.SPIDER);
        public final static int PoisonSpider = addP(ProductionItems.POISON_SPIDER);
        public final static int Creeper = addP(ProductionItems.CREEPER);
        public final static int Warden = addP(ProductionItems.WARDEN);

        // Monster researches
        public final static int HusksUpgrade = addP(ProductionItems.RESEARCH_HUSKS);
        public final static int StrayUpgrade = addP(ProductionItems.RESEARCH_STRAYS);
        public final static int SpiderWebs = addP(ProductionItems.RESEARCH_SPIDER_WEBS);
        public final static int SpiderUpgrade = addP(ProductionItems.RESEARCH_POISON_SPIDERS);
        public final static int Silverfish = addP(ProductionItems.RESEARCH_SILVERFISH);
        public final static int SculkAmplifiers = addP(ProductionItems.RESEARCH_SCULK_AMPLIFIERS);
        public final static int LightningRod = addP(ProductionItems.RESEARCH_LAB_LIGHTNING_ROD, ResearchLabLightningRod.itemName);
    }

    public static class Piglins {
        public final static int HeroMerchant = addH(ProductionItems.PIGLIN_MERCHANT, ProductionItems.PIGLIN_MERCHANT_REVIVE, "entity.reignofnether.piglin_merchant_unit");
        public final static int InfernalPortal = addB(Buildings.INFERNAL_PORTAL);
        public final static int Altar = InfernalPortal;

        // Piglin structures
        public final static int MainPortal = addB(Buildings.CENTRAL_PORTAL);
        public final static int Portal = addB(Buildings.PORTAL_BASIC, PortalBasic.buildingName);
        public final static int Farm = addB(Buildings.NETHERWART_FARM, NetherwartFarm.buildingName);
        public final static int Bastion = addB(Buildings.BASTION);
        public final static int WitherShrine = addB(Buildings.WITHER_SHRINE);
        public final static int HoglinStables = addB(Buildings.HOGLIN_STABLES);
        public final static int FlameSanctuary = addB(Buildings.FLAME_SANCTUARY);
        public final static int Fortress = addB(Buildings.FORTRESS);

        // Piglin units
        public final static int Grunt = addP(ProductionItems.GRUNT);
        public final static int Brute = addP(ProductionItems.BRUTE);
        public final static int Headhunter = addP(ProductionItems.HEADHUNTER);
        public final static int WitherSkeleton = addP(ProductionItems.WITHER_SKELETON);
        public final static int Hoglin = addP(ProductionItems.HOGLIN);
        public final static int Blaze = addP(ProductionItems.BLAZE);
        public final static int Ghast = addP(ProductionItems.GHAST);

        // Piglin researches
        public final static int BruteShield = addP(ProductionItems.RESEARCH_BRUTE_SHIELDS);
        public final static int HeadhunterTrident = addP(ProductionItems.RESEARCH_HEAVY_TRIDENTS);
        public final static int HoglinCavalry = addP(ProductionItems.RESEARCH_HOGLIN_CAVALRY, ResearchHoglinCavalry.itemName);
        public final static int FireResistance = addP(ProductionItems.RESEARCH_FIRE_RESISTANCE, ResearchFireResistance.itemName);
        public final static int BlazeFirewall = addP(ProductionItems.RESEARCH_BLAZE_FIREWALL);
        public final static int WitherCloud = addP(ProductionItems.RESEARCH_WITHER_CLOUDS);
        public final static int SoulFireballs = addP(ProductionItems.RESEARCH_SOUL_FIREBALLS, ResearchSoulFireballs.itemName);
        public final static int Bloodlust = addP(ProductionItems.RESEARCH_BLOODLUST);
        public final static int AdvancedPortals = addP(ProductionItems.RESEARCH_ADVANCED_PORTALS);
        public final static int CivilianPortal = addP(ProductionItems.RESEARCH_PORTAL_FOR_CIVILIAN);
        public final static int MilitaryPortal = addP(ProductionItems.RESEARCH_PORTAL_FOR_MILITARY);
        public final static int TransportPortal = addP(ProductionItems.RESEARCH_PORTAL_FOR_TRANSPORT);
    }

    public static class Orders {
        public final static int Repair = add(UnitAction.BUILD_REPAIR);
        public final static int Attack = add("smart_attack");
        public final static int AttackUnit = add(UnitAction.ATTACK);
        public final static int AttackBuilding = add(UnitAction.ATTACK_BUILDING);
        public final static int AttackMove = add(UnitAction.ATTACK_MOVE);
        public final static int AttackGround = add(UnitAction.ATTACK_GROUND);
        public final static int HarvestFarm = add(UnitAction.FARM);
        public final static int HarvestBlock = add(UnitAction.MOVE);
        public final static int Move = add(UnitAction.MOVE);
        public final static int Follow = add(UnitAction.FOLLOW);
        public final static int Return = add(UnitAction.RETURN_RESOURCES);
        public final static int Stop = add(UnitAction.STOP);

        // abilities
        public final static int Roar = add(UnitAction.ROAR);
        public final static int MaceSlam = add(UnitAction.MACE_SLAM);
        public final static int Taunt = add(UnitAction.TAUNTING_CRY);
        public final static int Avatar = add(UnitAction.AVATAR);

        public final static int CallLightning = add(UnitAction.CALL_LIGHTNING);
        public final static int SonicBoom = add(UnitAction.CAST_SONIC_BOOM);
        public final static int RaiseDead = add(UnitAction.RAISE_DEAD);
        public final static int InsomniaCurse = add(UnitAction.INSOMNIA_CURSE);
        public final static int BloodMoon = add(UnitAction.BLOOD_MOON);

        public final static int BloodLust = add(UnitAction.BLOOD_LUST);
        public final static int ShieldOn = add(UnitAction.ENABLE_SHIELD_RAISE);
        public final static int ShieldOff = add(UnitAction.DISABLE_SHIELD_RAISE);
        public final static int Firewall = add(UnitAction.SHOOT_FIREWALL);
        public final static int WitherCloud = add(UnitAction.WITHER_CLOUD);
        public final static int ThrowTnt = add(UnitAction.THROW_TNT);
        public final static int FancyFeast = add(UnitAction.FANCY_FEAST);
        public final static int LootExplosion = add(UnitAction.LOOT_EXPLOSION);
    }

    public static String toItemName(int typeId) {
        return TypeIdToName.getOrDefault(typeId, null);
    }

    public static String toStructureName(int typeId) {
        return TypeIdToStructureName.getOrDefault(typeId, null);
    }

    public static ResourceCost toItemCost(int typeId) {
        return TypeIdToCost.getOrDefault(typeId, null);
    }

    //public static ResourceCost toReviveCost(int typeId) {
    //    return TypeIdToReviveCost.getOrDefault(typeId, null);
    //}

    public static UnitAction toUnitAction(int typeId, Object target) {
        if (typeId == Resources.FoodEntity) {
            return UnitAction.ATTACK;
        }

        if (typeId == Resources.FoodFarm || typeId == Orders.HarvestFarm) {
            return UnitAction.FARM;
        }

        if (typeId == Resources.FoodBlock || typeId == Resources.WoodBlock || typeId == Resources.OreBlock) {
            return UnitAction.MOVE;
        }

        var itemName = TypeIdToName.getOrDefault(typeId, UnitAction.NONE.name());
        if (itemName.equals("smart_attack")) {
            if (target instanceof Building) return UnitAction.ATTACK_BUILDING;
            if (target instanceof BlockPos) return UnitAction.ATTACK_MOVE;
            if (target instanceof Unit) return UnitAction.ATTACK;
            if (target instanceof LivingEntity) return UnitAction.ATTACK;
            return UnitAction.NONE;
        }

        if (typeId == Orders.Return) {
            return target != null ? UnitAction.RETURN_RESOURCES : UnitAction.RETURN_RESOURCES_TO_CLOSEST;
        }

        try {
            return UnitAction.valueOf(itemName);
        } catch (Exception e) {
            return UnitAction.NONE;
        }
    }

    public static ProductionItem toProductionItem(int typeId) {
        return TypeIdToProductionItem.get(typeId);
    }

    public static ReviveHeroProductionItem toReviveItem(int typeId) {
        return TypeIdToReviveItem.get(typeId);
    }

    public static boolean isHero(int typeId) {
        return TypeIdToReviveItem.containsKey(typeId);
    }

    public static String toHeroName(int typeId) {
        return TypeIdToHeroName.getOrDefault(typeId, null);
    }

    public static Building toBuildingData(int typeId) {
        return TypeIdToBuildingData.get(typeId);
    }
}
