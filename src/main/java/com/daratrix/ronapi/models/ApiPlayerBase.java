package com.daratrix.ronapi.models;

import com.daratrix.ronapi.models.interfaces.IBoxed;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.ILocated;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BinaryOperator;

public class ApiPlayerBase implements ILocated, IBoxed {
    public final String playerName;
    public AABB boundingBox;
    public final ArrayList<IBuilding> buildings = new ArrayList<>();

    private final BlockPos.MutableBlockPos minPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos maxPos = new BlockPos.MutableBlockPos();

    private int capitols = 0;

    /**
     * Create a new player-base object based on the provided building
     *
     * @param building The building to build the base from
     */
    public ApiPlayerBase(IBuilding building) {
        this.playerName = building.getOwnerName();
        this.buildings.add(building);
        this.minPos.set(building.getMinPos());
        this.minPos.move(-20, -10, -20);
        this.maxPos.set(building.getMaxPos());
        this.maxPos.move(20, 10, 20);
        this.boundingBox = new AABB(this.minPos, this.maxPos);
    }

    private static void adjust(BlockPos.MutableBlockPos oldPos, BlockPos newPos, int offset, BinaryOperator<Integer> operation) {
        oldPos.setX(operation.apply(oldPos.getX(), newPos.getX() + offset));
        oldPos.setY(operation.apply(oldPos.getY(), newPos.getY() + offset / 2)); // also offset for cliffs/flying units
        oldPos.setZ(operation.apply(oldPos.getZ(), newPos.getZ() + offset));
    }

    /**
     * Attempts to update this base by adding/removing the provided building.
     * This will most likely cause an update of the bounding box and min/max positions.
     *
     * @param building The building to add/remove to the base
     * @return false is the base is empty (should be deleted), true otherwise (should be kept)
     */
    public boolean track(IBuilding building) {
        if (!building.isAlive() || !Objects.equals(building.getOwnerName(), this.playerName)) {
            return this.untrack(building);
        }

        if (!this.buildings.contains(building)) {
            this.buildings.add(building);
            adjust(this.minPos, building.getMinPos(), -20, Math::min);
            adjust(this.maxPos, building.getMaxPos(), 20, Math::max);
            this.boundingBox = new AABB(this.minPos, this.maxPos);
            if(building.isCapitol()) {
                ++this.capitols;
            }
        }

        return true; // added/already in
    }

    /**
     * Attempts to update this base by removing the provided building.
     * This will most likely cause an update of the bounding box and min/max positions.
     *
     * @param building The building to remove to the base
     * @return false is the base is empty (should be deleted), true otherwise (should be kept)
     */
    public boolean untrack(IBuilding building) {
        if (this.buildings.contains(building)) {
            this.buildings.remove(building);

            if(building.isCapitol()) {
                ++this.capitols;
            }

            var buildingIterator = this.buildings.iterator();
            if (!buildingIterator.hasNext()) {
                return false; // removed and empty
            }

            building = buildingIterator.next();
            this.minPos.set(building.getMinPos());
            this.minPos.move(-20, -10, -20);
            this.maxPos.set(building.getMaxPos());
            this.maxPos.move(20, 10, 20);

            while (buildingIterator.hasNext()) {
                building = buildingIterator.next();
                adjust(this.minPos, building.getMinPos(), -20, Math::min);
                adjust(this.maxPos, building.getMaxPos(), 20, Math::max);
            }

            this.boundingBox = new AABB(this.minPos, this.maxPos);
        }

        return true; // not empty
    }

    @Override
    public float getX() {
        return (this.minPos.getX() + this.maxPos.getX()) * 0.5f;
    }

    @Override
    public float getY() {
        return (this.minPos.getY());
    }

    @Override
    public float getZ() {
        return (this.minPos.getZ() + this.maxPos.getZ()) * 0.5f;
    }

    @Override
    public int getMinX() {
        return this.minPos.getX();
    }

    @Override
    public int getMaxX() {
        return this.maxPos.getX();
    }

    @Override
    public int getMinY() {
        return this.minPos.getY();
    }

    @Override
    public int getMaxY() {

        return this.maxPos.getY();
    }

    @Override
    public int getMinZ() {
        return this.minPos.getZ();
    }

    @Override
    public int getMaxZ() {
        return this.maxPos.getZ();
    }

    @Override
    public BlockPos getMinPos() {
        return this.minPos.immutable();
    }

    @Override
    public BlockPos getMaxPos() {
        return this.maxPos.immutable();
    }

    @Override
    public AABB getBoundingBox() {
        return this.boundingBox;
    }

    public boolean hasCapitol() {
        return this.capitols > 0;
    }
}