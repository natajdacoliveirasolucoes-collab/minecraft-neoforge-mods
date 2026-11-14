package com.tacz.guns.nata;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Keeps inventory-fed gun metadata consistent with the authoritative ammo supply.
 * Patch local por Natã Oliveira (nata). Does not consume ammo or bypass shot validation. */
public final class InventoryAmmoSync {
 public static void onServerTick(ServerTickEvent.Pre event){
  for(ServerPlayer player:event.getServer().getPlayerList().getPlayers())synchronize(player,player.getMainHandItem());
 }
 public static void synchronize(LivingEntity entity,ItemStack stack){
  if(!(entity instanceof ServerPlayer player))return;
  IGun gun=IGun.getIGunOrNull(stack);
  if(gun==null||!gun.useInventoryAmmo(stack))return;
  int count;
  if(!IGunOperator.fromLivingEntity(player).needCheckAmmo())count=9999;
  else if(gun.useDummyAmmo(stack))count=gun.getDummyAmmoAmount(stack);
  else {
   long total=0;
   var inventory=player.getInventory();
   for(int i=0;i<inventory.getContainerSize();i++){
    ItemStack ammo=inventory.getItem(i);
    if(ammo.getItem() instanceof IAmmo a&&a.isAmmoOfGun(stack,ammo))total+=ammo.getCount();
    if(ammo.getItem() instanceof IAmmoBox box&&box.isAmmoBoxOfGun(stack,ammo)){
     if(box.isAllTypeCreative(ammo)||box.isCreative(ammo)){total=9999;break;}
     total+=box.getAmmoCount(ammo);
    }
   }
   count=(int)Math.min(Integer.MAX_VALUE,Math.max(0,total));
  }
  if(gun.getCurrentAmmoCount(stack)!=count)gun.setCurrentAmmoCount(stack,count);
 }
}
