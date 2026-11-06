package com.tandy.trophyheads.mixin;

import java.util.List;

import com.tandy.trophyheads.TrophyHolder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    private void titlesystem$storeTrophy(Level level, BlockPos pos, BlockState state, LivingEntity placer,
            ItemStack stack, CallbackInfo callback) {
        if (stack.getItem() instanceof PlayerHeadItem && level.getBlockEntity(pos) instanceof TrophyHolder holder) {
            holder.titlesystem$setTrophy(stack.copy());
        }
    }

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void titlesystem$dropTrophy4(BlockState state, ServerLevel level, BlockPos pos,
            BlockEntity blockEntity, CallbackInfoReturnable<List<ItemStack>> callback) {
        titlesystem$replaceDrop(callback, blockEntity);
    }

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void titlesystem$dropTrophy6(BlockState state, ServerLevel level, BlockPos pos,
            BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> callback) {
        titlesystem$replaceDrop(callback, blockEntity);
    }

    @Unique
    private static void titlesystem$replaceDrop(CallbackInfoReturnable<List<ItemStack>> callback, BlockEntity blockEntity) {
        if (blockEntity instanceof TrophyHolder holder) {
            ItemStack trophy = holder.titlesystem$getTrophy();
            if (trophy != null && !trophy.isEmpty()) {
                callback.setReturnValue(List.of(trophy.copy()));
            }
        }
    }
}
