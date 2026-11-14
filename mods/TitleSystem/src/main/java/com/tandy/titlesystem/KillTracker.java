package com.tandy.titlesystem;

import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = TitleSystem.MODID)
public final class KillTracker {
    private KillTracker() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // The new entity is created on death and on returning from the End. Copy even if
        // titles are temporarily disabled, so disabling/re-enabling cannot erase progress.
        if (event.getEntity() instanceof ServerPlayer) {
            TitleData.copyProgress(event.getOriginal(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (Config.ENABLED.get() && event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!Config.ENABLED.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim) {
            return;
        }

        try {
            boolean collectorBefore = TitleData.capturedAll(killer);
            TitleData.addKill(killer, victim);
            boolean collectorAfter = TitleData.capturedAll(killer);

            AdvancementAwards.awardTiers(killer);
            refresh(killer);

            if (!collectorBefore && collectorAfter) {
                notifyUnlock(killer, "colecionador");
            }
        } catch (Exception exception) {
            TitleSystem.LOGGER.error("Failed to process a PvP kill for titles", exception);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.ENABLED.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        try {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            // The known-player set may have grown; re-evaluate and sync everyone.
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                refresh(online);
            }
        } catch (Exception exception) {
            TitleSystem.LOGGER.error("Failed to refresh titles on login", exception);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!Config.ENABLED.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TitlePreviews.discard(player);
        try {
            TitleNetwork.broadcast(player.getServer(), player.getUUID(), Optional.empty());
        } catch (Exception exception) {
            TitleSystem.LOGGER.error("Failed to clear a title on logout", exception);
        }
    }

    public static void refresh(ServerPlayer player) {
        int kills = TitleData.kills(player);
        boolean collector = TitleData.capturedAll(player);

        String equippedId = TitleData.equipped(player);
        Title equipped = Titles.byId(equippedId).orElse(null);
        boolean valid = equipped != null && Titles.isUnlocked(equipped, kills, collector);

        if (!valid) {
            Title best = Titles.highest(kills, collector).orElse(null);
            String newId = best == null ? "" : best.id();
            TitleData.setEquipped(player, newId);

            if (equipped != null && equipped.collector() && Config.ANNOUNCE_CHANGES.get()) {
                player.sendSystemMessage(prefix()
                        .append(Component.literal("Título ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("COLECIONADOR DE CABEÇAS")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                        .append(Component.literal(" revogado: há jogadores que você ainda não derrotou.")
                                .withStyle(ChatFormatting.GRAY)));
            }
        }

        Title title = effectiveTitle(player);
        TitleNetwork.broadcast(player.getServer(), player.getUUID(),
                title == null ? Optional.empty() : Optional.of(title.display()));
    }

    public static Title effectiveTitle(ServerPlayer player) {
        Title preview = TitlePreviews.current(player);
        if (preview != null) {
            return preview;
        }
        if (TitleData.hidden(player)) {
            return null;
        }
        String id = TitleData.equipped(player);
        Title title = Titles.byId(id).orElse(null);
        if (title == null || !Titles.isUnlocked(title, TitleData.kills(player), TitleData.capturedAll(player))) {
            return null;
        }
        return title;
    }

    private static void notifyUnlock(ServerPlayer player, String id) {
        if (!Config.ANNOUNCE_CHANGES.get()) {
            return;
        }
        Titles.byId(id).ifPresent(title -> player.sendSystemMessage(prefix()
                .append(Component.literal("Você desbloqueou o título ").withStyle(ChatFormatting.GRAY))
                .append(title.display())
                .append(Component.literal("! Use /titulos para ver e /titulos " + id + " para equipar.")
                        .withStyle(ChatFormatting.GRAY))));
    }

    private static MutableComponent prefix() {
        return Component.literal("[Títulos] ").withStyle(ChatFormatting.DARK_AQUA);
    }
}
