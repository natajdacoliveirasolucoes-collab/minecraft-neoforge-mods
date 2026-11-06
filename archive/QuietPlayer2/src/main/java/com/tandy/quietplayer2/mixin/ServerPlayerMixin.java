package com.tandy.quietplayer2.mixin;
import com.tandy.quietplayer2.QuietPlayer2;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void quietPlayer2$filterNotice(Component message, boolean overlay, CallbackInfo ci) {
        if (QuietPlayer2.shouldSuppress(message)) ci.cancel();
    }
}
