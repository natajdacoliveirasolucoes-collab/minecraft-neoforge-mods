package com.tandy.titlesystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public final class TitleData {
    private static final String KILLS = "titlesystem_pvp_kills";
    private static final String CAPTURED = "titlesystem_captured";
    private static final String EQUIPPED = "titlesystem_equipped";
    private static final String HIDDEN = "titlesystem_hidden";

    private TitleData() {}

    public static int kills(ServerPlayer player) {
        return player.getPersistentData().getInt(KILLS);
    }

    public static void setKills(ServerPlayer player, int value) {
        player.getPersistentData().putInt(KILLS, Math.max(0, value));
    }

    public static void addKill(ServerPlayer killer, ServerPlayer victim) {
        CompoundTag data = killer.getPersistentData();
        data.putInt(KILLS, data.getInt(KILLS) + 1);

        CompoundTag captured = data.getCompound(CAPTURED);
        captured.putString(victim.getUUID().toString(), victim.getName().getString());
        data.put(CAPTURED, captured);
    }

    public static CompoundTag captured(ServerPlayer player) {
        return player.getPersistentData().getCompound(CAPTURED);
    }

    public static boolean capturedAll(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        Set<UUID> known = knownPlayers(server);
        known.remove(player.getUUID());
        if (known.isEmpty()) {
            return false;
        }
        CompoundTag captured = captured(player);
        for (UUID uuid : known) {
            if (!captured.contains(uuid.toString())) {
                return false;
            }
        }
        return true;
    }

    public static String equipped(ServerPlayer player) {
        return player.getPersistentData().getString(EQUIPPED);
    }

    public static void setEquipped(ServerPlayer player, String id) {
        player.getPersistentData().putString(EQUIPPED, id == null ? "" : id);
    }

    public static boolean hidden(ServerPlayer player) {
        return player.getPersistentData().getBoolean(HIDDEN);
    }

    public static void setHidden(ServerPlayer player, boolean hidden) {
        player.getPersistentData().putBoolean(HIDDEN, hidden);
    }

    public static Set<UUID> knownPlayers(MinecraftServer server) {
        Set<UUID> result = new HashSet<>();

        // Only players who actually played on this server (have a playerdata file).
        // This avoids counting profiles that were merely cached (e.g. by /give).
        try {
            Path playerData = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            if (Files.isDirectory(playerData)) {
                try (var stream = Files.list(playerData)) {
                    stream.forEach(path -> {
                        String name = path.getFileName().toString();
                        if (!name.endsWith(".dat")) {
                            return;
                        }
                        try {
                            result.add(UUID.fromString(name.substring(0, name.length() - 4)));
                        } catch (Exception ignored) {
                            // not a uuid file, skip
                        }
                    });
                }
            }
        } catch (Exception exception) {
            TitleSystem.LOGGER.warn("Could not read the playerdata directory", exception);
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            result.add(player.getUUID());
        }
        return result;
    }
}
