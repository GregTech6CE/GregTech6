package gregtech.content.blockentity.machine;

import gregtech.engine.machine.crucible.CrucibleMaterialStack;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.engine.multiblock.MultiblockLayout;
import gregtech.engine.multiblock.MultiblockPortOwner;
import gregtech.engine.multiblock.PartBindings;
import gregtech.content.block.machine.LargeCrucibleControllerBlock;
import gregtech.content.material.Materials;
import gregtech.content.multiblock.LargeCrucibleSpecs;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GT6 MultiTileEntityCrucible: 3x3x3 open vessel, per-material wall, shared smelting engine. */
//? if >=1.21.1 {
/*public final class LargeCrucibleControllerBlockEntity extends gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity implements gregtech.engine.multiblock.CrucibleStructureOwner, gregtech.engine.multiblock.tracking.TrackedMultiblock {
*///?} else {
public final class LargeCrucibleControllerBlockEntity extends SmeltingCrucibleBlockEntity implements MultiblockPortOwner, gregtech.engine.multiblock.tracking.TrackedMultiblock {
//?}
    public static final long MAX_AMOUNT = gregtech.content.multiblock.OriginalLargeCrucibleParameters.CAPACITY_UNITS;
    private static final long MB_UNIT = GTValues.U / 144L;
    private final LargeCrucibleSpecs.Variant variant;
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private final gregtech.engine.multiblock.tracking.StructureDefinition definition =
            gregtech.engine.multiblock.tracking.FixedStructures.declarative(this, this::structureCells, List.of());
    private final IFluidHandler fluids = new MoltenPort();
    private final IItemHandler itemInput = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return slot == 0 ? cacheHandler().getStackInSlot(0).copy() : ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? cacheHandler().insertItem(0, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return slot == 0 ? 64 : 0; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && cacheHandler().isItemValid(0, stack); }
    };
