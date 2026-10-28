package gregtech.content.blockentity.machine;

import gregtech.engine.energy.EnergyBlockViews;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.machine.BasicMachineEnergy;
import gregtech.engine.energy.IEnergyBlock;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.machine.MachineAutoIO;
import gregtech.engine.machine.MachineDirections;
import gregtech.engine.machine.MachineEnergyPort;
import gregtech.engine.machine.MachineInventory;
import gregtech.engine.machine.MachineItemHandler;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
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
import java.util.Collections;

public class BasicMachineBlockEntity extends GTEnergyBlockEntity implements MenuProvider, IFluidHandler, gregtech.engine.inventory.BlockContents, gregtech.api.machine.MachineControl.Provider, gregtech.content.cover.PanelCoverHost, gregtech.content.cover.CoverStore.Hooks, gregtech.content.item.behavior.ItemBehaviors.Ignitable, gregtech.api.sensor.CompressionSensorSource, MachineEnergyPort.Host, MachineAutoIO.Host {
    private static final String NBT_SPEC = "gt.spec";
    private static final String NBT_INVENTORY = "gt.inventory";
    private static final String NBT_TANKS_IN = "gt.tanks_input";
    private static final String NBT_TANKS_OUT = "gt.tanks_output";
    private static final String NBT_ENERGY = "gt.energy";
    private static final String NBT_FACE_CONFIG = "gt.face_config";

    private BasicMachineSpec spec;
    /** A save loaded before this machine had a spec; sanitize once the spec arrives. */
    private boolean specDirty;
    private RecipeMap recipeMap;
    private final MachineAutoIO autoIO;
    private MachineInventory inventory;


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

    // ── Processing state ─────────────────────────────────────────────────
    private final MachineEnergyPort energyPort = new MachineEnergyPort();
    private final gregtech.engine.machine.RecipeProcessor.Config recipeConfig =
            new gregtech.engine.machine.RecipeProcessor.Config() {
        public RecipeMap recipeMap() { return BasicMachineBlockEntity.this.recipeMap; }
        public boolean searchOutputTanks() { return usesOutputTankSearch(); }
        public boolean exclusiveOutputs() { return usesExclusiveOutputs(); }
        public int parallelLimit() { return spec == null ? 1 : spec.parallelLimit(); }
        public boolean parallelScalesDuration() { return BasicMachineBlockEntity.this.parallelScalesDuration(); }
        public boolean cheapOverclocking() { return BasicMachineBlockEntity.this.cheapOverclocking(); }
        public boolean usesTimeEnergy() { return BasicMachineBlockEntity.this.usesTimeEnergy(); }
        public int efficiency() { return BasicMachineBlockEntity.this.efficiency(); }
        public long inputMinimum() { return BasicMachineBlockEntity.this.inputMinimum(); }
        public long inputMaximum() { return BasicMachineBlockEntity.this.inputMaximum(); }
        public long nominalEnergyInput() { return spec == null ? 0 : spec.energyIn(); }
        public long startupEnergyFor(Recipe recipe) {
            return spec != null && spec.startupEnergy() && recipe.mSpecialValue > 0 ? recipe.mSpecialValue : 0;
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
    private GregTechTags.Tag auxTag;
    private boolean mActive;        // currently processing a recipe
    private boolean mRunning;       // machine is in running state (energy present)
    private int sourceIgnitionTicks;
    private boolean sourceRequiresIgnition() {return spec!=null&&gregtech.content.machine.OriginalBasicMachineRules.requiresIgnition(spec.machineName(),spec.tier());}
    @Override public long onIgnite(Level level,BlockPos pos,Direction side,Player player,ItemStack igniter,
            boolean sneaking,float hitX,float hitY,float hitZ) {
        if(!sourceRequiresIgnition())return 0;
        if(!level.isClientSide) {sourceIgnitionTicks=gregtech.content.machine.OriginalBasicMachineRules.IGNITION_TICKS;mInventoryChanged=true;setChanged();}
        return 10000;
    }
    private boolean controlStopped;
    /** External control input per absolute direction, 0–15; persisted with the machine. */
    private final byte[] externalSignals = new byte[6];
    private boolean adjacentEnergySourcesChanged = true;
    private Direction adjacentEnergyFacing;
    private int adjacentEnergyInputs = -1;
    public void adjacentEnergySourcesChanged() { adjacentEnergySourcesChanged = true; }
    protected void updateAdjacentToggleableEnergySources() {
        if (level == null || level.isClientSide || spec == null || isRemoved()) return;
        for (var side : Direction.values()) if (isEnergyAcceptingFrom(spec.energyTag(), side, true)) {
            var target = worldPosition.relative(side);
            if (level.hasChunkAt(target)) gregtech.engine.machine.AdjacentEnergyControl.update(
                    level.getBlockEntity(target), spec.energyTag(), side.getOpposite(), !controlStopped);
        }
    }
    private void refreshAdjacentEnergySources() {
        var facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if (adjacentEnergySourcesChanged || adjacentEnergyFacing != facing || adjacentEnergyInputs != faceConfig.energyInputs()) {
            adjacentEnergySourcesChanged = false; adjacentEnergyFacing = facing; adjacentEnergyInputs = faceConfig.energyInputs();
            updateAdjacentToggleableEnergySources();
        }
    }
    private final gregtech.content.cover.CoverStore coverStore = new gregtech.content.cover.CoverStore(this);
    @Override public gregtech.content.cover.PanelCoverRuntime panels(){return coverStore.panels();}
    @Override public boolean coverSupportsPossible(){return true;}
    @Override public boolean coverPossible(Direction side){return coverState().possible();}
    private boolean successful,workPossible;
    private long coverTicks;
    private final int[] coverSignals=new int[6];
    private final boolean[] coverStrong=new boolean[6];
    private final gregtech.api.machine.MachineControl control = new gregtech.api.machine.MachineControl() {
        public boolean available(){return !isRemoved();}
        public boolean enabled(){return available()&&!controlStopped;}
        public boolean setEnabled(boolean enabled){
            if(!available())return false;
            if(level!=null&&level.isClientSide)return !controlStopped;
            boolean changed=controlStopped==enabled;controlStopped=!enabled;mInventoryChanged=true;
            if(changed)updateAdjacentToggleableEnergySources();
            if(!enabled){energyPort.clearStored();mActive=false;mRunning=false;onControlStopped();updateBlockStates(false,false);}
            setChanged();return !controlStopped;
        }
        public boolean running(){return available()&&controlRunning();}
        public boolean active(){return available()&&controlActive();}
        public long progress(){return available()?controlProgress():0;}
        public long progressMax(){return available()?controlProgressMax():0;}
        public long energy(){return available()?energyPort.stored():0;}
        public long energyCapacity(){return available()?inputMaximum():0;}
        public long progressPerTick(){return available()?controlProgressPerTick():0;}
        public int externalSignal(Direction side){
            return available()&&side!=null?externalSignals[side.ordinal()]&15:0;
        }
        public boolean setExternalSignal(Direction side,int level){
            if(!available()||side==null||level<0||level>15)return false;
            if(externalSignals[side.ordinal()]==(byte)level)return true;
            externalSignals[side.ordinal()]=(byte)level;
            setChanged();
            return true;
        }
    };
    /** Dedicated executors override read state without duplicating switch/external-signal ownership. */
    protected boolean controlRunning() { return mRunning; }
    protected boolean controlActive() { return mActive; }
    protected long controlProgress() { return successful ? 1 : processor.progress(); }
    protected long controlProgressMax() { return successful ? 1 : processor.totalWork(); }
    protected long controlProgressPerTick() { return mActive ? Math.min(inputMaximum(), energyPort.stored()) : 0; }
    protected void onControlStopped() {}

    @Override public gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private int packedExternalSignals(){
        int packed=0;
        for(int i=0;i<6;i++)packed|=(externalSignals[i]&15)<<(4*i);
        return packed;
    }
    private void unpackExternalSignals(int packed){
        for(int i=0;i<6;i++)externalSignals[i]=(byte)((packed>>>(4*i))&15);
    }
    private boolean mInventoryChanged = true; // inventory changed, re-check recipe

//? if >=1.21.1 {
/**///?} else {

    // Internal caps (null side / GUI)
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(this::itemHandler);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> this);

    // Sided caps — created lazily
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] itemCapsBySide = new LazyOptional[6];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IFluidHandler>[] fluidCapsBySide = new LazyOptional[6];
//?}


