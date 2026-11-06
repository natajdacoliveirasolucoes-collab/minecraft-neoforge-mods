package com.tandy.titlesystem.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tandy.titlesystem.client.ClientTitles;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(
            method = "renderNameTag",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = At.Shift.BEFORE))
    private void titlesystem$renderTitle(Entity entity, Component displayName, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, float partialTick, CallbackInfo callback) {
        ClientTitles.render(entity, poseStack, bufferSource, packedLight);
    }
}
