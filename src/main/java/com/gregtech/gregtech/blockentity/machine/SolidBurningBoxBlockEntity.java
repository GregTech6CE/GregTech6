package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.machine.FurnaceFuelHelper;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.machine.GTFacingMachineBlock;
import com.gregtech.gregtech.block.machine.SolidBurningBoxBlock;
import com.gregtech.gregtech.util.GTEntityHelper;
import com.gregtech.gregtech.util.GTPlacementCode;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
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
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Solid-fuel burning box (GT6 {@code MultiTileEntityGeneratorSolid}).
 * Emits HU from the top face; fuel and ashes use front-face click inventory (no GUI).
 */
public class SolidBurningBoxBlockEntity extends GTFacingMachineBlockEntity implements com.gregtech.gregtech.api.tool.PoweredToolTarget {
    public static final int SLOT_LIMIT = 64;
    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;

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
        be.tickServer();
    }

    private void tickServer() {
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
        setChanged();
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
        if (ItemStack.isSameItemSameTags(pendingAsh, stack)) {
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
        if (!ItemStack.isSameItemSameTags(ash, stack)) {
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
                com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        if (pendingAsh != null && !pendingAsh.isEmpty()) {
            com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), pendingAsh);
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
                level.playSound(null, worldPosition, com.gregtech.gregtech.registry.GTSounds.WRENCH.get(),
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
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
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
        if (ItemStack.isSameItemSameTags(fuel, held)) {
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
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getLong(GregTechConstants.NBT_ENERGY);
        burning = tag.getBoolean(GregTechConstants.NBT_ACTIVE);
        if (tag.contains("gt.pending_ash", Tag.TAG_COMPOUND)) {
            pendingAsh = ItemStack.of(tag.getCompound("gt.pending_ash"));
        } else {
            pendingAsh = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(GregTechConstants.NBT_ENERGY, energy);
        tag.putBoolean(GregTechConstants.NBT_ACTIVE, burning);
        tag.putLong(GregTechConstants.NBT_OUTPUT, spec.outputRate());
        tag.putInt(GregTechConstants.NBT_EFFICIENCY, spec.efficiency());
        if (pendingAsh != null && !pendingAsh.isEmpty()) {
            tag.put("gt.pending_ash", pendingAsh.save(new CompoundTag()));
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

    @Override public boolean usePoweredIgniter(net.minecraft.core.Direction side,long budget,int quality) {
        if(level==null || level.isClientSide || budget<=0 || !(!burning && !inventory.getStackInSlot(FUEL_SLOT).isEmpty() && isFrontAir())) return false;
        burning=true;
        setChanged(); return true;
    }
}