    public BasicMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.autoIO = new MachineAutoIO(this);
    }

    // ── Spec and RecipeMap ────────────────────────────────────────────────

    public void setSpec(BasicMachineSpec spec) {
        this.spec = spec;
        this.auxTag = spec == null || spec.auxiliaryEnergyType() == null ? null
                : GregTechTags.Energy.ALL.stream()
                        .filter(tag -> tag.getShortName().equalsIgnoreCase(spec.auxiliaryEnergyType()))
                        .findFirst().orElse(null);
        adjacentEnergySourcesChanged();
        this.recipeMap = spec.recipeMap();
        processor.invalidate();
        this.faceConfig = spec.faceConfig();
        this.inventory = new MachineInventory(recipeMap, this::inventoryChanged);
        if (specDirty) sanitizeForSpec();
    }

    public BasicMachineSpec spec() { return spec; }
    public RecipeMap recipeMap() { return recipeMap; }
    public FaceConfig faceConfig() { return faceConfig; }
    public FluidTankGT[] getTanksInput() { return tanksInput(); }
    @Override public long gibblValue(int side) { return java.util.Arrays.stream(tanksInput()).mapToLong(FluidTankGT::getAmount).sum(); }
    @Override public long gibblMaximum(int side) { return java.util.Arrays.stream(tanksInput()).mapToLong(FluidTankGT::capacity).sum(); }
    public FluidTankGT[] getTanksOutput() { return tanksOutput(); }
    public long getEnergyTick() { return energyPort.stored(); }
    /** Sanitized per-tick cost of the current job; 0 when idle. */
    public long minimumEnergy() { return processor.minimumEnergy(); }
    /** Clamped batch size of the current job. */
    public int parallelCount() { return processor.parallelCount(); }

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
        if(sourceRequiresIgnition()&&sourceIgnitionTicks>0)sourceIgnitionTicks--;
        beforeMachineTick();
        beginCoverTick();
        if(!structureComplete()) { successful=false;workPossible=false;if(mActive||mRunning) {mActive=false;mRunning=false;updateBlockStates(false,false);}updateCoverSignals();return; }
        refreshWorkPossible();
        boolean allowed=switchesAllowRunning();successful=false;
        if(!allowed) {
            energyPort.clearStored();
            if(mActive||mRunning){mActive=false;mRunning=false;updateBlockStates(false,false);}
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

        if(successful){mInventoryChanged=true;refreshWorkPossible();}
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
        if (auxTag == null || spec.auxiliaryFaceMask() == 0) return;
        long auxSize = spec.auxiliaryEnergyIn();
        long auxCap = spec.auxiliaryEnergyCapacity();
        for (int dir = 0; dir <= 5; dir++) {
            if (!FaceConfig.has(spec.auxiliaryFaceMask(), dir)) continue;
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
            successful=true;
            onProcessFinished();
            processor.reset();
            energyPort.clearStartup();
            // Immediately check for next recipe
            if (aEnergy > 0 && processor.totalWork() <= 0) checkRecipe();
        }
        autoIO(level, pos);
        markMachineDataChanged();
    }

    // ── Covers (GT6 cover system, first iteration) ──────────────────────────

    public ItemStack getCover(Direction side) {
        return coverStore.get(side);
    }

    /** Attaches a cover (one item from the stack); false if the face is occupied. */
    public boolean attachCover(Direction side, ItemStack stack) {
        return coverStore.attach(side, stack);
    }

    /** Removes and returns the cover on a face (EMPTY if none). */
    public ItemStack removeCover(Direction side) {
        return coverStore.remove(side);
    }

    @Override
    public void attached(Direction side, ItemStack stack) {
        mInventoryChanged=true;
        invalidateSideCaps();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        updateCoverSignals();
    }

    @Override
    public void removed(Direction side, ItemStack cover) {
        var removedSpec=MachineCoverSpec.of(cover);
        if(removedSpec!=null&&!removedSpec.detector()||gregtech.content.cover.PanelCover.of(cover)==gregtech.content.cover.PanelCover.STATUS){controlStopped=false;updateAdjacentToggleableEnergySources();}
        mInventoryChanged=true;
        invalidateSideCaps();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        updateCoverSignals();
    }


//? if >=1.21.1 {
/*    private void invalidateSideCaps() { invalidateCapabilities(); }
*///?} else {
    private void invalidateSideCaps() {
        for (int i = 0; i < 6; i++) {
            if (itemCapsBySide[i] != null) { itemCapsBySide[i].invalidate(); itemCapsBySide[i] = null; }
            if (fluidCapsBySide[i] != null) { fluidCapsBySide[i].invalidate(); fluidCapsBySide[i] = null; }
        }
    }
