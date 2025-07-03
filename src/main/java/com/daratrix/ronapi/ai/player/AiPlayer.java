package com.daratrix.ronapi.ai.player;

import com.daratrix.ronapi.ai.AiDependencies;
import com.daratrix.ronapi.ai.MicroUtils;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.models.ApiBuilding;
import com.daratrix.ronapi.models.ApiPlayer;
import com.daratrix.ronapi.models.ApiPlayerBase;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class AiPlayer extends ApiPlayer implements IAiPlayer {

    public AiPlayer(RTSPlayer player) {
        super(player);
    }

    private final Map<Integer, Integer> existing = new HashMap<>();
    private final Map<Integer, Integer> existingDone = new HashMap<>();

    private final List<IBuilding> constructingBuildings = new ArrayList<>();

    private final List<IUnit> heroes = new ArrayList<>();

    @Override
    public boolean hasHero(int typeId) {
        if (this.heroes.stream().anyMatch(u -> u.is(typeId))) {
            return true;
        }

        var heroName = TypeIds.toHeroName(typeId);
        return HeroUnit.getFallenHero(false, this.name, heroName) != null;
    }

    @Override
    public int count(int priority) {
        var baseId = AiDependencies.getUpgradeSourceTypeId(priority);
        if (baseId == 0) {
            // default behaviour when counting something that is not a building upgrade
            return existing.getOrDefault(priority, 0) + (int) buildings.stream().filter(b -> b.getCurrentOrderId() == priority).count();
        }

        return (int) this.units.stream().filter(u -> u.is(priority)).count()
                + (int) this.buildings.stream().filter(b -> b.is(priority) || (b.is(baseId) && b.getCurrentOrderId() == priority)).count();
    }

    @Override
    public int count(int priority, AiProductionPriorities.Location location) {
        return count(priority); // TODO
    }

    @Override
    public int countDone(int priority) {
        var baseId = AiDependencies.getUpgradeSourceTypeId(priority);
        if (baseId == 0) {
            // default behaviour when counting something that is not a building upgrade
            return existingDone.getOrDefault(priority, 0);
        }

        return (int) this.units.stream().filter(u -> u.is(priority)).count()
                + (int) this.buildings.stream().filter(b -> b.isDone() && b.is(priority)).count();
    }

    @Override
    public int countDone(int priority, AiProductionPriorities.Location location) {
        return countDone(priority); // TODO
    }

    @Override
    public Stream<IUnit> getIdleUnits(int priority) {
        return this.getUnitsFiltered(u -> u.isIdle() && u.is(priority));
    }

    @Override
    public Stream<IUnit> getIdleUnits(Collection<Integer> priorities) {
        if (priorities == null) {
            return Stream.empty();
        }

        return this.getUnitsFiltered(u -> u.isIdle() && priorities.contains(u.getTypeId()));
    }

    @Override
    public Stream<IUnit> getUnits(int priority) {
        return this.getUnitsFiltered(u -> u.is(priority));
    }

    @Override
    public Stream<IUnit> getUnits(Collection<Integer> priorities) {
        if (priorities == null) {
            return Stream.empty();
        }

        return this.getUnitsFiltered(u -> priorities.contains(u.getTypeId()) || u.isAnyOf(priorities));
    }

    @Override
    public Stream<IBuilding> getIdleBuildings(int priority) {
        return this.getBuildingsFiltered(b -> b.isDone() && b.isIdle() && b.is(priority));
    }

    @Override
    public Stream<IBuilding> getIdleBuildings(Collection<Integer> priorities) {
        if (priorities == null) {
            return Stream.empty();
        }

        return this.getBuildingsFiltered(b -> b.isDone() && b.isIdle() && (priorities.contains(b.getTypeId()) || b.isAnyOf(priorities)));
    }

    @Override
    public Stream<IBuilding> getBuildings(int priority) {
        return this.getBuildingsFiltered(b -> b.is(priority));
    }

    @Override
    public Stream<IBuilding> getBuildings(Collection<Integer> priorities) {
        if (priorities == null) {
            return Stream.empty();
        }

        return this.getBuildingsFiltered(b -> priorities.contains(b.getTypeId()) || b.isAnyOf(priorities));
    }

    @Override
    public Stream<IBuilding> getConstrutingBuildings() {
        return this.constructingBuildings.stream();
    }

    @Override
    public void checkCompletedBuildings() {
        synchronized (constructingBuildings) {
            constructingBuildings.forEach(b -> {
                if (!b.isDone()) {
                    return;
                }

                var previous = existingDone.getOrDefault(b.getTypeId(), 0);
                var current = previous + 1;
                existingDone.put(b.getTypeId(), current);
                System.out.println("Completed " + b.getName() + " (" + previous + "->" + current + ")");
            });

            constructingBuildings.removeIf(b -> b.isDone());
        }
    }

    @Override
    public void reset() {
        super.reset();
        existing.clear();
        existingDone.clear();
    }

    @Override
    public boolean track(IUnit unit) {
        if (super.track(unit)) {
            existing.computeIfPresent(unit.getTypeId(), (id, count) -> ++count);
            existing.putIfAbsent(unit.getTypeId(), 1);
            existingDone.computeIfPresent(unit.getTypeId(), (id, count) -> ++count);
            existingDone.putIfAbsent(unit.getTypeId(), 1);

            MicroUtils.enableAutocastAbilities(unit);

            if (unit.isHero()) {
                this.heroes.removeIf(u -> u.getTypeId() == unit.getTypeId());
                this.heroes.add(unit);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean track(IBuilding building) {
        if (super.track(building)) {
            synchronized (existing) {
                existing.computeIfPresent(building.getTypeId(), (id, count) -> ++count);
                existing.putIfAbsent(building.getTypeId(), 1);
            }
            if (building.isDone()) {
                synchronized (existingDone) {
                    existingDone.computeIfPresent(building.getTypeId(), (id, count) -> ++count);
                    existingDone.putIfAbsent(building.getTypeId(), 1);
                }
            } else {
                synchronized (constructingBuildings) {
                    if (!constructingBuildings.contains(building)) {
                        constructingBuildings.add(building);
                    }
                }
            }
            System.out.println(this.getName() + " tracking " + building.getName() + " as " + building.getTypeId() + " (" + countDone(building.getTypeId()) + "/" + count(building.getTypeId()) + ")");
            return true;
        }

        return false;
    }

    @Override
    public boolean untrack(IUnit unit) {
        if (super.untrack(unit)) {
            existing.computeIfPresent(unit.getTypeId(), (id, count) -> --count);
            existingDone.computeIfPresent(unit.getTypeId(), (id, count) -> --count);
            System.out.println(this.getName() + " tracking " + unit.getName() + " (" + countDone(unit.getTypeId()) + "/" + count(unit.getTypeId()) + ")");
            return true;
        }

        return false;
    }

    @Override
    public boolean untrack(IBuilding building) {
        if (super.untrack(building)) {
            existing.computeIfPresent(building.getTypeId(), (id, count) -> --count);
            if (building.isDone()) {
                existingDone.computeIfPresent(building.getTypeId(), (id, count) -> --count);
            } else {
                constructingBuildings.remove(building);
            }
            return true;
        }
        return false;
    }
}
