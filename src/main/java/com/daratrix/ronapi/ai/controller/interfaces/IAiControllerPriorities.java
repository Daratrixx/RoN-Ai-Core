package com.daratrix.ronapi.ai.controller.interfaces;

import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;

public interface IAiControllerPriorities {
    AiProductionPriorities getBuildingPriorities();
    AiProductionPriorities getUnitPriorities();
    AiProductionPriorities getResearchPriorities();
    AiHarvestPriorities getHarvestingPriorities();
    AiArmyPriorities getArmyPriorities();

    void reset();
}
