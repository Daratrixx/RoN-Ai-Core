package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.utils.FileLogger;

public class AiControllerPriorities implements IAiControllerPriorities {
    public final FileLogger logger;
    private final AiProductionPriorities buildingPriorities;
    private final AiProductionPriorities unitPriorities;
    private final AiProductionPriorities researchPriorities;
    private final AiHarvestPriorities harvestingPriorities;
    private final AiArmyPriorities armyPriorities;

    public AiControllerPriorities(FileLogger logger) {
        this.logger = logger;
        this.buildingPriorities = new AiProductionPriorities(logger);
        this.unitPriorities = new AiProductionPriorities(logger);
        this.researchPriorities = new AiProductionPriorities(logger);
        this.harvestingPriorities = new AiHarvestPriorities(logger);
        this.armyPriorities = new AiArmyPriorities(logger);
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
