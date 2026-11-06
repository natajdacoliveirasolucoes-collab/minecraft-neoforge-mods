package com.tandy.trophyheads.mixin.client;

import com.tandy.trophyheads.client.TrophyTextures;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private static void trophyheads$snapshot(SkullBlock.Type type, ResolvableProfile profile,
            CallbackInfoReturnable<RenderType> callback) {
        if (type == SkullBlock.Types.PLAYER && profile != null) {
            callback.setReturnValue(RenderType.entityTranslucent(TrophyTextures.texture(profile.gameProfile())));
        }
    }
}
