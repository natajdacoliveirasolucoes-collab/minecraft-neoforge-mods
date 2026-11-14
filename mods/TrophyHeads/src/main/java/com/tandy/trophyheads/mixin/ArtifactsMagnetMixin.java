package com.tandy.trophyheads.mixin;

import com.tandy.trophyheads.TrophyDelivery;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "artifacts.effect.MagnetismMobEffect", remap = false)
public abstract class ArtifactsMagnetMixin {
    @Redirect(method = "applyEffectTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private <T extends Entity> List<T> trophyheads$magnetism(Level level, Class<T> type, AABB box) {
        return TrophyDelivery.withoutProtected(level.getEntitiesOfClass(type, box));
    }
}
