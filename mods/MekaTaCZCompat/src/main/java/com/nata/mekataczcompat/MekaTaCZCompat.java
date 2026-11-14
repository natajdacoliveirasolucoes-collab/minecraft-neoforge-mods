package com.nata.mekataczcompat;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = MekaTaCZCompat.MOD_ID, dist = Dist.CLIENT)
public final class MekaTaCZCompat {
    public static final String MOD_ID = "mekataczcompat";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static boolean loggedFirstBypass;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Skip the MekaSuit first-person arm replacement while holding a TaCZ gun.",
                "Only affects local rendering; armor protection and third-person appearance are preserved.")
                .define("enabled", true);
        SPEC = builder.build();
    }

    public MekaTaCZCompat(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
        NeoForge.EVENT_BUS.addListener(MekaTaCZCompat::registerCommands);
        LOGGER.info("[MekaTaCZCompat] Client compatibility patch loaded. Patch local por Natã Oliveira (nata).");
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static void recordBypass() {
        if (!loggedFirstBypass) {
            loggedFirstBypass = true;
            LOGGER.info("[MekaTaCZCompat] Preserved TaCZ arm pose; skipped Mekanism first-person arm replacement.");
        }
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mekataczcompat")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "MekaSuit/TaCZ arm fix: " + (isEnabled() ? "ON" : "OFF")), false);
                    return 1;
                })
                .then(Commands.literal("enabled")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "value");
                                    ENABLED.set(enabled);
                                    SPEC.save();
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "MekaSuit/TaCZ arm fix: " + (enabled ? "ON" : "OFF")), false);
                                    return 1;
                                }))));
    }
}
