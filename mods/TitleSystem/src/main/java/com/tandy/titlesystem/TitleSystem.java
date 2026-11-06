package com.tandy.titlesystem;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(TitleSystem.MODID)
public class TitleSystem {
    public static final String MODID = "titlesystem";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TitleSystem(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        }
        LOGGER.info("Title System initialized");
    }
}
