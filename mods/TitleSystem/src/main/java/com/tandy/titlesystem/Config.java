package com.tandy.titlesystem;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable the title/kill-count system on this server.")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue ANNOUNCE_CHANGES = BUILDER
            .comment("Send a chat message when a title is unlocked or revoked.")
            .define("announceTitleChanges", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
