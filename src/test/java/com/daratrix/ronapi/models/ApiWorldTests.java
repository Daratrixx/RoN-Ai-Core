package com.daratrix.ronapi.models;

import com.daratrix.ronapi.utils.SpiralCounter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApiWorldTests {

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
