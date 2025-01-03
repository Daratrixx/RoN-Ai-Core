package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;

public class AiControllerPriorities implements IAiControllerPriorities {
    private final AiProductionPriorities buildingPriorities = new AiProductionPriorities();
    private final AiProductionPriorities unitPriorities = new AiProductionPriorities();
    private final AiProductionPriorities researchPriorities = new AiProductionPriorities();
    private final AiHarvestPriorities harvestingPriorities = new AiHarvestPriorities();
    private final AiArmyPriorities armyPriorities = new AiArmyPriorities();

    public AiControllerPriorities() {
    }

    @Override
    public AiProductionPriorities getBuildingPriorities() {
        return this.buildingPriorities;
    }

    @Override
    public AiProductionPriorities getUnitPriorities() {
        return this.unitPriorities;
    }

    @Override
    public AiProductionPriorities getResearchPriorities() {
        return this.researchPriorities;
    }

    @Override
    public AiHarvestPriorities getHarvestingPriorities() {
        return this.harvestingPriorities;
    }

    @Override
    public AiArmyPriorities getArmyPriorities() {
        return this.armyPriorities;
    }

    @Override
    public void reset() {
        this.buildingPriorities.reset();
        this.unitPriorities.reset();
        this.researchPriorities.reset();
        this.harvestingPriorities.reset();
        this.armyPriorities.reset();
    }
}
