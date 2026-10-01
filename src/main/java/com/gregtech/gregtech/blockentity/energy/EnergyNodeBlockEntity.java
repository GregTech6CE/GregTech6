package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 energy-net node: motors/dynamos/transformers (energy conversion), steam
 * turbines (steam fluid → RU), solar panels (sun → EU) and battery boxes /
 * storage cabinets (EU buffer). Inputs on every face except the front; output
 * packets of {@code outputRate} leave through the front (FACING).
 */
public class EnergyNodeBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.inventory.BlockContents, com.gregtech.gregtech.api.machine.MachineControl.Provider, com.gregtech.gregtech.content.cover.PanelCoverHost {

    private java.util.Map<Direction,net.minecraft.world.item.ItemStack> batteryCovers=java.util.Map.of();
    private com.gregtech.gregtech.content.cover.PanelCoverRuntime batteryPanels;
    private com.gregtech.gregtech.content.energy.ElectricTransformerControl transformerControl;
    public boolean hasControlPanels(){return isBatteryBox()||isElectricTransformer();}
    @Override public net.minecraft.world.item.ItemStack getCover(Direction side){return batteryCovers.getOrDefault(side,net.minecraft.world.item.ItemStack.EMPTY);}
    @Override public com.gregtech.gregtech.content.cover.PanelCoverRuntime panels(){
        if(batteryPanels==null)batteryPanels=new com.gregtech.gregtech.content.cover.PanelCoverRuntime(this);
        return batteryPanels;
    }
    @Override public boolean coverSupportsPossible(){return hasControlPanels();}
    @Override public boolean coverPossible(Direction side){return isBatteryBox()?batteryEnergy.buffer()>spec.outputRate():isElectricTransformer()&&transformerControl.running();}
    @Override public boolean attachCover(Direction side,net.minecraft.world.item.ItemStack stack){
        var panel=com.gregtech.gregtech.content.cover.PanelCover.of(stack);
        if(!hasControlPanels()||panel==null||(isElectricTransformer()&&panel==com.gregtech.gregtech.content.cover.PanelCover.SHUTTER)
                ||!getCover(side).isEmpty()||!panels().canAttach(side,stack))return false;
        batteryCovers.put(side,stack.copyWithCount(1));panels().attached(side);return true;
    }
    @Override public net.minecraft.world.item.ItemStack removeCover(Direction side){
        if(!hasControlPanels())return net.minecraft.world.item.ItemStack.EMPTY;
        var removed=batteryCovers.remove(side);
        if(removed==null)return net.minecraft.world.item.ItemStack.EMPTY;
        var panel=com.gregtech.gregtech.content.cover.PanelCover.of(removed);
        if(panel!=null&&panel.selector())machineControl(side).setMode(0);
        panels().changed();panels().afterTick();return removed;
    }

    private EnergyNodeSpec spec;
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
    /** Turbines: installed rotor (required to run) and its accumulated wear. */
    private net.minecraft.world.item.ItemStack rotor = net.minecraft.world.item.ItemStack.EMPTY;
    private long rotorDamage;
    /** Approximate GT6 rotor service life in emitted packets. */
    private static final long ROTOR_LIFE = 153_600;
    @Nullable
    private FluidTankGT steamTank;
    @Nullable
    private LazyOptional<IFluidHandler> steamCap;

    public EnergyNodeBlockEntity(BlockPos pos, BlockState state) {
        this(com.gregtech.gregtech.registry.GTBlockEntities.ENERGY_NODE.get(), pos, state);
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
        if(level!=null&&!level.isClientSide&&!isRemoved()&&getBlockState().getBlock() instanceof com.gregtech.gregtech.block.energy.ChemicalBatteryBlock){
            var replacement=new ChemicalBatteryBlockEntity(worldPosition,getBlockState());
            var tag=new CompoundTag();tag.putLong(com.gregtech.gregtech.item.ChemicalBatteryItem.CHARGE,buffer);
            replacement.load(tag);level.setBlockEntity(replacement);replacement.setChanged();
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        }
    }

    public void setSpec(EnergyNodeSpec spec) {
        this.spec = spec;
        if (isBatteryBox()) {
            batteries=net.minecraft.core.NonNullList.withSize(spec.batterySlots(),net.minecraft.world.item.ItemStack.EMPTY);
            batteryEnergy = new com.gregtech.gregtech.content.energy.BatteryBoxEnergy(batteries, spec.inputRate());
            if(!(batteryCovers instanceof java.util.EnumMap))batteryCovers=new java.util.EnumMap<>(Direction.class);
        }
        if(isElectricTransformer()){transformerControl=new com.gregtech.gregtech.content.energy.ElectricTransformerControl(this::setChanged);if(!(batteryCovers instanceof java.util.EnumMap))batteryCovers=new java.util.EnumMap<>(Direction.class);}
        if (spec.kind() == EnergyNodeSpec.Kind.TURBINE) {
            steamTank = new FluidTankGT(Math.max(16000, spec.inputRate() * 64)).setOnChanged(this::setChanged);
        }
    }

