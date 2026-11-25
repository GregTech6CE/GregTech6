package gregtech.engine.fluid;

import gregtech.content.block.machine.TankBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
//?}

/** Handheld tanks keep the same contents and material restrictions as their placed form. */
public final class PortableTankHandler implements IFluidHandlerItem {
    private final ItemStack container;
    private final FluidTankGT tank;

    public static boolean accepts(Item item) {
        return item instanceof BlockItem block && block.getBlock() instanceof TankBlock;
    }

    public PortableTankHandler(ItemStack stack) {
        container = stack;
        var spec = ((TankBlock) ((BlockItem) stack.getItem()).getBlock()).spec();
        tank = new FluidTankGT(spec.capacity()).setMaxTemperature(spec.maxTemperature());
        var data = data();
        if (data.contains("gt.tank")) {
            tank.readFromNBT(data.getCompound("gt.tank"), gregtech.engine.stack.RegistryContext.current());
        }
        tank.setOnChanged(this::save);
    }

    private CompoundTag data() {
        return gregtech.engine.stack.BlockEntityData.readOrEmpty(container);
    }

    private void save() {
        var data = data();
        if (tank.isEmpty()) data.remove("gt.tank");
        else {
            var contents = new CompoundTag();
            tank.writeToNBT(contents, gregtech.engine.stack.RegistryContext.current());
            data.put("gt.tank", contents);
        }
        gregtech.engine.stack.BlockEntityData.write(
                container, data, gregtech.content.registry.GTBlockEntities.TANK.get());
    }

    @Override public ItemStack getContainer() { return container; }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int slot) { return tank.getFluidInTank(slot); }
    @Override public int getTankCapacity(int slot) { return tank.getTankCapacity(slot); }
    @Override public boolean isFluidValid(int slot, FluidStack fluid) { return tank.isFluidValid(slot, fluid); }
    @Override public int fill(FluidStack fluid, FluidAction action) {
        return container.getCount() == 1 ? tank.fill(fluid, action) : 0;
    }
    @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
        return container.getCount() == 1 ? tank.drain(fluid, action) : FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        return container.getCount() == 1 ? tank.drain(amount, action) : FluidStack.EMPTY;
    }
}
