package gregtech.blockentity.machine;

import gregtech.api.energy.EnergyTransfer;
import gregtech.api.machine.*;
import gregtech.api.fluid.FluidTankGT;
import gregtech.api.recipe.Recipe;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.tool.GTToolHelper;
import gregtech.api.tool.GTToolType;
import gregtech.block.machine.BurningBoxBlock;
import gregtech.blockentity.machine.GTFacingMachineBlockEntity;
import gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {



/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
//?}
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static gregtech.data.FuelRecipeMaps.*;

/**
 * Generic GT6 Burning Box (generator) for liquid, gas, and fluidized-bed fuel types.
 *
 * <p>Liquid  — fluid fuel, FM.Burn RecipeMap, exhaust voided.
 * Gas     — fluid fuel (gas-only), FM.Gas RecipeMap, exhaust voided, redstone control.
 * Fluidized Bed — slot fuel (dust) + fluid calcite, FM.FluidBed RecipeMap, ash output.
 * All emit HU upward. Dense versions multiply output rate by 4x.</p>
 */
//? if >=1.21.1 {
/*public class BurningBoxBlockEntity extends GTFacingMachineBlockEntity implements gregtech.item.behavior.ManualToolBehaviorAccess.Ignitable,gregtech.api.tool.PoweredToolTarget {
*///?} else {
public class BurningBoxBlockEntity extends GTFacingMachineBlockEntity implements gregtech.api.tool.PoweredToolTarget {
//?}

    private static final int FLAME_RANGE = 2;

    protected final BurningBoxFuelType fuelType;
    protected final MachineSpec spec;
    protected final RecipeMap recipeMap;
    protected long energy;
    protected boolean burning;
    protected int cooldown;
    protected ItemStackHandler inventory;
    protected FluidTankGT fuelTank;
    @Nullable protected ItemStack pendingAsh;
//? if >=1.21.1 {
    /*private final net.neoforged.neoforge.items.IItemHandler automation = new net.neoforged.neoforge.items.IItemHandler() {
*///?} else {
    private final net.minecraftforge.items.IItemHandler automation = new net.minecraftforge.items.IItemHandler() {
//?}
        public int getSlots() { return inventory == null ? 0 : inventory.getSlots(); }
        public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && recipeMap != null && recipeMap.containsInput(stack); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return isItemValid(slot, stack) ? inventory.insertItem(slot, stack, simulate) : stack; }
        public ItemStack extractItem(int slot, int count, boolean simulate) { return slot == 1 ? inventory.extractItem(slot, count, simulate) : ItemStack.EMPTY; }
    };

    public BurningBoxBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.registry.GTBlockEntities.BURNING_BOX.get(), pos, state, specFrom(state));
        this.spec = specFrom(state);
        BurningBoxBlock block = (BurningBoxBlock) state.getBlock();
        this.fuelType = block.fuelType();
        this.recipeMap = switch (fuelType) {
            case LIQUID -> Burn;
            case GAS -> Gas;
            case FLUIDIZED_BED -> FluidBed;
            case SOLID -> null;
        };
        initInventoryAndTank();
    }

    private static MachineSpec specFrom(BlockState state) {
        if (state.getBlock() instanceof BurningBoxBlock box) {
            return box.spec();
        }
        throw new IllegalStateException("BurningBoxBlockEntity on wrong block: " + state.getBlock());
    }

    private void initInventoryAndTank() {
        switch (fuelType) {
            case FLUIDIZED_BED -> {
                inventory = new ItemStackHandler(2) {
                    @Override protected void onContentsChanged(int slot) { setChanged(); }
                };
                inventory.setStackInSlot(0, ItemStack.EMPTY); // fuel dust
                inventory.setStackInSlot(1, ItemStack.EMPTY); // ash
            }
            case LIQUID, GAS -> inventory = new ItemStackHandler(0);
            case SOLID -> inventory = new ItemStackHandler(0);
        }
        if (fuelType == BurningBoxFuelType.LIQUID || fuelType == BurningBoxFuelType.GAS || fuelType == BurningBoxFuelType.FLUIDIZED_BED) {
            fuelTank = new FluidTankGT(spec.outputRate() * 10);
        }
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, BurningBoxBlockEntity be) {
        if (be.spec == null || level.isClientSide) return;
        be.tickServer();
    }

