package com.tandy.titlesystem;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_TITLES = BUILDER
            .comment("Show player titles under their nametags on your client.")
            .define("showTitles", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {}
}
