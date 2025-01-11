package com.daratrix.ronapi;;

import com.daratrix.ronapi.ai.registers.AiGameRuleRegister;
import com.daratrix.ronapi.ai.scripts.MonsterScript;
import com.daratrix.ronapi.ai.scripts.VillagerScript;
import com.daratrix.ronapi.registers.ClientEventRegister;
import com.daratrix.ronapi.registers.GameRuleRegister;
import com.daratrix.ronapi.registers.ServerEventRegister;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RonApi.MOD_ID)
public class RonApi {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "ronapi";

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public RonApi() {
        GameRuleRegister.init();
        AiGameRuleRegister.init();

        final ClientEventRegister clientRegister = new ClientEventRegister();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> clientRegister::registerClientEvents);

        final ServerEventRegister serverRegister = new ServerEventRegister();
        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> serverRegister::registerServerEvents);

        // todo: move to a separate add-on
        VillagerScript.register();
        MonsterScript.register();
    }
}