//?}


    private boolean coverIs(Direction side, String id) {
        return id.equals(gregtech.content.cover.CoverItems.behavior(getCover(side)));
    }

    private boolean hasCover(String id) {
        for (Direction side : Direction.values()) {
            if (coverIs(side, id)) return true;
        }
        return false;
    }

    /** Cover behaviour id of a face; {@code null} when no cover is attached. */
    private String getCoverId(Direction side) {
        return gregtech.content.cover.CoverItems.behavior(getCover(side));
    }

    // ── Cover behaviors (ticked each server tick) ───────────────────────

    /** Cover-only lifecycle: no recipe work, energy polling or automatic machine output. */
    protected final void beginCoverTick() {
        coverTicks++;
        panels().beforeTick();
    }

    protected final void tickCovers(Level level, BlockPos pos) {
        if(panels().stopped())return;
        for (Direction side : Direction.values()) {
            ItemStack stack = getCover(side);
            String id = getCoverId(side);
            if (id == null) continue;
            switch (id) {
                case gregtech.content.cover.CoverItems.PUMP -> tickPumpCover(level, pos, side, stack);
                case gregtech.content.cover.CoverItems.CONVEYOR -> tickConveyorCover(level, pos, side, stack);
                case gregtech.content.cover.CoverItems.ROBOT_ARM -> tickRobotArmCover(level, pos, side, stack);
                case gregtech.content.cover.CoverAttachmentBehaviors.DRAIN -> tickDrainCover(level, pos, side);
                case gregtech.content.cover.CoverAttachmentBehaviors.AIR_VENT -> tickAirVentCover(level, pos, side);
            }
        }
        // the remaining cover behaviours (item retriever, pressure valve, filters, ...)
        // is dispatched by tickUtilityCovers below; it walks the six faces itself and has its own
        // panels.stopped() gate, so calling it here is enough to make those covers act.
        tickUtilityCovers(level, pos);
    }

    /** Original component covers share direction, tier cadence and one-operation transfer rules. */
    private void tickPumpCover(Level level, BlockPos pos, Direction side, ItemStack cover) {
        gregtech.content.cover.ComponentCoverRuntime.tick(this, side, level.getGameTime());
    }
    private void tickConveyorCover(Level level, BlockPos pos, Direction side, ItemStack cover) {
        gregtech.content.cover.ComponentCoverRuntime.tick(this, side, level.getGameTime());
    }
    private void tickRobotArmCover(Level level, BlockPos pos, Direction side, ItemStack cover) {
        gregtech.content.cover.ComponentCoverRuntime.tick(this, side, level.getGameTime());
    }

    /**
     * Drain cover: pull the neighbouring fluid block and the rain into the input tanks.
     *
     * <p>World fluids, rain, XP orbs and animal products are implemented by
     * CoverAttachmentBehaviors; quantum fluid blocks and optional integrations remain incomplete.
     * This host passes its persisted local cover counter. GT6 used SERVER_TIME and also reacted
     * immediately to block updates, so matching periods alone does not preserve its schedule.</p>
     */
    private void tickDrainCover(Level level, BlockPos pos, Direction side) {
        gregtech.content.cover.CoverAttachmentBehaviors
                .tickDrain(level, pos, side, this, coverTicks);
    }

    /**
     * Air vent cover: GT6 {@code CoverVent.onTickPre} ({@code CoverVent:43-65}) — every 360 ticks, in
     * this face's own slot of the cycle, air from in front of the vent is poured into the input
     * tanks: {@code Air}, {@code Air_Nether} or {@code Air_End} depending on the dimension. The whole
     * behaviour, including the {@code WD.collectable_air} front-face gate and the partial-fill
     * semantics of {@code FL.fill_}, lives in {@code CoverAttachmentBehaviors.tickVent}.
     *
     * <p>This shares the machine's local cover clock with the drain. It keeps a 360-tick
     * period but not GT6's global phase; manual test-loop convenience is not parity evidence.</p>
     */
    private void tickAirVentCover(Level level, BlockPos pos, Direction side) {
        gregtech.content.cover.CoverAttachmentBehaviors
                .tickVent(level, pos, side, this, coverTicks);
    }

    // ── Redstone from covers ────────────────────────────────────────────

    public boolean hasRedstoneCover() {
        for (Direction side : Direction.values()) {
            String id = getCoverId(side);
            var spec=MachineCoverSpec.of(getCover(side));
            if(spec!=null&&spec.detector())return true;
            if (gregtech.content.cover.CoverItems.REDSTONE_TORCH.equals(id)
                    || gregtech.content.cover.CoverItems.REDSTONE_REPEATER.equals(id)) return true;
        }
        return false;
    }

    public int getRedstoneSignal() {
        int signal=0;for(var side:Direction.values())signal=Math.max(signal,getRedstoneSignal(side));return signal;
    }
    public int getRedstoneSignal(Direction output){return gregtech.content.cover.PanelCover.of(getCover(output))!=null?panels().signal(output):coverSignals[output.ordinal()];}
    public int getStrongRedstoneSignal(Direction output){return gregtech.content.cover.PanelCover.of(getCover(output))!=null?panels().strongSignal(output):coverStrong[output.ordinal()]?getRedstoneSignal(output):0;}
    public MachineCoverSpec.State coverState(){return new MachineCoverSpec.State(workPossible||processor.totalWork()>0,mRunning,mActive,successful);}
    private void refreshWorkPossible(){
        boolean needed=false;
        for(var stack:coverStore.faces()){var cover=MachineCoverSpec.of(stack);if(cover==MachineCoverSpec.POSSIBLE||cover==MachineCoverSpec.AUTOMATIC||gregtech.content.cover.PanelCover.of(stack)==gregtech.content.cover.PanelCover.STATUS){needed=true;break;}}
        if(!needed){workPossible=false;return;}
        if(processor.totalWork()>0){workPossible=true;return;}
        if(!mInventoryChanged&&level.getGameTime()%12!=0)return;
        workPossible=false;
        var evaluation=processor.evaluation();
        workPossible=evaluation!=null&&evaluation.parallel()>0&&evaluation.cost()!=null;
    }
    protected final void updateCoverSignals(){
        if(level==null||level.isClientSide||isRemoved())return;
        panels().afterTick();
        boolean changed=false;var state=coverState();
        for(var side:Direction.values()){
            var stack=getCover(side);var cover=MachineCoverSpec.of(stack);
            int value=cover!=null?cover.signal(state,MachineCoverSpec.inverted(stack)):redstoneCoverSignal(side);
            boolean strong=cover!=null&&cover.detector()&&MachineCoverSpec.strong(stack);
            if(value!=coverSignals[side.ordinal()]||strong!=coverStrong[side.ordinal()]){
                boolean wasStrong=coverStrong[side.ordinal()];coverSignals[side.ordinal()]=value;coverStrong[side.ordinal()]=strong;changed=true;
                if((strong||wasStrong)&&level.hasChunkAt(worldPosition.relative(side)))level.updateNeighborsAt(worldPosition.relative(side),getBlockState().getBlock());
            }
        }
        if(changed)level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
    }
    /**
     * GT6's redstone torch cover is an <em>inverter</em> and its repeater a <em>follower</em>
     * ({@code CoverRedstoneTorch:43} / {@code CoverRedstoneRepeater:43}, both through
     * {@code AbstractCoverAttachmentTorch:50-57}: {@code mVisuals == 0 ? 15 : 0}). The wire they hang
     * on is this machine's powered state, which is {@code mRunning}. Until both covers were
     * answered with the same {@code mRunning ? 15 : 0}, so the torch was inverted.
     */
    private int redstoneCoverSignal(Direction side){
        if(coverIs(side,gregtech.content.cover.CoverItems.REDSTONE_TORCH))
            return gregtech.content.cover.CoverUtilityBehaviors.torchSignal(
                    gregtech.content.cover.CoverUtilityBehaviors.torchVisual(mRunning));
        if(coverIs(side,gregtech.content.cover.CoverItems.REDSTONE_REPEATER))
            return gregtech.content.cover.CoverUtilityBehaviors.torchSignal(
                    gregtech.content.cover.CoverUtilityBehaviors.repeaterVisual(mRunning));
        return 0;
    }

    public boolean configureControlCover(Direction side,boolean cutter){
        var stack=getCover(side);var cover=MachineCoverSpec.of(stack);
        if(cover==null||(cutter?!cover.detector():!cover.invertible()))return false;
        if(level!=null&&level.isClientSide)return true;
        String key=cutter?"gt.cover.strong":"gt.cover.inverted";

//? if >=1.21.1 {
/*        var tag=gregtech.content.cover.CoverStackData.readOrEmpty(stack);tag.putBoolean(key,!tag.getBoolean(key));gregtech.content.cover.CoverStackData.write(stack,tag);setChanged();
*///?} else {
        var tag=stack.getOrCreateTag();tag.putBoolean(key,!tag.getBoolean(key));setChanged();
//?}

        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        updateCoverSignals();return true;
    }

    /**
     * The recipe tag selected by an attached selector circuit, or -2 when none is attached.
     *
     * <p>GT6 registers the programmed circuit itself as {@code CoverSelectorTag(i)}
     * ({@code ItemIntegratedCircuit:87}), so the circuit's variant <em>is</em> the tag; the port's
     * {@code integrated_circuit_N} items follow the same rule. NBT {@code gt.circuit} is still read
     * for covers configured by the older port-only selector item.</p>
     */
    public int getTagSelectorCircuit() {
        for (Direction side : Direction.values()) {
            if (!gregtech.content.cover.CoverItems.TAG_SELECTOR.equals(getCoverId(side))) continue;
            ItemStack cover = getCover(side);
            int tag = gregtech.content.cover.CoverItems.selectorTag(cover);
            if (tag >= 0) return tag;

//? if >=1.21.1 {
/*            CompoundTag nbt = gregtech.content.cover.CoverStackData.read(cover);
*///?} else {
            CompoundTag nbt = cover.getTag();
//?}

            if (nbt != null && nbt.contains("gt.circuit")) return nbt.getInt("gt.circuit");
            return -1; // selector present but not configured
        }
        return -2; // no selector circuit cover
    }

    /** Shutter covers seal the face against item/fluid IO. */
    public boolean isFaceShuttered(Direction side) {
        return panels().shuttered(side);
    }

    /** Controllers own the switch while attached. The last controller in side order owns the switch. */
    protected boolean switchesAllowRunning() {
        if (level == null) return !controlStopped;
        var state=coverState();
        boolean controlled=false,allowed=true;
        for(var side:Direction.values()){
            var stack=getCover(side);var cover=MachineCoverSpec.of(stack);
            if(cover==null||cover.detector())continue;
            controlled=true;
            var neighbor=worldPosition.relative(side);
            // An external control input feeds the same face gate as the cover's redstone reading.
            int signal=Math.max(level.hasChunkAt(neighbor)?level.getSignal(neighbor,side):0,
                    externalSignals[side.ordinal()]&15);
            allowed=cover.allows(state,signal,MachineCoverSpec.inverted(stack),coverTicks);
        }
        if(controlled&&controlStopped==allowed){controlStopped=!allowed;setChanged();updateAdjacentToggleableEnergySources();}
        return !controlStopped;
    }

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
    protected void onProcessFinished() {if(sourceRequiresIgnition())sourceIgnitionTicks=gregtech.content.machine.OriginalBasicMachineRules.IGNITION_TICKS;}

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

    private void autoIO(Level level, BlockPos pos) {
        if (faceConfig == null) return;

        int autoIn = faceConfig.itemAutoInput();
        if (FaceConfig.autoValid(autoIn) && inventory != null) {
            autoInputItems(level, pos, autoIn);
        } else if (inventory != null) {
            positionedItemInput();
        }

        int autoOut = faceConfig.itemAutoOutput();
        if (FaceConfig.autoValid(autoOut) && inventory != null) {
            autoOutputItems(level, pos, autoOut);
        } else if (inventory != null) {
            positionedItemOutput();
        }

        int fluidAutoIn = faceConfig.fluidAutoInput();
        if (FaceConfig.autoValid(fluidAutoIn) && inventory != null && inventory.tanksInput().length > 0) {
            autoInputFluids(level, pos, fluidAutoIn);
        } else if (inventory != null && inventory.tanksInput().length > 0) {
            positionedFluidInput();
        }

        int fluidAutoOut = faceConfig.fluidAutoOutput();
        if (FaceConfig.autoValid(fluidAutoOut) && inventory != null && inventory.tanksOutput().length > 0) {
            autoOutputFluids(level, pos, fluidAutoOut);
        }
    }

    @Override public MachineInventory autoIOInventory() { return inventory; }
    @Override public Direction autoIORelativeToAbsolute(int relDir) { return relativeToAbsolute(relDir); }
    @Override public boolean autoIOShutters(Direction side) { return isFaceShuttered(side); }
    @Override public boolean autoIOAllowsItemTraffic(Direction side, ItemStack stack, boolean inserting) { return allowsItemTraffic(side, stack, inserting); }
    @Override public boolean autoIOAcceptsInput(ItemStack stack) { return acceptsAutomaticInput(stack); }
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

    private int relativeDir(Direction absolute) {
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
        if (!isEnergyType(energyType, side, false) || (!theoretical && controlStopped)) return false;
        if (side == null) return true;
        return (spec.energyType().equalsIgnoreCase(energyType.getShortName())
                && FaceConfig.has(faceConfig.energyInputs(), relativeDir(side)))
                || usesAuxiliaryInput(energyType, side);
    }

    /** Disjoint faces distinguish two inputs of the same type; unsided calls retain auxiliary priority. */
    @Override
    public boolean usesAuxiliaryInput(GregTechTags.Tag energyType, @Nullable Direction side) {
        return auxTag != null && auxTag == energyType
                && (side == null || FaceConfig.has(spec.auxiliaryFaceMask(), relativeDir(side)));
    }

    @Override public GregTechTags.Tag auxiliaryTag() { return auxTag; }
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
        if(inventory==null)return null;
        if(side==null)return inventory.items();
        int rel=relativeDir(side);
        boolean in=FaceConfig.has(faceConfig.itemInputs(),rel),out=FaceConfig.has(faceConfig.itemOutputs(),rel);
        return in||out?new SidedItemHandler(this,rel,inventory.items(),in,out):null;
