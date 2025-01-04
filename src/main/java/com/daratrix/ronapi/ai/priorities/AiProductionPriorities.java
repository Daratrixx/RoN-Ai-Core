package com.daratrix.ronapi.ai.priorities;

import com.daratrix.ronapi.utils.FileLogger;

public class AiProductionPriorities extends AiAbstractPriorities<AiProductionPriorities.AiProductionPriority> {

    public final FileLogger logger;
    public boolean allowHunting = true;
    public boolean onlyNetherBlocks = false;

    public AiProductionPriorities(FileLogger logger) {
        this.logger = logger;
    }

    public void addPriority(int typeId, int count) {
        priorities.add(new AiProductionPriority(typeId, count));
    }

    public void addPriority(int typeId, int count, Location location) {
        priorities.add(new AiProductionPriority(typeId, count, location));
    }

    public void addPriorityAtMain(int typeId, int count) {
        priorities.add(new AiProductionPriority(typeId, count, Location.MAIN));
    }

    public void addPriorityAtWood(int typeId, int count) {
        priorities.add(new AiProductionPriority(typeId, count, Location.WOOD));
    }

    public void addPriorityAtOre(int typeId, int count) {
        priorities.add(new AiProductionPriority(typeId, count, Location.ORE));
    }

    public static class AiProductionPriority extends AiAbstractPriorities.AiAbstractPriority {
        public final Location location;
        public final int builders;

        public AiProductionPriority(int typeId, int count) {
            this(typeId, count, Location.ANY, 1);
        }

        public AiProductionPriority(int typeId, int count, Location location) {
            this(typeId, count, location, 1);
        }

        public AiProductionPriority(int typeId, int count, int builders) {
            this(typeId, count, Location.ANY, builders);
        }

        public AiProductionPriority(int typeId, int count, Location location, int builders) {
            super(typeId, count);
            this.location = location;
            this.builders = builders;
        }
    }

    public enum Location {
        ANY,
        MAIN,
        FARM,
        WOOD,
        ORE,
    }
}
