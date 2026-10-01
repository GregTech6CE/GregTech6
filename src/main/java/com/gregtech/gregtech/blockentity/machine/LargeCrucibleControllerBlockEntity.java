package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.MultiblockPortOwner;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.multiblock.LargeCrucibleSpecs;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GT6 MultiTileEntityCrucible: 3x3x3 open vessel, per-material wall, shared smelting engine. */
public final class LargeCrucibleControllerBlockEntity extends SmeltingCrucibleBlockEntity implements MultiblockPortOwner {
    public static final long MAX_AMOUNT = 16L * 3 * 3 * 3 * GTValues.U;
    private static final long MB_UNIT = GTValues.U / 144L;
    private final LargeCrucibleSpecs.Variant variant;
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
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
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.empty();
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();

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
        return Math.round(variant.material().getMeltingPoint() * 1.10D);
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

    @Override public void setRemoved() {
        bindings.clear(this::release);
        super.setRemoved();
    }

    public boolean isFormedForRendering() {
        var state = getBlockState();
        var property = com.gregtech.gregtech.block.machine.MultiblockPortBlock.CRUCIBLE_FORMED;
        return state.hasProperty(property) && state.getValue(property);
    }

    @Override public boolean isStructureOk() {
        if (level != null && level.isClientSide) return isFormedForRendering();
        boolean formed = checkStructure();
        if (level != null && !isRemoved()) {
            var property = com.gregtech.gregtech.block.machine.MultiblockPortBlock.CRUCIBLE_FORMED;
            var state = level.getBlockState(worldPosition);
            if (state.hasProperty(property) && state.getValue(property) != formed)
                level.setBlock(worldPosition, state.setValue(property, formed), 3);
        }
        return formed;
    }

    @Override public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
    }

    private boolean checkStructure() {
        if (level == null || isRemoved() || !level.hasChunkAt(worldPosition)) return false;
        Block wall = variant.wall();
        Map<BlockPos, MultiblockLayout.Role> candidates = new LinkedHashMap<>(24);
        for (int y = 0; y < 3; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = worldPosition.offset(x, y, z);
            if (!level.hasChunkAt(pos)) { bindings.clear(this::release); return false; }
            if (x == 0 && z == 0) {
                if (y > 0 && !level.getBlockState(pos).isAir()) { bindings.clear(this::release); return false; }
                continue;
            }
            if (!level.getBlockState(pos).is(wall)) { bindings.clear(this::release); return false; }
            candidates.put(pos, y == 0 ? MultiblockLayout.Role.ENERGY_INPUT
                    : y == 1 ? MultiblockLayout.Role.CRUCIBLE : MultiblockLayout.Role.ITEM_FLUID_IO);
        }
        return bindings.update(candidates,
                pos -> level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port && port.canBind(worldPosition),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::release);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LargeCrucibleControllerBlockEntity be) {
        if (level.isClientSide) return;
        if (be.isStructureOk()) {
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
                if (com.gregtech.gregtech.api.fluid.FluidVisualPolicy.material(entry.getValue()).resolve() != material.resolve()) continue;
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
            GTMaterial material = com.gregtech.gregtech.api.fluid.FluidVisualPolicy.material(entry);
            return material.isValid() && (variant.acidProof() || !material.has(MaterialProperty.ACID));
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!isStructureOk() || !isFluidValid(0, resource)) return 0;
            GTMaterial material = com.gregtech.gregtech.api.fluid.FluidVisualPolicy.material(
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
            if (resource.isEmpty() || offered.isEmpty() || !offered.isFluidEqual(resource)) return FluidStack.EMPTY;
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
