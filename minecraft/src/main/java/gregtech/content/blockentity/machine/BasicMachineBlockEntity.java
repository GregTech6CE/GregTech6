package gregtech.content.blockentity.machine;

import gregtech.api.machine.BlockFace;
import gregtech.engine.machine.MachineToolSettings.Transfer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;


import gregtech.engine.energy.EnergyBlockViews;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.energy.IEnergyBlock;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.machine.MachineAutoIO;
import gregtech.engine.machine.MachineDirections;
import gregtech.engine.machine.MachineEnergyPort;
import gregtech.engine.machine.MachineInventory;
import gregtech.engine.machine.MachineItemHandler;
import gregtech.content.cover.CoverUtilityBehaviors.FilterAction;
import gregtech.content.cover.MachineCoverSpec;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.GTFacingMachineBlock;
import gregtech.content.blockentity.GTEnergyBlockEntity;
import gregtech.client.gui.BasicMachineContainerMenu;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
//?}

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class BasicMachineBlockEntity extends GTEnergyBlockEntity implements MenuProvider, IFluidHandler, gregtech.engine.fluid.FluidToolTarget, gregtech.engine.inventory.BlockContents, gregtech.api.platform.machine.MachineControl.Provider, gregtech.content.cover.PanelCoverHost, gregtech.content.cover.CoverStore.Hooks, gregtech.content.item.behavior.ManualToolBehaviorAccess.Ignitable, gregtech.api.sensor.CompressionSensorSource, MachineEnergyPort.Host, MachineAutoIO.Host {
    private static final String NBT_INVENTORY = "gt.inventory";
    private static final String NBT_TANKS_IN = "gt.tanks_input";
    private static final String NBT_TANKS_OUT = "gt.tanks_output";
    private static final String NBT_ENERGY = "gt.energy";

    private BasicMachineSpec spec;
    /** A save loaded before this machine had a spec; sanitize once the spec arrives. */
    private boolean specDirty;
    private RecipeMap recipeMap;
    private final MachineAutoIO autoIO;
    private MachineInventory inventory;
    private final BasicMachineIOPorts ioPorts = new BasicMachineIOPorts(this);


//? if >=1.21.1 {
/*    private final net.neoforged.neoforge.items.ItemStackHandler autocraftingProgram = new net.neoforged.neoforge.items.ItemStackHandler(1) {
*///?} else {
    private final net.minecraftforge.items.ItemStackHandler autocraftingProgram = new net.minecraftforge.items.ItemStackHandler(1) {
//?}

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return gregtech.content.recipe.AutocraftingRecipes.isProgram(stack);
        }
        @Override protected void onContentsChanged(int slot) { inventoryChanged(); }
    };

//? if >=1.21.1 {
/*    public net.neoforged.neoforge.items.IItemHandler autocraftingProgram() { return autocraftingProgram; }
*///?} else {
    public net.minecraftforge.items.IItemHandler autocraftingProgram() { return autocraftingProgram; }
