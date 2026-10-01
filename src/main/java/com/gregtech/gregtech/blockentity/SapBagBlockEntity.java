package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.block.wood.TreeHoleBlock;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GT6's sap bag ({@code MultiTileEntitySapBag}): a bag hung on the side of a tree hole that empties
 * the hole into itself.
 *
 * <p>GT6's tile entity checks the block it faces every tick: if that neighbour is a tree hole with
 * resin, it extracts the resin, fills its 8000 mB tank with the sap fluid and puts the resin item
 * into its single slot (the rubber hole yields an item instead of a fluid). Right-clicking hands out
 * the stored item first and then fills whatever container the player holds. Breaking the bag loses
 * the tank contents, exactly like GT6's {@code GarbageGT.trash(mTank)}.
 */
public class SapBagBlockEntity extends BlockEntity implements IFluidHandler {
    /** GT6 {@code new FluidTankGT(8000)} — 32 taps of 250 mB. */
    public static final long CAPACITY = 8000;

    private final FluidTankGT tank = new FluidTankGT(CAPACITY).setOnChanged(this::contentsChanged);
    private ItemStack stored = ItemStack.EMPTY;
    private LazyOptional<IFluidHandler> handler = LazyOptional.of(() -> this);

    public SapBagBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.SAP_BAG.get(), pos, state);
    }

    public FluidTankGT tank() { return tank; }

    public ItemStack stored() { return stored; }

    public void setStored(ItemStack stack) {
        stored = stack == null ? ItemStack.EMPTY : stack;
        contentsChanged();
    }
    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** True when the bag holds anything (GT6's {@code mFull}, used for its full overlay). */
    public boolean isFull() {
        return tank.getAmount() > 0 || !stored.isEmpty();
    }

    public static <T extends SapBagBlockEntity> void serverTick(net.minecraft.world.level.Level level,
                                                               BlockPos pos, BlockState state, T be) {
        be.collect();
    }

    /** One GT6 tick: ask the hole we are attached to for its resin. */
    public void collect() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof com.gregtech.gregtech.block.tool.SapBagBlock)) return;
        // GT6's bag faces the block it hangs on (getAdjacentTileEntity(mFacing)).
        Direction facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        BlockPos neighbour = worldPosition.relative(facing);
        if (!level.hasChunkAt(neighbour)) return;
        if (!(level.getBlockState(neighbour).getBlock() instanceof TreeHoleBlock hole)) return;
        BlockState holeState = level.getBlockState(neighbour);
        if (!holeState.getValue(TreeHoleBlock.RESIN) || holeState.getValue(TreeHoleBlock.FACING) != facing.getOpposite()) return;
        // GT6 extractResin(): empty the hole, then take its yield.
        level.setBlock(neighbour, holeState.setValue(TreeHoleBlock.RESIN, false), 3);
        if (hole.yieldsItem()) {
            ItemStack resin = hole.resinItem();
            if (!resin.isEmpty()) {
                if (stored.isEmpty()) {
                    setStored(resin);
                } else if (ItemStack.isSameItemSameTags(stored, resin)
                        && stored.getCount() + resin.getCount() <= Math.min(64, stored.getMaxStackSize())) {
                    stored.grow(resin.getCount());
                    contentsChanged();
                }
            }
        } else {
            tank.fill(hole.resinFluid(), FluidAction.EXECUTE);
            setChanged();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag);
        stored = ItemStack.of(tag.getCompound("stored"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tank.writeToNBT(tag);
        if (!stored.isEmpty()) tag.put("stored", stored.save(new CompoundTag()));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ── IFluidHandler (GT6's bag is a plain tank, so pipes can drain it) ──────────────────────

    @Override
    public int getTanks() { return 1; }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) { return this.tank.getFluid(); }

    @Override
    public int getTankCapacity(int tank) { return FluidTankGT.bindInt(CAPACITY); }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return this.tank.isFluidValid(tank, stack); }

    @Override
    public int fill(FluidStack resource, FluidAction action) { return tank.fill(resource, action); }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return tank.drain(resource, action); }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return tank.drain(maxDrain, action); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return handler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }
    @Override public void reviveCaps() { super.reviveCaps(); handler = LazyOptional.of(() -> this); }
}
