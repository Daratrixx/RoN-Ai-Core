package com.daratrix.ronapi.timer;

import it.unimi.dsi.fastutil.floats.FloatConsumer;

public class Timer {

    private static final float ticksPerSecond = 20.f;

    private final int interval;
    private int elapsed;
    private final FloatConsumer callback;
    private final boolean looping;
    private boolean paused;

    public Timer(float interval, FloatConsumer callback) {
        this.interval = Math.round(interval * ticksPerSecond);
        this.elapsed = 0;
        this.callback = callback;
        this.looping = false;
        System.out.println("started timer with tick interval: " + this.interval);
    }

    public Timer(float interval, FloatConsumer callback, boolean looping) {
        this.interval = Math.max(1, Math.round(interval * ticksPerSecond));
        this.elapsed = 0;
        this.callback = callback;
        this.looping = looping;
        System.out.println("started timer with tick interval: " + this.interval);
    }

    public boolean tick() {
        if (this.paused) {
            return false; // don't progress timer when paused
        }

        ++this.elapsed;
        if (this.elapsed % this.interval == 0) {
            this.callback.accept(this.elapsed / ticksPerSecond);
            if (!this.looping) {
                this.paused = true;
            }

            return true;
        }

        return false;
    }

    public boolean shouldDelete() {
        return !this.looping && this.paused && this.elapsed >= this.interval;
    }

    public Timer pause() {
        this.paused = true;
        return this;
    }

    public Timer unpause() {
        this.paused = false;
        return this;
    }
}