//? if >=1.21.1 {
//?} else {
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.empty();
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
//?}

    public LargeCrucibleControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.LARGE_CRUCIBLE.get(), pos, state, variantOf(state).crucible());
        this.variant = variantOf(state);
    }

    private static LargeCrucibleSpecs.Variant variantOf(BlockState state) {
        return state.getBlock() instanceof LargeCrucibleControllerBlock block
                ? block.variant() : LargeCrucibleSpecs.LEGACY;
    }

    public LargeCrucibleSpecs.Variant variant() { return variant; }

    @Override protected long maxMaterialAmount() { return MAX_AMOUNT; }
    @Override public long getMeltDownLimitK() {
        return gregtech.content.multiblock.OriginalLargeCrucibleParameters.meltDownTemperatureK(variant.material().getMeltingPoint());
    }
    @Override protected gregtech.engine.machine.crucible.CrucibleHazards.Profile hazardProfile() {
        return gregtech.engine.machine.crucible.CrucibleHazards.LARGE;
    }
    @Override protected AABB itemSuctionArea() {
        return new AABB(worldPosition.getX() - 0.5D, worldPosition.getY() + 0.125D,
                worldPosition.getZ() - 0.5D, worldPosition.getX() + 1.5D,
                worldPosition.getY() + 3.125D, worldPosition.getZ() + 1.5D);
    }

    private void release(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port) port.release(worldPosition);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) gregtech.engine.multiblock.tracking.StructureTracking.attach(this);
    }
    @Override public void onChunkUnloaded() {
        gregtech.engine.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }
    @Override public void setRemoved() {
        if (!gregtech.engine.multiblock.tracking.StructureTracking.unloading(this)) bindings.clear(this::release);
        gregtech.engine.multiblock.tracking.StructureTracking.onRemoved(this);
        super.setRemoved();
    }

    public boolean isFormedForRendering() {
        var state = getBlockState();
        var property = gregtech.content.block.machine.MultiblockPortBlock.CRUCIBLE_FORMED;
        return state.hasProperty(property) && state.getValue(property);
    }

    @Override public gregtech.engine.multiblock.tracking.StructureDefinition structureDefinition() {
        return definition;
    }
    private List<gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell> structureCells(Direction front) {
        var defs = gregtech.content.multiblock.SharedLargeCrucibleStructure.CELLS;
        int wall = variant.wallId() < 0 ? 18000 : variant.wallId();
        var cells = new java.util.ArrayList<gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell>(defs.size());
        for (var cell : defs) {
            // The source flags these cells air without changing their role; the pattern must declare AIR.
            var role = cell.air() ? MultiblockLayout.Role.AIR : MultiblockLayout.Role.valueOf(cell.role().name());
            var spec = new gregtech.content.multiblock.LargeMachineLayouts.Cell(
                    cell.x(), cell.y(), cell.z(), cell.air() ? 0 : wall, role);
            cells.add(new gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell(
                    cell.x(), cell.y(), cell.z(), spec));
        }
        return cells;
    }
    @Override public boolean applyStructure(gregtech.engine.multiblock.tracking.PatternMatch match) {
        if (level == null || isRemoved()) return false;
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var fixed : match.fixedCells()) {
            var cell = fixed.cell();
            if (cell.role() != MultiblockLayout.Role.AIR) parts.put(fixed.pos(), cell.role());
        }
        return gregtech.engine.multiblock.tracking.StructureClaims.update(this, bindings, parts,
                position -> level.getBlockEntity(position) instanceof MultiblockPortBlockEntity port
                        && port.canBind(worldPosition),
                (position, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(position)).bind(worldPosition, role),
                this::release) != gregtech.engine.multiblock.tracking.StructureClaims.Result.FAILED;
    }
    @Override public void releaseStructureClaims() {
        bindings.clear(this::release);
        gregtech.engine.multiblock.tracking.StructureClaims.cleared(this);
    }
    private boolean structureFormed;
    private boolean structureOk() {
        return structureFormed;
    }
    @Override public void structureFormedChanged(boolean formed) {
        structureFormed = formed;
        if (level == null || level.isClientSide || isRemoved()) return;
        gregtech.engine.multiblock.tracking.StructureScanProbe.pause();
        try {
            var property = gregtech.content.block.machine.MultiblockPortBlock.CRUCIBLE_FORMED;
            var state = level.getBlockState(worldPosition);
            if (state.hasProperty(property) && state.getValue(property) != formed)
                level.setBlock(worldPosition, state.setValue(property, formed), 3);
        } finally {
            gregtech.engine.multiblock.tracking.StructureScanProbe.resume();
        }
    }
    @Override public boolean isStructureOk() {
        return gregtech.engine.multiblock.tracking.StructureTracking.query(this);
    }
//? if >=1.21.1 {
    /*public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX()-1,worldPosition.getY(),worldPosition.getZ()-1,worldPosition.getX()+2,worldPosition.getY()+3,worldPosition.getZ()+2);
*///?} else {
    @Override public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
