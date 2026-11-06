package com.tandy.titlesystem;

import java.util.Optional;
import java.util.UUID;

import com.tandy.titlesystem.client.ClientTitles;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public final class TitleNetwork {
    public static final CustomPacketPayload.Type<TitleSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TitleSystem.MODID, "title_sync"));

    private TitleNetwork() {}

    public record TitleSyncPayload(UUID player, Optional<Component> display) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, TitleSyncPayload> STREAM_CODEC = StreamCodec
                .composite(
                        UUIDUtil.STREAM_CODEC, TitleSyncPayload::player,
                        ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC, TitleSyncPayload::display,
                        TitleSyncPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = TitleSystem.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("2").optional().playToClient(TYPE, TitleSyncPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> ClientTitles.accept(payload)));
        }
    }

    public static void send(ServerPlayer target, UUID subject, Optional<Component> display) {
        if (!NetworkRegistry.hasChannel(target.connection.getConnection(), ConnectionProtocol.PLAY, TYPE.id())) {
            return;
        }
        PacketDistributor.sendToPlayer(target, new TitleSyncPayload(subject, display));
    }

    public static void broadcast(MinecraftServer server, UUID subject, Optional<Component> display) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, subject, display);
        }
    }
}
