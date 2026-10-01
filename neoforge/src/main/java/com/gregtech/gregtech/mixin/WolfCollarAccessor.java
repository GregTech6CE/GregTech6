package com.gregtech.gregtech.mixin;
import net.minecraft.world.entity.animal.Wolf;import net.minecraft.world.item.DyeColor;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.Invoker;
/** Native collar setter is private; invoke the original method without reflective lookups. */
@Mixin(Wolf.class)public interface WolfCollarAccessor {@Invoker("setCollarColor")void gregtech$setCollarColor(DyeColor color);}
