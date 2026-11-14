package com.tandy.packrules.mixin;

import com.tandy.packrules.PackRules;
import mobchampions.config.ConfigHandler;
import mobchampions.event.MobChampionEventHandler;
import mobchampions.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Thin native rank lottery only, before attributes, equipment and announcements are assigned. */
@Pseudo
@Mixin(targets = "mobchampions.event.MobChampionEventHandler", remap = false)
public abstract class ChampionSpawnMixin {
    @Inject(method = "addRandomChampion", at = @At("HEAD"), cancellable = true, remap = false)
    private static void packrules$limitNewChampions(LivingEntity entity, CallbackInfo callback) {
        if (entity.level().isClientSide() || entity.getPersistentData().getBoolean("packrules_champion_demo")) return;
        double chance = PackRules.championRollChance();
        if (chance >= 1.0) return;
        if (!ConfigHandler.Common.getChampionWhitelist().contains(
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) return;
        if (ConfigHandler.Common.isBabyChampionsDisabled() && entity.isBaby()) return;
        var data = Services.PLATFORM.getMobChampionData(entity);
        if (data.isEmpty() || data.get().getEntityId() != -1) return;
        if (entity.getRandom().nextDouble() >= chance) {
            MobChampionEventHandler.addNormalMob(entity);
            callback.cancel();
        }
    }
}
