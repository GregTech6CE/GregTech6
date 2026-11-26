package gregtech.client.gui;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Fluid hover boundary for optional recipe viewers; no viewer classes are loaded by a screen. */
public interface FluidRecipeScreen {
    FluidStack fluidAt(int x, int y);
}
