package com.daratrix.ronapi.apis;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.ProductionItem;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class TypeIds {

    private static int TypeIdCount = 0;
    private static Map<String, Integer> NameToTypeId = new HashMap<>();
    private static Map<Integer, String> TypeIdToName = new HashMap<>();
    private static Map<Integer, String> TypeIdToStructureName = new HashMap<>();
    private static Map<Integer, ResourceCost> TypeIdToCost = new HashMap<>();

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
        TypeIdToCost.put(typeId, cost);
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

    public static <T extends Building> int addB(Class<T> buildingClass) {
        try {
            var nameField = buildingClass.getField("buildingName");
            var costField = buildingClass.getField("cost");
            var typeId = add((String) nameField.get(null), (ResourceCost) costField.get(null));
            var structrueNameField = buildingClass.getField("structureName");
            NameToTypeId.put((String) structrueNameField.get(null), TypeIdCount);
            TypeIdToStructureName.put(typeId, (String) structrueNameField.get(null));
            return typeId;
        } catch (Exception e) {
            return 0;
        }
    }

    public static <T extends ProductionItem> int addP(Class<T> prodClass) {
        try {
            var nameField = prodClass.getField("itemName");
            var costField = prodClass.getField("cost");
            return add((String) nameField.get(null), (ResourceCost) costField.get(null));
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
        if (unit instanceof com.solegendary.reignofnether.unit.units.monsters.WardenProd) return Monsters.Warden;

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
        // Villager structures
        public final static int TownCentre = addB(com.solegendary.reignofnether.building.buildings.villagers.TownCentre.class);
        public final static int Stockpile = addB(com.solegendary.reignofnether.building.buildings.villagers.OakStockpile.class);
        public final static int House = addB(com.solegendary.reignofnether.building.buildings.villagers.VillagerHouse.class);
        public final static int Farm = addB(com.solegendary.reignofnether.building.buildings.villagers.WheatFarm.class);
        public final static int Barracks = addB(com.solegendary.reignofnether.building.buildings.villagers.Barracks.class);
        public final static int Blacksmith = addB(com.solegendary.reignofnether.building.buildings.villagers.Blacksmith.class);
        public final static int Watchtower = addB(com.solegendary.reignofnether.building.buildings.villagers.Watchtower.class);
        public final static int ArcaneTower = addB(com.solegendary.reignofnether.building.buildings.villagers.ArcaneTower.class);
        public final static int Library = addB(com.solegendary.reignofnether.building.buildings.villagers.Library.class);
        public final static int Castle = addB(com.solegendary.reignofnether.building.buildings.villagers.Castle.class);

        // Villager units
        public final static int Villager = addP(com.solegendary.reignofnether.unit.units.villagers.VillagerProd.class);
        public final static int Vindicator = addP(com.solegendary.reignofnether.unit.units.villagers.VindicatorProd.class);
        public final static int Pillager = addP(com.solegendary.reignofnether.unit.units.villagers.PillagerProd.class);
        public final static int IronGolem = addP(com.solegendary.reignofnether.unit.units.villagers.IronGolemProd.class);
        public final static int Witch = addP(com.solegendary.reignofnether.unit.units.villagers.WitchProd.class);
        public final static int Evoker = addP(com.solegendary.reignofnether.unit.units.villagers.EvokerProd.class);
        public final static int Ravager = addP(com.solegendary.reignofnether.unit.units.villagers.RavagerProd.class);

        // Villager researches
        public final static int DiamondAxes = addP(com.solegendary.reignofnether.research.researchItems.ResearchVindicatorAxes.class);
        public final static int MultishotCrossbows = addP(com.solegendary.reignofnether.research.researchItems.ResearchPillagerCrossbows.class);
        public final static int OfficersQuarters = addP(com.solegendary.reignofnether.research.researchItems.ResearchCastleFlag.class);
    }

    public static class Monsters {
        // Monster structures
        public final static int Mausoleum = addB(com.solegendary.reignofnether.building.buildings.monsters.Mausoleum.class);
        public final static int Stockpile = addB(com.solegendary.reignofnether.building.buildings.monsters.SpruceStockpile.class);
        public final static int House = addB(com.solegendary.reignofnether.building.buildings.monsters.HauntedHouse.class);
        public final static int Farm = addB(com.solegendary.reignofnether.building.buildings.monsters.PumpkinFarm.class);
        public final static int Graveyard = addB(com.solegendary.reignofnether.building.buildings.monsters.Graveyard.class);
        public final static int Laboratory = addB(com.solegendary.reignofnether.building.buildings.monsters.Laboratory.class);
        public final static int Watchtower = addB(com.solegendary.reignofnether.building.buildings.monsters.DarkWatchtower.class);
        public final static int SpiderLair = addB(com.solegendary.reignofnether.building.buildings.monsters.SpiderLair.class);
        public final static int Dungeon = addB(com.solegendary.reignofnether.building.buildings.monsters.Dungeon.class);
        public final static int Stronghold = addB(com.solegendary.reignofnether.building.buildings.monsters.Stronghold.class);
        public final static int SculkCatalyst = addB(com.solegendary.reignofnether.building.buildings.monsters.SculkCatalyst.class);

        // Monster units
        public final static int Villager = addP(com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerProd.class);
        public final static int Zombie = addP(com.solegendary.reignofnether.unit.units.monsters.ZombieProd.class);
        public final static int Husk = addP(com.solegendary.reignofnether.unit.units.monsters.HuskProd.class);
        public final static int Drowned = addP(com.solegendary.reignofnether.unit.units.monsters.DrownedProd.class);
        public final static int Skeleton = addP(com.solegendary.reignofnether.unit.units.monsters.SkeletonProd.class);
        public final static int Stray = addP(com.solegendary.reignofnether.unit.units.monsters.StrayProd.class);
        public final static int Spider = addP(com.solegendary.reignofnether.unit.units.monsters.SpiderProd.class);
        public final static int PoisonSpider = addP(com.solegendary.reignofnether.unit.units.monsters.PoisonSpiderProd.class);
        public final static int Creeper = addP(com.solegendary.reignofnether.unit.units.monsters.CreeperProd.class);
        public final static int Warden = addP(com.solegendary.reignofnether.unit.units.monsters.WardenProd.class);

        // Monster researches
        public final static int HusksUpgrade = addP(com.solegendary.reignofnether.research.researchItems.ResearchHusks.class);
        public final static int StrayUpgrade = addP(com.solegendary.reignofnether.research.researchItems.ResearchStrays.class);
        public final static int LightningRodUpgrade = addP(com.solegendary.reignofnether.research.researchItems.ResearchLabLightningRod.class);
    }

    public static class Piglins {
        // Piglin structures
        public final static int MainPortal = addB(com.solegendary.reignofnether.building.buildings.piglins.CentralPortal.class);
        public final static int Portal = addB(com.solegendary.reignofnether.building.buildings.piglins.Portal.class);
        public final static int Farm = addB(com.solegendary.reignofnether.building.buildings.piglins.NetherwartFarm.class);
        public final static int Bastion = addB(com.solegendary.reignofnether.building.buildings.piglins.Bastion.class);
        public final static int WitherShrine = addB(com.solegendary.reignofnether.building.buildings.piglins.WitherShrine.class);
        public final static int HoglinStables = addB(com.solegendary.reignofnether.building.buildings.piglins.HoglinStables.class);
        public final static int FlameSanctuary = addB(com.solegendary.reignofnether.building.buildings.piglins.FlameSanctuary.class);
        public final static int Fortress = addB(com.solegendary.reignofnether.building.buildings.piglins.Fortress.class);

        // Piglin units
        public final static int Grunt = addP(com.solegendary.reignofnether.unit.units.piglins.GruntProd.class);
        public final static int Brute = addP(com.solegendary.reignofnether.unit.units.piglins.BruteProd.class);
        public final static int Headhunter = addP(com.solegendary.reignofnether.unit.units.piglins.HeadhunterProd.class);
        public final static int WitherSkeleton = addP(com.solegendary.reignofnether.unit.units.piglins.WitherSkeletonProd.class);
        public final static int Hoglin = addP(com.solegendary.reignofnether.unit.units.piglins.HoglinProd.class);
        public final static int Blaze = addP(com.solegendary.reignofnether.unit.units.piglins.BlazeProd.class);
        public final static int Ghast = addP(com.solegendary.reignofnether.unit.units.piglins.GhastProd.class);

        // Piglin researches
        public final static int BruteShield = addP(com.solegendary.reignofnether.research.researchItems.ResearchBruteShields.class);
        public final static int HeadhunterTrident = addP(com.solegendary.reignofnether.research.researchItems.ResearchHeavyTridents.class);
        public final static int WitherCloud = addP(com.solegendary.reignofnether.research.researchItems.ResearchWitherClouds.class);
        public final static int CivilianPortalUpgrade = addP(com.solegendary.reignofnether.research.researchItems.ResearchPortalForCivilian.class);
        public final static int MilitaryPortalUpgrade = addP(com.solegendary.reignofnether.research.researchItems.ResearchPortalForMilitary.class);
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

    public static UnitAction toUnitAction(int typeId, Object target) {
        if (typeId == Resources.FoodEntity) {
            return UnitAction.ATTACK;
        }

        if (typeId == Resources.FoodFarm || typeId == Orders.HarvestFarm) {
            return UnitAction.FARM;
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
}