//?}


    private FaceConfig faceConfig = FaceConfig.ALL_SIDES;
    private final BasicMachineTools tools = new BasicMachineTools(this, this::markMachineDataChanged);

    // ── Processing state ─────────────────────────────────────────────────
    private final MachineEnergyPort energyPort = new MachineEnergyPort();
    private final gregtech.engine.machine.RecipeProcessor.Config recipeConfig =
            new gregtech.engine.machine.RecipeProcessor.Config() {
        public RecipeMap recipeMap() { return BasicMachineBlockEntity.this.recipeMap; }
        public boolean searchOutputTanks() { return usesOutputTankSearch(); }
        public boolean exclusiveOutputs() { return usesExclusiveOutputs(); }
        public boolean requireEmptyOutputs() { return tools.emptyOutputsOnly(); }
        public int parallelLimit() { return spec == null ? 1 : spec.parallelLimit(); }
        public boolean limitFluidOutputAccumulation() { return usesOriginalFluidBuffers(); }
        public boolean parallelScalesDuration() { return BasicMachineBlockEntity.this.parallelScalesDuration(); }
        public boolean cheapOverclocking() { return BasicMachineBlockEntity.this.cheapOverclocking(); }
        public boolean usesTimeEnergy() { return BasicMachineBlockEntity.this.usesTimeEnergy(); }
        public int efficiency() { return BasicMachineBlockEntity.this.efficiency(); }
        public long inputMinimum() { return BasicMachineBlockEntity.this.inputMinimum(); }
        public long inputMaximum() { return BasicMachineBlockEntity.this.inputMaximum(); }
        public long nominalEnergyInput() { return spec == null ? 0 : spec.energyIn(); }
        public long startupEnergyFor(Recipe recipe) {
            var auxiliary = spec == null ? null : spec.auxiliaryEnergy();
            return auxiliary != null && auxiliary.startup() && recipe.specialValue > 0 ? recipe.specialValue : 0;
        }
    };
    private final gregtech.engine.machine.RecipeProcessor.IO recipeIO =
            new gregtech.engine.machine.RecipeProcessor.IO() {
        public MachineInventory inventory() { return BasicMachineBlockEntity.this.inventory; }
        public Level level() { return BasicMachineBlockEntity.this.level; }
        public ItemStack autocraftingProgram() { return autocraftingProgram.getStackInSlot(0); }
        public void setStartupEnergy(long value) { energyPort.setStartup(value); }
        public void clearStartupEnergy() { energyPort.clearStartup(); }
        public void markJobStarted() { setChanged(); }
        public BlockEntity owner() { return BasicMachineBlockEntity.this; }
    };
    private final gregtech.engine.machine.RecipeProcessor.Gate recipeGate =
            new gregtech.engine.machine.RecipeProcessor.Gate() {
        public boolean structureComplete() { return BasicMachineBlockEntity.this.structureComplete(); }
        public boolean switchesAllowRunning() { return BasicMachineBlockEntity.this.switchesAllowRunning(); }
        public boolean recipeStartAllowed() { return BasicMachineBlockEntity.this.recipeStartAllowed(); }
        public boolean lightAllowsStart() { return BasicMachineBlockEntity.this.lightAllowsStart(); }
    };
    private final gregtech.engine.machine.RecipeProcessor processor =
            new gregtech.engine.machine.RecipeProcessor(recipeConfig, recipeIO, recipeGate);
    private boolean mActive;        // currently processing a recipe
    private boolean mRunning;       // machine is in running state (energy present)
    private int sourceIgnitionTicks;
    private boolean sourceRequiresIgnition() {return spec!=null&&gregtech.content.machine.OriginalBasicMachineRules.requiresIgnition(spec.machineName(),spec.tier());}
    @Override public long onIgnite(Level level,BlockPos pos,Direction side,Player player,ItemStack igniter,
            boolean sneaking,float hitX,float hitY,float hitZ) {
        if (!sourceRequiresIgnition())return 0;
        if (!level.isClientSide) {sourceIgnitionTicks=gregtech.content.machine.OriginalBasicMachineRules.IGNITION_TICKS;mInventoryChanged=true;setChanged();}
        return 10000;
    }
    private boolean adjacentEnergySourcesChanged = true;
    private Direction adjacentEnergyFacing;
    private int adjacentEnergyInputs = -1;
    public void adjacentEnergySourcesChanged() { adjacentEnergySourcesChanged = true; }
    protected void updateAdjacentToggleableEnergySources() {
        if (level == null || level.isClientSide || spec == null || isRemoved()) return;
        for (var side : Direction.values()) if (isEnergyAcceptingFrom(spec.energyTag(), side, true)) {
            var target = worldPosition.relative(side);
            if (level.hasChunkAt(target)) gregtech.engine.machine.AdjacentEnergyControl.update(
                    level.getBlockEntity(target), spec.energyTag(), side.getOpposite(), !covers.stopped());
        }
    }
    private void refreshAdjacentEnergySources() {
        var facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if (adjacentEnergySourcesChanged || adjacentEnergyFacing != facing || adjacentEnergyInputs != faceConfig.energyInputs()) {
            adjacentEnergySourcesChanged = false; adjacentEnergyFacing = facing; adjacentEnergyInputs = faceConfig.energyInputs();
            updateAdjacentToggleableEnergySources();
        }
    }
    private final BasicMachineCovers covers = new BasicMachineCovers(this);
    @Override public gregtech.content.cover.PanelCoverRuntime panels() { return covers.panels(); }
    @Override public boolean coverSupportsPossible() { return true; }
    @Override public boolean coverPossible(Direction side) { return coverState().possible(); }

    private final gregtech.api.platform.machine.MachineControl control = new gregtech.api.platform.machine.MachineControl() {
        public boolean available(){return !isRemoved();}
        public boolean enabled(){return available() && !covers.stopped();}
        public boolean setEnabled(boolean enabled){
            if (!available())return false;
            if (level != null && level.isClientSide) return !covers.stopped();
            boolean changed = covers.setStopped(!enabled);
            mInventoryChanged = true;
            if (changed)updateAdjacentToggleableEnergySources();
            if (!enabled){energyPort.clearStored();mActive=false;mRunning=false;onControlStopped();updateBlockStates(false,false);}
            setChanged();return !covers.stopped();
        }
        public boolean running(){return available()&&controlRunning();}
        public boolean active(){return available()&&controlActive();}
        public long progress(){return available()?controlProgress():0;}
        public long progressMax(){return available()?controlProgressMax():0;}
        public long energy(){return available()?energyPort.stored():0;}
        public long energyCapacity(){return available()?inputMaximum():0;}
        public long progressPerTick(){return available()?controlProgressPerTick():0;}
        public int externalSignal(Direction side){
            return available() && side != null ? covers.externalSignal(side) : 0;
        }
        public boolean setExternalSignal(Direction side,int level){
            if (!available()||side==null||level<0||level>15)return false;
            covers.setExternalSignal(side, level);
            return true;
        }
    };
    /** Dedicated executors override read state without duplicating switch/external-signal ownership. */
    protected boolean controlRunning() { return mRunning; }
    protected boolean controlActive() { return mActive; }
    protected long controlProgress() { return covers.successful() ? 1 : processor.progress(); }
    protected long controlProgressMax() { return covers.successful() ? 1 : processor.totalWork(); }
    protected long controlProgressPerTick() { return mActive ? Math.min(inputMaximum(), energyPort.stored()) : 0; }
    protected void onControlStopped() {}

    @Override public gregtech.api.platform.machine.MachineControl machineControl(Direction side){return control;}
    private boolean mInventoryChanged = true; // inventory changed, re-check recipe

    public BasicMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.autoIO = new MachineAutoIO(this);
    }

    // ── Spec and RecipeMap ────────────────────────────────────────────────

    public void setSpec(BasicMachineSpec spec) {
        this.spec = spec;
        adjacentEnergySourcesChanged();
        this.recipeMap = spec.recipeMap();
        processor.invalidate();
        this.faceConfig = spec.faceConfig();
        this.inventory = new MachineInventory(recipeMap, spec.parallelLimit(), this::inventoryChanged);
        ioPorts.rebuildItemView();
        this.inventory.items().setEmptyInputsOnly(tools::emptyInputsOnly);
        if (specDirty) sanitizeForSpec();
    }

    public BasicMachineSpec spec() { return spec; }
    private boolean usesOriginalFluidBuffers() {
        return spec != null && "gt6ce".equals(spec.namespace())
                && this instanceof gregtech.engine.multiblock.tracking.TrackedMultiblock;
    }
    public RecipeMap recipeMap() { return recipeMap; }
    public FaceConfig faceConfig() { return faceConfig; }
    public FluidTankGT[] getTanksInput() { return tanksInput(); }
    @Override public long gibblValue(BlockFace side) { return java.util.Arrays.stream(tanksInput()).mapToLong(FluidTankGT::getAmount).sum(); }
    @Override public long gibblMaximum(BlockFace side) { return java.util.Arrays.stream(tanksInput()).mapToLong(FluidTankGT::capacity).sum(); }
    public FluidTankGT[] getTanksOutput() { return tanksOutput(); }
    /** Output-buffer stop threshold for GUI and inspection, not the physical overflow capacity. */
    public long fluidOutputDisplayCapacity(int tankIndex) { return processor.fluidOutputDisplayCapacity(tankIndex); }
    public long getEnergyTick() { return energyPort.stored(); }
    /** Sanitized per-tick cost of the current job; 0 when idle. */
    public long minimumEnergy() { return processor.minimumEnergy(); }
    /** Clamped batch size of the current job. */
    public int parallelCount() { return processor.parallelCount(); }
    @Nullable public gregtech.engine.machine.RecipeProcessor.RecipeDetails recipeDetails() { return processor.recipeDetails(); }

    final MachineInventory inventoryStorage() { return inventory; }
    private MachineItemHandler itemHandler() { return inventory == null ? null : inventory.items(); }
    private FluidTankGT[] tanksInput() { return inventory == null ? null : inventory.tanksInput(); }
    private FluidTankGT[] tanksOutput() { return inventory == null ? null : inventory.tanksOutput(); }

    public double averageEnergyInput() {
        return energyPort.average(level);
    }
    public boolean isEnergyOverloaded() { return energyPort.overloaded(level); }
    public boolean isEnergyInsufficient() { return energyPort.insufficient(level); }
    @Override public void onInsufficientEnergy(GregTechTags.Tag type, @Nullable Direction side) {
        if (!usesAuxiliaryInput(type, side)) energyPort.recordInsufficient(level);
    }
    public boolean isRunning() { return mRunning; }
    public int getProgressPercent() {
        return (int) (getProgressFraction() * 100);
    }

    public double getProgressFraction() {
        if (processor.totalWork() <= 0) return 0;
        return Math.max(0, Math.min(1, (double) processor.progress() / processor.totalWork()));
    }

    // ── Server tick ──────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, BasicMachineBlockEntity be) {
        be.beginMachineChanges();
        try { be.tickServer(level, pos); }
        finally { be.endMachineChanges(); }
    }

    private void tickServer(Level level, BlockPos pos) {
        if (faceConfig == null || spec == null) return;
        refreshAdjacentEnergySources();
        if (sourceRequiresIgnition()&&sourceIgnitionTicks>0)sourceIgnitionTicks--;
        beforeMachineTick();
        beginCoverTick();
        if (!structureComplete()) {
            covers.clearWorkState();
            if (mActive || mRunning) {
                mActive = mRunning = false;
                updateBlockStates(false, false);
            }
            updateCoverSignals();
            return;
        }
        covers.refreshWorkPossible(this.level, processor, mInventoryChanged);
        boolean allowed = switchesAllowRunning();
        covers.setSuccessful(false);
        if (!allowed) {
            energyPort.clearStored();
            if (mActive||mRunning){mActive=false;mRunning=false;updateBlockStates(false,false);}
            tickCovers(level,pos);
            updateCoverSignals();
            return;
        }

        long energyIn = spec.energyIn();
        long energyInMin = inputMinimum();
        long energyInMax = inputMaximum();

        if (usesTimeEnergy() && switchesAllowRunning()) energyPort.addPackets(inputMaximum(),1,1);
        boolean wasRunning = mRunning;
        boolean wasActive = mActive;

        // ── Poll energy from adjacent sources ────────────────────────────
        if (mRunning || mInventoryChanged || level.getGameTime() % 12 == 0) {
            pollEnergy(level, pos, energyIn);
        }

        if (processor.totalWork() > 0 && processor.progress() >= processor.totalWork()) {
            doActive(level, pos, 0);
            mActive = false;
            mRunning = false;
        } else if (energyPort.stored() >= energyInMin && energyPort.stored() >= processor.minimumEnergy()) {
            // A failed lookup must not be repeated in the same tick just because inventory changed.
            // Try to start a new recipe even if one isn't in progress
            if (processor.totalWork() <= 0) {
                checkRecipe();
            }
            if (processor.totalWork() > 0) {
                mActive = true;
                mRunning = true;
                doActive(level, pos, Math.min(energyInMax, energyPort.stored()));
            } else {
                mActive = false;
                mRunning = true; // energy present but no valid recipe — standby
                autoIO(level, pos);
            }
        } else {
            // No energy — shut down
            if (requiresConstantEnergy() && processor.totalWork() > 0 && processor.progress() > 0) {
                // Constant-energy machines lose progress, not the already-rolled job outputs.
                processor.loseProgress();
                markMachineDataChanged();
            }
            mActive = false;
            mRunning = false;
        }

        // ── Drain buffer ─────────────────────────────────────────────────
        energyPort.drain(energyInMax);

        if (covers.successful()) {
            mInventoryChanged = true;
            covers.refreshWorkPossible(this.level, processor, mInventoryChanged);
        }
        mInventoryChanged = false;

        // ── Cover behaviors ──────────────────────────────────────────────────
        tickCovers(level, pos);

        // ── Sync machine states to block ─────────────────────────────────
        if (mRunning != wasRunning || mActive != wasActive) {
            updateBlockStates(mRunning, mActive);
        }
        updateCoverSignals();
    }

    private void updateBlockStates(boolean running, boolean active) {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (state.hasProperty(BasicMachineBlock.RUNNING)) {
            state = state.setValue(BasicMachineBlock.RUNNING, running);
        }
        if (state.hasProperty(GTFacingMachineBlock.LIT)) {
            state = state.setValue(GTFacingMachineBlock.LIT, active);
        }
        level.setBlock(worldPosition, state, 3);
        if (this instanceof gregtech.engine.multiblock.tracking.TrackedMultiblock tracked)
            gregtech.engine.multiblock.tracking.StructureTracking.refreshDesigns(tracked);
    }

    private void pollEnergy(Level level, BlockPos pos, long size) {
        if (faceConfig.energyInputs() == 0) return;
        for (int dir = 0; dir <= 5; dir++) {
            if (!FaceConfig.has(faceConfig.energyInputs(), dir)) continue;
            Direction absDir = relativeToAbsolute(dir);
            BlockPos adj = pos.relative(absDir);
            if (!level.hasChunkAt(adj)) continue;
            BlockEntity be = level.getBlockEntity(adj);

//? if >=1.21.1 {
/*            if (be instanceof gregtech.engine.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) continue;
*///?} else {
            if (be instanceof MultiblockPortBlockEntity part && part.isBoundTo(worldPosition)) continue;
//?}

            IEnergyBlock source = EnergyBlockViews.view(be);
            if (source == null) continue;
            if (!source.isEnergyEmittingTo(energyTag(), absDir.getOpposite(), false)) continue;
            long offered = source.getEnergyOffered(energyTag(), absDir.getOpposite(), size);
            if (offered <= 0) continue;
            long requested = energyPort.accepted(size, offered, inputMaximum());
            if (requested <= 0) continue;
            long extracted = source.doEnergyExtraction(energyTag(), absDir.getOpposite(), size, requested, true);
            if (extracted > 0) energyPort.addReceived(size, extracted, inputMaximum(), level);
        }
        var auxiliary = spec.auxiliaryEnergy();
        if (auxiliary == null) return;
        var auxTag = auxiliary.type();
        long auxSize = auxiliary.inputPerTick();
        long auxCap = auxiliary.capacity();
        for (int dir = 0; dir <= 5; dir++) {
            if (!FaceConfig.has(auxiliary.faceMask(), dir)) continue;
            Direction absDir = relativeToAbsolute(dir);
            BlockPos adj = pos.relative(absDir);
            if (!level.hasChunkAt(adj)) continue;
            BlockEntity be = level.getBlockEntity(adj);
//? if >=1.21.1 {
/*            if (be instanceof gregtech.engine.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) continue;
*///?} else {
            if (be instanceof MultiblockPortBlockEntity part && part.isBoundTo(worldPosition)) continue;
//?}
            IEnergyBlock source = EnergyBlockViews.view(be);
            if (source == null || !source.isEnergyEmittingTo(auxTag, absDir.getOpposite(), false)) continue;
            long offered = source.getEnergyOffered(auxTag, absDir.getOpposite(), auxSize);
            if (offered <= 0) continue;
            long requested = energyPort.auxAccepted(auxSize, offered, auxCap);
            if (requested <= 0) continue;
            long extracted = source.doEnergyExtraction(auxTag, absDir.getOpposite(), auxSize, requested, true);
            if (extracted > 0) {
                energyPort.addAux(auxSize, extracted, auxCap);
                energyPort.payStartup();
            }
        }
    }

    private void doActive(Level level, BlockPos pos, long aEnergy) {
        if (energyPort.startup() > 0) {
            energyPort.payStartup();
            if (energyPort.startup() > 0) {
                markMachineDataChanged();
                return;
            }
        }
        if (processor.advance(aEnergy)) {
            if (!produceOutputs()) {
                processor.holdAtTotal();
                autoIO(level, pos);
                markMachineDataChanged();
                return;
            }
            covers.setSuccessful(true);
            onProcessFinished();
            processor.reset();
            energyPort.clearStartup();
            // Immediately check for next recipe
            if (aEnergy > 0 && processor.totalWork() <= 0) checkRecipe();
        }
        autoIO(level, pos);
        markMachineDataChanged();
    }

    // ── Covers ──────────────────────────────────────────────────────────

    public ItemStack getCover(Direction side) { return covers.get(side); }

    /** Attaches a cover (one item from the stack); false if the face is occupied. */
    public boolean attachCover(Direction side, ItemStack stack) { return covers.attach(side, stack); }

    /** Removes and returns the cover on a face (EMPTY if none). */
    public ItemStack removeCover(Direction side) { return covers.remove(side); }

    @Override
    public void attached(Direction side, ItemStack stack) {
        coverChanged();
    }

    @Override
    public void removed(Direction side, ItemStack cover) {
        covers.releaseControl(cover);
        coverChanged();
    }

    private void coverChanged() {
        mInventoryChanged = true;
        invalidateSideCaps();
        covers.syncCover(level, worldPosition);
        updateCoverSignals();
    }

