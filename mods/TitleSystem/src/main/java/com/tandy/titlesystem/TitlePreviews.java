package com.tandy.titlesystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Temporary visual overrides only: never writes player progression or equipped-title data. */
@EventBusSubscriber(modid = TitleSystem.MODID)
public final class TitlePreviews {
    private record Preview(MinecraftServer server, Title title, long expiresAt) {}
    private static final Map<UUID, Preview> ACTIVE = new HashMap<>();

    private TitlePreviews() {}

    public static void start(ServerPlayer player, Title title, int seconds) {
        ACTIVE.put(player.getUUID(), new Preview(player.getServer(), title,
                System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds)));
        TitleNetwork.broadcast(player.getServer(), player.getUUID(), Optional.of(title.display()));
    }

    public static Title current(ServerPlayer player) {
        Preview preview = ACTIVE.get(player.getUUID());
        return preview != null && preview.server() == player.getServer()
                && System.nanoTime() < preview.expiresAt() ? preview.title() : null;
    }

    public static long remainingSeconds(ServerPlayer player) {
        Preview preview = ACTIVE.get(player.getUUID());
        return current(player) == null ? 0 : Math.max(1,
                (preview.expiresAt() - System.nanoTime() + 999_999_999L) / 1_000_000_000L);
    }

    public static boolean stop(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) == null) return false;
        restore(player);
        return true;
    }

    public static void discard(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    private static void restore(ServerPlayer player) {
        Title title = KillTracker.effectiveTitle(player);
        TitleNetwork.broadcast(player.getServer(), player.getUUID(),
                title == null ? Optional.empty() : Optional.of(title.display()));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        // Remove first, then restore: the real title must be evaluated without the preview.
        var expired = ACTIVE.entrySet().stream()
                .filter(entry -> entry.getValue().server() == event.getServer()
                        && System.nanoTime() >= entry.getValue().expiresAt())
                .map(Map.Entry::getKey).toList();
        for (UUID id : expired) {
            ACTIVE.remove(id);
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            if (player != null) restore(player);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        ACTIVE.values().removeIf(preview -> preview.server() == event.getServer());
    }
}
