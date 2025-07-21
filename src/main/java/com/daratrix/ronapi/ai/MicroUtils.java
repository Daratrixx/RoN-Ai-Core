package com.daratrix.ronapi.ai;

import com.daratrix.ronapi.apis.PlayerApi;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiUnit;
import com.daratrix.ronapi.models.interfaces.IBoxed;
import com.daratrix.ronapi.models.interfaces.IOrderable;
import com.daratrix.ronapi.models.interfaces.IPlayerWidget;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.ability.heroAbilities.monster.InsomniaCurse;
import com.solegendary.reignofnether.ability.heroAbilities.monster.RaiseDead;
import com.solegendary.reignofnether.ability.heroAbilities.monster.SoulSiphonPassive;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.FancyFeast;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.GreedIsGoodPassive;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.LootExplosion;
import com.solegendary.reignofnether.ability.heroAbilities.piglin.ThrowTNT;
import com.solegendary.reignofnether.ability.heroAbilities.villager.Avatar;
import com.solegendary.reignofnether.ability.heroAbilities.villager.MaceSlam;
import com.solegendary.reignofnether.ability.heroAbilities.villager.TauntingCry;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import com.solegendary.reignofnether.unit.units.monsters.WardenUnit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.WitherSkeletonUnit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

public class MicroUtils {

    public static HashMap<Integer, Consumer<IPlayerWidget>> defaultMicroLogics = new HashMap<>();

    static {
        defaultMicroLogics.put(TypeIds.Villagers.Ravager, MicroUtils::microRavager);
        defaultMicroLogics.put(TypeIds.Villagers.HeroRoyalGuard, MicroUtils::microRoyalGuard);

        defaultMicroLogics.put(TypeIds.Monsters.Warden, MicroUtils::microWarden);
        defaultMicroLogics.put(TypeIds.Monsters.HeroNecromancer, MicroUtils::microNecromancer);

        defaultMicroLogics.put(TypeIds.Piglins.Brute, MicroUtils::microBrute);
        defaultMicroLogics.put(TypeIds.Piglins.WitherSkeleton, MicroUtils::microWitherSkeleton);
        defaultMicroLogics.put(TypeIds.Piglins.HeroMerchant, MicroUtils::microMerchant);
    }

    public static Consumer<IPlayerWidget> getMicroLogic(int typeId) {
        return defaultMicroLogics.getOrDefault(typeId, null);
    }

    public static void enableAutocastAbilities(IPlayerWidget u) {
        if (u.is(TypeIds.Villagers.Witch)) {
            u.getAbility(ThrowLingeringRegenPotion.class).setAutocast(true);
            u.getAbility(ThrowWaterPotion.class).setAutocast(true);
        }

        if (u.is(TypeIds.Villagers.Evoker)) {
            u.getAbility(CastSummonVexes.class).setAutocast(true);
        }

        if (u.isAnyOf(TypeIds.Monsters.Spider, TypeIds.Monsters.PoisonSpider)) {
            u.getAbility(SpinWebs.class).setAutocast(true);
        }
    }

