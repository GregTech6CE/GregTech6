package com.gregtech.gregtech.mixin;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla bucket fallback for cover sampling when an item exposes no fluid capability. */
@Mixin(BucketItem.class)
public interface BucketContentAccessor {
    @Accessor("content") Fluid gregtech$bucketContent();
}
