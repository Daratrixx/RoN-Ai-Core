package com.daratrix.ronapi.models;

import com.daratrix.ronapi.utils.SpiralCounter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApiWorldTests {

    @Test
    public void testGetChunkXY() {
        // -1 because we skip s-2²
        assertEquals(-1, ApiWorld.getChunkX(1-1, 3));
        assertEquals(1, ApiWorld.getChunkZ(1-1, 3));
        assertEquals(0, ApiWorld.getChunkX(2-1, 3));
        assertEquals(1, ApiWorld.getChunkZ(2-1, 3));

        assertEquals(1, ApiWorld.getChunkX(3-1, 3));
        assertEquals(1, ApiWorld.getChunkZ(3-1, 3));
        assertEquals(1, ApiWorld.getChunkX(4-1, 3));
        assertEquals(0, ApiWorld.getChunkZ(4-1, 3));

        assertEquals(1, ApiWorld.getChunkX(5-1, 3));
        assertEquals(-1, ApiWorld.getChunkZ(5-1, 3));
        assertEquals(0, ApiWorld.getChunkX(6-1, 3));
        assertEquals(-1, ApiWorld.getChunkZ(6-1, 3));

        assertEquals(-1, ApiWorld.getChunkZ(7-1, 3));
        assertEquals(-1, ApiWorld.getChunkX(7-1, 3));
        assertEquals(-1, ApiWorld.getChunkX(8-1, 3));
        assertEquals(0, ApiWorld.getChunkZ(8-1, 3));

        // -9 because we skip s-2²
        assertEquals(-2, ApiWorld.getChunkX(9-9, 5));
        assertEquals(2, ApiWorld.getChunkZ(9-9, 5));
    }

    @Test
    public void spiralTest() {
        var spiral = new SpiralCounter();

        spiral.skipS(1);
        assertEquals(-1, spiral.getX());
        assertEquals(1, spiral.getZ());
        spiral.next();
        assertEquals(0, spiral.getX());
        assertEquals(1, spiral.getZ());

        spiral.reset();

        spiral.skipS(2);
        assertEquals(-2, spiral.getX());
        assertEquals(2, spiral.getZ());
        spiral.next();
        assertEquals(-1, spiral.getX());
        assertEquals(2, spiral.getZ());

        spiral.reset();

        spiral.skipS(3);
        assertEquals(-3, spiral.getX());
        assertEquals(3, spiral.getZ());
        spiral.next();
        assertEquals(-2, spiral.getX());
        assertEquals(3, spiral.getZ());

        spiral.reset();

        spiral.skipS(4);
        assertEquals(-4, spiral.getX());
        assertEquals(4, spiral.getZ());
        spiral.next();
        assertEquals(-3, spiral.getX());
        assertEquals(4, spiral.getZ());
    }
}