//?}
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LargeCrucibleControllerBlockEntity be) {
        if (level.isClientSide) return;
        if (be.structureOk()) {
            be.collectRainwater();
            be.tickServer();
        }
        else be.coolWithoutStructure();
    }

    /** GT6 fills an open, formed crucible every 600 server ticks during warm rain. */
    private void collectRainwater() {
        if (level == null || level.getGameTime() % 600 != 10
                || !level.isRainingAt(worldPosition.above())) return;
        var climate = level.getBiome(worldPosition).value().getModifiedClimateSettings();
        float downfall = climate.downfall();
        if (downfall <= 0 || climate.temperature() < 0.2F) return;
        long units = (GTValues.U / 100) * Math.max(1L, (long) (downfall * 100));
        if (level.isThundering()) units *= 2;
        addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Water, units)),
                SmelteryBlockEntityHelper.environmentTemperature(level, worldPosition));
    }

    @Override public IItemHandler portItems(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ITEM_FLUID_IO ? itemInput : null;
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ITEM_FLUID_IO ? fluids : null;
    }
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ENERGY_INPUT
                ? List.of(GregTechTags.Energy.HU, GregTechTags.Energy.KU, GregTechTags.Energy.CU) : List.of();
    }
    @Override public long portEnergyInputRecommended(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return role == MultiblockLayout.Role.ENERGY_INPUT && accepts(type) ? 2048 : 0;
    }
    @Override public long portEnergyInputMin(MultiblockLayout.Role role, GregTechTags.Tag type) { return accepts(type) ? 1 : 0; }
    @Override public long portEnergyInputMax(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return accepts(type) ? Long.MAX_VALUE : 0;
    }
    @Override public long portEnergyStored(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return type == GregTechTags.Energy.HU ? getEnergyBuffer() : 0;
    }
    @Override public long portEnergyCapacity(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return accepts(type) ? Long.MAX_VALUE : 0;
    }
    @Override public long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type,
                                            long size, long amount, boolean execute) {
        return role == MultiblockLayout.Role.ENERGY_INPUT ? doInject(type, null, size, amount, execute) : 0;
    }

    private static boolean accepts(GregTechTags.Tag type) {
        return type == GregTechTags.Energy.HU || type == GregTechTags.Energy.KU || type == GregTechTags.Energy.CU;
    }
    @Override public boolean isEnergyType(GregTechTags.Tag type, @Nullable Direction side, boolean emitting) {
        return !emitting && accepts(type);
    }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return List.of(GregTechTags.Energy.HU, GregTechTags.Energy.KU, GregTechTags.Energy.CU);
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return accepts(type) && (side == Direction.DOWN || side == null) && (theoretical || isStructureOk());
    }
    @Override public boolean hasEnergySurface(@Nullable Direction side) { return side == Direction.DOWN; }
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type, @Nullable Direction side) { return accepts(type) ? 1 : 0; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, @Nullable Direction side) { return accepts(type) ? 2048 : 0; }
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type, @Nullable Direction side) { return accepts(type) ? Long.MAX_VALUE : 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, @Nullable Direction side, long size) {
        return accepts(type) && size > 0 ? Long.MAX_VALUE - getEnergyBuffer() : 0;
    }
    @Override public long getEnergyStored(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.HU ? getEnergyBuffer() : 0;
    }
    @Override public long getEnergyCapacity(GregTechTags.Tag type, @Nullable Direction side) {
        return accepts(type) ? Long.MAX_VALUE : 0;
    }
    @Override public long doInject(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
        if (!accepts(type) || size <= 0 || amount <= 0 || !isStructureOk()) return 0;
        if (execute) {
            long units = size > Long.MAX_VALUE / amount ? Long.MAX_VALUE : size * amount;
            if (type == GregTechTags.Energy.KU) {
                if (level != null && level.getBlockState(worldPosition.above(3)).isAir()) {
                    long room = Math.max(0, MAX_AMOUNT - getCrucibleContentAmount());
                    long air = java.math.BigInteger.valueOf(units)
                            .multiply(java.math.BigInteger.valueOf(GTValues.U))
                            .divide(java.math.BigInteger.valueOf(1000))
                            .min(java.math.BigInteger.valueOf(room)).longValue();
                    reactWithAir(air);
                }
            } else if (type == GregTechTags.Energy.CU) {
                removeHeatEnergy(units);
            } else {
                addHeatEnergy(units);
            }
        }
        return amount;
    }

//? if >=1.21.1 {
    /*public IFluidHandler fluidCapability(Direction side){return isRemoved()?null:fluids;}
    public IItemHandler itemCapability(Direction side){return isRemoved()?null:itemInput;}
*///?} else {
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!isRemoved() && cap == ForgeCapabilities.FLUID_HANDLER) {
            if (!fluidCap.isPresent()) fluidCap = LazyOptional.of(() -> fluids);
            return fluidCap.cast();
        }
        if (!isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER) {
            if (!itemCap.isPresent()) itemCap = LazyOptional.of(() -> itemInput);
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps(); fluidCap.invalidate(); itemCap.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps(); fluidCap = LazyOptional.empty(); itemCap = LazyOptional.empty();
    }