//? if >=1.21.1 {
/*    private void invalidateSideCaps() { invalidateCapabilities(); }
*///?} else {
    private void invalidateSideCaps() {
        ioPorts.invalidateSideCaps();
    }
//?}

    /** Cover-only lifecycle; recipe processing and energy polling stay on the machine's tick. */
    protected final void beginCoverTick() { covers.beginTick(); }
    protected final void tickCovers(Level level, BlockPos pos) { covers.tick(level, pos); }

    public boolean hasRedstoneCover() { return covers.hasRedstoneCover(); }

    public int getRedstoneSignal() {
        int signal = 0;
        for (Direction side : Direction.values()) signal = Math.max(signal, getRedstoneSignal(side));
        return signal;
    }

    public int getRedstoneSignal(Direction output) { return covers.signal(output); }
    public int getStrongRedstoneSignal(Direction output) { return covers.strongSignal(output); }
    public MachineCoverSpec.State coverState() { return covers.state(mRunning, mActive, processor.totalWork()); }
    final boolean coverPowered() { return mRunning; }
    protected final void updateCoverSignals() { covers.updateSignals(level, worldPosition); }

    public boolean configureControlCover(Direction side, boolean cutter) {
        return covers.configureControl(side, cutter, level, worldPosition);
    }

    /** The attached selector's recipe tag, or -2 when no selector is attached. */
    public int getTagSelectorCircuit() { return covers.tagSelectorCircuit(); }

    /** Shutter covers seal the face against item and fluid transfer. */
    public boolean isFaceShuttered(Direction side) { return panels().shuttered(side); }

    /** The last controller in side order owns the switch. */
    protected boolean switchesAllowRunning() { return covers.switchesAllowRunning(level, worldPosition); }

    /** Multiblock controllers override this to require a valid structure. */
    protected boolean usesTimeEnergy() { return spec!=null&&spec.energyTag()==GregTechTags.Energy.TU; }
    protected long inputMinimum() { return spec.energyInMin(); }
    protected long inputMaximum() { return spec.energyInMax(); }
    protected boolean parallelScalesDuration() { return spec == null || spec.parallelScalesDuration(); }
    protected boolean cheapOverclocking() { return spec != null && spec.cheapOverclocking(); }
    protected boolean requiresConstantEnergy() { return gregtech.GregTechConfig.Rule.CONSTANT_ENERGY.enabled() && !usesTimeEnergy() && (spec == null || spec.constantEnergy()); }
    protected int efficiency() { return spec==null?10000:spec.processingEfficiency(); }

    protected boolean structureComplete() {
        return true;
    }

    public java.util.List<ItemStack> inspectionPendingItems() {
        var pending = processor.pendingOutputs();
        return pending == null ? java.util.List.of() : pending.inspectionItems(24);
    }
    public java.util.List<FluidStack> inspectionPendingFluids() {
        var pending = processor.pendingOutputs();
        return pending == null ? java.util.List.of() : pending.inspectionFluids(16);
    }
    public int inspectionPendingItemCount() {
        var pending = processor.pendingOutputs();
        return pending == null ? 0 : pending.pendingItemStacks();
    }

    protected void beforeMachineTick() {}
    protected boolean recipeStartAllowed() { return !sourceRequiresIgnition()||sourceIgnitionTicks>0||mActive; }
    protected void onProcessFinished() {if (sourceRequiresIgnition())sourceIgnitionTicks=gregtech.content.machine.OriginalBasicMachineRules.IGNITION_TICKS;}

    /** 1.7.10 {@code World.getBlockLightValue}: max of sky and block light, with night not subtracted. */
    private boolean lightAllowsStart() {
        int minimum = spec == null ? 0 : spec.minimumLightAbove();
        if (minimum <= 0 || level == null) return true;
        BlockPos above = worldPosition.above();
        if (!level.hasChunkAt(above)) return false;
        return level.getRawBrightness(above, 0) >= minimum;
    }

    private void checkRecipe() {
        if (processor.startJob() == gregtech.engine.machine.RecipeProcessor.StartResult.NO_RECIPE) energyPort.clearStartup();
    }

    private boolean usesOutputTankSearch() { return spec != null && spec.searchOutputTanks(); }
    private boolean usesExclusiveOutputs() { return spec != null && spec.exclusiveOutputs(); }

    private boolean produceOutputs() {
        boolean finished = processor.flushOutputs();
        setChanged(); return finished;
    }

    /** Deduct only deliveries confirmed by the shared transfer engine. */
    protected final void pushOutputItems(IItemHandler target, Direction side) {
        autoIO.pushOutputItems(target, side);
    }

    /** A changed source/target must not make an already extracted item disappear. */
    protected void returnOrDrop(ItemStack leftover, IItemHandler origin) {
        autoIO.returnOrDrop(leftover, origin);
    }

    // ── Auto I/O ─────────────────────────────────────────────────────────

    /** A declared item-output cell. Face auto-output stays on {@link #autoOutputItems}. */
    protected void positionedItemOutput() {}

    /** A declared item-input cell. Face auto-input stays on {@link #autoInputItems}. */
    protected void positionedItemInput() {}

    /** A declared fluid-input cell. Face auto-input stays on {@link #autoInputFluids}. */
    protected void positionedFluidInput() {}

    private void autoIO(Level level, BlockPos pos) { tools.autoIO(level, pos, faceConfig); }

    protected boolean automaticTransferEnabled(Transfer transfer) {
        return tools.enabled(transfer);
    }

    protected boolean toolModeAllowsStart() {
        return tools.allowsRecipeStart(inventory);
    }

    /** A reset cancels consumed work, but never removes items already in the machine's slots. */
    protected void resetProcessingWithTool() {
        processor.cancelJob();
        covers.clearWorkState();
        mActive = mRunning = false;
        updateBlockStates(false, false);
    }

    @Override
    public boolean consumesTool(ItemStack tool) {
        return tools.consumes(tool)
                || (sourceRequiresIgnition() && gregtech.content.item.behavior.ItemBehaviors.isIgniter(tool));
    }

    public InteractionResult useMachineTool(Player player, InteractionHand hand, BlockHitResult hit) {
        return tools.use(player, hand, hit, faceConfig, spec != null);
    }

    protected void appendMachineInspection(java.util.List<Component> messages) {
        tools.appendInspection(messages, faceConfig);
    }

    @Override public MachineInventory autoIOInventory() { return inventory; }
    @Override public Direction autoIORelativeToAbsolute(int relDir) { return relativeToAbsolute(relDir); }
    @Override public boolean autoIOShutters(Direction side) { return isFaceShuttered(side); }
    @Override public boolean autoIOAllowsItemTraffic(Direction side, ItemStack stack, boolean inserting) { return BasicMachineIOPorts.allowsItemTraffic(this, side, stack, inserting); }
    @Override public boolean autoIOAcceptsInput(int slot, ItemStack stack) { return canInsertAutomatically(slot, stack); }
    @Override public IFluidHandler autoIOFluidOutputTarget(Level level, BlockPos pos, Direction absolute) { return automaticFluidOutputTarget(level, pos, absolute); }
    @Override public BlockEntity autoIOOwner() { return this; }

    protected void autoInputItems(Level level, BlockPos pos, int relDir) {
        autoIO.inputItems(level, pos, relDir);
    }

    protected void autoOutputItems(Level level, BlockPos pos, int relDir) {
        autoIO.outputItems(level, pos, relDir);
    }

    protected void autoInputFluids(Level level, BlockPos pos, int relDir) {
        autoIO.inputFluids(level, pos, relDir);
    }

    protected void autoOutputFluids(Level level, BlockPos pos, int relDir) {
        autoIO.outputFluids(level, pos, relDir);
    }

    /** Dedicated controllers may output beneath their structure rather than the main block. */
    protected IFluidHandler automaticFluidOutputTarget(Level level,BlockPos pos,Direction absolute) {
        return autoIO.defaultFluidOutputTarget(level, pos, absolute);
    }

    // ── Direction helpers ─────────────────────────────────────────────────

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    final int relativeDir(Direction absolute) {
        return MachineDirections.relative(facing(), absolute);
    }

    /** Convert machine-relative direction index (TOP/BOTTOM/LEFT/RIGHT/FRONT/BACK)
     *  to the absolute world {@link Direction} based on the block's facing. */
    protected Direction relativeToAbsolute(int relDir) {
        return MachineDirections.absolute(facing(), relDir);
    }

    // ── IFluidHandler ────────────────────────────────────────────────────

    @Override
    public int getTanks() { return inventory == null ? 0 : inventory.getTanks(); }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return inventory == null ? FluidStack.EMPTY : inventory.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return inventory == null ? 0 : inventory.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return inventory != null && inventory.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return inventory == null ? 0 : inventory.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return inventory == null ? FluidStack.EMPTY : inventory.drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return inventory == null ? FluidStack.EMPTY : inventory.drain(maxDrain, action);
    }

    /**
     * Funnel and nozzle access is separate from the facing capability: funnels only fill the
     * input tanks, taps and nozzles drain outputs first and then fall back to the inputs.
     */
    @Override
    public IFluidHandler fluidToolHandler(Direction side, boolean filling) {
        if (isRemoved() || inventory == null) return null;
        return filling ? inventory.toolFillHandler() : inventory.toolDrainHandler();
    }

    // ── IEnergyBlock ──────────────────────────────────────────────────────

    private GregTechTags.Tag energyTag() {
        return spec == null ? null : spec.energyTag();
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return energyPort.isEnergyType(this, energyType, side, emitting);
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return energyPort.getEnergyTypes(this, side);
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (!isEnergyType(energyType, side, false) || (!theoretical && covers.stopped())) return false;
        if (side == null) return true;
        return (spec.energyType().equalsIgnoreCase(energyType.getShortName())
                && FaceConfig.has(faceConfig.energyInputs(), relativeDir(side)))
                || usesAuxiliaryInput(energyType, side);
    }

    /** Disjoint faces distinguish two inputs of the same type; unsided calls retain auxiliary priority. */
    @Override
    public boolean usesAuxiliaryInput(GregTechTags.Tag energyType, @Nullable Direction side) {
        var auxiliary = spec == null ? null : spec.auxiliaryEnergy();
        return auxiliary != null && auxiliary.type() == energyType
                && (side == null || FaceConfig.has(auxiliary.faceMask(), relativeDir(side)));
    }

    @Override
    public GregTechTags.@Nullable Tag auxiliaryTag() {
        var auxiliary = spec == null ? null : spec.auxiliaryEnergy();
        return auxiliary == null ? null : auxiliary.type();
    }
    @Override public long energyInputMaximum() { return inputMaximum(); }
    @Override public void markEnergyDataChanged() { markMachineDataChanged(); }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return energyPort.isEnergyEmittingTo(this, energyType, side, theoretical);
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side,
                         long size, long amount, boolean doInject) {
        return energyPort.doInject(this, energyType, side, size, amount, doInject);
    }

    /** Original root overload policy, shared by EU, RU, KU and other machine inputs. */
    @Override public void overloadEnergy(long size, GregTechTags.Tag energyType) {
        energyPort.recordOverload(level);
        gregtech.engine.energy.MachineOverload.apply(this, size, energyType);
    }

    @Override
    public long doExtract(GregTechTags.Tag energyType, @Nullable Direction side,
                           long size, long amount, boolean doExtract) {
        return energyPort.doExtract(this, energyType, side, size, amount, doExtract);
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return energyPort.getEnergyDemanded(this, energyType, side, size);
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return energyPort.getEnergyOffered(this, energyType, side, size);
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyPort.getEnergySizeInputRecommended(this, energyType, side);
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyPort.getEnergySizeInputMin(this, energyType, side);
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyPort.getEnergySizeInputMax(this, energyType, side);
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyPort.getEnergySizeOutputRecommended(this, energyType, side);
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyPort.getEnergySizeOutputMin(this, energyType, side);
    }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyPort.getEnergySizeOutputMax(this, energyType, side);
    }

    // ── Capabilities with side filtering ─────────────────────────────────


