package com.tandy.trophyheads;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Delivery ownership belongs to the dropped entity, never to the collectible item itself. */
@EventBusSubscriber(modid = TrophyHeads.MODID)
public final class TrophyDelivery {
    private static final String RECIPIENT = "examplemod:trophy_recipient";

    private TrophyDelivery() {}

    public static void spawn(ServerPlayer killer, ItemStack head) {
        ItemEntity drop = new ItemEntity(killer.level(), killer.getX(), killer.getY() + 0.25,
                killer.getZ(), head, 0, 0, 0);
        drop.setTarget(killer.getUUID());
        drop.setNoPickUpDelay();
        drop.getPersistentData().putUUID(RECIPIENT, killer.getUUID());
        // Shared magnet convention used by Sophisticated Core and Draconic Evolution.
        drop.getPersistentData().putBoolean("PreventRemoteMovement", true);
        killer.level().addFreshEntity(drop);
    }

    public static boolean isProtected(Entity entity) {
        return entity instanceof ItemEntity && entity.getPersistentData().hasUUID(RECIPIENT);
    }

    public static <T extends Entity> List<T> withoutProtected(List<T> entities) {
        return entities.stream().filter(entity -> !isProtected(entity)).toList();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity drop = event.getItemEntity();
        if (isProtected(drop) && !drop.getPersistentData().getUUID(RECIPIENT).equals(event.getPlayer().getUUID())) {
            event.setCanPickup(TriState.FALSE);
        }
    }
}