//?}

    /** GT6 top ring's fluid bridge; 144 mB = one material unit, sharing the crucible material list. */
    private final class MoltenPort implements IFluidHandler {
        private CrucibleMaterialStack lightestMolten() {
            return getContentView().stream().filter(stack -> stack.amount > 0
                            && getTemperature() >= stack.material.getMeltingPoint())
                    .filter(stack -> liquidOf(stack.material) != null)
                    .min(java.util.Comparator.comparingDouble(stack -> stack.material.getDensity()))
                    .orElse(null);
        }
        private FluidStack liquidOf(GTMaterial material) {
            FluidStack generated = GTFluids.stack("GenMolten_" + material.getName(), 1);
            if (generated != null) return generated;
            for (var entry : RegisteredFluids.all().entrySet()) {
                if (entry.getValue().gas()) continue;
                if (gregtech.engine.fluid.FluidVisualPolicy.material(entry.getValue()).resolve() != material.resolve()) continue;
                FluidStack found = GTFluids.stack(entry.getKey(), 1);
                if (found != null) return found;
            }
            return null;
        }
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (tank != 0) return FluidStack.EMPTY;
            CrucibleMaterialStack top = lightestMolten();
            if (top == null) return FluidStack.EMPTY;
            FluidStack fluid = liquidOf(top.material);
            if (fluid == null) return FluidStack.EMPTY;
            fluid.setAmount((int) Math.min(Integer.MAX_VALUE, top.amount / MB_UNIT));
            return fluid;
        }
        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? (int) (MAX_AMOUNT / MB_UNIT) : 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) return false;
            var entry = GTFluids.entryForFluid(stack.getFluid());
            if (entry == null || entry.gas()) return false;
            GTMaterial material = gregtech.engine.fluid.FluidVisualPolicy.material(entry);
            return material.isValid() && (variant.acidProof() || !material.has(MaterialProperty.ACID));
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!isStructureOk() || !isFluidValid(0, resource)) return 0;
            GTMaterial material = gregtech.engine.fluid.FluidVisualPolicy.material(
                    GTFluids.entryForFluid(resource.getFluid()));
            long units = (long) resource.getAmount() * MB_UNIT;
            if (units > MAX_AMOUNT - getCrucibleContentAmount()) return 0;
            if (action.execute()) {
                long fluidTemperature = resource.getFluid().getFluidType().getTemperature(resource);
                addMaterialStacks(List.of(CrucibleMaterialStack.of(material, units)),
                        Math.max(fluidTemperature, material.getMeltingPoint() + 25L));
            }
            return resource.getAmount();
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack offered = getFluidInTank(0);
//? if >=1.21.1 {
            /*if (resource.isEmpty() || offered.isEmpty() || !FluidStack.isSameFluidSameComponents(offered,resource)) return FluidStack.EMPTY;
*///?} else {
            if (resource.isEmpty() || offered.isEmpty() || !offered.isFluidEqual(resource)) return FluidStack.EMPTY;
//?}
            return drain(resource.getAmount(), action);
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            if (!isStructureOk() || amount <= 0) return FluidStack.EMPTY;
            CrucibleMaterialStack top = lightestMolten();
            if (top == null) return FluidStack.EMPTY;
            FluidStack fluid = liquidOf(top.material);
            if (fluid == null) return FluidStack.EMPTY;
            int drained = (int) Math.min(amount, top.amount / MB_UNIT);
            if (drained <= 0) return FluidStack.EMPTY;
            fluid.setAmount(drained);
            if (action.execute()) takeMaterial(top.material, (long) drained * MB_UNIT);
            return fluid;
        }
    }
}
