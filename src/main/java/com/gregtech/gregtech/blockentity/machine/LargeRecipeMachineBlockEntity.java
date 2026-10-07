package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

/** Shared execution and atomic part ownership for original large recipe-machine layouts. */
public class LargeRecipeMachineBlockEntity extends BasicMachineBlockEntity implements MultiblockPortOwner {
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    public LargeRecipeMachineBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    // Original multiblock base suppresses the single-block six-neighbor request; each source layout opts in.
    @Override protected void updateAdjacentToggleableEnergySources() {}
    @Override protected boolean usesTimeEnergy() { return spec()!=null&&spec().energyTag()==GregTechTags.Energy.TU; }
    @Override protected long inputMinimum() { return com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules.inputMinimum(usesTimeEnergy(),spec().energyTag()==GregTechTags.Energy.HU); }
    @Override protected long inputMaximum() { return com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules.inputMaximum(usesTimeEnergy()); }
    @Override protected boolean cheapOverclocking() { return !usesTimeEnergy(); }
    @Override protected boolean requiresConstantEnergy() { return com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules.constantEnergy(usesTimeEnergy(),spec().machineName()); }
    @Override protected int efficiency() { return com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules.efficiency(spec().machineName()); }
    @Override protected boolean parallelScalesDuration() { return !usesTimeEnergy(); }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side) { return usesTimeEnergy()?1:512; }
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type,Direction side) { return inputMinimum(); }
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type,Direction side) { return inputMaximum(); }
    @Override protected boolean structureComplete() { return isStructureOk(); }
    protected java.util.List<LargeMachineLayouts.Cell> structureCells() { return LargeMachineLayouts.cells(spec().machineName()); }
    protected boolean validStructureParts() {return true;}
    protected boolean matchesPart(LargeMachineLayouts.Cell cell,net.minecraft.world.level.block.Block expected,net.minecraft.world.level.block.Block actual) {return actual==expected;}
    @Override public boolean isStructureOk() {
        if(level==null||spec()==null||isRemoved()) return false;
        var layout=structureCells();if(layout==null)return false;
        if(!validStructureParts()){bindings.clear(this::release);return false;}
        var front=getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        var parts=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        net.minecraft.world.level.block.Block ovenCoil=null;
        for(var cell:layout) {
            var pos=cell.at(worldPosition,front);
            if(!level.hasChunkAt(pos)) {bindings.clear(this::release);return false;}
            if(cell.role()==MultiblockLayout.Role.AIR) {if(level.isEmptyBlock(pos))continue;bindings.clear(this::release);return false;}
            var expected=cell.block();
            if(cell.part()==18042) {
                if(ovenCoil==null) {
                    ovenCoil=level.getBlockState(pos).getBlock();
                    if(ovenCoil!=expected&&ovenCoil!=com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(18043)) {bindings.clear(this::release);return false;}
                }
                expected=ovenCoil;
            }
            if(!matchesPart(cell,expected,level.getBlockState(pos).getBlock()) || !(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)) {bindings.clear(this::release);return false;}
            parts.put(pos,cell.role());
        }
        return bindings.update(parts,pos->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).canBind(worldPosition),
                (pos,role)->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).bind(worldPosition,role),this::release);
    }
    private void release(BlockPos pos) { if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part) part.release(worldPosition); }
    @Override public void setRemoved() {bindings.clear(this::release);super.setRemoved();}
    private static boolean input(MultiblockLayout.Role role) { return role==MultiblockLayout.Role.ITEM_FLUID_IO||role==MultiblockLayout.Role.ITEM_FLUID_INPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY; }
    private static boolean output(MultiblockLayout.Role role) { return role==MultiblockLayout.Role.FLUID_OUTPUT||role==MultiblockLayout.Role.ITEM_FLUID_IO||role==MultiblockLayout.Role.ITEM_FLUID_OUTPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY; }
    private static boolean energy(MultiblockLayout.Role role) {return role==MultiblockLayout.Role.ENERGY_INPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY;}
    private final Map<MultiblockLayout.Role,IItemHandler> itemPorts=new EnumMap<>(MultiblockLayout.Role.class);
    private final Map<MultiblockLayout.Role,IFluidHandler> fluidPorts=new EnumMap<>(MultiblockLayout.Role.class);
    @Override public IItemHandler portItems(MultiblockLayout.Role role) {
        return role==MultiblockLayout.Role.FLUID_OUTPUT||(!input(role)&&!output(role))?null:itemPorts.computeIfAbsent(role,this::createItemPort);
    }
    private IItemHandler createItemPort(MultiblockLayout.Role role) {
        return new IItemHandler() {
            public int getSlots() {return inventory().getSlots();}
            public ItemStack getStackInSlot(int slot) {return inventory().getStackInSlot(slot);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {return input(role)?inventory().insertItem(slot,stack,simulate):stack;}
            public ItemStack extractItem(int slot,int amount,boolean simulate) {return output(role)&&slot>=inputSlots()?inventory().extractItem(slot,amount,simulate):ItemStack.EMPTY;}
            public int getSlotLimit(int slot) {return inventory().getSlotLimit(slot);}
            public boolean isItemValid(int slot,ItemStack stack) {return input(role)&&slot<inputSlots();}
        };
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return !input(role)&&!output(role)?null:fluidPorts.computeIfAbsent(role,this::createFluidPort);
    }
    private IFluidHandler createFluidPort(MultiblockLayout.Role role) {
        return new IFluidHandler() {
            public int getTanks(){return LargeRecipeMachineBlockEntity.this.getTanks();}
            public FluidStack getFluidInTank(int tank){return LargeRecipeMachineBlockEntity.this.getFluidInTank(tank);}
            public int getTankCapacity(int tank){return LargeRecipeMachineBlockEntity.this.getTankCapacity(tank);}
            public boolean isFluidValid(int tank,FluidStack stack){return input(role)&&LargeRecipeMachineBlockEntity.this.isFluidValid(tank,stack);}
            public int fill(FluidStack stack,FluidAction action){return input(role)?LargeRecipeMachineBlockEntity.this.fill(stack,action):0;}
            public FluidStack drain(FluidStack stack,FluidAction action){return output(role)?LargeRecipeMachineBlockEntity.this.drain(stack,action):FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){return output(role)?LargeRecipeMachineBlockEntity.this.drain(amount,action):FluidStack.EMPTY;}
        };
    }
    @Override public long portEnergyInputMin(MultiblockLayout.Role role,GregTechTags.Tag type) {return inputMinimum();}
    @Override public long portEnergyInputMax(MultiblockLayout.Role role,GregTechTags.Tag type) {return inputMaximum();}
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role){return !usesTimeEnergy()&&energy(role)?List.of(spec().energyTag()):List.of();}
    @Override public long portEnergyStored(MultiblockLayout.Role role,GregTechTags.Tag type){return portEnergyTypes(role).contains(type)?getEnergyTick():0;}
    @Override public long portEnergyCapacity(MultiblockLayout.Role role,GregTechTags.Tag type){return portEnergyTypes(role).contains(type)?inputMaximum():0;}
    @Override public long injectPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type,long size,long amount,boolean execute){return portEnergyTypes(role).contains(type)&&isStructureOk()?doEnergyInjection(type,null,size,amount,execute):0;}
}
