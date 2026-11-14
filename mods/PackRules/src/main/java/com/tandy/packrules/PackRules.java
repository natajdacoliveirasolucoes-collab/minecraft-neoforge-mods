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
    private static final ModConfigSpec.DoubleValue CHAMPION_ROLL_CHANCE = BUILDER
        .comment("Fraction of eligible new mobs allowed to run the native MobChampions rank lottery.",
            "Rejected rolls become ordinary mobs; mobs are not removed. 1.0 preserves native frequency.",
            "Previously initialized champions retain their rank and equipment.")
        .defineInRange("championRollChance", 1.0, 0.0, 1.0);
    private static final ModConfigSpec.Builder PRICING_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue PROJECTE_PURCHASE_MULTIPLIER = PRICING_BUILDER
        .comment("Purchase-price factor already applied to the selected namespaces in ProjectE's saved EMC table.",
            "Restore their original base values only during item consumption, including nested stored items.",
            "Use 1 to disable. Keep this synchronized with pregenerated_emc.json; this option does not inflate the table.")
        .defineInRange("projectePurchaseMultiplier", 10, 1, 1000);
    private static final ModConfigSpec.BooleanValue PROJECTE_GLOBAL_PURCHASE_PRICING = PRICING_BUILDER
        .comment("Apply the purchase-price factor to all mapped items instead of only the original selected mods.",
            "The saved EMC table must already contain that factor. Consumption restores the original base value.")
        .define("projecteGlobalPurchasePricing", false);
    private static final ModConfigSpec SPEC = BUILDER.build();
    private static final ModConfigSpec PRICING_SPEC = PRICING_BUILDER.build();

    public static int projectePurchaseMultiplier() {
        return PROJECTE_PURCHASE_MULTIPLIER.get();
    }

    public static boolean projecteGlobalPurchasePricing() {
        return PROJECTE_GLOBAL_PURCHASE_PRICING.get();
    }

    public static double championRollChance() {
        return CHAMPION_ROLL_CHANCE.get();
    }

    public PackRules(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
        container.registerConfig(ModConfig.Type.COMMON, PRICING_SPEC, "packrules-projecte.toml");
        NeoForge.EVENT_BUS.addListener(this::joining);
        if (ModList.get().isLoaded("projecte")) ProjectEEmcAudit.register(container);
        if (ModList.get().isLoaded("mobchampions"))
            NeoForge.EVENT_BUS.addListener(ChampionDemoCommand::register);
    }
    private void joining(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && BLOCKED.get().contains(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString()))
            event.setCanceled(true);
    }
}
