/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import com.daratrix.ronapi.apis.TypeIds;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * @author Daratrix
 */
public interface IHero extends IUnit {
    public HeroUnit getHero();
    public int getHeroLevel();
    public int getSkillPoints();

    public <T extends HeroAbility> boolean upgradeSkill(Class<T> skillAbilityClass, int level);
}
