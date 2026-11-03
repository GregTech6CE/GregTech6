package gregtech.engine.fluid;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Flow levels belong to world blocks; containers store the source identity of that same fluid. */
public final class ContainerFluidIdentity {
    private ContainerFluidIdentity() {}

    public static Fluid source(Fluid fluid) {
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }

    /** Does not mutate the caller's stack, including its components or opaque fluid metadata. */
    public static FluidStack canonical(FluidStack stack) {
        if (stack.isEmpty()) return stack;
        Fluid source = source(stack.getFluid());
        if (source == stack.getFluid()) return stack;
//? if >=1.21.1 {
        /*return new FluidStack(source.builtInRegistryHolder(), stack.getAmount(), stack.getComponentsPatch());
*///?} else {
        return new FluidStack(source, stack.getAmount(), stack.getTag() == null ? null : stack.getTag().copy());
//?}
    }
}
