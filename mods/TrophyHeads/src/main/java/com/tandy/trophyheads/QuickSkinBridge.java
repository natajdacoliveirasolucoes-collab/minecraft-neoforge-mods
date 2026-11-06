package com.tandy.trophyheads;

import java.lang.reflect.Method;
import java.util.UUID;
import net.neoforged.fml.ModList;

/** Optional integration: no Quick Skin classes are linked when the mod is absent. */
final class QuickSkinBridge {
    private static Method repositoryInstance, appearance, skinId, cacheInstance, texture;
    private static boolean attempted;
    private QuickSkinBridge() {}

    static byte[] skin(UUID player) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("quickskin")) return null;
        if (!attempted) {
            Class<?> repository = Class.forName("com.quickskin.mod.server.data.ServerPlayerAppearanceRepository");
            Class<?> data = Class.forName("com.quickskin.mod.common.data.PlayerAppearance");
            Class<?> cache = Class.forName("com.quickskin.mod.server.storage.ServerTextureCache");
            repositoryInstance = repository.getMethod("getInstance");
            appearance = repository.getMethod("getAppearance", UUID.class);
            skinId = data.getMethod("getSkinId");
            cacheInstance = cache.getMethod("getInstance");
            texture = cache.getMethod("getTexture", String.class);
            attempted = true;
        }
        Object look = appearance.invoke(repositoryInstance.invoke(null), player);
        if (look == null) return null;
        String skin = (String)skinId.invoke(look);
        if (skin == null || skin.isEmpty()) return null;
        if (!skin.startsWith("local_skin:")) {
            TrophyHeads.LOGGER.warn("Quick Skin appearance '{}' for {} has no uploaded PNG; retaining vanilla profile", skin, player);
            return null;
        }
        byte[] bytes = (byte[])texture.invoke(cacheInstance.invoke(null), skin.substring("local_skin:".length()));
        if (bytes == null) throw new IllegalStateException("Active Quick Skin PNG is missing for " + player);
        return bytes;
    }
}
