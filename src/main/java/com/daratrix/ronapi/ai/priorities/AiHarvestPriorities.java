package com.daratrix.ronapi.ai.priorities;

import com.daratrix.ronapi.utils.FileLogger;

public class AiHarvestPriorities extends AiAbstractPriorities<AiHarvestPriorities.AiHarvestPriority> {

    public final FileLogger logger;
    public boolean allowHunting = true;
    public boolean onlyNetherBlocks = false;

    public AiHarvestPriorities(FileLogger logger) {
        this.logger = logger;
    }

    public AiHarvestPriority addPriority(int typeId, int count) {
        var priority = new AiHarvestPriority(typeId, count);
        priorities.add(priority);
        return priority;
    }

    public static class AiHarvestPriority extends AiAbstractPriorities.AiAbstractPriority {
        public AiHarvestPriority(int typeId, int count) {
            super(typeId, count);
        }
    }
}