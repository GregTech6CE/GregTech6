package gregtech.blockentity.machine;

import gregtech.internal.energy.EnergyTransfer;
import gregtech.internal.machine.FurnaceFuelHelper;
import gregtech.internal.machine.MachineSpec;
import gregtech.internal.tool.GTToolHelper;
import gregtech.internal.tool.GTToolType;
import gregtech.block.machine.GTFacingMachineBlock;
import gregtech.block.machine.SolidBurningBoxBlock;
import gregtech.util.GTEntityHelper;
import gregtech.util.GTPlacementCode;
import gregtech.data.GregTechConstants;
import gregtech.data.GregTechTags;
import gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.ItemHandlerHelper;
*///?} else {
import net.minecraftforge.items.ItemHandlerHelper;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Solid-fuel burning box (GT6 {@code MultiTileEntityGeneratorSolid}).
 * Emits HU from the top face; fuel and ashes use front-face click inventory (no GUI).
 */
//? if >=1.21.1 {
/*public class SolidBurningBoxBlockEntity extends GTFacingMachineBlockEntity implements gregtech.item.behavior.ManualToolBehaviorAccess.Ignitable,gregtech.internal.tool.PoweredToolTarget {
*///?} else {
public class SolidBurningBoxBlockEntity extends GTFacingMachineBlockEntity implements gregtech.internal.tool.PoweredToolTarget {
//?}
    public static final int SLOT_LIMIT = 64;
    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;

//? if >=1.21.1 {
    /*private final net.neoforged.neoforge.items.IItemHandler automation = new net.neoforged.neoforge.items.IItemHandler() {
*///?} else {
    private final net.minecraftforge.items.IItemHandler automation = new net.minecraftforge.items.IItemHandler() {
//?}
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL_SLOT && gregtech.internal.machine.FurnaceFuelValue.getBurnValue(stack) > 0;
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == ASH_SLOT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
//? if >=1.21.1 {
    /*public net.neoforged.neoforge.items.IItemHandler itemCapability(@Nullable Direction side) {
        return isRemoved() || side == getFrontFacing() ? null : automation;
    }
*///?} else {
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> itemCapability =
            net.minecraftforge.common.util.LazyOptional.of(() -> automation);
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != getFrontFacing())
            return itemCapability.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        itemCapability = net.minecraftforge.common.util.LazyOptional.of(() -> automation);
    }
//?}

    private long energy;
    private boolean burning;
    @Nullable
    private ItemStack pendingAsh;

    public SolidBurningBoxBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.SOLID_BURNING_BOX.get(), pos, state, specFrom(state));
    }

    @Override
    protected int getInventorySlotLimit(int slot) {
        return SLOT_LIMIT;
    }

    private static MachineSpec specFrom(BlockState state) {
        if (state.getBlock() instanceof SolidBurningBoxBlock box) {
            return box.spec();
        }
        throw new IllegalStateException("SolidBurningBoxBlockEntity on wrong block: " + state.getBlock());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolidBurningBoxBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        be.beginMachineChanges();
        try { be.tickServer(); }
        finally { be.endMachineChanges(); }
    }

