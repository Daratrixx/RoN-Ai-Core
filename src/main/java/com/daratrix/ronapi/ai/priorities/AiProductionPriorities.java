package com.daratrix.ronapi.ai.priorities;

import com.daratrix.ronapi.utils.FileLogger;

public class AiProductionPriorities extends AiAbstractPriorities<AiProductionPriorities.AiProductionPriority> {

    public final FileLogger logger;
    public boolean allowHunting = true;
    public boolean onlyNetherBlocks = false;
    private Location defaultLocation = Location.ANY;
    private Proximity defaultProximity = Proximity.RANDOM;

    public void setDefaultLocation(Location defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public void setDefaultProximity(Proximity defaultProximity) {
        this.defaultProximity = defaultProximity;
    }

    public AiProductionPriorities(FileLogger logger) {
        this.logger = logger;
    }

    public AiProductionPriority addPriority(int typeId, int count) {
        var priority = new AiProductionPriority(typeId, count).atLocation(this.defaultLocation).atProximity(this.defaultProximity);
        priorities.add(priority);
        return priority;
    }

    public static class AiProductionPriority extends AiAbstractPriorities.AiAbstractPriority<AiProductionPriority> {
        public Location location = Location.ANY;
        public Proximity proximity = Proximity.RANDOM;
        public int builders = 1;

        public AiProductionPriority(int typeId, int count) {
            super(typeId, count);
        }

        public int builders() {
            return this.builders;
        }

        public AiProductionPriority builders(int builders) {
            this.builders = Math.max(1, builders);
            return this;
        }

        public AiProductionPriority atLocation(Location location) {
            this.location = location;
            return this;
        }

        public AiProductionPriority atFarm() {
            this.location = Location.FARM;
            return this;
        }

        public AiProductionPriority atProximity(Proximity proximity) {
            this.proximity = proximity;
            return this;
        }

        public AiProductionPriority atFront() {
            this.proximity = Proximity.FRONT;
            return this;
        }

        public AiProductionPriority atBack() {
            this.proximity = Proximity.BACK;
            return this;
        }

        public AiProductionPriority atSide() {
            this.proximity = Proximity.SIDE;
            return this;
        }
    }

    public enum Location {
        ANY,
        CAPITOL,
        MAIN,
        FARM,
    }

    public enum Proximity {
        RANDOM,
        FRONT,
        BACK,
        SIDE,
    }
}
