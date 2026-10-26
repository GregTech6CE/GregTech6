package gregtech.mixin;

import gregtech.content.block.GTWorldFluidBlock;
import gregtech.content.registry.GTWorldWaterFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
*///?} else {
import net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep loader container simulation consistent with the vanilla water bucket returned on pickup. */
@Mixin(value = BucketPickupHandlerWrapper.class, remap = false)
public abstract class WorldWaterBucketPickupMixin {
//? if >=1.21.1 {
    /*@Redirect(method = {"getFluidInTank",
            "drain(ILnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;",
            "drain(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;"}, at = @At(value = "INVOKE",
*///?} else {
    @Redirect(method = {"getFluidInTank",
            "drain(ILnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;",
            "drain(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;"}, at = @At(value = "INVOKE",
//?}
            target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;",
            remap = true), require = 3)
    private FluidState gt6ce$containerFluid(Level level, BlockPos pos) {
        FluidState state = level.getFluidState(pos);
        if (!(level.getBlockState(pos).getBlock() instanceof GTWorldFluidBlock)) return state;
        if (!state.isSource() || state.getType().getBucket() == Items.AIR) return Fluids.EMPTY.defaultFluidState();
        return state.getType() instanceof GTWorldWaterFluid.Source ? Fluids.WATER.defaultFluidState() : state;
    }
}
