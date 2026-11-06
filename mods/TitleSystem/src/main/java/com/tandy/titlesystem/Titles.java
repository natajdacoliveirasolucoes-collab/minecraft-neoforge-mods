package com.tandy.titlesystem;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class Titles {
    private static final int[] RAINBOW = {
            0xFF0000, 0xFF7F00, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x007FFF, 0x8B00FF
    };

    public static final List<Title> ALL = List.of(
            new Title("colecionador", rainbow("COLECIONADOR DE CABEÇAS"), 100, Integer.MAX_VALUE, true));

    private Titles() {}

    public static Optional<Title> byId(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return ALL.stream().filter(title -> title.id().equals(id)).findFirst();
    }

    public static boolean isUnlocked(Title title, int kills, boolean collectorComplete) {
        return title.collector() ? collectorComplete : kills >= title.killsRequired();
    }

    public static List<Title> unlocked(int kills, boolean collectorComplete) {
        return ALL.stream().filter(title -> isUnlocked(title, kills, collectorComplete)).toList();
    }

    public static Optional<Title> highest(int kills, boolean collectorComplete) {
        return unlocked(kills, collectorComplete).stream()
                .max(Comparator.comparingInt(Title::priority));
    }

    private static Component rainbow(String text) {
        MutableComponent result = Component.empty();
        for (int index = 0; index < text.length(); index++) {
            result.append(Component.literal(String.valueOf(text.charAt(index)))
                    .withStyle(Style.EMPTY
                            .withColor(TextColor.fromRgb(RAINBOW[index % RAINBOW.length]))
                            .withBold(true)));
        }
        return result;
    }
}
