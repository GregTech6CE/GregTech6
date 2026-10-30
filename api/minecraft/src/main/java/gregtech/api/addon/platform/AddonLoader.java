/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import gregtech.api.addon.InstalledService;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/** Loader entry for types whose signatures differ between Forge and NeoForge. GregTech installs it once. */
public final class AddonLoader {
    private static final InstalledService<Services> INSTALLED = new InstalledService<>();

    private AddonLoader() {}

    /** GT bootstrap only. Addons must not install or replace the host implementation. */
    public static void bind(Services implementation) {
        INSTALLED.bind(implementation, "addon loader services", "GregTech addon loader API is already installed");
    }

    public static Services services() {
        return INSTALLED.get("GregTech addon loader API is not installed");
    }

    /** GT implementation contract; addon consumers use {@link AddonFluids} and {@link AddonRecipes}. */
    public interface Services {
        /**
         * {@link FluidStack#EMPTY} for an unknown field, a null argument, or a non-positive amount.
         * Never null.
         */
        FluidStack fluid(String name, AddonFluids.Kind kind, int millibuckets);

        /** Null when no machine recipe map uses {@code name}. */
        AddonRecipes.@Nullable RecipeMap map(String name);

        /** True when {@code fluid} is a gas. Empty and liquids are false. */
        boolean gas(Fluid fluid);

        /** True when {@code fluid}'s material carries GT6's acid property. */
        boolean acid(Fluid fluid);

        /** True when {@code fluid} was declared as plasma. */
        boolean plasma(Fluid fluid);

        /** True when {@code fluid} carries GT6's magic flag or a magical material. */
        boolean magic(Fluid fluid);
    }
}
