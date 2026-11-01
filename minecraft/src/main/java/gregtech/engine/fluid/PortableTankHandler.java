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
        tank = new FluidTankGT(spec.capacity()).setGasProof(spec.gasProof()).setAcidProof(spec.acidProof())
                .setPlasmaProof(spec.plasmaProof()).setMaxTemperature(spec.maxTemperature());
        var data = data();
        if (data.contains("gt.tank")) {
//? if >=1.21.1 {
            /*tank.readFromNBT(data.getCompound("gt.tank"), FluidDisplayBinding.currentLookup());
*///?} else {
            tank.readFromNBT(data.getCompound("gt.tank"));
//?}
        }
        tank.setOnChanged(this::save);
    }

    private CompoundTag data() {
//? if >=1.21.1 {
        /*var data = container.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
*///?} else {
        var data = container.getTagElement("BlockEntityTag");
        return data == null ? new CompoundTag() : data.copy();
//?}
    }

    private void save() {
        var data = data();
        if (tank.isEmpty()) data.remove("gt.tank");
        else {
            var contents = new CompoundTag();
//? if >=1.21.1 {
            /*tank.writeToNBT(contents, FluidDisplayBinding.currentLookup());
*///?} else {
            tank.writeToNBT(contents);
//?}
            data.put("gt.tank", contents);
        }
//? if >=1.21.1 {
        /*if (data.size() == 1 && data.contains("id")) data.remove("id");
        if (!data.isEmpty()) data.putString("id", net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE
                .getKey(gregtech.content.transport.fluid.FluidTransportRegistries.TANK.get()).toString());
        if (data.isEmpty()) container.remove(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        else container.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.of(data));
*///?} else {
        if (data.isEmpty()) container.removeTagKey("BlockEntityTag");
        else container.addTagElement("BlockEntityTag", data);
//?}
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
