package com.tandy.trophyheads.mixin;

import com.tandy.trophyheads.TrophyHolder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

@Mixin(SkullBlockEntity.class)
public abstract class SkullBlockEntityMixin implements TrophyHolder {
    @Unique
    private ItemStack titlesystem$trophy = ItemStack.EMPTY;

    @Override
    public ItemStack titlesystem$getTrophy() {
        return this.titlesystem$trophy;
    }

    @Override
    public void titlesystem$setTrophy(ItemStack stack) {
        this.titlesystem$trophy = stack == null ? ItemStack.EMPTY : stack.copy();
        ((SkullBlockEntity) (Object) this).setChanged();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void titlesystem$save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        if (!this.titlesystem$trophy.isEmpty()) {
            tag.put("titlesystem:trophy", this.titlesystem$trophy.save(registries));
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void titlesystem$load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        if (tag.contains("titlesystem:trophy")) {
            this.titlesystem$trophy = ItemStack.parseOptional(registries, tag.getCompound("titlesystem:trophy"));
        }
    }
}
