package com.nata.mekataczcompat.mixin;

import com.nata.mekataczcompat.MekaTaCZCompat;
import com.tacz.guns.api.item.IGun;
import mekanism.client.render.RenderTickHandler;
import net.minecraft.world.entity.EquipmentSlot;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderTickHandler.class, remap = false)
public abstract class MekaSuitArmMixin {
    @Inject(method = "renderArm(Lnet/neoforged/neoforge/client/event/RenderArmEvent;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void nata$preserveGunArmPose(RenderArmEvent event, CallbackInfo callback) {
        if (MekaTaCZCompat.isEnabled()
                && event.getPlayer().getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ItemMekaSuitArmor
                && IGun.mainHandHoldGun(event.getPlayer())) {
            MekaTaCZCompat.recordBypass();
            // Return from Mekanism's listener, not from the render event. TaCZ/vanilla
            // still renders the hand using the original event and its gun pose.
            callback.cancel();
        }
    }
}
