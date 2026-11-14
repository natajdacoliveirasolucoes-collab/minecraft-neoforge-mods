package com.tandy.packrules;

import java.util.Set;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.core.registries.BuiltInRegistries;

/** Keeps the server's native component processing intact while distinguishing input from output. */
public final class ProjectEPricing {
    private static final Set<String> NAMESPACES = Set.of(
        "ae2", "mekanism", "mekanismadditions", "mekanismgenerators", "mekanismtools", "modernfoundry", "ceramics");
    private static final ThreadLocal<Integer> SALE_DEPTH = new ThreadLocal<>();

    private ProjectEPricing() { }

    public static void enterSale() {
        Integer depth = SALE_DEPTH.get();
        SALE_DEPTH.set(depth == null ? 1 : depth + 1);
    }

    public static void exitSale() {
        Integer depth = SALE_DEPTH.get();
        if (depth == null || depth <= 1) SALE_DEPTH.remove();
        else SALE_DEPTH.set(depth - 1);
    }

    public static long originalSaleBase(ItemInfo info, long value) {
        Integer depth = SALE_DEPTH.get();
        if (depth == null || depth == 0 || value <= 0) return value;
        int factor = PackRules.projectePurchaseMultiplier();
        if (factor <= 1 || (!PackRules.projecteGlobalPurchasePricing()
                && !NAMESPACES.contains(BuiltInRegistries.ITEM.getKey(info.getItem().value()).getNamespace()))) return value;
        return value / factor;
    }
}
