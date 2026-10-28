package gregtech.mixin;

import gregtech.engine.fluid.WaterFamilyIdentity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep vanilla height and fluid-tag checks while accepting tagged water blocks above the spawn. */
@Mixin(WaterAnimal.class)
public abstract class SurfaceWaterAnimalSpawnMixin {
    @Redirect(
            method = "checkSurfaceWaterAnimalSpawnRules",
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