*///?} else {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (side == null) {
            if (cap == ForgeCapabilities.ITEM_HANDLER && inventory != null)
                return itemCap.cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER && inventory != null)
                return fluidCap.cast();
            return super.getCapability(cap, null);
        }

        // Keep a live interface while a shutter is closed. The wrappers gate each operation,
        // including handles first obtained in the closed state and cached by neighboring pipes.

        int rel = relativeDir(side);

        if (cap == ForgeCapabilities.ITEM_HANDLER && inventory != null) {
            boolean canIn = FaceConfig.has(faceConfig.itemInputs(), rel);
            boolean canOut = FaceConfig.has(faceConfig.itemOutputs(), rel);
            if (!canIn && !canOut) return LazyOptional.empty();
            return getOrCreateItemCap(rel, canIn, canOut).cast();
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER && inventory != null) {
            boolean canIn = FaceConfig.has(faceConfig.fluidInputs(), rel);
            boolean canOut = FaceConfig.has(faceConfig.fluidOutputs(), rel);
            if (!canIn && !canOut) return LazyOptional.empty();
            return getOrCreateFluidCap(rel, canIn, canOut).cast();
        }

        return super.getCapability(cap, side);
//?}

    }

//? if >=1.21.1 {
/*    public IFluidHandler fluidCapability(@Nullable Direction side) {
        if(inventory==null)return null;
        if(side==null)return this;
        int rel=relativeDir(side);
        boolean in=FaceConfig.has(faceConfig.fluidInputs(),rel),out=FaceConfig.has(faceConfig.fluidOutputs(),rel);
        return in||out?new SidedFluidHandler(this,rel,in,out):null;
*///?} else {

    private LazyOptional<IItemHandler> getOrCreateItemCap(int rel, boolean canIn, boolean canOut) {
        if (itemCapsBySide[rel] == null) {
            itemCapsBySide[rel] = LazyOptional.of(() -> new SidedItemHandler(this, rel, inventory.items(), canIn, canOut));
        }
        return itemCapsBySide[rel];
    }

    private LazyOptional<IFluidHandler> getOrCreateFluidCap(int rel, boolean canIn, boolean canOut) {
        if (fluidCapsBySide[rel] == null) {
            fluidCapsBySide[rel] = LazyOptional.of(() -> new SidedFluidHandler(this, rel, canIn, canOut));
        }
        return fluidCapsBySide[rel];
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        fluidCap.invalidate();
        for (int i = 0; i < 6; i++) {
            if (itemCapsBySide[i] != null) itemCapsBySide[i].invalidate();
            if (fluidCapsBySide[i] != null) fluidCapsBySide[i].invalidate();
        }
//?}

    }

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

