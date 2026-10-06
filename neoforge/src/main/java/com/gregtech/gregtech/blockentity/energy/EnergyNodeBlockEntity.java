package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;



import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 energy-net node: motors/dynamos/transformers (energy conversion), steam
 * turbines (steam fluid → RU), solar panels (sun → EU) and battery boxes /
 * storage cabinets (EU buffer). Inputs on every face except the front; output
 * packets of {@code outputRate} leave through the front (FACING).
 */
public class EnergyNodeBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.inventory.BlockContents, com.gregtech.gregtech.api.machine.MachineControl.Provider, com.gregtech.gregtech.content.cover.PanelCoverHost {

    private java.util.Map<Direction,net.minecraft.world.item.ItemStack> batteryCovers=new java.util.EnumMap<>(Direction.class);
    private com.gregtech.gregtech.content.cover.PanelCoverRuntime batteryPanels;
    private com.gregtech.gregtech.content.energy.ElectricTransformerControl transformerControl;
    private com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State rotaryConverter;
    public boolean isOriginalRotaryConverter(){return rotaryConverter != null;}
    public boolean isOriginalMotor(){return isOriginalRotaryConverter() && com.gregtech.gregtech.content.energy.OriginalRotaryConverter.motor(spec);}
    public boolean reverseMotor(){buffer=0;boolean reversed=rotaryConverter.reverse();setChanged();return reversed;}
    public boolean motorCounterClockwise(){return isOriginalMotor() && rotaryConverter.counterClockwise();}
    public boolean hasControlPanels(){return isOriginalRotaryConverter()||isBatteryBox()||isElectricTransformer()||isSolar();}
    @Override public net.minecraft.world.item.ItemStack getCover(Direction side){return batteryCovers.getOrDefault(side,net.minecraft.world.item.ItemStack.EMPTY);}
    @Override public com.gregtech.gregtech.content.cover.PanelCoverRuntime panels(){
        if(batteryPanels==null)batteryPanels=new com.gregtech.gregtech.content.cover.PanelCoverRuntime(this);
        return batteryPanels;
    }
    @Override public boolean coverSupportsPossible(){return hasControlPanels();}
    @Override public boolean coverPossible(Direction side){return isOriginalRotaryConverter()||isSolar()|| (isBatteryBox()?batteryEnergy.buffer()>spec.outputRate():isElectricTransformer()&&transformerControl.running());}
    @Override public boolean attachCover(Direction side,net.minecraft.world.item.ItemStack stack){
        var panel=com.gregtech.gregtech.content.cover.PanelCover.of(stack);
        if(!com.gregtech.gregtech.content.cover.CoverItems.isCover(stack)||!getCover(side).isEmpty()||!panels().canAttach(side,stack))return false;
        batteryCovers.put(side,stack.copyWithCount(1));panels().attached(side);return true;
    }
    @Override public net.minecraft.world.item.ItemStack removeCover(Direction side){
        var removed=batteryCovers.remove(side);
        if(removed==null)return net.minecraft.world.item.ItemStack.EMPTY;
        var panel=com.gregtech.gregtech.content.cover.PanelCover.of(removed);
        if(panel!=null&&panel.selector())machineControl(side).setMode(0);
        return panels().removed(side,removed);
    }

    private EnergyNodeSpec spec;
    private com.gregtech.gregtech.content.energy.SolarPanelEnergy solarEnergy;
    /** Internal buffer, counted in input units (steam L for turbines). */
    private long buffer;
    /** Transformers: soft-hammer toggled step-up mode (rates swapped). */
    private boolean inverted;
    /** RU rotation direction for the dedicated rotational transformer path. */
    private boolean rotationNegativeInput;
    /** GT6 bipolar magnets: stopped state and 0..15 output current limit. */
    private boolean magnetStopped;
    private byte magnetMode;
    private int magnetOverloads;
    private com.gregtech.gregtech.content.energy.BatteryBoxEnergy batteryEnergy;
    /** Battery boxes store complete energy items in fixed slots. */
    private net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> batteries =
            net.minecraft.core.NonNullList.withSize(0, net.minecraft.world.item.ItemStack.EMPTY);
    /** Retained only to return rotor items installed in older versions of the port. */
    private net.minecraft.world.item.ItemStack rotor = net.minecraft.world.item.ItemStack.EMPTY;
    private long turbinePending, steamRemainder;
    private boolean converterStopped;
    @Nullable
    private FluidTankGT steamTank;

    public EnergyNodeBlockEntity(BlockPos pos, BlockState state) {
        this(com.gregtech.gregtech.registry.GTEnergyNodes.ENERGY_NODE.get(), pos, state);
    }

