package com.tandy.packrules;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/** Reads the real ProjectE capability, including zero, only at connection events and periodic samples. */
public final class ProjectEEmcAudit {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", true);
    private static final ModConfigSpec.IntValue INTERVAL = BUILDER
        .comment("Wall-clock minutes between samples of connected players. No machine scanning.")
        .defineInRange("intervalMinutes", 15, 1, 1440);
    private static final ModConfigSpec.BooleanValue LOGIN = BUILDER.define("logOnLogin", true);
    private static final ModConfigSpec.BooleanValue LOGOUT = BUILDER.define("logOnLogout", true);
    private static final ModConfigSpec SPEC = BUILDER.build();
    private static EmcAuditWriter writer;
    private static MinecraftServer currentServer;
    private static long nextSample;

    private ProjectEEmcAudit() { }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "packrules-emc-audit.toml");
        NeoForge.EVENT_BUS.addListener(ProjectEEmcAudit::started);
        NeoForge.EVENT_BUS.addListener(ProjectEEmcAudit::stopped);
        NeoForge.EVENT_BUS.addListener(ProjectEEmcAudit::login);
        NeoForge.EVENT_BUS.addListener(ProjectEEmcAudit::logout);
        NeoForge.EVENT_BUS.addListener(ProjectEEmcAudit::tick);
    }

    private static void started(ServerStartedEvent event) {
        currentServer = event.getServer();
        writer = null;
        nextSample = System.nanoTime() + TimeUnit.MINUTES.toNanos(INTERVAL.get());
    }

    private static void stopped(ServerStoppedEvent event) { currentServer = null; writer = null; }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (LOGIN.get() && event.getEntity() instanceof ServerPlayer player) sample(player, "ENTRADA");
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (LOGOUT.get() && event.getEntity() instanceof ServerPlayer player) sample(player, "SAIDA");
    }

    private static void tick(ServerTickEvent.Post event) {
        if (!ENABLED.get() || event.getServer() != currentServer || System.nanoTime() - nextSample < 0) return;
        nextSample = System.nanoTime() + TimeUnit.MINUTES.toNanos(INTERVAL.get());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) sample(player, "PERIODICO");
    }

    private static void sample(ServerPlayer player, String event) {
        if (!ENABLED.get() || player.server != currentServer) return;
        var knowledge = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
        if (knowledge == null) return;
        try {
            if (writer == null) writer = new EmcAuditWriter(FMLPaths.GAMEDIR.get().resolve("logs/emc"));
            writer.append(player.getUUID(), player.getGameProfile().getName(), knowledge.getEmc(), event, Instant.now());
        } catch (IOException | RuntimeException failure) {
            LOGGER.error("Could not append administrative EMC audit sample for {}", player.getUUID(), failure);
        }
    }
}
