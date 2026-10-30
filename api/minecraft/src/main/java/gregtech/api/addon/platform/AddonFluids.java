/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * GregTech fluids. {@code name} is the fluid field, such as {@code DistW}, not a material name.
 * An unknown field, a null argument, or a non-positive amount is {@link FluidStack#EMPTY}, never null.
 * That is the same absence as {@link AddonItems#stack(String, String)} returning {@link net.minecraft.world.item.ItemStack#EMPTY}.
 */
public final class AddonFluids {
    private AddonFluids() {}

    public enum Kind { STILL, FLOWING }

    public static FluidStack fluid(String name, Kind kind, int millibuckets) {
        return AddonLoader.services().fluid(name, kind, millibuckets);
    }

    public static FluidStack still(String name, int millibuckets) {
        return fluid(name, Kind.STILL, millibuckets);
    }

    /** True when {@code stack} is a gas. An empty stack is false. */
    public static boolean gas(FluidStack stack) {
        return stack != null && !stack.isEmpty() && gas(stack.getFluid());
    }

    /** True when {@code fluid} is a gas. Null and empty are false. */
    public static boolean gas(@Nullable Fluid fluid) {
        return fluid != null && AddonLoader.services().gas(fluid);
    }

    /** True when {@code stack} is acid. An empty stack is false. */
    public static boolean acid(FluidStack stack) {
        return stack != null && !stack.isEmpty() && acid(stack.getFluid());
    }

    /** True when {@code fluid} is acid. Null and empty are false. */
    public static boolean acid(@Nullable Fluid fluid) {
        return fluid != null && AddonLoader.services().acid(fluid);
    }

    /** True when {@code stack} is plasma. An empty stack is false. */
    public static boolean plasma(FluidStack stack) {
        return stack != null && !stack.isEmpty() && plasma(stack.getFluid());
    }

    /** True when {@code fluid} is plasma. Null and empty are false. */
    public static boolean plasma(@Nullable Fluid fluid) {
        return fluid != null && AddonLoader.services().plasma(fluid);
    }

    /** True when {@code stack} is magical. An empty stack is false. */
    public static boolean magic(FluidStack stack) {
        return stack != null && !stack.isEmpty() && magic(stack.getFluid());
    }

    /** True when {@code fluid} is magical. Null and empty are false. */
    public static boolean magic(@Nullable Fluid fluid) {
        return fluid != null && AddonLoader.services().magic(fluid);
    }
}
