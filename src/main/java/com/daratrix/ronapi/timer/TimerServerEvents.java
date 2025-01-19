package com.daratrix.ronapi.timer;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TimerServerEvents {

    private final static List<Timer> timers = new ArrayList<Timer>();
    private final static List<Timer> timersToDelete = new ArrayList<Timer>();
    private final static List<Timer> queuedTimers = new ArrayList<Timer>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        synchronized (timersToDelete) {
            timers.removeAll(timersToDelete);
            timersToDelete.clear();
        }

        synchronized (queuedTimers) {
            timers.addAll(queuedTimers);
            queuedTimers.clear();
        }

        var expiredTimers = new ArrayList<Timer>();
        for (Timer timer : timers) {
            timer.tick(evt.getServer());
            if (timer.shouldDelete()) {
                expiredTimers.add(timer);
            }
        }

        synchronized (timersToDelete) {
            timersToDelete.removeAll(expiredTimers);
        }
    }

    public static Timer queueTimer(float interval, Consumer<MinecraftServer> callback) {
        Timer t = new Timer(interval, callback, false);
        synchronized (queuedTimers) {
            queuedTimers.add(t);
        }

        return t;
    }

    public static Timer queueTimerLooping(float interval, Consumer<MinecraftServer> callback) {
        Timer t = new Timer(interval, callback, true);
        synchronized (queuedTimers) {
            queuedTimers.add(t);
        }

        return t;
    }

    public static void destroyTimer(Timer t) {
        if (t != null) {
            synchronized (timersToDelete) {
                timersToDelete.add(t);
            }
        }
    }
}