//? if >=1.21.1 {
    /*protected void tickServer() {
*///?} else {
    private void tickServer() {
//?}
        long oldEnergy = energy;
        boolean wasBurning = burning;
        long rate = spec.outputRate();
        if (burning) {
            if (energy >= rate) {
                EnergyTransfer.emitEnergyToSide(GregTechTags.Energy.HU, Direction.UP, 1, Math.min(rate, energy), this);
                energy -= rate;
            }
            flushPendingAsh();
            if (energy < rate * 2) {
                tryConsumeFuel();
            }
            if (energy < rate) {
                burning = false;
            }
        } else if (level != null && level.random.nextInt(200) == 0 && isFrontIgnited()) {
            burning = true;
        }
        if (energy < 0) {
            energy = 0;
        }
        setMachineLit(burning);
        if (oldEnergy != energy || wasBurning != burning) markMachineDataChanged();
    }

    private boolean isFrontIgnited() {
        if (level == null) {
            return false;
        }
        BlockState front = level.getBlockState(frontPos());
        return front.is(Blocks.FIRE) || front.is(Blocks.SOUL_FIRE);
    }

    private boolean canConsumeFuel() {
        return isFrontAir() && (pendingAsh == null || pendingAsh.isEmpty());
    }

    private void tryConsumeFuel() {
        if (!canConsumeFuel()) {
            return;
        }
        ItemStack fuel = inventory.getStackInSlot(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return;
        }
        var result = FurnaceFuelHelper.burnOne(fuel, spec.efficiency());
        if (result.isEmpty()) {
            return;
        }
        FurnaceFuelHelper.FuelBurnResult burn = result.get();
        if (!canStoreAsh(burn.container()) || !canStoreAsh(burn.byproduct())) {
            return;
        }
        energy += burn.heatUnits();
        inventory.extractItem(FUEL_SLOT, 1, false);
        if (burn.container() != null && !burn.container().isEmpty()) {
            queueAsh(burn.container());
        }
        if (burn.byproduct() != null && !burn.byproduct().isEmpty()) {
            queueAsh(burn.byproduct());
        }
        flushPendingAsh();
    }

    private void queueAsh(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (pendingAsh == null || pendingAsh.isEmpty()) {
            pendingAsh = stack.copy();
            return;
        }
//? if >=1.21.1 {
        /*if (ItemStack.isSameItemSameComponents(pendingAsh, stack)) {
*///?} else {
        if (ItemStack.isSameItemSameTags(pendingAsh, stack)) {
//?}
            pendingAsh.grow(stack.getCount());
            pendingAsh.setCount(Math.min(SLOT_LIMIT, pendingAsh.getCount()));
        } else {
            flushPendingAsh();
            if (pendingAsh == null || pendingAsh.isEmpty()) {
                pendingAsh = stack.copy();
            }
        }
    }

    private void flushPendingAsh() {
        if (pendingAsh == null || pendingAsh.isEmpty()) {
            pendingAsh = null;
            return;
        }
        ItemStack leftover = inventory.insertItem(ASH_SLOT, pendingAsh, false);
        pendingAsh = leftover.isEmpty() ? null : leftover;
    }

    private boolean canStoreAsh(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        ItemStack ash = inventory.getStackInSlot(ASH_SLOT);
        if (ash.isEmpty()) {
            return stack.getCount() <= SLOT_LIMIT;
        }
//? if >=1.21.1 {
        /*if (!ItemStack.isSameItemSameComponents(ash, stack)) {
*///?} else {
        if (!ItemStack.isSameItemSameTags(ash, stack)) {
//?}
            return false;
        }
        return ash.getCount() + stack.getCount() <= SLOT_LIMIT;
    }

    /** Drops fuel, ash, and pending ash when the block is removed (GT6 inventory spill). */
    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        if (pendingAsh != null && !pendingAsh.isEmpty()) {
            gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), pendingAsh);
            pendingAsh = null;
        }
    }

    public InteractionResult handleToolUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null || level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);

        if (GTToolHelper.matchesTool(held, GTToolType.WRENCH)) {
            float lx = (float) (hit.getLocation().x - hit.getBlockPos().getX());
            float ly = (float) (hit.getLocation().y - hit.getBlockPos().getY());
            float lz = (float) (hit.getLocation().z - hit.getBlockPos().getZ());
            Direction target = GTPlacementCode.getSideWrenching(hit.getDirection(), lx, ly, lz);
            if (!isValidWrenchFacing(target)) {
                return InteractionResult.SUCCESS;
            }
            if (target != getFrontFacing()) {
                level.setBlock(worldPosition, getBlockState().setValue(GTFacingMachineBlock.FACING, target), 3);
//? if >=1.21.1 {
                /*level.playSound(null, worldPosition, gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),
*///?} else {
                level.playSound(null, worldPosition, gregtech.registry.GTSounds.WRENCH.get(),
//?}
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                GTToolHelper.damageForToolClickReturn(held, 10000L, player);
            }
            return InteractionResult.CONSUME;
        }

        if (hit.getDirection() == getFrontFacing() && GTToolHelper.matchesTool(held, GTToolType.SHOVEL)) {
            ItemStack ash = inventory.getStackInSlot(ASH_SLOT);
            if (ash.isEmpty()) {
                return InteractionResult.PASS;
            }
            ItemStack removed = ash.copy();
            inventory.setStackInSlot(ASH_SLOT, ItemStack.EMPTY);
            if (!player.addItem(removed)) {
                player.drop(removed, false);
            }
            GTToolHelper.damageForToolClickReturn(held, 1000L * removed.getCount(), player);
            setChanged();
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private boolean isValidWrenchFacing(Direction side) {
        if (!side.getAxis().isHorizontal()) {
            return false;
        }
        return !burning || side == getFrontFacing();
    }

    public InteractionResult handleUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null || level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (hit.getDirection() != getFrontFacing()) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return extractFromFront(player, hand);
        }
        if (held.is(Items.FLINT_AND_STEEL)) {
            if (usePoweredIgniter(hit.getDirection(),Long.MAX_VALUE,0)) {
                if (!player.isCreative()) {
//? if >=1.21.1 {
                    /*held.hurtAndBreak(1, player, hand == InteractionHand.OFF_HAND ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
*///?} else {
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
//?}
                }
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        if (!FurnaceFuelHelper.burnOne(held, spec.efficiency()).isPresent()) {
            return InteractionResult.PASS;
        }
        ItemStack fuel = inventory.getStackInSlot(FUEL_SLOT);
        if (fuel.isEmpty()) {
            ItemStack inserted = ItemHandlerHelper.insertItem(inventory, held.copy(), false);
            if (inserted.getCount() < held.getCount()) {
                if (!player.isCreative()) {
                    held.setCount(inserted.getCount());
                }
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
//? if >=1.21.1 {
        /*if (ItemStack.isSameItemSameComponents(fuel, held)) {
*///?} else {
        if (ItemStack.isSameItemSameTags(fuel, held)) {
//?}
            int space = SLOT_LIMIT - fuel.getCount();
            if (space > 0) {
                int moved = Math.min(space, held.getCount());
                fuel.grow(moved);
                if (!player.isCreative()) {
                    held.shrink(moved);
                }
                setChanged();
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    private InteractionResult extractFromFront(Player player, InteractionHand hand) {
        ItemStack ash = inventory.getStackInSlot(ASH_SLOT);
        if (!ash.isEmpty()) {
            player.setItemInHand(hand, ash.copy());
            inventory.setStackInSlot(ASH_SLOT, ItemStack.EMPTY);
            if (burning) {
                float damage = Math.max(1.0F, Math.min(5.0F, spec.outputRate() / 20.0F));
                GTEntityHelper.applyHeatDamage(player, damage);
            }
            return InteractionResult.CONSUME;
        }
        ItemStack fuel = inventory.getStackInSlot(FUEL_SLOT);
        if (!burning && !fuel.isEmpty()) {
            player.setItemInHand(hand, fuel.copy());
            inventory.setStackInSlot(FUEL_SLOT, ItemStack.EMPTY);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        energy = tag.getLong(GregTechConstants.NBT_ENERGY);
        burning = tag.getBoolean(GregTechConstants.NBT_ACTIVE);
        if (tag.contains("gt.pending_ash", Tag.TAG_COMPOUND)) {
//? if >=1.21.1 {
            /*pendingAsh = ItemStack.parseOptional(lookup, tag.getCompound("gt.pending_ash"));
*///?} else {
            pendingAsh = ItemStack.of(tag.getCompound("gt.pending_ash"));
//?}
        } else {
            pendingAsh = null;
        }
    }

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        tag.putLong(GregTechConstants.NBT_ENERGY, energy);
        tag.putBoolean(GregTechConstants.NBT_ACTIVE, burning);
        tag.putLong(GregTechConstants.NBT_OUTPUT, spec.outputRate());
        tag.putInt(GregTechConstants.NBT_EFFICIENCY, spec.efficiency());
        if (pendingAsh != null && !pendingAsh.isEmpty()) {
//? if >=1.21.1 {
            /*tag.put("gt.pending_ash", pendingAsh.saveOptional(lookup));
*///?} else {
            tag.put("gt.pending_ash", pendingAsh.save(new CompoundTag()));
//?}
        }
    }

    public long getStoredHeat() {
        return energy;
    }

    public boolean isBurning() {
        return burning;
    }

    public ItemStack getFuelStack() {
        return inventory.getStackInSlot(FUEL_SLOT);
    }

    public ItemStack getAshStack() {
        return inventory.getStackInSlot(ASH_SLOT);
    }

    /** GT6 {@code onEntityCollidedWithBlock} heat when burning. */
    public float getHeatContactDamage() {
        return Math.min(10.0F, spec.outputRate() / 10.0F);
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return emitting && energyType == GregTechTags.Energy.HU;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return GregTechTags.Energy.HU.asList();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return side == Direction.UP && super.isEnergyEmittingTo(energyType, side, theoretical);
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (energyType != GregTechTags.Energy.HU || side != Direction.UP || !burning) {
            return 0;
        }
        return Math.min(spec.outputRate(), energy);
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU ? spec.outputRate() : 0;
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return getEnergySizeOutputRecommended(energyType, side);
    }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return getEnergySizeOutputRecommended(energyType, side);
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 0;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0;
    }

//? if >=1.21.1 {
    /*public boolean usePoweredIgniter(net.minecraft.core.Direction side,long budget,int quality) {
*///?} else {
    @Override public boolean usePoweredIgniter(net.minecraft.core.Direction side,long budget,int quality) {
//?}
        if(level==null || level.isClientSide || budget<=0 || !(!burning && !inventory.getStackInSlot(FUEL_SLOT).isEmpty() && isFrontAir())) return false;
        burning=true;
        setChanged(); return true;
    }
//? if >=1.21.1 {
    /*@Override public long onIgnite(Level world,BlockPos pos,Direction side,@Nullable Player player,ItemStack igniter,boolean sneaking,float hx,float hy,float hz){
        if(side!=getFrontFacing())return 0;
        return usePoweredIgniter(side,Long.MAX_VALUE,0)?10000L:0L;
    }
*///?} else {
//?}
}