    public static void microRavager(IPlayerWidget unit) {
        var roarAbility = unit.getAbility(Roar.class);
        if (!roarAbility.isOffCooldown()) {
            return;
        }

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()) && GeometryUtils.isWithinDistance(unit, e, 4))) {
            unit.issueOrder(TypeIds.Orders.Roar);
            return;
        }
    }

    public static void microRoyalGuard(IPlayerWidget unit) {
        var target = unit.getAttackTarget();
        LivingEntity entityTarget = (target instanceof LivingEntity e) && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()))
                ? e
                : null;
        if (entityTarget == null) {
            return;
        }

        var maceAbility = unit.getAbility(MaceSlam.class);
        if (maceAbility.rank > 0 && maceAbility.isOffCooldown() && GeometryUtils.isWithinDistance(unit, entityTarget, 4)) {
            unit.issueWidgetOrder(entityTarget, TypeIds.Orders.MaceSlam);
            return;
        }

        boolean hasLowHealth = unit.getHealth() < unit.getMaxHealth() * 0.5; // <50%
        var tauntAbility = unit.getAbility(TauntingCry.class);
        if (tauntAbility.rank > 0 && !hasLowHealth && tauntAbility.isOffCooldown() && GeometryUtils.isWithinDistance(unit, entityTarget, 6)) {
            unit.issueOrder(TypeIds.Orders.Taunt);
            return;
        }

        var avatarAbility = unit.getAbility(Avatar.class);
        if (avatarAbility.rank > 0 && avatarAbility.isOffCooldown() && GeometryUtils.isWithinDistance(unit, entityTarget, 8)) {
            unit.issueOrder(TypeIds.Orders.Avatar);
            return;
        }
    }

    public static void microWarden(IPlayerWidget unit) {
        var sonicBoomAbility = unit.getAbility(SonicBoom.class);
        if (!sonicBoomAbility.isOffCooldown()) {
            return;
        }

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && e.getHealth() > 30 && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()))) {
            unit.issueWidgetOrder(e, TypeIds.Orders.SonicBoom);
            return;
        }

        if (target instanceof BuildingPlacement b && !b.ownerName.equals(unit.getOwnerName())) {
            unit.issueWidgetOrder(b, TypeIds.Orders.SonicBoom);
            return;
        }
    }

    public static void microNecromancer(IPlayerWidget unit) {
        var target = unit.getAttackTarget();
        LivingEntity entityTarget = (target instanceof LivingEntity e) && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()))
                ? e
                : null;

        var siphonAbility = unit.getAbility(SoulSiphonPassive.class);

        var raiseDeadAbility = unit.getAbility(RaiseDead.class);
        if (raiseDeadAbility.rank > 0 && entityTarget != null && raiseDeadAbility.isOffCooldown() && GeometryUtils.isWithinDistance(unit, entityTarget, 10)) {
            siphonAbility.setAutocast(true); // always boost Raise Dead
            unit.issueOrder(TypeIds.Orders.RaiseDead);
            return;
        }

        var insomniaAbility = unit.getAbility(InsomniaCurse.class);
        if (insomniaAbility.rank > 0 && entityTarget != null && insomniaAbility.isOffCooldown() && GeometryUtils.isWithinDistance(unit, entityTarget, 12)) {
            siphonAbility.setAutocast(false); // always boost Insomnia
            unit.issueWidgetOrder(entityTarget, TypeIds.Orders.InsomniaCurse);
            return;
        }

        // just spam the ultimate kekw
        var bloodMoonAbility = unit.getAbility(InsomniaCurse.class);
        if (bloodMoonAbility.rank > 0 && bloodMoonAbility.isOffCooldown()) {
            siphonAbility.setAutocast(false); // never boost Blood Moon
            unit.issueOrder(TypeIds.Orders.BloodMoon);
            return;
        }
    }

    public static void microBrute(IPlayerWidget unit) {
        var shieldAbility = unit.getAbility(ToggleShield.class);
        var bloodlustAbility = unit.getAbility(Bloodlust.class);
        var canShield = ResearchServerEvents.playerHasResearch(unit.getOwnerName(), ProductionItems.RESEARCH_BRUTE_SHIELDS);
        var canBloodlust = ResearchServerEvents.playerHasResearch(unit.getOwnerName(), ProductionItems.RESEARCH_BLOODLUST)
                && unit.getHealth() > unit.getMaxHealth() / 2; // only cast bloodlust when over 50% health

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName())) && GeometryUtils.isWithinDistance(unit, e, 10)) {
            if (canBloodlust && bloodlustAbility.isOffCooldown()) unit.issueOrder(TypeIds.Orders.BloodLust);
            if (target instanceof RangedAttackerUnit) {
                if (canShield && !shieldAbility.isAutocasting()) unit.issueOrder(TypeIds.Orders.ShieldOn);
            }
            return;
        }

        if (target instanceof BuildingPlacement b && !b.ownerName.equals(unit.getOwnerName())) {
            if (canBloodlust && bloodlustAbility.isOffCooldown()) unit.issueOrder(TypeIds.Orders.BloodLust);
            if (canShield && !shieldAbility.isAutocasting()) unit.issueOrder(TypeIds.Orders.ShieldOn);
            return;
        }

        if (canShield && shieldAbility.isAutocasting()) unit.issueOrder(TypeIds.Orders.ShieldOff);
    }

    public static void microWitherSkeleton(IPlayerWidget unit) {
        var witherCloudAbility = unit.getAbility(WitherCloud.class);
        if (!witherCloudAbility.isOffCooldown()) {
            return;
        }

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName())) && GeometryUtils.isWithinDistance(unit, e, 4)) {
            unit.issueOrder(TypeIds.Orders.WitherCloud);
            return;
        }
    }

    public static void microMerchant(IPlayerWidget unit) {
        var target = unit.getAttackTarget();
        LivingEntity entityTarget = (target instanceof LivingEntity e) && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()))
                ? e
                : null;

        // control greedisgood before casting any other ability

        var player = WorldApi.getSingleton().players.get(unit.getOwnerName());
        boolean highFood = player.getFood() > 500;
        boolean highWood = player.getWood() > 500;
        boolean highOre = player.getOre() > 500;
        var greedAbility = unit.getAbility(GreedIsGoodPassive.class);

        var tntAbility = unit.getAbility(ThrowTNT.class);
        if (tntAbility.rank > 0 && tntAbility.isOffCooldown() && entityTarget != null && GeometryUtils.isWithinDistance(unit, entityTarget, 20)) {
            greedAbility.setAutocast(highWood);
            unit.issueWidgetOrder(entityTarget, TypeIds.Orders.ThrowTnt);
            return;
        }

        boolean hasLowHealth = unit.getHealth() < unit.getMaxHealth() * 0.5; // <50%
        var feastAbility = unit.getAbility(FancyFeast.class);
        if (feastAbility.rank > 0 && feastAbility.isOffCooldown() && hasLowHealth) {
            greedAbility.setAutocast(highFood);
            unit.issuePointOrder(unit.getPos(), TypeIds.Orders.FancyFeast);
            return;
        }

        // just spam the ultimate kekw
        boolean hasHighMana = unit.getMana() > unit.getMaxMana() * 0.5; // >50%
        var lootAbility = unit.getAbility(LootExplosion.class);
        if (lootAbility.rank > 0 && lootAbility.isOffCooldown() && hasHighMana) {
            greedAbility.setAutocast(highOre);
            unit.issueOrder(TypeIds.Orders.LootExplosion);
            return;
        }
    }
}
