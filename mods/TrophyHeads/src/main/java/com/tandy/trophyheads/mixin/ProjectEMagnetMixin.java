package com.tandy.trophyheads.mixin;

import com.tandy.trophyheads.TrophyDelivery;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** ProjectE's Black Hole Band does not observe pickup ownership or PreventRemoteMovement. */
@Pseudo
@Mixin(targets = "moze_intel.projecte.gameObjs.items.rings.BlackHoleBand", remap = false)
public abstract class ProjectEMagnetMixin {
    @Redirect(method = {"inventoryTick", "updateInAlchBag"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private <T extends Entity> List<T> trophyheads$playerMagnet(Level level, Class<T> type, AABB box) {
        return TrophyDelivery.withoutProtected(level.getEntitiesOfClass(type, box));
    }

    @Redirect(method = {"updateInPedestal", "updateInAlchChest"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <T extends Entity> List<T> trophyheads$machineMagnet(Level level, Class<T> type, AABB box, Predicate<? super T> predicate) {
        return TrophyDelivery.withoutProtected(level.getEntitiesOfClass(type, box, predicate));
    }
}
