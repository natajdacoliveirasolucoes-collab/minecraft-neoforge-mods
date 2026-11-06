package com.tandy.packrules;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Native config handles the other rules; these two Born mobs have unavoidable hand-item theft. */
@Mod("packrules")
public final class PackRules {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED = BUILDER
        .comment("Entity types excluded from this pack, including spawn eggs, summons and saved entities.",
            "Krampus and its henchman steal the target's held items with no native theft toggle.",
            "Only explicitly listed types are affected. Existing unrelated entities are untouched.")
        .defineListAllowEmpty("blockedEntityTypes", List.of("born_in_chaos_v1:krampus", "born_in_chaos_v1:krampus_henchman"),
            () -> "", value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
    private static final ModConfigSpec SPEC = BUILDER.build();

    public PackRules(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
        NeoForge.EVENT_BUS.addListener(this::joining);
        if (ModList.get().isLoaded("mobchampions"))
            NeoForge.EVENT_BUS.addListener(ChampionDemoCommand::register);
    }
    private void joining(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && BLOCKED.get().contains(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString()))
            event.setCanceled(true);
    }
}
