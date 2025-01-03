package com.daratrix.ronapi.utils;

public class SpiralCounter {

    private int n = 0;
    private int s = 1;
    private int x = 0;
    private int z = 0;

    public void reset() {
        this.n = 0;
        this.s = 1;
        this.x = 0;
        this.z = 0;
    }

    public void next() {
        ++this.n;
        if (this.n >= this.s * this.s) {
            this.s += 2;
        }
        this.updateX();
        this.updateZ();
    }

    public void skipS(int skip) {
        this.s += 2 * skip;
        this.n = (s - 2) * (s - 2);
        this.updateX();
        this.updateZ();
    }

    public void skipN(int skip) {
        this.n += skip;
        while (this.n >= this.s * this.s) {
            this.s += 2;
        }
        this.updateX();
        this.updateZ();
    }

    private void updateX() {
        var n = this.n - (s - 2) * (s - 2);
        var s2 = s / 2;
        int side = n / (s - 1);
        this.x = switch (side) {
            case 0 -> (n % (s - 1)) - s2;
            case 1 -> s2;
            case 2 -> s2 - (n % (s - 1));
            default -> -s2;
        };

    }

    private void updateZ() {
        var n = this.n - (s - 2) * (s - 2);
        var s2 = s / 2;
        int side = n / (s - 1);
        this.z = switch (side) {
            case 0 -> s2;
            case 1 -> s2 - (n % (s - 1));
            case 2 -> -s2;
            default -> (n % (s - 1)) - s2;
        };
    }

    public int getN() {
        return this.n;
    }

    public int getS() {
        return this.s;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }
}
