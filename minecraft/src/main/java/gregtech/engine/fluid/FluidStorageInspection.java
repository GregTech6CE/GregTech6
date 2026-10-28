package gregtech.engine.fluid;

import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Read-only totals: native long storage takes precedence over an int capability view. */
public record FluidStorageInspection(long amount, long capacity) {
    public static FluidStorageInspection read(BlockEntity entity, IFluidHandler handler) {
        if (entity instanceof TankBlockEntity vessel) {
            var tank = vessel.getFluidTank();
            return new FluidStorageInspection(tank.getAmount(), tank.capacity());
        }
        if (entity instanceof MultiblockTankControllerBlockEntity vessel)
            return new FluidStorageInspection(vessel.fluidAmountForInspection(), vessel.fluidCapacityForInspection());
        long amount = 0, capacity = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            amount += handler.getFluidInTank(i).getAmount();
            capacity += handler.getTankCapacity(i);
        }
        return new FluidStorageInspection(amount, capacity);
    }
}