//? if >=1.21.1 {
    /*protected void tickServer() {
*///?} else {
    private void tickServer() {
//?}
        long rate = spec.outputRate();
        if (burning) {
            if (energy >= rate) {
                EnergyTransfer.emitEnergyToSide(GregTechTags.Energy.HU, Direction.UP, 1, Math.min(rate, energy), this);
                energy -= rate;
            }
            flushPendingAsh();
            if (energy < rate * 2) {
                burning = tryConsumeFuel();
            }
            if (energy < rate) {
                burning = false;
            }
        } else if (level != null && level.random.nextInt(200) == 0 && isFrontIgnited()) {
            burning = true;
            cooldown = 100;
        }
        if (energy < 0) energy = 0;
        if (cooldown > 0) cooldown--;
        setMachineLit(burning);
        setChanged();
    }

    private boolean isFrontIgnited() {
        if (level == null) return false;
        BlockPos front = worldPosition.relative(getFrontFacing());
        return level.getBlockState(front).is(net.minecraft.tags.BlockTags.FIRE);
    }

    // ── Fuel consumption ─────────────────────────────────────────────────────

    private boolean tryConsumeFuel() {
        if (!isFrontAir()) return false;
        return switch (fuelType) {
            case LIQUID, GAS -> tryConsumeFluidFuel();
            case FLUIDIZED_BED -> tryConsumeFluidBedFuel();
            case SOLID -> false;
        };
    }

    private boolean tryConsumeFluidFuel() {
        if (fuelTank == null || fuelTank.isEmpty()) return false;
        FluidStack tankFluid = fuelTank.getFluid();
        if (tankFluid.isEmpty()) return false;
        List<FluidStack> fluids = List.of(tankFluid);
        Recipe recipe = recipeMap.findRecipe(Collections.emptyList(), fluids, false, 0, 0);
        if (recipe == null) return false;
        long eut = Math.abs(recipe.mEUt);
        if (eut == 0) return false;
        // Consume input fluids
        for (FluidStack fluidIn : recipe.mFluidInputs) {
            fuelTank.remove(fluidIn.getAmount());
        }
        energy += BurningBoxFuelRules.recipeHeat(eut, recipe.mDuration, spec.efficiency());
        return true;
    }

    private boolean tryConsumeFluidBedFuel() {
        flushPendingAsh();
        if (pendingAsh != null && !pendingAsh.isEmpty()) return false;
        ItemStack fuel = inventory.getStackInSlot(0);
        if (fuel.isEmpty() || fuelTank == null || fuelTank.isEmpty()) return false;
        FluidStack tankFluid = fuelTank.getFluid();
        if (tankFluid.isEmpty()) return false;
        List<ItemStack> items = List.of(fuel);
        List<FluidStack> fluids = List.of(tankFluid);
        Recipe recipe = recipeMap.findRecipe(items, fluids, false, 1, 1);
        if (recipe == null) return false;
        if (recipe.mOutputs.length > 0 && !canStoreAsh(recipe.mOutputs[0])) return false;
        long eut = Math.abs(recipe.mEUt);
        if (eut == 0) return false;
        // Consume inputs
        for (FluidStack fluidIn : recipe.mFluidInputs) {
            fuelTank.remove(fluidIn.getAmount());
        }
        inventory.extractItem(0, recipe.mInputs[0].getCount(), false);
        energy += BurningBoxFuelRules.recipeHeat(eut, recipe.mDuration, spec.efficiency());
        // Queue ash from first output
        ItemStack[] outputs = recipe.mOutputs;
        if (outputs.length > 0 && outputs[0] != null && !outputs[0].isEmpty())
            queueAsh(outputs[0].copy());
        flushPendingAsh();
        return true;
    }

    // ── Ash (Fluidized Bed) ──────────────────────────────────────────────────

    private boolean canStoreAsh() {
        if (inventory == null || inventory.getSlots() < 2) return true;
        return true;
    }

    private boolean canStoreAsh(@Nullable ItemStack stack) {
        if (inventory == null || inventory.getSlots() < 2) return true;
        if (stack == null || stack.isEmpty()) return true;
        ItemStack ashSlot = inventory.getStackInSlot(1);
        if (ashSlot.isEmpty()) return true;
//? if >=1.21.1 {
        /*return ItemStack.isSameItemSameComponents(ashSlot, stack)
*///?} else {
        return ItemStack.isSameItemSameTags(ashSlot, stack)
//?}
                && ashSlot.getCount() + stack.getCount() <= ashSlot.getMaxStackSize();
    }

    private void queueAsh(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (pendingAsh == null) pendingAsh = stack.copy();
//? if >=1.21.1 {
        /*else if (ItemStack.isSameItemSameComponents(pendingAsh, stack))
*///?} else {
        else if (ItemStack.isSameItemSameTags(pendingAsh, stack))
//?}
            pendingAsh.grow(stack.getCount());
        else flushPendingAsh();
    }

    private void flushPendingAsh() {
        if (pendingAsh == null || pendingAsh.isEmpty() || inventory == null || inventory.getSlots() < 2) {
            pendingAsh = null;
            return;
        }
        ItemStack leftover = inventory.insertItem(1, pendingAsh.copy(), false);
        pendingAsh = leftover.isEmpty() ? null : leftover;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                    inventory.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        }
        if (pendingAsh != null && !pendingAsh.isEmpty()) {
            gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), pendingAsh);
            pendingAsh = null;
        }
    }

    // ── Player interaction ───────────────────────────────────────────────────

    public InteractionResult handleUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null || level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);

        // Flint and steel: ignite
        if (held.is(Items.FLINT_AND_STEEL)) {
            if (usePoweredIgniter(hit.getDirection(),Long.MAX_VALUE,0)) {
                cooldown = 100;
//? if >=1.21.1 {
                /*if (!player.isCreative()) held.hurtAndBreak(1, player, hand == InteractionHand.OFF_HAND ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
*///?} else {
                if (!player.isCreative()) held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
//?}
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }

        // Empty hand: extract items from front
        if (held.isEmpty() && hit.getDirection() == getFrontFacing()) {
            if (inventory != null && inventory.getSlots() > 1) {
                ItemStack ash = inventory.getStackInSlot(1);
                if (!ash.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, inventory.extractItem(1, ash.getCount(), false));
                    return InteractionResult.CONSUME;
                }
                if (!burning && inventory.getSlots() > 0) {
                    ItemStack fuel = inventory.getStackInSlot(0);
                    if (!fuel.isEmpty()) {
                        ItemHandlerHelper.giveItemToPlayer(player, inventory.extractItem(0, fuel.getCount(), false));
                        return InteractionResult.CONSUME;
                    }
                }
            }
            return InteractionResult.PASS;
        }

        // Fluid container: fill/drain from tank
        if (fuelTank != null) {
            boolean result = FluidUtil.interactWithFluidHandler(player, hand, fuelTank);
            if (result) { setChanged(); return InteractionResult.CONSUME; }
        }

        // Insert item fuel for FluidizedBed
        if (inventory != null && inventory.getSlots() > 0 && hit.getDirection() == getFrontFacing()) {
            if (fuelType == BurningBoxFuelType.FLUIDIZED_BED
                    && recipeMap != null
                    && recipeMap.containsInput(held)) {
                return insertFuelItem(player, held);
            }
        }

        return InteractionResult.PASS;
    }

    private InteractionResult insertFuelItem(Player player, ItemStack held) {
        ItemStack fuelSlot = inventory.getStackInSlot(0);
        if (fuelSlot.isEmpty()) {
            ItemStack inserted = inventory.insertItem(0, held.copy(), false);
            if (inserted.getCount() < held.getCount()) {
                if (!player.isCreative()) held.setCount(inserted.getCount());
                setChanged();
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
//? if >=1.21.1 {
        /*if (ItemStack.isSameItemSameComponents(fuelSlot, held)) {
*///?} else {
        if (ItemStack.isSameItemSameTags(fuelSlot, held)) {
//?}
            int space = fuelSlot.getMaxStackSize() - fuelSlot.getCount();
            if (space > 0) {
                int moved = Math.min(space, held.getCount());
                fuelSlot.grow(moved);
                if (!player.isCreative()) held.shrink(moved);
                setChanged();
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult handleToolUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null || level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);

        // Wrench: rotate facing
        if (GTToolHelper.matchesTool(held, GTToolType.WRENCH)
                || GTToolHelper.matchesTool(held, GTToolType.MONKEY_WRENCH)) {
            Direction target = gregtech.api.tool.ToolInteractions.selectedFace(hit);
            if (burning && target != getFrontFacing()) return InteractionResult.SUCCESS;
//? if >=1.21.1 {
            /*if (!target.getAxis().isHorizontal()) return InteractionResult.SUCCESS;
            if (target != getFrontFacing()) {
                level.setBlock(worldPosition,getBlockState().setValue(gregtech.block.machine.GTFacingMachineBlock.FACING,target),3);
                level.invalidateCapabilities(worldPosition);
                level.playSound(null,worldPosition,gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),SoundSource.BLOCKS,1,1);
                GTToolHelper.damageForToolClickReturn(held,10000L,player);
            }
*///?} else {
            gregtech.api.tool.ToolInteractions.use(getBlockState(), level, worldPosition, player, hand, hit);
//?}
            return InteractionResult.CONSUME;
        }

        // Shovel: extract ash (FluidizedBed)
        if (fuelType == BurningBoxFuelType.FLUIDIZED_BED
                && hit.getDirection() == getFrontFacing()
                && (GTToolHelper.matchesTool(held, GTToolType.SHOVEL)
                    || GTToolHelper.matchesTool(held, GTToolType.SPADE)
                    || GTToolHelper.matchesTool(held, GTToolType.UNIVERSAL_SPADE))) {
            if (inventory != null && inventory.getSlots() > 1) {
                ItemStack ash = inventory.getStackInSlot(1);
                if (!ash.isEmpty()) {
                    ItemStack removed = ash.copy();
                    inventory.setStackInSlot(1, ItemStack.EMPTY);
                    if (!player.addItem(removed)) player.drop(removed, false);
                    GTToolHelper.damageForToolClickReturn(held, 1000L * removed.getCount(), player);
                    setChanged();
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.PASS;
    }

    // ── Capabilities ─────────────────────────────────────────────────────────

//? if >=1.21.1 {
    /*public net.neoforged.neoforge.items.IItemHandler itemCapability(@Nullable Direction side){return !isRemoved() && inventory != null && side != getFrontFacing() ? automation : null;}
    public net.neoforged.neoforge.fluids.capability.IFluidHandler fluidCapability(@Nullable Direction side){return fuelTank;}
*///?} else {
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Both sinks are side-independent, so one cached LazyOptional each (memory audit: this used to
        // allocate a new handle on every query, i.e. one object per neighbouring machine per tick).
        if (cap == ForgeCapabilities.ITEM_HANDLER && inventory != null && side != getFrontFacing()) {
            if (itemCap == null) itemCap = LazyOptional.of(() -> automation);
            return itemCap.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER && fuelTank != null) {
            if (fluidCap == null) fluidCap = LazyOptional.of(() -> fuelTank);
            return fluidCap.cast();
        }
        return super.getCapability(cap, side);
    }

    private LazyOptional<net.minecraftforge.items.IItemHandler> itemCap;
    private LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluidCap;

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (itemCap != null) itemCap.invalidate();
        if (fluidCap != null) fluidCap.invalidate();
        itemCap = null;
        fluidCap = null;
    }
//?}

    // ── Energy ───────────────────────────────────────────────────────────────

    @Override public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return emitting && energyType == GregTechTags.Energy.HU;
    }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return side == Direction.UP && super.isEnergyEmittingTo(energyType, side, theoretical);
    }
    @Override public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return Math.min(spec.outputRate(), energy);
    }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return spec.outputRate();
    }
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return spec.outputRate();
    }
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return spec.outputRate();
    }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return List.of(GregTechTags.Energy.HU);
    }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 0;
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return false;
    }
    @Override public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0;
    }

    // ── State ────────────────────────────────────────────────────────────────

    public boolean isBurning() { return burning; }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        tag.putLong("gt.energy", energy);
        tag.putBoolean("gt.active", burning);
        tag.putInt("gt.cooldown", cooldown);
        if (fuelTank != null) {
            CompoundTag t = new CompoundTag();
//? if >=1.21.1 {
            /*fuelTank.writeToNBT(t, lookup);
*///?} else {
            fuelTank.writeToNBT(t);
//?}
            tag.put("gt.tank", t);
        }
        if (inventory != null) {
//? if >=1.21.1 {
            /*tag.put("gt.inv", inventory.serializeNBT(lookup));
*///?} else {
            tag.put("gt.inv", inventory.serializeNBT());
//?}
        }
        if (pendingAsh != null && !pendingAsh.isEmpty()) {
            CompoundTag ash = new CompoundTag();
//? if >=1.21.1 {
            /*ash = (CompoundTag) pendingAsh.saveOptional(lookup);
*///?} else {
            pendingAsh.save(ash);
//?}
            tag.put("gt.pending_ash", ash);
        }
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        energy = tag.getLong("gt.energy");
        burning = tag.getBoolean("gt.active");
        cooldown = tag.getInt("gt.cooldown");
        if (fuelTank != null && tag.contains("gt.tank"))
