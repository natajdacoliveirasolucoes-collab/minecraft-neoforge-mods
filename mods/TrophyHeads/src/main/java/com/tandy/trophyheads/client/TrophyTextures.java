package com.tandy.trophyheads.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.tandy.trophyheads.SkinSnapshot;
import com.tandy.trophyheads.TrophyHeads;
import com.tandy.trophyheads.mixin.client.SkinManagerInvoker;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** All calls run on the render thread. No Quick Skin lookup or mutable player UUID cache. */
public final class TrophyTextures {
    private static final int MAX_TEXTURES = 64;
    private static final Map<String, ResourceLocation> PNGS = new LinkedHashMap<>(16, .75f, true);
    private static final Map<String, CompletableFuture<PlayerSkin>> PROFILES = new LinkedHashMap<>(16, .75f, true);
    private TrophyTextures() {}

    public static ResourceLocation texture(GameProfile profile) {
        String key = SkinSnapshot.key(profile);
        if (key != null) {
            ResourceLocation cached = PNGS.get(key);
            if (cached != null) return cached;
            byte[] bytes = SkinSnapshot.png(profile);
            if (bytes != null) {
                try {
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath("examplemod", "trophy_skin/" + key);
                    Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
                    PNGS.put(key, location);
                    if (PNGS.size() > MAX_TEXTURES) {
                        String eldest = PNGS.keySet().iterator().next();
                        Minecraft.getInstance().getTextureManager().release(PNGS.remove(eldest));
                    }
                    return location;
                } catch (Exception invalid) {
                    TrophyHeads.LOGGER.warn("Invalid trophy snapshot {}; using saved vanilla texture", key, invalid);
                }
            }
        }
        // Older heads keep their signed vanilla texture even if Quick Skin now overrides
        // that player's live look. Invoke vanilla's registration path, bypassing its mixins.
        var textures = profile.getProperties().get("textures");
        if (textures.isEmpty()) return DefaultPlayerSkin.get(profile).texture();
        var property = textures.iterator().next();
        String legacyKey = SkinSnapshot.digest((profile.getId() + ":" + property.value()
                + ":" + property.signature()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var future = PROFILES.get(legacyKey);
        if (future == null) {
            try {
                var minecraft = Minecraft.getInstance();
                var unpacked = minecraft.getMinecraftSessionService().unpackTextures(property);
                future = ((SkinManagerInvoker)minecraft.getSkinManager()).trophyheads$registerTextures(profile.getId(), unpacked);
            } catch (Exception invalid) {
                future = CompletableFuture.completedFuture(DefaultPlayerSkin.get(profile));
            }
            PROFILES.put(legacyKey, future);
            if (PROFILES.size() > MAX_TEXTURES) PROFILES.remove(PROFILES.keySet().iterator().next());
        }
        PlayerSkin fallback = DefaultPlayerSkin.get(profile);
        if (future.isCompletedExceptionally() || future.isCancelled()) return fallback.texture();
        return future.getNow(fallback).texture();
    }
}
