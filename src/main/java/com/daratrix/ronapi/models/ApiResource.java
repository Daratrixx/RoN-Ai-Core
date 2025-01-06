/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models;

import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.models.interfaces.IResource;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Daratrix
 */
public class ApiResource implements IResource {

    private int resourceTypeId;
    private ResourceName resourceName;
    private ArrayList<BlockPos> blocks = new ArrayList<>();

    private BlockPos.MutableBlockPos centerPos = new BlockPos.MutableBlockPos(0, 0, 0);
    private BlockPos.MutableBlockPos minPos = new BlockPos.MutableBlockPos(0, 0, 0);
    private BlockPos.MutableBlockPos maxPos = new BlockPos.MutableBlockPos(0, 0, 0);
    private int resourceAmount;
    private AABB boundingBox = null;

    public ApiResource(ResourceName resourceName) {
        this.resourceTypeId = TypeIds.get(resourceName);
        this.resourceName = resourceName;
    }

    @Override
    public int getAmount() {
        return this.resourceAmount;
    }

    @Override
    public ResourceName getResourceType() {
        return this.resourceName;
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
    public int getBlockCount() {
        return this.blocks.size();
    }

    @Override
    public Stream<BlockPos> getBlocks() {
        return this.blocks.stream();
    }

    @Override
    public AABB getBoundingBox() {
        return this.boundingBox;
    }

    @Override
    public void destroy() {

    }

    @Override
    public int getTypeId() {
        return this.resourceTypeId;
    }

    public void track(Level level, BlockPos pos) {
        this.blocks.add(pos.immutable());
        updateValueAndBoundingBox(level);
    }

    public void untrack(Level level, BlockPos pos) {
        this.blocks.removeIf(p -> p.equals(pos));
        updateValueAndBoundingBox(level);
    }

    public void updateValueAndBoundingBox(Level level) {
        this.resourceAmount = 0;
        this.minPos.set(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.maxPos.set(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        for (BlockPos pos : this.blocks) {
            var resourceSource = ResourceSources.getFromBlockPos(pos, level);
            if (resourceSource == null) {
                continue;
            }

            this.minPos.setX(Math.min(this.minPos.getX(), pos.getX()));
            this.minPos.setY(Math.min(this.minPos.getY(), pos.getY()));
            this.minPos.setZ(Math.min(this.minPos.getZ(), pos.getZ()));

            this.maxPos.setX(Math.max(this.maxPos.getX(), pos.getX()));
            this.maxPos.setY(Math.max(this.maxPos.getY(), pos.getY()));
            this.maxPos.setZ(Math.max(this.maxPos.getZ(), pos.getZ()));

            this.resourceAmount += resourceSource.resourceValue;
        }

        this.centerPos.setX((this.minPos.getX() + this.maxPos.getX()) / 2);
        this.centerPos.setX((this.minPos.getY() + this.maxPos.getY()) / 2);
        this.centerPos.setZ((this.minPos.getZ() + this.maxPos.getZ()) / 2);

        this.boundingBox = GeometryUtils.getBoundingBox(this.minPos, this.maxPos.offset(1, 1, 1));
    }

    @Override
    public float getX() {
        return centerPos.getX();
    }

    @Override
    public float getY() {
        return centerPos.getY();
    }

    @Override
    public float getZ() {
        return centerPos.getZ();
    }

    @Override
    public BlockPos getPos() {
        return this.centerPos;
    }
}
