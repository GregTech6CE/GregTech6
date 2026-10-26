package gregtech.mixin;

import gregtech.engine.fluid.WaterFamilyIdentity;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve the depth/darkness requirements and broaden only the water-block identity check. */
@Mixin(GlowSquid.class)
public abstract class GlowSquidSpawnMixin {
//? if >=1.21.1 {
    /*@Redirect(method = "checkGlowSquidSpawnRules", at = @At(value = "INVOKE",
*///?} else {
    @Redirect(method = "checkGlowSquideSpawnRules", at = @At(value = "INVOKE",
//?}
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"), require = 1)
    private static boolean gregtech$waterBody(BlockState state, Block expected) {
        return WaterFamilyIdentity.matchesWaterBlock(state, expected);
    }
}
