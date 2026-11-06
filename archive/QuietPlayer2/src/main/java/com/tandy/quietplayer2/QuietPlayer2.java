package com.tandy.quietplayer2;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
@Mod("quietplayer2")
public final class QuietPlayer2 {
    private static final Set<String> NOTICES = Set.of(
        "message.playerengine.auth.authorize_prompt",
        "message.playerengine.auth.dedicated_hint");
    private static final ModConfigSpec.BooleanValue SUPPRESS;
    private static final ModConfigSpec SPEC;
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SUPPRESS = builder.comment("Hide Player2 authorization link and dedicated-server hint only. Does not disable authentication or NPC features.")
            .define("suppressAuthNotices", true);
        SPEC = builder.build();
    }
    public QuietPlayer2(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }
    public static boolean shouldSuppress(Component message) {
        return SUPPRESS.getAsBoolean()
            && message.getContents() instanceof TranslatableContents translated
            && NOTICES.contains(translated.getKey());
    }
}