//? if >=1.21.1 {
            /*fuelTank.readFromNBT(tag.getCompound("gt.tank"), lookup);
*///?} else {
            fuelTank.readFromNBT(tag.getCompound("gt.tank"));
//?}
        if (inventory != null && tag.contains("gt.inv"))
//? if >=1.21.1 {
            /*inventory.deserializeNBT(lookup, tag.getCompound("gt.inv"));
*///?} else {
            inventory.deserializeNBT(tag.getCompound("gt.inv"));
//?}
        if (tag.contains("gt.pending_ash"))
//? if >=1.21.1 {
            /*pendingAsh = ItemStack.parseOptional(lookup, tag.getCompound("gt.pending_ash"));
        else pendingAsh = null;
*///?} else {
            pendingAsh = ItemStack.of(tag.getCompound("gt.pending_ash"));
//?}
    }

    public MachineSpec spec() { return spec; }

//? if >=1.21.1 {
    /*public boolean usePoweredIgniter(net.minecraft.core.Direction side,long budget,int quality) {
*///?} else {
    @Override public boolean usePoweredIgniter(net.minecraft.core.Direction side,long budget,int quality) {
//?}
        if(level==null || level.isClientSide || budget<=0 || !(!burning && isFrontAir())) return false;
        burning=true;
        cooldown=100;
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
