package gregtech.mixin;

import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Shared mapped collar setter: private in Minecraft 1.21.1, public in 1.20.1. */
@Mixin(Wolf.class)
public interface WolfCollarAccessor {
    @Invoker("setCollarColor")
    void gregtech$setCollarColor(DyeColor color);
}
