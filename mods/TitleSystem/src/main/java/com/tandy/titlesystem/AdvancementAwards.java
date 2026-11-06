package com.tandy.titlesystem;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementAwards {
    private static final int[] TIERS = {1, 25, 50, 100, 500, 1000};
    private static final String[] IDS = {
            "primeiro_sangue", "cacador_25", "cacador_50", "cacador_100", "cacador_500", "cacador_1000"
    };

    private AdvancementAwards() {}

    public static void awardTiers(ServerPlayer player) {
        int kills = TitleData.kills(player);
        for (int index = 0; index < TIERS.length; index++) {
            if (kills >= TIERS[index]) {
                award(player, IDS[index]);
            }
        }
    }

    private static void award(ServerPlayer player, String id) {
        ServerAdvancementManager manager = player.getServer().getAdvancements();
        AdvancementHolder holder = manager.get(ResourceLocation.fromNamespaceAndPath(TitleSystem.MODID, id));
        if (holder == null) {
            return;
        }
        if (!player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "kills");
        }
    }
}