    public EnergyNodeSpec spec() { return spec; }
    public long stored() { return isBatteryBox() ? batteryEnergy.buffer() : buffer; }
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

    // ── Turbine rotors (GT6: turbines need a rotor part that wears out) ────

    public boolean isTurbine() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.TURBINE;
    }

    public static boolean isRotorItem(net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof com.gregtech.gregtech.item.MaterialItem mat
                && mat.getPrefix() == com.gregtech.gregtech.data.MaterialPrefix.rotor;
    }

    public boolean hasRotor() { return !rotor.isEmpty(); }

    public boolean installRotor(net.minecraft.world.item.ItemStack stack) {
        if (!isTurbine() || !rotor.isEmpty() || !isRotorItem(stack)) return false;
        rotor = stack.copyWithCount(1);
        rotorDamage = 0;
        setChanged();
        return true;
    }

    public net.minecraft.world.item.ItemStack removeRotor() {
        net.minecraft.world.item.ItemStack out = rotor;
        rotor = net.minecraft.world.item.ItemStack.EMPTY;
        rotorDamage = 0;
        setChanged();
        return out;
    }

    /** Wear the rotor by the emitted packet count; destroys it at end of life. */
    private void wearRotor(long packets) {
        if (rotor.isEmpty()) return;
        rotorDamage += packets;
        if (rotorDamage >= ROTOR_LIFE) {
            rotor = net.minecraft.world.item.ItemStack.EMPTY;
            rotorDamage = 0;
            if (level != null) {
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        setChanged();
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
        return spec.capacity();
    }

    // ── Battery box GUI (GT6 TileEntityBase10EnergyBatBox opens one) ────────

    /** Fixed slot identities: a vacant slot never shifts another stack. */
    private final net.minecraftforge.items.IItemHandlerModifiable batterySlots =
            new net.minecraftforge.items.IItemHandlerModifiable() {
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
    public net.minecraftforge.items.IItemHandlerModifiable batteryInventory() { return batterySlots; }
    @Nullable private LazyOptional<net.minecraftforge.items.IItemHandler> batteryCap;

    private java.util.Map<Direction,LazyOptional<net.minecraftforge.items.IItemHandler>> sidedBatteryCaps;
    private net.minecraftforge.items.IItemHandler sidedBatteryInventory(Direction side) {
        return new net.minecraftforge.items.IItemHandler() {
            private boolean blocked(){return panels().shuttered(side);}
            @Override public int getSlots(){return batterySlots.getSlots();}
            @Override public net.minecraft.world.item.ItemStack getStackInSlot(int slot){return batterySlots.getStackInSlot(slot);}
            @Override public net.minecraft.world.item.ItemStack insertItem(int slot,net.minecraft.world.item.ItemStack stack,boolean simulate){
                return blocked()?stack:batterySlots.insertItem(slot,stack,simulate);
            }
            @Override public net.minecraft.world.item.ItemStack extractItem(int slot,int amount,boolean simulate){
                return blocked()?net.minecraft.world.item.ItemStack.EMPTY:batterySlots.extractItem(slot,amount,simulate);
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
        if (be.isBatteryBox()) {
            be.panels().beforeTick();
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
        if (be.isMagnet()) {
            be.tickMagnet();
            if (level.getGameTime() % 600 == 5 && be.magnetOverloads > 0) be.magnetOverloads--;
            return;
        }
        switch (be.spec.kind()) {
            case TURBINE -> be.tickTurbine();
            case SOLAR -> be.tickSolar(level, pos);
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
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }

    private void tickTurbine() {
        if (steamTank == null || spec.inputRate() <= 0 || rotor.isEmpty()) return;
        long space = capacity() - buffer;
        long packetsBySteam = steamTank.getAmount() / spec.inputRate();
        long packetsBySpace = space / spec.inputRate();
        long packets = Math.min(8, Math.min(packetsBySteam, packetsBySpace));
        if (packets <= 0) return;
        steamTank.drain((int) (packets * spec.inputRate()), IFluidHandler.FluidAction.EXECUTE);
        buffer += packets * spec.inputRate();
        setChanged();
    }

    private void tickSolar(Level level, BlockPos pos) {
        if (!level.isDay() || level.isRaining()) return;
        if (!level.canSeeSky(pos.above())) return;
        buffer = Math.min(capacity(), buffer + spec.outputRate());
        setChanged();
    }

    private void emitOutput() {
        if (level == null) return;
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
            if (isTurbine()) wearRotor(emitted);
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
        var storage = be.getCapability(ForgeCapabilities.ENERGY, facing().getOpposite())
                .resolve().orElse(null);
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
        if (isMagnet()) return (theoretical || !magnetStopped)
                && (side == null || (side != facing() && side != facing().getOpposite()));
        if(isElectricTransformer())return (theoretical||transformerControl.accepts())&&(side==null||(inverted?side!=facing():side==facing()));
        if (isRotationTransformer()) return side == null || side == rotationInputFace();
        return side == null || side != facing();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (spec == null || energyType != spec.outType()) return false;
        if (isBatteryBox()) return (theoretical || batteryEnergy.enabled()) && (side == null || side == facing());
        if (isMagnet()) return side == null || side == facing() || side == facing().getOpposite();
        if(isElectricTransformer())return side==null||(inverted?side==facing():side!=facing());
        if (isRotationTransformer()) return side == null || side == rotationOutputFace();
        return side == null || side == facing();
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return acceptsEnergyInput() && energyType == spec.inType()
                ? ((isRotationTransformer() || isElectricTransformer()) ? rotationInputRecommended() : inRate()) : 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return spec != null && energyType == spec.outType() ? outRate() : 0;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if(isBatteryBox())return energyType==spec.inType()?(inRate()<=16?1:inRate()/2):0;
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.inType()) return super.getEnergySizeInputMin(energyType, side);
        if (!inverted) return spec.inputRate() <= 16 ? 1 : spec.inputRate() / 2;
        long originalOutputMin = Math.max(1, spec.outputRate() / 2);
        return originalOutputMin <= 8 ? 1 : originalOutputMin;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return (isRotationTransformer() || isElectricTransformer()) && energyType == spec.inType()
                ? rotationInputMaximum() : super.getEnergySizeInputMax(energyType, side);
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isBatteryBox()) return energyType==spec.outType()?outRate():0;
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.outType()) return super.getEnergySizeOutputMin(energyType, side);
        return inverted ? spec.inputRate() * 3 / 4 : Math.max(1, spec.outputRate() / 2);
    }

    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type, @Nullable Direction side) {
        return isBatteryBox() ? (type==spec.outType()?outRate():0) : super.getEnergySizeOutputMax(type,side);
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
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
        // GT6 energy converters emit during their own tick. The inherited
        // TileEntityBase01Root.doExtract() returns zero for pull requests.
        if (isRotationTransformer() || isElectricTransformer() || isMagnet() || isBatteryBox()) return 0;
        long magnitude = size;
        if (spec == null || energyType != spec.outType() || magnitude <= 0) return 0;
        long packets = buffer / costPerPacket();
        return Math.min(packets, (packets * outRate()) / magnitude);
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
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
        if (isRotationTransformer() || isElectricTransformer() || isMagnet() || isBatteryBox()) return 0;
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
        return isBatteryBox()?List.of(GregTechTags.Energy.EU):super.getEnergyCapacitorTypes(side);
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag energyType, @Nullable Direction side) {
        if(isBatteryBox())return energyType==spec.inType()?batteryEnergy.stored():0;
        return spec != null && (energyType == spec.inType() || energyType == spec.outType()) ? buffer : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable Direction side) {
        if(isBatteryBox())return energyType==spec.inType()?batteryEnergy.capacity():0;
        return spec != null ? capacity() : 0;
    }

    private void batteryOvervoltage(long packet) {
        if(level==null||level.isClientSide)return;
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
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
        if(isElectricTransformer())return transformerControl;
        if(!isBatteryBox())return null;
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

    @Nullable
    private LazyOptional<net.minecraftforge.energy.IEnergyStorage> feCap;
    private final java.util.EnumMap<Direction, LazyOptional<net.minecraftforge.energy.IEnergyStorage>> magnetFeCaps =
            new java.util.EnumMap<>(Direction.class);

    private net.minecraftforge.energy.IEnergyStorage magnetFeStorage(Direction side) {
        return new net.minecraftforge.energy.IEnergyStorage() {
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

    private final net.minecraftforge.energy.IEnergyStorage feStorage =
            new net.minecraftforge.energy.IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
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
        @Override public boolean canReceive() { return !isMagnet() || !magnetStopped; }
    };

    // ── Fluid capability (turbines) ─────────────────────────────────────────

    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && spec != null && spec.inType() == GregTechTags.Energy.RF
                && (side == null || (side != facing() && (!isMagnet() || side != facing().getOpposite())))) {
            if (isMagnet() && side != null) {
                return magnetFeCaps.computeIfAbsent(side,
                        key -> LazyOptional.of(() -> magnetFeStorage(key))).cast();
            }
            if (feCap == null || !feCap.isPresent()) {
                feCap = LazyOptional.of(() -> feStorage);
            }
            return feCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER && isBatteryBox()) {
            if(side!=null){
                if(sidedBatteryCaps==null)sidedBatteryCaps=new java.util.EnumMap<>(Direction.class);
                return sidedBatteryCaps.computeIfAbsent(side,key->LazyOptional.of(()->sidedBatteryInventory(key))).cast();
            }
            if (batteryCap == null || !batteryCap.isPresent()) batteryCap = LazyOptional.of(() -> batterySlots);
            return batteryCap.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER && steamTank != null
                && (side == null || side != facing())) {
            if (steamCap == null || !steamCap.isPresent()) {
                steamCap = LazyOptional.of(() -> steamTank);
            }
            return steamCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (batteryCap != null) { batteryCap.invalidate(); batteryCap = null; }
        if(sidedBatteryCaps!=null){sidedBatteryCaps.values().forEach(LazyOptional::invalidate);sidedBatteryCaps.clear();}
        if (steamCap != null) { steamCap.invalidate(); steamCap = null; }
        if (feCap != null) { feCap.invalidate(); feCap = null; }
        magnetFeCaps.values().forEach(LazyOptional::invalidate);
        magnetFeCaps.clear();
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("gt.buffer", buffer);
        if(isBatteryBox()){
            batteryEnergy.save(tag);

        }
        if(hasControlPanels())batteryCovers.forEach((side,stack)->tag.put("gt.battery_cover_"+side.ordinal(),stack.save(new CompoundTag())));
        if(isElectricTransformer())transformerControl.save(tag);
        tag.putBoolean("gt.inverted", inverted);
        if (isMagnet()) {
            tag.putBoolean("gt.magnet_stopped", magnetStopped);
            tag.putByte("gt.magnet_mode", magnetMode);
            tag.putInt("gt.magnet_overloads", magnetOverloads);
        }
        if (isRotationTransformer()) tag.putBoolean("gt.rotation_negative_input", rotationNegativeInput);
        tag.putLong("gt.rotor_damage", rotorDamage);
        if (!rotor.isEmpty()) tag.put("gt.rotor", rotor.save(new CompoundTag()));
        if (!batteries.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (var b : batteries) list.add(b.save(new CompoundTag()));
            tag.put("gt.batteries", list);
        }
        if (steamTank != null) {
            CompoundTag t = new CompoundTag();
            steamTank.writeToNBT(t);
            tag.put("gt.steam", t);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer = tag.getLong("gt.buffer");
        if(isBatteryBox()){
            batteryEnergy.load(tag);
        }
        if(isElectricTransformer())transformerControl.load(tag);
        if(hasControlPanels()){
            batteryCovers.clear();
            for(var side:Direction.values()){
                String key="gt.battery_cover_"+side.ordinal();
                if(tag.contains(key))batteryCovers.put(side,net.minecraft.world.item.ItemStack.of(tag.getCompound(key)));
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
        rotorDamage = tag.getLong("gt.rotor_damage");
        rotor = tag.contains("gt.rotor")
                ? net.minecraft.world.item.ItemStack.of(tag.getCompound("gt.rotor"))
                : net.minecraft.world.item.ItemStack.EMPTY;
        batteries.clear();
        if (tag.contains("gt.batteries")) {
            net.minecraft.nbt.ListTag list = tag.getList("gt.batteries", 10);
            for (int i = 0; i < Math.min(batteries.size(), list.size()); i++) {
                batteries.set(i, net.minecraft.world.item.ItemStack.of(list.getCompound(i)).copyWithCount(1));
            }
        }
        if (steamTank != null && tag.contains("gt.steam")) {
            steamTank.readFromNBT(tag.getCompound("gt.steam"));
        }
    }
    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        for(var cover:batteryCovers.values())com.gregtech.gregtech.api.inventory.BlockContents.drop(this,cover);
        if(!batteryCovers.isEmpty())batteryCovers.clear();
        for (var battery : batteries) com.gregtech.gregtech.api.inventory.BlockContents.drop(this, battery);
        batteries.clear();
        com.gregtech.gregtech.api.inventory.BlockContents.drop(this, rotor); rotor = net.minecraft.world.item.ItemStack.EMPTY;
        setChanged();
    }
}
