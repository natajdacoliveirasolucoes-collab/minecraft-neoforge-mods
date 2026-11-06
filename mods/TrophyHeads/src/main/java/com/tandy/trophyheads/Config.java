package com.tandy.trophyheads;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DROP_HEAD_ONLY_ON_PVP = BUILDER
            .comment("If true, player heads only drop when the player is killed by another player (PvP).",
                    "If false, they drop on any death.")
            .define("dropHeadOnlyOnPvp", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
