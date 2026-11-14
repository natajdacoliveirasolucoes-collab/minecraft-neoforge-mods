package com.tandy.packrules.mixin;

import com.tandy.packrules.ProjectEPricing;
import moze_intel.projecte.api.ItemInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "moze_intel.projecte.emc.EMCMappingHandler", remap = false)
public abstract class ProjectEStoredValueMixin {
    @Inject(method = "getStoredEmcValue(Lmoze_intel/projecte/api/ItemInfo;)J", at = @At("RETURN"), cancellable = true, remap = false)
    private static void packrules$restoreBaseDuringConsumption(ItemInfo info, CallbackInfoReturnable<Long> callback) {
        callback.setReturnValue(ProjectEPricing.originalSaleBase(info, callback.getReturnValueJ()));
    }
}
