package gregtech.mixin;

import gregtech.engine.fluid.WaterFamilyIdentity;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Tropical fish also check the upper water block before their biome/height exception. */
@Mixin(TropicalFish.class)
public abstract class TropicalFishSpawnMixin {
    @Redirect(
            method = "checkTropicalFishSpawnRules",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"),
            require = 1)
    private static boolean gregtech$waterBody(BlockState state, Block expected) {
        return WaterFamilyIdentity.matchesWaterBlock(state, expected);
    }
}
