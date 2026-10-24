package gregtech.internal.fluid;

import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/**
 * A block that takes compressed gas on one face and can give it back through that face.
 * Ordinary pipes and buckets do not use this path.
 */
public interface CompressedGasReceiver {
    /** Gas arriving on {@code side} of this block. Returns the millibuckets stored. */
    int receiveCompressedGas(Direction side, FluidStack stack);

    /**
     * Gas this block will give through {@code side}.
     * {@code execute} false leaves the contents in place.
     */
    FluidStack extractCompressedGas(Direction side, int amount, boolean execute);
}