//? if >=1.21.1 {
/*    public void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        Object codecLookup=lookup;
*///?} else {
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        Object codecLookup=null;
//?}

        if(sourceRequiresIgnition())tag.putByte(gregtech.content.machine.OriginalBasicMachineRules.IGNITION_NBT,(byte)sourceIgnitionTicks);
        tag.putBoolean("gt.control_stopped",controlStopped);
        tag.putInt("gt.external_signals",packedExternalSignals());
        tag.putLong("gt.cover_ticks",coverTicks);
        if (inventory != null)

//? if >=1.21.1 {
/*            tag.put(NBT_INVENTORY, inventory.items().serializeNBT(lookup));
*///?} else {
            tag.put(NBT_INVENTORY, inventory.items().serializeNBT());
//?}

        if (recipeMap == MachineRecipeMaps.Autocrafter || !autocraftingProgram.getStackInSlot(0).isEmpty())

//? if >=1.21.1 {
/*            tag.put("gt.autocrafting.program", autocraftingProgram.serializeNBT(lookup));
*///?} else {
            tag.put("gt.autocrafting.program", autocraftingProgram.serializeNBT());
//?}

        tag.putLong(NBT_ENERGY, energyPort.stored());
        processor.save(tag, codecLookup);
        if (auxTag != null) {
            tag.putLong("gt.aux_energy", energyPort.auxStored());
            tag.putLong("gt.startup_energy", energyPort.startup());
        }

        if (inventory != null) {
            for (int i = 0; i < inventory.tanksInput().length; i++) {
                CompoundTag t = new CompoundTag();

                gregtech.engine.stack.NbtCompat.writeTank(inventory.tanksInput()[i],t,codecLookup);

                tag.put(NBT_TANKS_IN + i, t);
            }
        }
        if (inventory != null) {
            for (int i = 0; i < inventory.tanksOutput().length; i++) {
                CompoundTag t = new CompoundTag();

                gregtech.engine.stack.NbtCompat.writeTank(inventory.tanksOutput()[i],t,codecLookup);

                tag.put(NBT_TANKS_OUT + i, t);
            }
        }

//? if >=1.21.1 {
/*        saveCovers(tag,lookup);
*///?} else {
        saveCovers(tag);
//?}

    }


//? if >=1.21.1 {
/*    private void saveCovers(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        coverStore.save(tag,lookup);
    }

    private void loadCovers(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        coverStore.load(tag,lookup);
*///?} else {
    private void saveCovers(CompoundTag tag) {
        coverStore.save(tag, null);
    }

    private void loadCovers(CompoundTag tag) {
        coverStore.load(tag, null);
//?}
    }

    // ── Client sync (covers only — everything else travels via menus) ───────

    @Override

