package com.tandy.titlesystem.client;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

import com.tandy.titlesystem.ClientConfig;
import com.tandy.titlesystem.TitleNetwork;
import com.tandy.titlesystem.TitleSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = TitleSystem.MODID, value = Dist.CLIENT)
public final class ClientTitles {
    private static final Map<UUID, Component> TITLES = new ConcurrentHashMap<>();

    private ClientTitles() {}

    public static void accept(TitleNetwork.TitleSyncPayload payload) {
        payload.display().ifPresentOrElse(
                display -> TITLES.put(payload.player(), display),
                () -> TITLES.remove(payload.player()));
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TITLES.clear();
    }

    private static Optional<Component> titleFor(UUID uuid) {
        return Optional.ofNullable(TITLES.get(uuid));
    }

    public static void render(Entity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        try {
            if (!ClientConfig.SHOW_TITLES.get()) {
                return;
            }
            Optional<Component> maybeTitle = titleFor(entity.getUUID());
            if (maybeTitle.isEmpty()) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            Font font = minecraft.font;
            Component text = maybeTitle.get();
            float x = -font.width(text) / 2.0F;
            float y = font.lineHeight + 2.0F;
            Matrix4f matrix = poseStack.last().pose();
            boolean seeThrough = !entity.isDiscrete();
            float opacity = minecraft.options.getBackgroundOpacity(0.25F);
            int background = (int) (opacity * 255.0F) << 24;

            font.drawInBatch(text, x, y, 553648127, false, matrix, bufferSource,
                    seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, background, packedLight);
            if (seeThrough) {
                font.drawInBatch(text, x, y, -1, false, matrix, bufferSource,
                        Font.DisplayMode.NORMAL, 0, packedLight);
            }
        } catch (Exception exception) {
            TitleSystem.LOGGER.error("Failed to render a player title", exception);
        }
    }
}
