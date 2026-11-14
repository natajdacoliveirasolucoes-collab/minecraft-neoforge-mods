package com.tandy.packrules.mixin;

import com.tandy.packrules.ProjectEPricing;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.impl.EMCProxyImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "moze_intel.projecte.impl.EMCProxyImpl", remap = false)
public abstract class ProjectESaleContextMixin {
    @Redirect(method = "getSellValue(Lmoze_intel/projecte/api/ItemInfo;)J",
        at = @At(value = "INVOKE", target = "Lmoze_intel/projecte/impl/EMCProxyImpl;getValue(Lmoze_intel/projecte/api/ItemInfo;)J", remap = false),
        remap = false)
    private long packrules$originalConsumptionValue(EMCProxyImpl proxy, ItemInfo info) {
        ProjectEPricing.enterSale();
        try {
            return proxy.getValue(info);
        } finally {
            ProjectEPricing.exitSale();
        }
    }
}