//? if >=1.21.1 {
/*    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
*///?} else {
    public CompoundTag getUpdateTag() {
//?}

        CompoundTag tag = new CompoundTag();

//? if >=1.21.1 {
/*        saveCovers(tag,lookup);
*///?} else {
        saveCovers(tag);
//?}
        gregtech.engine.multiblock.tracking.ClientFormedSync.write(this, tag);
        return tag;
    }

    @Override

//? if >=1.21.1 {
/*    public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        loadCovers(tag,lookup);
*///?} else {
    public void handleUpdateTag(CompoundTag tag) {
        loadCovers(tag);
//?}
        gregtech.engine.multiblock.tracking.ClientFormedSync.read(this, tag);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,

//? if >=1.21.1 {
/*                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,net.minecraft.core.HolderLookup.Provider lookup) {
        if (pkt.getTag() != null) loadCovers(pkt.getTag(),lookup);
*///?} else {
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) loadCovers(pkt.getTag());
//?}

    }

    @Override

//? if >=1.21.1 {
/*    public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        Object codecLookup=lookup;
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
        Object codecLookup=null;
//?}

        if (tag.contains("gt.autocrafting.program", 10)) {
            CompoundTag savedProgram = tag.getCompound("gt.autocrafting.program").copy();
            savedProgram.putInt("Size", 1);

//? if >=1.21.1 {
/*            autocraftingProgram.deserializeNBT(lookup, savedProgram);
*///?} else {
            autocraftingProgram.deserializeNBT(savedProgram);
//?}

        }
        sourceIgnitionTicks=Math.max(0,tag.getByte(gregtech.content.machine.OriginalBasicMachineRules.IGNITION_NBT));
        controlStopped=tag.getBoolean("gt.control_stopped");
        unpackExternalSignals(tag.getInt("gt.external_signals"));
        adjacentEnergySourcesChanged();
        coverTicks=Math.max(0,tag.getLong("gt.cover_ticks"));successful=false;workPossible=false;mInventoryChanged=true;
        if (tag.contains(NBT_ENERGY)) energyPort.setStored(tag.getLong(NBT_ENERGY));
        processor.load(tag, codecLookup);
        energyPort.setAuxStored(auxTag == null ? 0
                : Math.max(0, Math.min(spec.auxiliaryEnergyCapacity(), tag.getLong("gt.aux_energy"))));
        energyPort.setStartup(auxTag == null ? 0 : Math.max(0, tag.getLong("gt.startup_energy")));
        if (processor.totalWork() <= 0) energyPort.clearStartup();
        if (inventory == null) {
            // Called before setSpec — defer
        } else if (tag.contains(NBT_INVENTORY)) {

//? if >=1.21.1 {
/*            inventory.items().deserializeNBT(lookup,tag.getCompound(NBT_INVENTORY));
*///?} else {
            inventory.items().deserializeNBT(tag.getCompound(NBT_INVENTORY));
//?}

        }
        if (inventory != null) {
            for (int i = 0; i < inventory.tanksInput().length; i++)
                if (tag.contains(NBT_TANKS_IN + i))

                    gregtech.engine.stack.NbtCompat.readTank(inventory.tanksInput()[i],tag.getCompound(NBT_TANKS_IN + i),codecLookup);

        }
        if (inventory != null) {
            for (int i = 0; i < inventory.tanksOutput().length; i++)
                if (tag.contains(NBT_TANKS_OUT + i))

                    gregtech.engine.stack.NbtCompat.readTank(inventory.tanksOutput()[i],tag.getCompound(NBT_TANKS_OUT + i),codecLookup);

        }

//? if >=1.21.1 {
/*        loadCovers(tag,lookup);
*///?} else {
        loadCovers(tag);
