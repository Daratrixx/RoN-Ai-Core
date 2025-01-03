package com.daratrix.ronapi.utils;

import com.daratrix.ronapi.models.interfaces.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.stream.Stream;

public class GeometryUtils {

    public static void setMinMaxFromBlocks(BlockPos.MutableBlockPos min, BlockPos.MutableBlockPos max, Stream<BlockPos> blocks) {
        min.set(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        max.set(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        blocks.forEach(pos -> {
            min.setX(Math.min(min.getX(), pos.getX()));
            min.setY(Math.min(min.getY(), pos.getY()));
            min.setZ(Math.min(min.getZ(), pos.getZ()));

            max.setX(Math.max(max.getX(), pos.getX()));
            max.setY(Math.max(max.getY(), pos.getY()));
            max.setZ(Math.max(max.getZ(), pos.getZ()));
        });
    }

    public static AABB getBoundingBox(Stream<BlockPos> blocks) {
        BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos();
        setMinMaxFromBlocks(min, max, blocks);
        return getBoundingBox(min, max);
    }

    public static AABB getBoundingBox(Stream<BlockPos> blocks, int expand) {
        BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos();
        setMinMaxFromBlocks(min, max, blocks);
        return getBoundingBox(min, max, expand);
    }

    public static AABB getBoundingBox(BlockPos min, BlockPos max) {
        return new AABB(min, max);
    }

    public static AABB getBoundingBox(BlockPos min, BlockPos max, int expand) {
        return new AABB(min.getX() - expand, min.getY() - expand, min.getZ() - expand,
                max.getX() + expand, max.getY() + expand, max.getZ() + expand);
    }


    public static double getSpanOverlap(double min1, double max1, double min2, double max2) {
        return Math.abs(max1 - min1 + max2 - min2) // maximum span
                - Math.abs(Math.max(max1, max2) - Math.min(min1, min2)) // actual span
                ;
    }

    public static boolean isOverlapping(double min1, double max1, double min2, double max2) {
        return getSpanOverlap(min1, max1, min2, max2) > 0;
    }

    public static boolean colliding(AABB a, AABB b) {
        return getSpanOverlap(a.minX, a.maxX, b.minX, b.maxX) > 0
                && getSpanOverlap(a.minZ, a.maxZ, b.minZ, b.maxZ) > 0;
    }

    public static float distanceX(IBoxed b, Vec3i v) {
        return Math.min(Math.abs(v.getX() - b.getMinX()), Math.abs(v.getX() - b.getMaxX()));
    }

    public static float distanceZ(IBoxed b, Vec3i v) {
        return Math.min(Math.abs(v.getZ() - b.getMinZ()), Math.abs(v.getZ() - b.getMaxZ()));
    }

    public static float distanceSquared(IBoxed b1, IBoxed b2) {
        var oX = (float) getSpanOverlap(b1.getMinX(), b1.getMaxX(), b2.getMinX(), b2.getMaxX());
        var oZ = (float) getSpanOverlap(b1.getMinZ(), b1.getMaxZ(), b2.getMinZ(), b2.getMaxZ());
        if (oX > 0 && oZ > 0) {
            return 0; // overlapping, distance is 0
        }
        return oX * oX + oZ * oZ;
    }

    public static float distanceSquared(IBoxed b, ILocated l) {
        var x = Math.min(Math.abs(l.getX() - b.getMinX()), Math.abs(l.getX() - b.getMaxX()));
        var z = Math.min(Math.abs(l.getZ() - b.getMinZ()), Math.abs(l.getZ() - b.getMaxZ()));
        return x * x + z * z;
    }

    public static float distanceSquared(IBoxed b, Entity e) {
        var x = Math.min(Math.abs(e.getX() - b.getMinX()), Math.abs(e.getX() - b.getMaxX()));
        var z = Math.min(Math.abs(e.getZ() - b.getMinZ()), Math.abs(e.getZ() - b.getMaxZ()));
        return (float) (x * x + z * z);
    }

    public static float distanceSquared(IBoxed b, Vec3i p) {
        var x = Math.min(Math.abs(p.getX() - b.getMinX()), Math.abs(p.getX() - b.getMaxX()));
        var z = Math.min(Math.abs(p.getZ() - b.getMinZ()), Math.abs(p.getZ() - b.getMaxZ()));
        return (float) (x * x + z * z);
    }

    public static float distanceSquared(ILocated l, IBoxed b) {
        var x = Math.min(Math.abs(b.getMinX() - l.getX()), Math.abs(b.getMaxX() - l.getX()));
        var z = Math.min(Math.abs(b.getMinZ() - l.getZ()), Math.abs(b.getMaxZ() - l.getZ()));
        return x * x + z * z;
    }

    public static float distanceSquared(ILocated l1, ILocated l2) {
        var x = l2.getX() - l1.getX();
        var z = l2.getZ() - l1.getZ();
        return x * x + z * z;
    }

    public static float distanceSquared(ILocated l, Entity e) {
        var x = e.getX() - l.getX();
        var z = e.getZ() - l.getZ();
        return (float) (x * x + z * z);
    }

    public static float distanceSquared(ILocated l, Vec3i p) {
        var x = p.getX() - l.getX();
        var z = p.getZ() - l.getZ();
        return (float) (x * x + z * z);
    }

    public static boolean isWithinDistance(IBoxed b1, IBoxed b2, float distance) {
        return distanceSquared(b1, b2) <= distance * distance;
    }

    public static boolean isWithinDistance(IBoxed b, ILocated l, float distance) {
        return distanceSquared(b, l) <= distance * distance;
    }

    public static boolean isWithinDistance(IBoxed b, Entity e, float distance) {
        return distanceSquared(b, e) <= distance * distance;
    }

    public static boolean isWithinDistance(IBoxed b, Vec3i p, float distance) {
        return distanceSquared(b, p) <= distance * distance;
    }

    public static boolean isWithinDistance(ILocated l, IBoxed b, float distance) {
        return distanceSquared(l, b) <= distance * distance;
    }

    public static boolean isWithinDistance(ILocated l1, ILocated l2, float distance) {
        return distanceSquared(l1, l2) <= distance * distance;
    }

    public static boolean isWithinDistance(ILocated l, Entity e, float distance) {
        return distanceSquared(l, e) <= distance * distance;
    }

    public static boolean isWithinDistance(ILocated l, Vec3i p, float distance) {
        return distanceSquared(l, p) <= distance * distance;
    }

    public static int distanceComparator(IBoxed l1, IBoxed l2, BlockPos from) {
        return Float.compare(distanceSquared(l1, from), distanceSquared(l2, from));
    }

    public static int distanceComparator(ILocated l1, ILocated l2, BlockPos from) {
        return Float.compare(distanceSquared(l1, from), distanceSquared(l2, from));
    }

    public static int distanceComparator(ILocated l1, ILocated l2, ILocated from) {
        return Float.compare(distanceSquared(l1, from), distanceSquared(l2, from));
    }

    public static int distanceComparator(Entity l1, Entity l2, ILocated from) {
        return Float.compare(distanceSquared(from, l1), distanceSquared(from, l2));
    }

}
