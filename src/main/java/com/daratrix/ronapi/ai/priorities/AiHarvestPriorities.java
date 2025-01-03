package com.daratrix.ronapi.ai.priorities;

public class AiHarvestPriorities extends AiAbstractPriorities<AiHarvestPriorities.AiHarvestPriority> {

    public boolean allowHunting = true;
    public boolean onlyNetherBlocks = false;

    public void addPriority(int typeId, int count) {
        priorities.add(new AiHarvestPriority(typeId, count));
    }

    public static class AiHarvestPriority extends AiAbstractPriorities.AiAbstractPriority {
        public AiHarvestPriority(int typeId, int count) {
            super(typeId, count);
        }
    }
}