//?}
        sanitizeForSpec();
    }

    /** Re-derive or bound every value that a save or {@code /data merge block} could have forged. */
    private void sanitizeForSpec() {
        if (spec == null) { specDirty = true; return; }
        specDirty = false;
        energyPort.setStored(clamp(energyPort.stored(), 0, inputMaximum()));
        processor.sanitize();
        if (!processor.hasPendingOutputs() || processor.totalWork() <= 0) energyPort.clearStartup();
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    // ── Sided wrappers ────────────────────────────────────────────────────

    /** Own-face policy applies to active auto-I/O as well as passive capability traffic. */
    private boolean allowsItemTraffic(Direction side, ItemStack stack, boolean inserting) {
        return !isRemoved() && !isFaceShuttered(side)
                && gregtech.content.cover.ComponentCoverRuntime.allowsItem(getCover(side), inserting)
                && !coverBlocksItemTraffic(side) && coverFilterPermits(side, stack);
    }

    private record SidedItemHandler(BasicMachineBlockEntity be, int relativeSide, IItemHandler inner, boolean canInsert, boolean canExtract)
            implements IItemHandler {
        @Override public int getSlots() { return inner.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inner.getStackInSlot(slot); }
        // the face's cover is the pipe filter. GT6 asks that from its item handler's
        // interceptItemInsert/interceptItemExtract hooks (CoverFilterItem:115-127), and the retriever
        // cover refuses both outright on its own face (CoverRetrieverItem:138-139, "aCoverSide ==
        // aSide"). A face without such a cover admits everything, which is what keeps every other
        // machine behaving exactly as before.
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return canInsert && be.acceptsAutomaticInput(stack) && be.allowsItemTraffic(side, stack, true)
                    ? inner.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < be.inputSlots()) return ItemStack.EMPTY;
            Direction side = be.relativeToAbsolute(relativeSide);
            ItemStack existing = inner.getStackInSlot(slot);
            return canExtract && be.allowsItemTraffic(side, existing, false)
                    ? inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inner.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return canInsert && be.acceptsAutomaticInput(stack) && be.allowsItemTraffic(side, stack, true)
                    && inner.isItemValid(slot, stack);
        }
    }

    private static class SidedFluidHandler implements IFluidHandler {
        private final BasicMachineBlockEntity be;
        private final boolean canFill, canDrain;
        private final int relativeSide;
        SidedFluidHandler(BasicMachineBlockEntity be, int relativeSide, boolean canFill, boolean canDrain) {
            this.be = be; this.relativeSide=relativeSide; this.canFill = canFill; this.canDrain = canDrain;
        }
        @Override public int getTanks() { return be.getTanks(); }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return be.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return be.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return gregtech.content.cover.ComponentCoverRuntime.allowsFluid(be.getCover(side),true) && canFill && !be.isRemoved() && !be.isFaceShuttered(side)
                    && be.coverFluidFilterPermits(side, stack) && be.isFluidValid(tank, stack);
        }
        // GT6 CoverFilterFluid:117-129 interceptFluidFill / interceptFluidDrain.
        @Override public int fill(FluidStack resource, FluidAction action) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return gregtech.content.cover.ComponentCoverRuntime.allowsFluid(be.getCover(side),true) && canFill && !be.isRemoved() && !be.isFaceShuttered(side)
                    && be.coverFluidFilterPermits(side, resource) ? be.fill(resource, action) : 0;
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return gregtech.content.cover.ComponentCoverRuntime.allowsFluid(be.getCover(side),false) && canDrain && !be.isRemoved() && !be.isFaceShuttered(side)
                    && be.coverFluidFilterPermits(side, resource) ? be.drain(resource, action) : FluidStack.EMPTY;
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            Direction side = be.relativeToAbsolute(relativeSide);
            FluidStack existing = be.drain(maxDrain, FluidAction.SIMULATE);
            return gregtech.content.cover.ComponentCoverRuntime.allowsFluid(be.getCover(side),false) && canDrain && !be.isRemoved() && !be.isFaceShuttered(side)
                    && be.coverFluidFilterPermits(side, existing) ? be.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    // ── Machine item handler ──────────────────────────────────────────────

    protected boolean acceptsAutomaticInput(ItemStack stack) {
        return recipeMap != MachineRecipeMaps.Autocrafter || level != null
                && (recipeMap.containsInput(stack) || gregtech.content.recipe.AutocraftingRecipes.containsInput(
                        level, this, autocraftingProgram.getStackInSlot(0), stack));
    }
    @Override public IItemHandler componentItems(Direction side) {
        if (inventory == null) return null;
        return new IItemHandler() {
            public int getSlots() { return inventory.items().getSlots(); }
            public ItemStack getStackInSlot(int slot) { return inventory.items().getStackInSlot(slot); }
            public int getSlotLimit(int slot) { return inventory.items().getSlotLimit(slot); }
            public boolean isItemValid(int slot, ItemStack stack) { return acceptsAutomaticInput(stack) && inventory.items().isItemValid(slot, stack); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return acceptsAutomaticInput(stack) ? inventory.items().insertItem(slot, stack, simulate) : stack;
            }
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot < inputSlots() ? ItemStack.EMPTY : inventory.items().extractItem(slot, amount, simulate);
            }
        };
    }
    public IItemHandlerModifiable inventory() { return inventory == null ? null : inventory.items(); }

    /** Normal handler changes and GUI live-stack merges both invalidate the inventory work state. */
    public void inventoryChanged() {
        setChanged();
        mInventoryChanged = true;
        processor.invalidate();
    }
    public int inputSlots() { return recipeMap != null ? recipeMap.mInputItemsCount : 0; }
    public int outputSlots() { return recipeMap != null ? recipeMap.mOutputItemsCount : 0; }

    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        gregtech.engine.inventory.BlockContents.drop(this, itemHandler());
        gregtech.engine.inventory.BlockContents.drop(this, autocraftingProgram);
        coverStore.drop();
        setChanged();
    }

    // ── Utility covers (round 108): retriever, pressure valve, filters, texture covers ──
    //
    // The behaviours live in gregtech.content.cover.CoverUtilityBehaviors; this
    // section only owns the dispatch and the per-face state that dispatch needs, exactly like the
    // drain/vent pair above. Nothing above that pair is touched.

    /**
     * Whether this machine's covers were suspended by a controller cover on the previous utility
     * tick, i.e. the {@code !aStopped} edge of GT6 {@code CoverRetrieverItem:55-57}
     * ({@code onStoppedUpdate} sets the retriever's pending value to 1 when it resumes).
     */
    private boolean utilityWasStopped;

    /**
     * GT6's {@code aData.mValues[aSide]} for the retriever ({@code CoverRetrieverItem:56,62}): a
     * per-face "run on the next tick even though the 20-tick clock has not come round" flag, set on
     * resume and currently cleared only after moving items. GT6 cleared it before the
     * eligible attempt, even when the search found nothing.
     *
     * <p>This pending array is not persisted. Its lifetime is currently success-based,
     * so it may remain set for multiple ticks; reload behavior needs separate validation.</p>
     */
    private final boolean[] coverPending = new boolean[6];

    /**
     * One utility-cover pass over all six faces: the retriever and the pressure valve.
     *
     * <p>Called once per server tick from the same place {@code tickCovers} is. The switch mirrors
     * {@code tickCovers}'s shape on purpose — {@code getCoverId(side)} is the single dispatcher, so
     * a face that {@code CoverItems.behavior} does not recognise is skipped here too rather than
     * being ticked behind that method's back.</p>
     *
     * <p>{@link #coverTicks} is handed to both behaviours in place of GT6's {@code SERVER_TIME} /
     * {@code aTimer}, for the reason spelled out on {@link #tickDrainCover}: the counter advances
     * once per server tick in a running game.</p>
     */
    public void tickUtilityCovers(Level level, BlockPos pos) {
        if (level == null) return;
        // CoverControllerCovers suspends the other covers; GT6 gates the retriever on !aData.mStopped
        // (CoverRetrieverItem:61) and the valve on the same flag (CoverPressureValve:50).
        if (panels().stopped()) { utilityWasStopped = true; return; }
        boolean resumed = utilityWasStopped;
        utilityWasStopped = false;

        for (Direction side : Direction.values()) {
            String id = getCoverId(side);
            if (id == null) continue;
            ItemStack stack = getCover(side);
            int index = side.ordinal();
            switch (id) {
                case gregtech.content.cover.CoverUtilityBehaviors.RETRIEVER_ITEM -> {
                    if (inventory == null) continue;
                    boolean acted = gregtech.content.cover.CoverUtilityBehaviors.tickRetriever(
                            level, pos, side, inventory.items(),

//? if >=1.21.1 {
/*                            gregtech.content.cover.CoverUtilityBehaviors.itemFilter(stack, level.registryAccess()),
*///?} else {
                            gregtech.content.cover.CoverUtilityBehaviors.itemFilter(stack),
//?}

                            gregtech.content.cover.MachineCoverSpec.inverted(stack),
                            coverTicks, coverPending[index] || resumed);
                    if (acted) coverPending[index] = false;
                    else if (resumed) coverPending[index] = true;
                }
                case gregtech.content.cover.CoverUtilityBehaviors.PRESSURE_VALVE -> {
                    if (inventory == null || inventory.tanksOutput().length == 0) continue;
                    gregtech.content.cover.CoverUtilityBehaviors.tickPressureValve(
                            level, pos, side, inventory.tanksOutput()[0], coverTicks);
                }
                default -> { }
            }
        }
    }

    /**
     * GT6 {@code CoverFilterItem:115-127} asked about a live face: does the filter cover on
     * {@code side} let {@code candidate} through?
     *
     * <p>GT6 asks this from its own item handler's {@code interceptItemInsert}/{@code
     * interceptItemExtract} hooks, one call per slot and side, with the face test
     * {@code aCoverSide != aSide} in front of it ({@code CoverFilterItem:116}). The port's
     * {@code MachineItemHandler} has no per-side insert hook to hang that on, so the predicate is
     * exposed here for whatever wiring does own the face, so the rule stays reachable from a real
     * attached cover.</p>
     *
     * <p>{@code panels.stopped()} stands in for GT6's {@code aData.mStopped}, which the port tracks
     * once per machine rather than once per cover.</p>
     */
    public boolean coverFilterPermits(Direction side, ItemStack candidate) {
        return gregtech.content.cover.CoverUtilityBehaviors.itemFilterPermits(

//? if >=1.21.1 {
/*                getCover(side), panels().stopped(), candidate, level.registryAccess());
*///?} else {
                getCover(side), panels().stopped(), candidate);
//?}

    }

    /** GT6 {@code CoverFilterFluid:117-129} asked about a live face. See {@link #coverFilterPermits}. */
    public boolean coverFluidFilterPermits(Direction side, FluidStack candidate) {
        return gregtech.content.cover.CoverUtilityBehaviors.fluidFilterPermits(

//? if >=1.21.1 {
/*                getCover(side), panels().stopped(), candidate, level.registryAccess());
*///?} else {
                getCover(side), panels().stopped(), candidate);
//?}

    }

    /**
     * GT6 {@code CoverRetrieverItem:138-139} — the retriever refuses <em>every</em> item insert
     * and extract on its own face ({@code return aCoverSide == aSide;}, both hooks), so a pipe can
     * neither push into nor pull out of the face the retriever owns; only the retriever itself moves
     * items there. That is the whole interception rule for that cover — unlike the item filter, whose
     * rule is a whitelist/blacklist test.
     */
    public boolean coverBlocksItemTraffic(Direction side) {
        return gregtech.content.cover.CoverUtilityBehaviors.RETRIEVER_ITEM.equals(
                gregtech.content.cover.CoverItems.behavior(getCover(side)));
    }

    /** The item filter stored on a face's cover, empty when there is none or it is not a filter. */
    public ItemStack coverItemFilter(Direction side) {

//? if >=1.21.1 {
/*        return gregtech.content.cover.CoverUtilityBehaviors.itemFilter(getCover(side), level.registryAccess());
*///?} else {
        return gregtech.content.cover.CoverUtilityBehaviors.itemFilter(getCover(side));
//?}

    }

    /** The fluid filter stored on a face's cover, empty when there is none or it is not a filter. */
    public FluidStack coverFluidFilter(Direction side) {

//? if >=1.21.1 {
/*        return gregtech.content.cover.CoverUtilityBehaviors.fluidFilter(getCover(side), level.registryAccess());
*///?} else {
        return gregtech.content.cover.CoverUtilityBehaviors.fluidFilter(getCover(side));
//?}

    }

    /**
     * GT6's right-click on a filter cover ({@code CoverFilterItem:88-112},
     * {@code CoverFilterFluid:92-114}) and on the retriever ({@code CoverRetrieverItem:123-136}):
     * the held stack becomes the filter, once.
     *
     * <p>For the fluid filter the original resolves the fluid out of the held stack, first through
     * {@code FL.getFluid(tStack, T)} and then through the item's container association
     * ({@code CoverFilterFluid:98-104}); the port keeps the first half — the Forge fluid-container
     * capability — and has no second half to keep, because the port has no ore-dictionary container
     * prefix.</p>
     *
     * @return whether a filter was stored
     */
    public boolean clickFilterCover(Direction side, ItemStack held) {
        String id = getCoverId(side);
        if (id == null || held == null || held.isEmpty()) return false;
        ItemStack stack = getCover(side);
        if (gregtech.content.cover.CoverUtilityBehaviors.FILTER_ITEM.equals(id)
                || gregtech.content.cover.CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id)) {

//? if >=1.21.1 {
/*            return gregtech.content.cover.CoverUtilityBehaviors.setItemFilter(stack, held, level.registryAccess());
*///?} else {
            return gregtech.content.cover.CoverUtilityBehaviors.setItemFilter(stack, held);
//?}

        }
        if (gregtech.content.cover.CoverUtilityBehaviors.FILTER_FLUID.equals(id)) {

//? if >=1.21.1 {
/*            var container = held.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
*///?} else {
            var container = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
//?}

            if (container == null) return false;
            FluidStack fluid = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);