    public EnergyNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof com.gregtech.gregtech.block.energy.EnergyNodeBlock node) {
            setSpec(node.spec());
        }
    }

    /** Convert a pre-standardization battery tile after an old world loads its retained block ID. */
    @Override public void onLoad(){
        super.onLoad();
        if (isSolar() && level != null && !level.isClientSide && !isRemoved() && facing() == Direction.UP)
            level.setBlock(worldPosition, getBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN), net.minecraft.world.level.block.Block.UPDATE_ALL);
        if(level!=null&&!level.isClientSide&&!isRemoved()&&getBlockState().getBlock() instanceof com.gregtech.gregtech.block.energy.ChemicalBatteryBlock){
            var replacement=new ChemicalBatteryBlockEntity(worldPosition,getBlockState());
            var tag=new CompoundTag();tag.putLong(com.gregtech.gregtech.item.ChemicalBatteryItem.CHARGE,buffer);
            replacement.loadAdditional(tag,level.registryAccess());level.setBlockEntity(replacement);replacement.setChanged();
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        }
    }

    public void setSpec(EnergyNodeSpec spec) {
        this.spec = spec;
        rotaryConverter = com.gregtech.gregtech.content.energy.OriginalRotaryConverter.handles(spec)
                ? new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.State(spec) : null;
        if (isSolar()) {
            solarEnergy = new com.gregtech.gregtech.content.energy.SolarPanelEnergy(spec.outputRate());
            if (!(batteryCovers instanceof java.util.EnumMap)) batteryCovers = new java.util.EnumMap<>(Direction.class);
        }
        if (isBatteryBox()) {
            batteries=net.minecraft.core.NonNullList.withSize(spec.batterySlots(),net.minecraft.world.item.ItemStack.EMPTY);
            batteryEnergy = new com.gregtech.gregtech.content.energy.BatteryBoxEnergy(batteries, spec.inputRate());
            if(!(batteryCovers instanceof java.util.EnumMap))batteryCovers=new java.util.EnumMap<>(Direction.class);
        }
        if(isElectricTransformer()){transformerControl=new com.gregtech.gregtech.content.energy.ElectricTransformerControl(this::setChanged);if(!(batteryCovers instanceof java.util.EnumMap))batteryCovers=new java.util.EnumMap<>(Direction.class);}
        if (spec.kind() == EnergyNodeSpec.Kind.TURBINE) {
            steamTank = new FluidTankGT(spec.inputRate() * 8).setOnChanged(this::setChanged);
        }
    }

    public EnergyNodeSpec spec() { return spec; }
    public long stored() { return isSolar() ? solarEnergy.energy() : isBatteryBox() ? batteryEnergy.buffer() : buffer; }
    public com.gregtech.gregtech.content.energy.BatteryBoxEnergy batteryEnergy() { return batteryEnergy; }

    // ── Transformer step-up/step-down ───────────────────────────────────────

    public boolean isInvertible() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.CONVERTER
                && spec.inType() == spec.outType();
    }

    public boolean isElectricTransformer(){return spec!=null&&spec.id().startsWith("transformer_")&&spec.inType()==GregTechTags.Energy.EU;}

    public boolean isRotationTransformer() {
        return spec != null && spec.id().startsWith("rotation_transformer_");
    }

    public boolean isSolar() { return spec != null && spec.kind() == EnergyNodeSpec.Kind.SOLAR; }
    public void checkSolarSky() { if (isSolar()) solarEnergy.checkSky(); }

    public boolean isMagnet() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.MAGNET;
    }

    public boolean magnetEnabled() { return isMagnet() && !magnetStopped; }

    public boolean toggleMagnetEnabled() {
        if (!isMagnet()) return false;
        magnetStopped = !magnetStopped;
        setChanged();
        return !magnetStopped;
    }

    public int magnetMode() { return Byte.toUnsignedInt(magnetMode); }

    public int cycleMagnetMode() {
        if (!isMagnet()) return 0;
        magnetMode = (byte) ((magnetMode + 1) & 15);
        setChanged();
        return magnetMode;
    }

    /** @return the new mode (true = step-up). */
    public boolean toggleInverted() {
        // GT6 clears the rotational transformer's stored energy when its direction changes.
        // Other converters keep their existing mode-switch behavior.
        if (isRotationTransformer() || isElectricTransformer()) {
            buffer = 0;
            rotationNegativeInput = false;
        }
        inverted = !inverted;
        if(isElectricTransformer())transformerControl.select(inverted);
        setChanged();
        return inverted;
    }

    private long inRate() { return inverted ? spec.outputRate() : spec.inputRate(); }
    private long outRate() { return inverted ? spec.inputRate() : spec.outputRate(); }
    private Direction rotationInputFace() { return inverted ? facing().getOpposite() : facing(); }
    private Direction rotationOutputFace() { return inverted ? facing() : facing().getOpposite(); }

    /**
     * GT6's bidirectional RU converter uses the original input rating as the
     * reverse converter's input recommendation, even though it accepts smaller
     * packets. The two modes share the same physical capacitor.
     */
    private long rotationInputRecommended() { return spec.inputRate(); }

    private long rotationInputMaximum() {
        return inverted ? Math.max(spec.inputRate(), spec.outputRate() * 8) : spec.inputRate() * 2;
    }

    private long rotationOutputSize() {
        // TE_Behavior_Energy_Converter.doConversion: floor(stored * outRec / inRec).
        // In reverse, both recommendations are the original input rate.
        return inverted ? buffer : buffer * spec.outputRate() / spec.inputRate();
    }

    // ── Battery boxes (GT6: capacity comes from installed battery cells) ───

    // ── Recovery of obsolete port rotor inventories ────

    public boolean isTurbine() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.TURBINE;
    }

    public static boolean isRotorItem(net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem mat
                && mat.getPrefix() == com.gregtech.gregtech.data.MaterialPrefix.rotor;
    }

    public boolean hasRotor() { return !rotor.isEmpty(); }

    /** The rotor is part of the turbine crafting recipe in GT6, not a runtime slot. */
    public boolean installRotor(net.minecraft.world.item.ItemStack stack) { return false; }

    public net.minecraft.world.item.ItemStack removeRotor() {
        net.minecraft.world.item.ItemStack out = rotor;
        rotor = net.minecraft.world.item.ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public boolean isBatteryBox() {
        return spec != null && spec.batterySlots() > 0;
    }

    /** Actual energy-item capacity; filled chemical cells are crafting ingredients, not batteries. */
    public static long batteryCapacityOf(net.minecraft.world.item.ItemStack stack) {
        return com.gregtech.gregtech.content.energy.BatteryBoxEnergy.accepts(stack)
                ? ((com.gregtech.gregtech.api.energy.item.IItemEnergy)stack.getItem())
                    .getEnergyCapacity(stack, GregTechTags.Energy.EU) : 0;
    }

    public boolean installBattery(net.minecraft.world.item.ItemStack stack) {
        if (!isBatteryBox() || stack.isEmpty()) return false;
        for (int slot = 0; slot < batteries.size(); slot++) {
            if (batterySlots.insertItem(slot, stack.copyWithCount(1), false).isEmpty()) return true;
        }
        return false;
    }

    public net.minecraft.world.item.ItemStack removeBattery() {
        for (int slot = batteries.size() - 1; slot >= 0; slot--) {
            if (!batteries.get(slot).isEmpty()) return batterySlots.extractItem(slot, 1, false);
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    /** Effective capacity: battery boxes scale with installed cells. */
    private long capacity() {
        if (isBatteryBox()) {
            long sum = 0;
            for (var b : batteries) sum += batteryCapacityOf(b);
            return sum;
        }
        return isOriginalRotaryConverter() || isSteamConverter() ? spec.inputRate() * 2 : spec.capacity();
    }

    // ── Battery box GUI (GT6 TileEntityBase10EnergyBatBox opens one) ────────

    /** Fixed slot identities: a vacant slot never shifts another stack. */
    private final net.neoforged.neoforge.items.IItemHandlerModifiable batterySlots =
            new net.neoforged.neoforge.items.IItemHandlerModifiable() {
        private void checkSlot(int slot) {
            if (slot < 0 || slot >= batteries.size()) throw new IndexOutOfBoundsException("Battery slot " + slot);
        }
        @Override public int getSlots() { return batteries.size(); }
        @Override public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
            checkSlot(slot);
            return batteries.get(slot);
        }
        @Override public net.minecraft.world.item.ItemStack insertItem(int slot,
                net.minecraft.world.item.ItemStack stack, boolean simulate) {
            checkSlot(slot);
            if (stack.isEmpty() || !isItemValid(slot, stack) || !batteries.get(slot).isEmpty()) return stack;
            if (!simulate) {
                batteries.set(slot, stack.copyWithCount(1));
                setChanged();
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }
        @Override public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            checkSlot(slot);
            if (amount <= 0 || batteries.get(slot).isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
            var out = batteries.get(slot).copy();
            if (!simulate) {
                batteries.set(slot, net.minecraft.world.item.ItemStack.EMPTY);
                setChanged();
            }
            return out;
        }
        @Override public void setStackInSlot(int slot, net.minecraft.world.item.ItemStack stack) {
            checkSlot(slot);
            if (!stack.isEmpty() && !isItemValid(slot, stack)) throw new IllegalArgumentException("Not an energy item");
            batteries.set(slot, stack.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : stack.copyWithCount(1));
            setChanged();
        }
        @Override public int getSlotLimit(int slot) { checkSlot(slot); return 1; }
        @Override public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            checkSlot(slot);
            return isBatteryBox() && com.gregtech.gregtech.content.energy.BatteryBoxEnergy.accepts(stack);
        }
    };

    /** Shared inventory used by the menu and sided automation. */
    public net.neoforged.neoforge.items.IItemHandlerModifiable batteryInventory() { return batterySlots; }

    private net.neoforged.neoforge.items.IItemHandler sidedBatteryInventory(Direction side) {
        return new net.neoforged.neoforge.items.IItemHandler() {
            private boolean blocked(){return panels().shuttered(side);}
            @Override public int getSlots(){return batterySlots.getSlots();}
            @Override public net.minecraft.world.item.ItemStack getStackInSlot(int slot){return batterySlots.getStackInSlot(slot);}
            @Override public net.minecraft.world.item.ItemStack insertItem(int slot,net.minecraft.world.item.ItemStack stack,boolean simulate){
                return blocked()||!com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsItem(getCover(side),true)?stack:batterySlots.insertItem(slot,stack,simulate);
            }
            @Override public net.minecraft.world.item.ItemStack extractItem(int slot,int amount,boolean simulate){
                return blocked()||!com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsItem(getCover(side),false)?net.minecraft.world.item.ItemStack.EMPTY:batterySlots.extractItem(slot,amount,simulate);
            }
            @Override public int getSlotLimit(int slot){return batterySlots.getSlotLimit(slot);}
            @Override public boolean isItemValid(int slot,net.minecraft.world.item.ItemStack stack){return !blocked()&&batterySlots.isItemValid(slot,stack);}
        };
    }

    public net.minecraft.world.MenuProvider batteryMenu() {
        return new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new com.gregtech.gregtech.client.gui.HopperContainerMenu(
                        com.gregtech.gregtech.registry.GTMenuTypes.forSlotCount(batteries.size()), id, inv, batterySlots),
                net.minecraft.network.chat.Component.translatable(getBlockState().getBlock().getDescriptionId()));
    }

    private Direction facing() {
        BlockState st = getBlockState();
        return st.hasProperty(DirectionalBlock.FACING) ? st.getValue(DirectionalBlock.FACING) : Direction.NORTH;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private long lastSyncedBuffer = -1;

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyNodeBlockEntity be) {
        if (be.spec == null) return;
        if(!be.isOriginalRotaryConverter()&&!be.isBatteryBox()&&!be.isSolar()&&!be.isElectricTransformer()&&!be.batteryCovers.isEmpty()) {
            be.panels().beforeTick();
            for(var side:Direction.values())com.gregtech.gregtech.content.cover.ComponentCoverRuntime.tick(be,side,level.getGameTime());
            be.panels().afterTick();
        }
        if (be.isBatteryBox()) {
            be.panels().beforeTick();
            for(var side:Direction.values())com.gregtech.gregtech.content.cover.ComponentCoverRuntime.tick(be,side,level.getGameTime());
            be.batteryEnergy.tick(level.getGameTime(),level,pos);
            long offered=be.batteryEnergy.offered();
            if(offered>0) be.batteryEnergy.emitted(EnergyTransfer.emitEnergyToSide(
                    be.spec.outType(),be.facing(),be.outRate(),offered,be));
            be.panels().afterTick();
            be.setChanged();
            if(level.getGameTime()%20==0){
                var property=com.gregtech.gregtech.block.energy.BatteryBoxBlock.CHARGE_STATE;
                if(state.hasProperty(property)&&state.getValue(property)!=be.batteryEnergy.displayState())
                    level.setBlock(pos,state.setValue(property,be.batteryEnergy.displayState()),3);
                else level.sendBlockUpdated(pos,state,state,2);
            }
            return;
        }
        if (be.isSolar()) {
            be.tickSolar(level, pos);
            return;
        }
        if (be.isOriginalRotaryConverter()) {
            be.tickOriginalRotaryConverter();
            return;
        }
        if (be.isMagnet()) {
            be.tickMagnet();
            if (level.getGameTime() % 600 == 5 && be.magnetOverloads > 0) be.magnetOverloads--;
            return;
        }
        switch (be.spec.kind()) {
            case TURBINE -> be.tickTurbine();
            default -> {} // converters/storage are push/pull driven; emission below
        }
        be.emitOutput();
        // charge bar: sync the buffer to clients at most every 20 ticks
        if (level.getGameTime() % 20 == 0 && be.lastSyncedBuffer != be.buffer) {
            be.lastSyncedBuffer = be.buffer;
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    // ── Client sync (charge bar display) ────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag,lookup);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider lookup) {
        loadAdditional(tag,lookup);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,HolderLookup.Provider lookup) {
        if (pkt.getTag() != null) loadAdditional(pkt.getTag(),lookup);
    }

    public boolean isSteamConverter() { return isTurbine(); }

    private void tickTurbine() {
        if (steamTank == null || spec.inputRate() <= 0) return;
        // Older port builds admitted arbitrary fluids. Never turn that saved water into RU.
        if (!steamTank.getFluid().isEmpty() && !turbineInlet().isFluidValid(0, steamTank.getFluid())) {
            steamTank.remove(steamTank.getAmount());
            setChanged();
        }
        var step = com.gregtech.gregtech.content.energy.SteamTurbineConversion.step(buffer, turbinePending,
                steamTank.getAmount(), steamRemainder, spec.inputRate());
        buffer = step.energy(); turbinePending = step.pending(); steamRemainder = step.remainder();
        if (step.consumed() > 0) steamTank.remove(step.consumed());
        emitCondensate(step.condensate());
        setChanged();
    }

    private void emitCondensate(long amount) {
        var water = com.gregtech.gregtech.registry.GTFluids.still("DistW");
        if (level == null || amount <= 0 || water == null) return;
        var fluid = new FluidStack(water.get(), (int) Math.min(Integer.MAX_VALUE, amount));
        for (Direction side : Direction.values()) {
            if (side.getAxis() == facing().getAxis() || !level.hasChunkAt(worldPosition.relative(side))) continue;
            var neighbor = level.getBlockEntity(worldPosition.relative(side));
            if (neighbor == null) continue;
            var target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
            if (target != null) fluid.shrink(Math.max(0, Math.min(fluid.getAmount(), target.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE))));
            if (fluid.isEmpty()) break;
        }
        // Source discards condensate that no adjacent tank recovers.
    }

    private IFluidHandler turbineInlet() { return new IFluidHandler() {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return steamTank.getFluid().copy(); }
        public int getTankCapacity(int tank) { return (int) steamTank.capacity(); }
        public boolean isFluidValid(int tank, FluidStack fluid) {
            var steam = com.gregtech.gregtech.registry.GTFluids.still("Steam");
            return !fluid.isEmpty() && steam != null && fluid.getFluid().isSame(steam.get());
        }
        public int fill(FluidStack fluid, FluidAction action) {
            return !isRemoved() && !converterStopped && isFluidValid(0, fluid) ? steamTank.fill(fluid, action) : 0;
        }
        public FluidStack drain(FluidStack fluid, FluidAction action) { return FluidStack.EMPTY; }
        public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    }; }

    private void emitSteamConverter() {
        long output = com.gregtech.gregtech.content.energy.SteamTurbineConversion.output(buffer, spec.inputRate(), spec.outputRate());
        if (output > spec.outputRate() * 2) magnetOverload(output);
        else if (output >= Math.max(1, spec.outputRate() / 2))
            EnergyTransfer.emitEnergyToSide(spec.outType(), facing(), output, 1, this);
        buffer = com.gregtech.gregtech.content.energy.SteamTurbineConversion.waste(buffer, spec.inputRate());
        setChanged();
    }

    private void tickSolar(Level level, BlockPos pos) {
        panels().beforeTick();
        var conditions = new com.gregtech.gregtech.content.energy.SolarPanelEnergy.Conditions(
                level.isDay(), level.isRaining(), level.isThundering(),
                level.dimension().location().toString().equals("twilightforest:twilight_forest"),
                level.getBiome(pos).value().getModifiedClimateSettings().downfall());
        solarEnergy.tick(() -> level.dimensionType().hasSkyLight() && level.canSeeSky(pos.above()),
                conditions, energy -> EnergyTransfer.emitEnergyToNetwork(spec.outType(), energy, 1, this));
        var state = getBlockState();
        var active = com.gregtech.gregtech.block.energy.SolarPanelBlock.ACTIVE;
        if (state.hasProperty(active) && state.getValue(active) != solarEnergy.active())
            level.setBlock(pos, state.setValue(active, solarEnergy.active()), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        panels().afterTick();
        setChanged();
    }

    private void tickOriginalRotaryConverter() {
        panels().beforeTick();
        for (var side : Direction.values())
            com.gregtech.gregtech.content.cover.ComponentCoverRuntime.tick(this, side, level.getGameTime());
        long before = buffer;
        var step = rotaryConverter.tick(buffer, converterStopped,
                (size, amount) -> EnergyTransfer.emitEnergyToNetwork(spec.outType(), size, amount, this));
        buffer = step.energy();
        if (step.overloaded()) rotaryOverload(before, spec.outType());
        if (isRemoved() || level.getBlockEntity(worldPosition) != this) return;
        if (level.getGameTime() % 600 == 5 && !step.possible() && magnetOverloads > 0) magnetOverloads--;
        var state = getBlockState();
        var next = state.setValue(com.gregtech.gregtech.block.energy.RotaryConverterBlock.ACTIVITY, step.visual());
        if (isOriginalMotor())
            next = next.setValue(com.gregtech.gregtech.block.energy.OriginalMotorBlock.COUNTER_CLOCKWISE, rotaryConverter.counterClockwise())
                    .setValue(com.gregtech.gregtech.block.energy.OriginalMotorBlock.FAST, step.fast());
        if (next != state) level.setBlock(worldPosition, next, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        else if (level.getGameTime() % 20 == 0 && lastSyncedBuffer != buffer) {
            lastSyncedBuffer = buffer;
            level.sendBlockUpdated(worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        panels().afterTick();
        setChanged();
    }

    private void rotaryOverload(long size, GregTechTags.Tag type) {
        buffer = 0;
        if (magnetOverloads < 100) { magnetOverloads++; setChanged(); return; }
        if (level != null && !level.isClientSide) {
            if (com.gregtech.gregtech.GregTechConfig.machineOvervoltageExplosions()) {
                float power = com.gregtech.gregtech.content.energy.OriginalRotaryConverter.overloadPower(size, type);
                level.removeBlock(worldPosition, false);
                if (power >= 1) level.explode(null, worldPosition.getX()+0.5, worldPosition.getY()+0.5,
                        worldPosition.getZ()+0.5, power, Level.ExplosionInteraction.BLOCK);
            }
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
        }
        setChanged();
    }

    private void emitOutput() {
        if (level == null || converterStopped) return;
        if (isSteamConverter()) { emitSteamConverter(); return; }
        if(isElectricTransformer()){emitElectricTransformer();return;}
        if (isRotationTransformer()) {
            emitRotationTransformer();
            return;
        }
        if (buffer < costPerPacket()) return;
        if (spec.outType() == GregTechTags.Energy.RF) {
            pushForgeEnergy();
            return;
        }
        long packets = buffer / costPerPacket();
        if (isRotationTransformer()) packets = Math.min(packets, inverted ? 1 : 4);
        long emitted = EnergyTransfer.emitEnergyToSide(
                spec.outType(), isRotationTransformer() ? rotationOutputFace() : facing(),
                isRotationTransformer() && rotationNegativeInput ? -outRate() : outRate(), packets, this);
        if (isRotationTransformer()) emitted = Math.min(packets, Math.max(0, emitted));
        if (emitted > 0) {
            buffer -= emitted * costPerPacket();
            setChanged();
        }
    }

    /** GT6 TileEntityBase11Bipolar.doConversion: two signed MU poles and waste energy. */
    private void tickMagnet() {
        if (level == null) return;
        long output = buffer * spec.outputRate() / spec.inputRate();
        if (magnetMode > 0) output = Math.min(output,
                spec.outputRate() * 2 * (16L - magnetMode) / 16L);
        boolean active = !magnetStopped && output >= getEnergySizeOutputMin(spec.outType(), null);
        if (active && output <= getEnergySizeOutputMax(spec.outType(), null)) {
            Direction front = facing();
            EnergyTransfer.emitEnergyToSide(spec.outType(), front, output, 1, this);
            EnergyTransfer.emitEnergyToSide(spec.outType(), front.getOpposite(), -output, 1, this);
        }
        // GT6 TE_Behavior_Energy_Converter.doBipolar: NBT_WASTE_ENERGY uses
        // ceil(input maximum * (16 - mode) / 16), even without a receiver.
        if (buffer > 0) {
            long inputMaximum = spec.inputRate() * 2;
            long waste = (inputMaximum * (16L - magnetMode) + 15L) / 16L;
            buffer = Math.max(0, buffer - waste);
            setChanged();
        }
        if (getBlockState().getBlock() instanceof com.gregtech.gregtech.block.energy.MagnetMachineBlock
                && getBlockState().getValue(com.gregtech.gregtech.block.energy.MagnetMachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition,
                    getBlockState().setValue(com.gregtech.gregtech.block.energy.MagnetMachineBlock.ACTIVE, active), 3);
        }
    }

    private void magnetOverload(long packetSize) {
        buffer = 0;
        if (magnetOverloads < 100) {
            magnetOverloads++;
            setChanged();
            return;
        }
        if (level != null && !level.isClientSide
                && com.gregtech.gregtech.GregTechConfig.machineOvervoltageExplosions()) {
            float power = Math.min(8.0f, 3.0f + (float) (Math.log(
                    (double) packetSize / Math.max(1, spec.inputRate() * 2)) / Math.log(2.0)));
            level.removeBlock(worldPosition, false);
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, Math.max(3.0f, power),
                    Level.ExplosionInteraction.BLOCK);
        }
        setChanged();
    }

    private void emitElectricTransformer(){
        panels().beforeTick();
        long packets=inverted?1:spec.inputRate()/spec.outputRate();
        long size=inverted?buffer:buffer/packets;
        long maximum=getEnergySizeOutputMax(spec.outType(),null);
        if(transformerControl.mode()>0)size=Math.min(size,transformerControl.limit(maximum));
        boolean possible=size>=getEnergySizeOutputMin(spec.outType(),null),emitted=false;
        if(possible){
            if(size>maximum)magnetOverload(size);
            else {
                long used=EnergyTransfer.emitEnergyToNetwork(spec.outType(),size,packets,this);
                if(used>0){buffer-=Math.min(packets,used)*size;emitted=true;}
            }
        }
        transformerControl.tick(possible,emitted);
        if(level.getGameTime()%600==5&&!possible&&magnetOverloads>0)magnetOverloads--;
        var property=com.gregtech.gregtech.block.energy.ElectricTransformerBlock.ACTIVITY;
        if(getBlockState().hasProperty(property)&&getBlockState().getValue(property)!=transformerControl.visual())
            level.setBlock(worldPosition,getBlockState().setValue(property,transformerControl.visual()),3);
        panels().afterTick();
    }

    private void emitRotationTransformer() {
        long size = rotationOutputSize();
        long minimum = getEnergySizeOutputMin(spec.outType(), rotationOutputFace());
        long maximum = getEnergySizeOutputMax(spec.outType(), rotationOutputFace());
        if (size >= minimum && size <= maximum) {
            EnergyTransfer.emitEnergyToSide(spec.outType(), rotationOutputFace(),
                    rotationNegativeInput ? -size : size, inverted ? 1 : 4, this);
        }
        // Both GT6 conversion modes have NBT_WASTE_ENERGY=T. Their capacitors
        // lose up to the active input maximum every tick, including when no
        // receiver exists or the energy is below the output packet threshold.
        if (buffer > 0) {
            buffer = Math.max(0, buffer - rotationInputMaximum());
            setChanged();
        }
    }

    /** RF output: push whole packets into the Forge Energy storage in front. */
    private void pushForgeEnergy() {
        var be = level.getBlockEntity(worldPosition.relative(facing()));
        if (be == null) return;
        var storage = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,be.getBlockPos(),facing().getOpposite());
        if (storage == null || !storage.canReceive()) return;
        long cost = costPerPacket();
        int out = (int) outRate();
        for (int i = 0; i < 64 && buffer >= cost; i++) {
            if (storage.receiveEnergy(out, true) < out) break;
            storage.receiveEnergy(out, false);
            buffer -= cost;
            setChanged();
        }
    }

    /** Input units consumed per emitted packet (solar generates, so 1:1 on output units). */
    private long costPerPacket() {
        return switch (spec.kind()) {
            case SOLAR, STORAGE -> outRate();
            default -> Math.max(1, inRate());
        };
    }

    // ── IEnergyBlock ─────────────────────────────────────────────────────────

    private boolean acceptsEnergyInput() {
        return spec != null && spec.kind() != EnergyNodeSpec.Kind.TURBINE
                && spec.kind() != EnergyNodeSpec.Kind.SOLAR;
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        if (spec == null) return false;
        return emitting ? energyType == spec.outType()
                : (acceptsEnergyInput() && energyType == spec.inType());
    }

    @Override
    public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        if (spec == null) return List.of();
        return spec.inType() == spec.outType() ? List.of(spec.outType()) : List.of(spec.inType(), spec.outType());
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (!acceptsEnergyInput() || energyType != spec.inType()) return false;
        if (isOriginalRotaryConverter()) return (theoretical || !converterStopped)
                && (side == null || (isOriginalMotor() ? side != facing() : side == facing().getOpposite()))
                && super.isEnergyAcceptingFrom(energyType, side, theoretical);
        if (isMagnet()) return (theoretical || !magnetStopped)
                && (side == null || (side != facing() && side != facing().getOpposite()));
        if(isElectricTransformer())return (theoretical||transformerControl.accepts())&&(side==null||(inverted?side!=facing():side==facing()));
        if (isRotationTransformer()) return side == null || side == rotationInputFace();
        return isSteamConverter() ? !converterStopped && (side == null || side == facing().getOpposite()) : side == null || side != facing();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (spec == null || energyType != spec.outType()) return false;
        if (isSolar()) return (side == null || side == facing()) && (theoretical || side == null || !panels().shuttered(side));
        if (isBatteryBox()) return (theoretical || batteryEnergy.enabled()) && (side == null || side == facing());
        if (isMagnet()) return side == null || side == facing() || side == facing().getOpposite();
        if(isElectricTransformer())return side==null||(inverted?side==facing():side!=facing());
        if (isRotationTransformer()) return side == null || side == rotationOutputFace();
        return side == null || side == facing();
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isOriginalRotaryConverter()) return energyType == spec.inType() ? spec.inputRate() : 0;
        return acceptsEnergyInput() && energyType == spec.inType()
                ? ((isRotationTransformer() || isElectricTransformer()) ? rotationInputRecommended() : inRate()) : 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isOriginalRotaryConverter()) return energyType == spec.outType() ? spec.outputRate() : 0;
        return spec != null && energyType == spec.outType() ? outRate() : 0;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if(isOriginalRotaryConverter())return energyType==spec.inType()?com.gregtech.gregtech.content.energy.OriginalRotaryConverter.inputMinimum(spec):0;
        if(isBatteryBox())return energyType==spec.inType()?com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.batteryInputMinimum(inRate()):0;
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.inType()) return super.getEnergySizeInputMin(energyType, side);
        if (!inverted) return spec.inputRate() <= 16 ? 1 : spec.inputRate() / 2;
        long originalOutputMin = Math.max(1, spec.outputRate() / 2);
        return originalOutputMin <= 8 ? 1 : originalOutputMin;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        if(isOriginalRotaryConverter())return energyType==spec.inType()?spec.inputRate()*2:0;
        return (isRotationTransformer() || isElectricTransformer()) && energyType == spec.inType()
                ? rotationInputMaximum() : super.getEnergySizeInputMax(energyType, side);
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if(isOriginalRotaryConverter())return energyType==spec.outType()?spec.outputRate()/2:0;
        if (isSolar()) return energyType == spec.outType() ? spec.outputRate() / 8 : 0;
        if (isBatteryBox()) return energyType==spec.outType()?outRate():0;
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.outType()) return super.getEnergySizeOutputMin(energyType, side);
        return inverted ? spec.inputRate() * 3 / 4 : Math.max(1, spec.outputRate() / 2);
    }

    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type, @Nullable Direction side) {
        if (isOriginalRotaryConverter()) return type == spec.outType() ? spec.outputRate() * 2 : 0;
        if (isSolar()) return type == spec.outType() ? spec.outputRate() : 0;
        return isBatteryBox() ? (type==spec.outType()?outRate():0) : super.getEnergySizeOutputMax(type,side);
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (isOriginalRotaryConverter()) return 0; // Source Root: push-only, discovery is via simulated injection.
        if (isBatteryBox()) {
            if(size==Long.MIN_VALUE || !isEnergyAcceptingFrom(energyType,side,false))return 0;
            return batteryEnergy.inject(Math.abs(size),Long.MAX_VALUE,false);
        }
        long magnitude = com.gregtech.gregtech.api.energy.EnergyPackets.magnitude(size, energyType == GregTechTags.Energy.RU);
        if (!acceptsEnergyInput() || energyType != spec.inType() || magnitude == 0) return 0;
        if ((isMagnet() || isElectricTransformer()) && !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        if ((isRotationTransformer() || isElectricTransformer()) && !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        return Math.max(0, capacity() - buffer) / magnitude;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (isSolar()) return isEnergyEmittingTo(energyType, side, false) ? solarEnergy.energy() : 0;
        // GT6 energy converters emit during their own tick. The inherited
        // TileEntityBase01Root.doExtract() returns zero for pull requests.
        if (isOriginalRotaryConverter() || isSteamConverter() || isRotationTransformer() || isElectricTransformer() || isMagnet() || isBatteryBox()) return 0;
        long magnitude = size;
        if (spec == null || energyType != spec.outType() || magnitude <= 0) return 0;
        long packets = buffer / costPerPacket();
        return Math.min(packets, (packets * outRate()) / magnitude);
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (isOriginalRotaryConverter()) {
            if (!isEnergyAcceptingFrom(energyType, side, false)) return 0;
            var step = rotaryConverter.inject(buffer, size, amount, doInject);
            if (doInject) {
                buffer = step.energy();
                if (step.overloaded()) rotaryOverload(Math.abs(size), energyType);
                if (step.consumed() > 0) setChanged();
            }
            return step.consumed();
        }
        if(isBatteryBox()) {
            if(size==Long.MIN_VALUE || size==0 || amount<=0 || !isEnergyAcceptingFrom(energyType,side,false) || !batteryEnergy.canReceive())return 0;
            long packet=Math.abs(size);
            if(packet>getEnergySizeInputMax(energyType,side)) {
                if(doInject)batteryOvervoltage(packet);
                return amount;
            }
            long used=batteryEnergy.inject(packet,amount,doInject);
            if(doInject&&used>0)setChanged();
            return used;
        }
        if (isSteamConverter()) {
            if (amount <= 0 || size == 0 || size == Long.MIN_VALUE || !isEnergyAcceptingFrom(energyType, side, false)) return 0;
            long magnitude = Math.abs(size);
            if (magnitude > spec.inputRate() * 2) { if (doInject) magnetOverload(magnitude); return amount; }
            long space = Math.max(0, capacity() - buffer);
            long accepted = Math.min(amount, space / magnitude + (space % magnitude == 0 ? 0 : 1));
            if (doInject && accepted > 0) { buffer += magnitude * accepted; setChanged(); }
            return accepted;
        }
        long magnitude = com.gregtech.gregtech.api.energy.EnergyPackets.magnitude(size, energyType == GregTechTags.Energy.RU);
        if (!acceptsEnergyInput() || energyType != spec.inType() || amount <= 0 || magnitude == 0) return 0;
        if ((isMagnet() || isElectricTransformer()) && !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        if ((isMagnet() || isElectricTransformer()) && magnitude > getEnergySizeInputMax(energyType, side)) {
            if (doInject) magnetOverload(magnitude);
            return amount;
        }
        if ((isRotationTransformer() || isElectricTransformer()) && !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        long space = Math.max(0, capacity() - buffer);
        long accepted = com.gregtech.gregtech.api.energy.EnergyPackets.fitting(amount, space, magnitude);
        if (doInject && accepted > 0) {
            if (isRotationTransformer()) rotationNegativeInput = size < 0;
            buffer += accepted * magnitude;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long doExtract(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doExtract) {
        if (isSolar()) {
            if (!isEnergyEmittingTo(energyType, side, false)) return 0;
            long extracted = solarEnergy.extract(size, amount, doExtract);
            if (doExtract && extracted > 0) setChanged();
            return extracted;
        }
        if (isOriginalRotaryConverter() || isSteamConverter() || isRotationTransformer() || isElectricTransformer() || isMagnet() || isBatteryBox()) return 0;
        long magnitude = size;
        if (spec == null || energyType != spec.outType() || amount <= 0 || magnitude <= 0) return 0;
        long cost = costPerPacket();
        long availablePackets = buffer / cost;
        long requestedPackets = (amount * magnitude) / Math.max(1, outRate());
        long extracted = Math.min(availablePackets, Math.max(0, requestedPackets));
        long extractedAmount = (extracted * outRate()) / magnitude;
        if (doExtract && extractedAmount > 0) {
            buffer -= extracted * cost;
            setChanged();
        }
        return extractedAmount;
    }

    @Override public java.util.Collection<GregTechTags.Tag> getEnergyCapacitorTypes(@Nullable Direction side) {
        return isSolar()?List.of():isBatteryBox()?List.of(GregTechTags.Energy.EU):super.getEnergyCapacitorTypes(side);
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isSolar()) return 0; // Source solar panels do not implement an energy capacitor.
        if(isBatteryBox())return energyType==spec.inType()?batteryEnergy.stored():0;
        return spec != null && (energyType == spec.inType() || energyType == spec.outType()) ? buffer : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isSolar()) return 0; // Source solar panels do not implement an energy capacitor.
        if(isBatteryBox())return energyType==spec.inType()?batteryEnergy.capacity():0;
        return spec != null ? capacity() : 0;
    }

    private void batteryOvervoltage(long packet) {
        if(level==null||level.isClientSide)return;
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        if(com.gregtech.gregtech.GregTechConfig.machineOvervoltageExplosions()) {
            dropContents();
            level.removeBlock(worldPosition,false);
            float power=(float)Math.min(8,Math.max(1,Math.log(packet/8.0)/Math.log(4)));
            level.explode(null,worldPosition.getX()+0.5,worldPosition.getY()+0.5,worldPosition.getZ()+0.5,
                    power,Level.ExplosionInteraction.BLOCK);
        }
    }
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side) {
        if (isOriginalRotaryConverter()) return new com.gregtech.gregtech.api.machine.MachineControl() {
            @Override public boolean available(){return !isRemoved();}
            @Override public boolean supportsProgress(){return false;}
            @Override public boolean supportsMode(){return isOriginalMotor();}
            @Override public int mode(){return rotaryConverter.mode();}
            @Override public int setMode(int value){if(isOriginalMotor())rotaryConverter.mode(value);setChanged();return mode();}
            @Override public boolean enabled(){return !converterStopped;}
            @Override public boolean setEnabled(boolean value){converterStopped=!value;setChanged();return value;}
            @Override public boolean running(){return rotaryConverter.possible();}
            @Override public boolean active(){return rotaryConverter.emitted();}
            @Override public long progress(){return 0;}
            @Override public long progressMax(){return 0;}
        };
        if (isSolar()) return new com.gregtech.gregtech.api.machine.MachineControl() {
            @Override public boolean supportsProgress() { return false; }
            @Override public boolean enabled() { return solarEnergy.enabled(); }
            @Override public boolean setEnabled(boolean value) { solarEnergy.enabled(value); setChanged(); return value; }
            @Override public boolean running() { return solarEnergy.active(); }
            @Override public boolean active() { return solarEnergy.emitting(); }
            @Override public long progress() { return 0; }
            @Override public long progressMax() { return 0; }
        };
        if(isElectricTransformer())return transformerControl;
        if (isSteamConverter()) return new com.gregtech.gregtech.api.machine.MachineControl() {
            @Override public boolean enabled() { return !converterStopped; }
            @Override public boolean setEnabled(boolean value) { converterStopped = !value; setChanged(); return value; }
            @Override public boolean running() { return !converterStopped && (buffer > 0 || turbinePending > 0); }
            @Override public boolean active() { return running(); }
            @Override public long progress() { return buffer; }
            @Override public long progressMax() { return capacity(); }
        };
        if(!isBatteryBox())return new com.gregtech.gregtech.api.machine.MachineControl(){
            public boolean available(){return !isRemoved();}
            public boolean supportsMode(){return isMagnet();}
            public int mode(){return isMagnet()?magnetMode:0;}
            public int setMode(int value){if(isMagnet()){magnetMode=(byte)(value&15);setChanged();}return mode();}
            public boolean enabled(){return isMagnet()?!magnetStopped:!converterStopped;}
            public boolean setEnabled(boolean value){if(isMagnet())magnetStopped=!value;else converterStopped=!value;setChanged();return value;}
            public boolean running(){return enabled()&&stored()>0;}
            public boolean active(){return running();}
            public long progress(){return stored();}
            public long progressMax(){return capacity();}
        };
        return new com.gregtech.gregtech.api.machine.MachineControl() {
            @Override public boolean supportsMode(){return true;}
            @Override public int mode(){return batteryEnergy.mode();}
            @Override public int setMode(int mode){batteryEnergy.mode(mode);setChanged();return batteryEnergy.mode();}
            @Override public boolean enabled(){return batteryEnergy.enabled();}
            @Override public boolean setEnabled(boolean value){batteryEnergy.enabled(value);setChanged();return value;}
            @Override public boolean running(){return batteryEnergy.running();}
            @Override public boolean active(){return batteryEnergy.emitted();}
            @Override public long progress(){return batteryEnergy.buffer();}
            @Override public long progressMax(){return batteryEnergy.bufferCapacity();}
        };
    }

    // ── Forge Energy capability (flux motors accept RF on input faces) ──────

    private net.neoforged.neoforge.energy.IEnergyStorage magnetFeStorage(Direction side) {
        return new net.neoforged.neoforge.energy.IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) {
                return isEnergyAcceptingFrom(GregTechTags.Energy.RF, side, false)
                        ? feStorage.receiveEnergy(amount, simulate) : 0;
            }
            @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
            @Override public int getEnergyStored() { return feStorage.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return feStorage.getMaxEnergyStored(); }
            @Override public boolean canExtract() { return false; }
            @Override public boolean canReceive() {
                return isEnergyAcceptingFrom(GregTechTags.Energy.RF, side, false);
            }
        };
    }

    private final net.neoforged.neoforge.energy.IEnergyStorage feStorage =
            new net.neoforged.neoforge.energy.IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (isOriginalRotaryConverter()) return (int) doEnergyInjection(GregTechTags.Energy.RF, null, 1, maxReceive, !simulate);
            if (isMagnet() && magnetStopped) return 0;
            long space = Math.max(0, capacity() - buffer);
            int accepted = (int) Math.min(maxReceive, space);
            if (!simulate && accepted > 0) {
                buffer += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, buffer); }
        @Override public int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, capacity()); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return isOriginalRotaryConverter() ? !converterStopped : !isMagnet() || !magnetStopped; }
    };

    public net.neoforged.neoforge.energy.IEnergyStorage energyCapability(@Nullable Direction side){
        if(spec==null||spec.inType()!=GregTechTags.Energy.RF||side!=null&&(side==facing()||isMagnet()&&side==facing().getOpposite()))return null;
        return isMagnet()&&side!=null?magnetFeStorage(side):feStorage;
    }
    public net.neoforged.neoforge.items.IItemHandler itemCapability(@Nullable Direction side){return !isBatteryBox()?null:side==null?batterySlots:sidedBatteryInventory(side);}
    public IFluidHandler fluidCapability(@Nullable Direction side){return steamTank!=null&&(side==null||side==facing().getOpposite())?side==null?turbineInlet():com.gregtech.gregtech.content.cover.ComponentCoverAccess.fluids(this,side,turbineInlet()):null;}

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putLong("gt.buffer", isSolar() ? solarEnergy.energy() : buffer);
        if (isSolar()) {
            tag.putBoolean("gt.solar_stopped", !solarEnergy.enabled());
            tag.putBoolean("gt.solar_active", solarEnergy.active());
            tag.putBoolean("gt.solar_emitting", solarEnergy.emitting());
        }
        if(isBatteryBox()){
            batteryEnergy.save(tag);

        }
        batteryCovers.forEach((side,stack)->tag.put("gt.battery_cover_"+side.ordinal(),stack.saveOptional(lookup)));
        if(isElectricTransformer())transformerControl.save(tag);
        tag.putBoolean("gt.inverted", inverted);
        if (isMagnet()) {
            tag.putBoolean("gt.magnet_stopped", magnetStopped);
            tag.putByte("gt.magnet_mode", magnetMode);
            tag.putInt("gt.magnet_overloads", magnetOverloads);
        }
        if (isRotationTransformer()) tag.putBoolean("gt.rotation_negative_input", rotationNegativeInput);
        tag.putLong("gt.turbine_pending", turbinePending);
        tag.putLong("gt.steam_remainder", steamRemainder);
        tag.putBoolean("gt.converter_stopped", converterStopped);
        if (isOriginalRotaryConverter()) {
            var rotary = rotaryConverter.snapshot();
            tag.putByte("gt.mode", (byte) rotary.mode());
            tag.putBoolean("gt.reversed", rotary.counterClockwise());
            tag.putBoolean("gt.active.energy", rotary.emitted());
            tag.putBoolean("gt.can.energy", rotary.possible());
            if (isOriginalMotor()) tag.putBoolean("gt.visual", rotary.fast());
        }
        if (!rotor.isEmpty()) tag.put("gt.rotor", rotor.saveOptional(lookup));
        if (!batteries.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (var b : batteries) list.add(b.saveOptional(lookup));
            tag.put("gt.batteries", list);
        }
        if (steamTank != null) {
            CompoundTag t = new CompoundTag();
            steamTank.writeToNBT(t,lookup);
            tag.put("gt.steam", t);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        buffer = tag.getLong("gt.buffer");
        if (isSolar()) {
            solarEnergy.restore(buffer, tag.getBoolean("gt.solar_active"), tag.getBoolean("gt.solar_emitting"), tag.getBoolean("gt.solar_stopped"));
            buffer = 0;
        }
        if(isBatteryBox()){
            batteryEnergy.load(tag);
        }
        if(isElectricTransformer())transformerControl.load(tag);
        {
            batteryCovers.clear();
            for(var side:Direction.values()){
                String key="gt.battery_cover_"+side.ordinal();
                if(tag.contains(key))batteryCovers.put(side,net.minecraft.world.item.ItemStack.parseOptional(lookup,tag.getCompound(key)));
            }
            panels().loaded();
        }
        inverted = tag.getBoolean("gt.inverted");
        if (isMagnet()) {
            magnetStopped = tag.getBoolean("gt.magnet_stopped");
            magnetMode = (byte) (tag.getByte("gt.magnet_mode") & 15);
            magnetOverloads = Math.max(0, tag.getInt("gt.magnet_overloads"));
        }
        if (isRotationTransformer()) rotationNegativeInput = tag.getBoolean("gt.rotation_negative_input");
        turbinePending = Math.max(0, tag.getLong("gt.turbine_pending"));
        steamRemainder = Math.floorMod(tag.getLong("gt.steam_remainder"), com.gregtech.gregtech.api.machine.BoilerSpec.STEAM_PER_WATER);
        converterStopped = tag.getBoolean("gt.converter_stopped");
        if (isOriginalRotaryConverter())
            rotaryConverter.restore(new com.gregtech.gregtech.content.energy.OriginalRotaryConverter.Snapshot(
                    tag.getByte("gt.mode"), tag.getBoolean("gt.reversed"), false,
                    tag.getBoolean("gt.can.energy"), tag.getBoolean("gt.active.energy"), tag.getBoolean("gt.visual")));
        rotor = tag.contains("gt.rotor")
                ? net.minecraft.world.item.ItemStack.parseOptional(lookup,tag.getCompound("gt.rotor"))
                : net.minecraft.world.item.ItemStack.EMPTY;
        batteries.clear();
        if (tag.contains("gt.batteries")) {
            net.minecraft.nbt.ListTag list = tag.getList("gt.batteries", 10);
            for (int i = 0; i < Math.min(batteries.size(), list.size()); i++) {
                batteries.set(i, net.minecraft.world.item.ItemStack.parseOptional(lookup,list.getCompound(i)).copyWithCount(1));
            }
        }
        if (steamTank != null && tag.contains("gt.steam")) {
            steamTank.readFromNBT(tag.getCompound("gt.steam"),lookup);
        }
    }
    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        if(!com.gregtech.gregtech.content.cover.CoverDrops.retained(this))for(var cover:batteryCovers.values())com.gregtech.gregtech.api.inventory.BlockContents.drop(this,cover);
        if(!batteryCovers.isEmpty())batteryCovers.clear();
        for (var battery : batteries) com.gregtech.gregtech.api.inventory.BlockContents.drop(this, battery);
        batteries.clear();
        com.gregtech.gregtech.api.inventory.BlockContents.drop(this, rotor); rotor = net.minecraft.world.item.ItemStack.EMPTY;
        setChanged();
    }
}