//? if >=1.21.1 {
/*    public IItemHandler itemCapability(@Nullable Direction side) {
        return ioPorts.itemCapability(side, faceConfig);
    }

    public IFluidHandler fluidCapability(@Nullable Direction side) {
        return ioPorts.fluidCapability(side, faceConfig);
    }
*///?} else {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (inventory != null) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) return ioPorts.itemCapability(side, faceConfig).cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER) return ioPorts.fluidCapability(side, faceConfig).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        ioPorts.invalidateCaps();
    }
//?}

    // ── MenuProvider ──────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block." + (spec != null ? spec.namespace() : "gt6ce") + "." + (spec != null ? spec.id() : "basic_machine"));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new BasicMachineContainerMenu(containerId, playerInv, this);
    }

    // ── NBT ──────────────────────────────────────────────────────────────

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);

        if (sourceRequiresIgnition())tag.putByte(gregtech.content.machine.OriginalBasicMachineRules.IGNITION_NBT,(byte)sourceIgnitionTicks);
        tools.save(tag);
        covers.saveControl(tag);
        if (inventory != null)
            tag.put(NBT_INVENTORY, gregtech.engine.stack.HandlerNbt.save(inventory.items(), lookup));

        if (inventory.potionInput() != null)
            tag.put("gt.potion_input", gregtech.engine.stack.HandlerNbt.save(inventory.potionInput(), lookup));

        if (recipeMap == MachineRecipeMaps.Autocrafter || !autocraftingProgram.getStackInSlot(0).isEmpty())
            tag.put("gt.autocrafting.program", gregtech.engine.stack.HandlerNbt.save(autocraftingProgram, lookup));

        tag.putLong(NBT_ENERGY, energyPort.stored());
        processor.save(tag, lookup);
        if (spec != null && spec.auxiliaryEnergy() != null) {
            tag.putLong("gt.aux_energy", energyPort.auxStored());
            tag.putLong("gt.startup_energy", energyPort.startup());
        }

        if (inventory != null) {
            for (int i = 0; i < inventory.tanksInput().length; i++) {
                CompoundTag t = new CompoundTag();

                gregtech.engine.stack.NbtCompat.writeTank(inventory.tanksInput()[i],t,lookup);

                tag.put(NBT_TANKS_IN + i, t);
            }
        }
        if (inventory != null) {
            for (int i = 0; i < inventory.tanksOutput().length; i++) {
                CompoundTag t = new CompoundTag();

                gregtech.engine.stack.NbtCompat.writeTank(inventory.tanksOutput()[i],t,lookup);

                tag.put(NBT_TANKS_OUT + i, t);
            }
        }

        covers.save(tag, lookup);
    }

    // ── Client sync (covers only — everything else travels via menus) ───────

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        covers.save(tag, lookup);
        gregtech.engine.multiblock.tracking.ClientFormedSync.write(this, tag);
    }

    @Override
    protected void readUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        covers.load(tag, lookup);
        gregtech.engine.multiblock.tracking.ClientFormedSync.read(this, tag);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);

        if (tag.contains("gt.autocrafting.program", 10)) {
            CompoundTag savedProgram = tag.getCompound("gt.autocrafting.program").copy();
            savedProgram.putInt("Size", 1);

            gregtech.engine.stack.HandlerNbt.load(autocraftingProgram, savedProgram, lookup);

        }
        sourceIgnitionTicks=Math.max(0,tag.getByte(gregtech.content.machine.OriginalBasicMachineRules.IGNITION_NBT));
        tools.load(tag);
        covers.loadControl(tag);
        adjacentEnergySourcesChanged();
        mInventoryChanged = true;
        if (tag.contains(NBT_ENERGY)) energyPort.setStored(tag.getLong(NBT_ENERGY));
        processor.load(tag, lookup);
        var auxiliary = spec == null ? null : spec.auxiliaryEnergy();
        energyPort.setAuxStored(auxiliary == null ? 0
                : Math.max(0, Math.min(auxiliary.capacity(), tag.getLong("gt.aux_energy"))));
        energyPort.setStartup(auxiliary == null ? 0 : Math.max(0, tag.getLong("gt.startup_energy")));
        if (processor.totalWork() <= 0) energyPort.clearStartup();
        if (inventory == null) {
            // Called before setSpec — defer
        } else if (tag.contains(NBT_INVENTORY)) {
            gregtech.engine.stack.HandlerNbt.load(inventory.items(), tag.getCompound(NBT_INVENTORY), lookup);
        }
        if (inventory != null && inventory.potionInput() != null && tag.contains("gt.potion_input")) {
            gregtech.engine.stack.HandlerNbt.load(inventory.potionInput(), tag.getCompound("gt.potion_input"), lookup);
        }
        if (inventory != null) {
            for (int i = 0; i < inventory.tanksInput().length; i++)
                if (tag.contains(NBT_TANKS_IN + i))

                    gregtech.engine.stack.NbtCompat.readTank(inventory.tanksInput()[i],tag.getCompound(NBT_TANKS_IN + i),lookup);

        }
        if (inventory != null) {
            for (int i = 0; i < inventory.tanksOutput().length; i++)
                if (tag.contains(NBT_TANKS_OUT + i))

                    gregtech.engine.stack.NbtCompat.readTank(inventory.tanksOutput()[i],tag.getCompound(NBT_TANKS_OUT + i),lookup);

        }

        covers.load(tag, lookup);
        sanitizeForSpec();
    }

    /** Re-derive or bound every value that a save or {@code /data merge block} could have forged. */
    private void sanitizeForSpec() {
        if (spec == null) { specDirty = true; return; }
        specDirty = false;
        if (inventory != null) inventory.restoreConfiguredTankCapacities();
        energyPort.setStored(clamp(energyPort.stored(), 0, inputMaximum()));
        processor.sanitize();
        if (!processor.hasPendingOutputs() || processor.totalWork() <= 0) energyPort.clearStartup();
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    // ── Machine item handler ──────────────────────────────────────────────

    protected boolean acceptsAutomaticInput(ItemStack stack) {
        if (recipeMap == null || stack.isEmpty()) return false;
        if (recipeMap.containsInput(stack)) return true;
        return recipeMap == MachineRecipeMaps.Autocrafter && level != null
                && gregtech.content.recipe.AutocraftingRecipes.containsInput(
                        level, this, autocraftingProgram.getStackInSlot(0), stack);
    }

    /**
     * GT6 {@code MultiTileEntityBasicMachine.canInsertItem2}: automatic transfer reaches input
     * slots only, admits recipe inputs (or potions in their own slot), and keeps one stack per slot.
     */
    protected boolean canInsertAutomatically(int viewSlot, ItemStack stack) {
        if (inventory == null || viewSlot < 0 || viewSlot >= visibleInputSlots()) return false;
        if (viewSlot >= inputSlots())
            return inventory.potionInput() != null && inventory.potionInput().isItemValid(0, stack);
        return acceptsAutomaticInput(stack) && inventory.items().automationSlotAccepts(viewSlot, stack);
    }
    @Override public IItemHandler componentItems(Direction side) {
        return ioPorts.componentItems();
    }

    public IItemHandlerModifiable inventory() { return inventory == null ? null : inventory.items(); }

    /** Normal handler changes and GUI live-stack merges both invalidate the inventory work state. */
    public void inventoryChanged() {
        setChanged();
        mInventoryChanged = true;
        processor.invalidate();
    }
    public int inputSlots() { return recipeMap != null ? recipeMap.itemInputSlots : 0; }
    public int potionSlots() { return recipeMap != null ? recipeMap.potionInputSlots : 0; }
    /** Boundary of the composite item view: ordinary inputs first, then the dedicated potion inputs. */
    public int visibleInputSlots() { return inputSlots() + potionSlots(); }
    public int outputSlots() { return recipeMap != null ? recipeMap.itemOutputSlots : 0; }
    /** Composite item view; its slot indices follow {@link #visibleInputSlots()}, not the raw handler. */
    public IItemHandler itemView() { return ioPorts.itemView(); }

    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        gregtech.engine.inventory.BlockContents.drop(this, itemHandler());
        if (inventory != null && inventory.potionInput() != null)
            gregtech.engine.inventory.BlockContents.drop(this, inventory.potionInput());
        gregtech.engine.inventory.BlockContents.drop(this, autocraftingProgram);
        covers.drop();
        setChanged();
    }

    public boolean coverFilterPermits(Direction side, ItemStack candidate) {
        return covers.itemFilterPermits(side, candidate, level);
    }

    public boolean coverFluidFilterPermits(Direction side, FluidStack candidate) {
        return covers.fluidFilterPermits(side, candidate, level);
    }

    public ItemStack coverItemFilter(Direction side) { return covers.itemFilter(side, level); }
    public FluidStack coverFluidFilter(Direction side) { return covers.fluidFilter(side, level); }

    public boolean clickFilterCover(Direction side, ItemStack held) {
        return covers.clickFilter(side, held, level, worldPosition);
    }

    public boolean configureFilterCover(Direction side, FilterAction action) {
        return covers.configureFilter(side, action, level, worldPosition);
    }

}
