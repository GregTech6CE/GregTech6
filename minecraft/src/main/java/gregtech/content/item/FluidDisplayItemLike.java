package gregtech.content.item;

import gregtech.content.data.RegisteredFluids;

/** Read-only shared contract for fluid display items across platform implementations. */
public interface FluidDisplayItemLike {
    RegisteredFluids.FluidEntry fluidEntry();
}
