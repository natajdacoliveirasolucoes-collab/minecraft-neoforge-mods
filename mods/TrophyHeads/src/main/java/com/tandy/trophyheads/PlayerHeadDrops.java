package com.tandy.trophyheads;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.mojang.authlib.GameProfile;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = TrophyHeads.MODID)
public final class PlayerHeadDrops {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private PlayerHeadDrops() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        try {
            Entity attacker = event.getSource().getEntity();
            boolean pvpKill = attacker instanceof ServerPlayer;

            if (Config.DROP_HEAD_ONLY_ON_PVP.getAsBoolean() && !pvpKill) {
                TrophyHeads.LOGGER.debug(
                        "Not dropping a head for {}: no player killer and dropHeadOnlyOnPvp=true",
                        victim.getName().getString());
                return;
            }

            // GameProfile and PropertyMap are mutable. SkinRestorer updates the live map in place;
            // retain an independent snapshot so existing items/placed skulls keep the captured skin.
            // Property values themselves are immutable, including the original texture signature.
            GameProfile liveProfile = victim.getGameProfile();
            GameProfile profile;
            try {
                profile = SkinSnapshot.capture(liveProfile, QuickSkinBridge.skin(victim.getUUID()));
            } catch (Exception snapshotError) {
                TrophyHeads.LOGGER.error("Cannot snapshot Quick Skin texture for {}; retaining vanilla trophy profile",
                        liveProfile.getName(), snapshotError);
                profile = SkinSnapshot.copy(liveProfile);
            }
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(profile));

            String headName = "Cabeça de " + profile.getName();
            MutableComponent name = pvpKill
                    ? Component.literal("[PvP] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                            .append(Component.literal(headName)
                                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                    : Component.literal(headName)
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD);

            head.set(DataComponents.CUSTOM_NAME, name);
            head.set(DataComponents.RARITY, pvpKill ? Rarity.EPIC : Rarity.COMMON);
            if (pvpKill) {
                head.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            }

            BlockPos pos = victim.blockPosition();
            String location = dimensionName(victim) + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            String when = LocalDateTime.now().format(TIMESTAMP);

            List<Component> lore = new ArrayList<>();
            if (pvpKill) {
                ServerPlayer killer = (ServerPlayer) attacker;
                MutableComponent weapon = killer.getMainHandItem().isEmpty()
                        ? Component.literal("Punhos").withStyle(ChatFormatting.WHITE)
                        : killer.getMainHandItem().getHoverName().copy().withStyle(ChatFormatting.WHITE);

                lore.add(Component.literal("Troféu de um inimigo caído")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD, ChatFormatting.ITALIC));
                lore.add(line("Morto por: ", Component.literal(killer.getName().getString())
                        .withStyle(ChatFormatting.WHITE)));
                lore.add(line("Arma: ", weapon));
                lore.add(line("Vida do assassino: ", Component.literal(String.format("%.1f", killer.getHealth()))
                        .withStyle(ChatFormatting.RED)));
                lore.add(line("Local: ", Component.literal(location).withStyle(ChatFormatting.GRAY)));
                lore.add(line("Capturado em: ", Component.literal(when).withStyle(ChatFormatting.GRAY)));
            } else {
                lore.add(Component.literal("Caído em batalha")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                lore.add(line("Causa: ", describeCause(event.getSource()).copy()
                        .withStyle(ChatFormatting.WHITE)));
                lore.add(line("Local: ", Component.literal(location).withStyle(ChatFormatting.GRAY)));
                lore.add(line("Coletado em: ", Component.literal(when).withStyle(ChatFormatting.GRAY)));
            }
            head.set(DataComponents.LORE, new ItemLore(lore));

            event.getDrops().add(new ItemEntity(
                    victim.level(),
                    victim.getX(), victim.getY(0.5), victim.getZ(),
                    head));

            TrophyHeads.LOGGER.debug("Dropped {}'s head at {} (pvp={})",
                    profile.getName(), victim.position(), pvpKill);
        } catch (Exception exception) {
            TrophyHeads.LOGGER.error("Failed to create the player head drop for {}",
                    victim.getName().getString(), exception);
        }
    }

    private static MutableComponent line(String label, Component value) {
        return Component.literal(label).withStyle(ChatFormatting.GRAY).append(value);
    }

    private static String dimensionName(ServerPlayer player) {
        ResourceLocation dimension = player.level().dimension().location();
        if (!dimension.getNamespace().equals("minecraft")) {
            return dimension.toString();
        }
        return switch (dimension.getPath()) {
            case "overworld" -> "Superfície";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> dimension.getPath();
        };
    }

    private static Component describeCause(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && !(attacker instanceof Player)) {
            return living.getType().getDescription();
        }

        return switch (source.getMsgId()) {
            case "fall" -> Component.literal("Dano de queda");
            case "lava" -> Component.literal("Lava");
            case "inFire", "onFire" -> Component.literal("Fogo");
            case "drown" -> Component.literal("Afogamento");
            case "starve" -> Component.literal("Fome");
            case "explosion", "explosion.player" -> Component.literal("Explosão");
            case "magic" -> Component.literal("Magia");
            case "wither" -> Component.literal("Wither");
            case "outOfWorld" -> Component.literal("Vazio");
            case "genericKill" -> Component.literal("Comando (/kill)");
            case "mob" -> Component.literal("Mob");
            default -> {
                String id = source.getMsgId();
                yield Component.literal(id.isEmpty() ? "Desconhecido"
                        : Character.toUpperCase(id.charAt(0)) + id.substring(1));
            }
        };
    }
}