//? if >=1.21.1 {
/*            return gregtech.content.cover.CoverUtilityBehaviors.setFluidFilter(stack, fluid, level.registryAccess());
*///?} else {
            return gregtech.content.cover.CoverUtilityBehaviors.setFluidFilter(stack, fluid);
//?}

        }
        return false;
    }

    /**
     * The screwdriver and soft-hammer halves of the same three covers: the screwdriver flips
     * whitelist/blacklist ({@code CoverFilterItem:58-62}, {@code CoverFilterFluid:62-66},
     * {@code CoverRetrieverItem:94-98}) and the soft hammer clears the filter
     * ({@code CoverFilterItem:63-66}, {@code CoverFilterFluid:67-70},
     * {@code CoverRetrieverItem:99-102}).
     *
     * <p>Shaped exactly like {@link #configureControlCover} next to it, including the client-side
     * early return, so the same caller can drive both.</p>
     */
    public boolean configureFilterCover(Direction side, boolean screwdriver, boolean softHammer) {
        String id = getCoverId(side);
        if (id == null) return false;
        boolean filterish = gregtech.content.cover.CoverUtilityBehaviors.FILTER_ITEM.equals(id)
                || gregtech.content.cover.CoverUtilityBehaviors.FILTER_FLUID.equals(id)
                || gregtech.content.cover.CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id);
        if (!filterish || (!screwdriver && !softHammer)) return false;
        if (level != null && level.isClientSide) return true;
        ItemStack stack = getCover(side);
        if (softHammer) gregtech.content.cover.CoverUtilityBehaviors.clearFilter(stack);
        else gregtech.content.cover.CoverUtilityBehaviors.toggleFilterMode(stack);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }
}
