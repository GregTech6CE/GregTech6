package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.machine.BasicMachineEnergy;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.content.cover.MachineCoverSpec;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.block.machine.GTFacingMachineBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.client.gui.BasicMachineContainerMenu;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.GregTechTags;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class BasicMachineBlockEntity extends GTEnergyBlockEntity implements MenuProvider, IFluidHandler, com.gregtech.gregtech.api.inventory.BlockContents, com.gregtech.gregtech.api.machine.MachineControl.Provider, com.gregtech.gregtech.content.cover.PanelCoverHost {
    private static final String NBT_SPEC = "gt.spec";
    private static final String NBT_INVENTORY = "gt.inventory";
    private static final String NBT_TANKS_IN = "gt.tanks_input";
    private static final String NBT_TANKS_OUT = "gt.tanks_output";
    private static final String NBT_ENERGY = "gt.energy";
    private static final String NBT_PROGRESS = "gt.progress";
    private static final String NBT_MAX_PROGRESS = "gt.max_progress";
    private static final String NBT_FACE_CONFIG = "gt.face_config";

    private BasicMachineSpec spec;
    private RecipeMap recipeMap;
    private MachineItemHandler itemHandler;
    private final net.neoforged.neoforge.items.ItemStackHandler autocraftingProgram = new net.neoforged.neoforge.items.ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return com.gregtech.gregtech.content.recipe.AutocraftingRecipes.isProgram(stack);
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); mInventoryChanged = true; }
    };
    public net.neoforged.neoforge.items.IItemHandler autocraftingProgram() { return autocraftingProgram; }

    private FluidTankGT[] tanksInput;
    private FluidTankGT[] tanksOutput;
    private FaceConfig faceConfig = FaceConfig.ALL_SIDES;

    // ── Processing state ─────────────────────────────────────────────────
    private long mEnergy;           // per-tick energy buffer (capped at energyInMax)
    private long mMinEnergy;        // per-tick energy cost for current recipe (after overclock)
    private long mMaxProgress;      // total energy needed for current recipe
    private int mParallelCount = 1; // GT6 large machine batch size for the running op
    private long mProgress;         // accumulated energy towards recipe completion
    private boolean mActive;        // currently processing a recipe
    private boolean mRunning;       // machine is in running state (energy present)
    private boolean controlStopped;
    private final com.gregtech.gregtech.content.cover.PanelCoverRuntime panels=new com.gregtech.gregtech.content.cover.PanelCoverRuntime(this);
    @Override public com.gregtech.gregtech.content.cover.PanelCoverRuntime panels(){return panels;}
    @Override public boolean coverSupportsPossible(){return true;}
    @Override public boolean coverPossible(Direction side){return coverState().possible();}
    private boolean successful,workPossible;
    private long coverTicks;
    private final int[] coverSignals=new int[6];
    private final boolean[] coverStrong=new boolean[6];
    private final com.gregtech.gregtech.api.machine.MachineControl control = new com.gregtech.gregtech.api.machine.MachineControl() {
        public boolean available(){return !isRemoved();}
        public boolean enabled(){return available()&&!controlStopped;}
        public boolean setEnabled(boolean enabled){
            if(!available())return false;
            if(level!=null&&level.isClientSide)return !controlStopped;
            controlStopped=!enabled;mInventoryChanged=true;
            if(!enabled){mEnergy=0;mActive=false;mRunning=false;updateBlockStates(false,false);}
            setChanged();return !controlStopped;
        }
        public boolean running(){return available()&&mRunning;}
        public boolean active(){return available()&&mActive;}
        public long progress(){return available()?mProgress:0;}
        public long progressMax(){return available()?mMaxProgress:0;}
    };
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private boolean mInventoryChanged = true; // inventory changed, re-check recipe
    private Recipe mCurrentRecipe;  // the recipe currently being processed

    public BasicMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ── Spec and RecipeMap ────────────────────────────────────────────────

    public void setSpec(BasicMachineSpec spec) {
        this.spec = spec;
        this.recipeMap = spec.recipeMap();
        this.faceConfig = spec.faceConfig();
        int totalSlots = recipeMap.mInputItemsCount + recipeMap.mOutputItemsCount;
        this.itemHandler = new MachineItemHandler(totalSlots, recipeMap.mInputItemsCount) {
            @Override protected void onContentsChanged(int slot) {
                BasicMachineBlockEntity.this.setChanged();
                mInventoryChanged = true;
            }
        };
        this.tanksInput  = new FluidTankGT[recipeMap.mInputFluidCount];
        this.tanksOutput = new FluidTankGT[recipeMap.mOutputFluidCount];
        for (int i = 0; i < tanksInput.length; i++) {
            final int idx = i;
            tanksInput[i] = new FluidTankGT(8000)
                    .setAdjustableCapacity(recipeMap.mMinInputTankSizes, 1)
                    .setOnChanged(() -> {
                        setChanged();
                        mInventoryChanged = true;
                        checkTankCapacity(idx, true);
                    });
        }
        for (int i = 0; i < tanksOutput.length; i++) {
            tanksOutput[i] = new FluidTankGT(8000)
                    .setOnChanged(() -> { setChanged(); mInventoryChanged = true; });
        }
    }

    private void checkTankCapacity(int idx, boolean isInput) {
        setChanged();
    }

    public BasicMachineSpec spec() { return spec; }
    public RecipeMap recipeMap() { return recipeMap; }
    public FaceConfig faceConfig() { return faceConfig; }
    public FluidTankGT[] getTanksInput() { return tanksInput; }
    public FluidTankGT[] getTanksOutput() { return tanksOutput; }
    public long getEnergyTick() { return mEnergy; }
    public boolean isRunning() { return mRunning; }
    public int getProgressPercent() {
        if (mMaxProgress <= 0) return 0;
        return (int) Math.min(100, (double)mProgress / mMaxProgress * 100);
    }

    // ── Server tick ──────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, BasicMachineBlockEntity be) {
        be.tickServer(level, pos);
    }

    private void tickServer(Level level, BlockPos pos) {
        if (faceConfig == null || spec == null) return;
        beforeMachineTick();
        coverTicks++;
        panels.beforeTick();
        if(!structureComplete()) { successful=false;workPossible=false;if(mActive||mRunning) {mActive=false;mRunning=false;updateBlockStates(false,false);}updateCoverSignals();return; }
        refreshWorkPossible();
        boolean allowed=switchesAllowRunning();successful=false;
        if(!allowed) {
            mEnergy=0;
            if(mActive||mRunning){mActive=false;mRunning=false;updateBlockStates(false,false);}
            tickCovers(level,pos);
            updateCoverSignals();
            return;
        }

        long energyIn = spec.energyIn();
        long energyInMin = inputMinimum();
        long energyInMax = inputMaximum();

        if (usesTimeEnergy() && switchesAllowRunning()) mEnergy=BasicMachineEnergy.add(mEnergy,inputMaximum(),1,1);
        boolean wasRunning = mRunning;
        boolean wasActive = mActive;

        // ── Poll energy from adjacent sources ────────────────────────────
        if (mRunning || mInventoryChanged || level.getGameTime() % 12 == 0) {
            pollEnergy(level, pos, energyIn);
        }

        if (mMaxProgress > 0 && mProgress >= mMaxProgress) {
            doActive(level, pos, 0);
            mActive = false;
            mRunning = false;
        } else if (mEnergy >= energyInMin && mEnergy >= mMinEnergy) {
            // Check recipe on inventory change or if no recipe in progress
            if (mInventoryChanged && mMaxProgress <= 0) {
                checkRecipe();
            }
            // Try to start a new recipe even if one isn't in progress
            if (mMaxProgress <= 0) {
                checkRecipe();
            }
            if (mMaxProgress > 0) {
                mActive = true;
                mRunning = true;
                doActive(level, pos, Math.min(energyInMax, mEnergy));
            } else {
                mActive = false;
                mRunning = true; // energy present but no valid recipe — standby
                autoIO(level, pos);
            }
        } else {
            // No energy — shut down
            if (requiresConstantEnergy() && mMaxProgress > 0 && mProgress > 0) {
                // Constant-energy machines lose progress, not the already-rolled job outputs.
                mProgress = 0;
                setChanged();
            }
            mActive = false;
            mRunning = false;
        }

        // ── Drain buffer ─────────────────────────────────────────────────
        mEnergy = BasicMachineEnergy.drain(mEnergy, energyInMax);

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
            BlockEntity be = level.getBlockEntity(adj);
            if (be instanceof com.gregtech.gregtech.api.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) continue;
            if (!(be instanceof IEnergyBlock source)) continue;
            if (!source.isEnergyEmittingTo(energyTag(), absDir.getOpposite(), false)) continue;
            long offered = source.getEnergyOffered(energyTag(), absDir.getOpposite(), size);
            if (offered <= 0) continue;
            long requested = BasicMachineEnergy.accepted(mEnergy, inputMaximum(), size, offered);
            if (requested <= 0) continue;
            long extracted = source.doEnergyExtraction(energyTag(), absDir.getOpposite(), size, requested, true);
            if (extracted > 0) mEnergy = BasicMachineEnergy.add(mEnergy, inputMaximum(), size, extracted);
        }
    }

    private void doActive(Level level, BlockPos pos, long aEnergy) {
        mProgress = com.gregtech.gregtech.api.recipe.MachineWorkCost.advance(mProgress, mMaxProgress, aEnergy);
        if (mProgress >= mMaxProgress) {
            if (!produceOutputs()) {
                mProgress = mMaxProgress;
                autoIO(level, pos);
                setChanged();
                return;
            }
            successful=true;
            onProcessFinished();
            mProgress = 0;
            mMaxProgress = 0;
            mMinEnergy = 0;
            mCurrentRecipe = null;
            // Immediately check for next recipe
            if (aEnergy > 0 && mMaxProgress <= 0) checkRecipe();
        }
        autoIO(level, pos);
        setChanged();
    }

    // ── Covers (GT6 cover system, first iteration) ──────────────────────────

    /** Attached cover items per absolute Direction ordinal (EMPTY = none). */
    private final ItemStack[] covers = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    public ItemStack getCover(Direction side) {
        return covers[side.ordinal()];
    }

    /** Attaches a cover (one item from the stack); false if the face is occupied. */
    public boolean attachCover(Direction side, ItemStack stack) {
        if (stack.isEmpty() || !covers[side.ordinal()].isEmpty() || !panels.canAttach(side,stack)) return false;
        covers[side.ordinal()] = stack.copyWithCount(1);
        panels.attached(side);
        mInventoryChanged=true;
        invalidateSideCaps();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        updateCoverSignals();
        return true;
    }

    /** Removes and returns the cover on a face (EMPTY if none). */
    public ItemStack removeCover(Direction side) {
        ItemStack cover = covers[side.ordinal()];
        if (cover.isEmpty()) return ItemStack.EMPTY;
        covers[side.ordinal()] = ItemStack.EMPTY;
        panels.beforeTick();
        var removedSpec=MachineCoverSpec.of(cover);
        if(removedSpec!=null&&!removedSpec.detector())controlStopped=false;
        mInventoryChanged=true;
        invalidateSideCaps();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        updateCoverSignals();
        return cover;
    }

    private void invalidateSideCaps() { invalidateCapabilities(); }

    private boolean coverIs(Direction side, String id) {
        return id.equals(com.gregtech.gregtech.content.cover.CoverItems.behavior(covers[side.ordinal()]));
    }

    private boolean hasCover(String id) {
        for (Direction side : Direction.values()) {
            if (coverIs(side, id)) return true;
        }
        return false;
    }

    /** Cover behaviour id of a face; {@code null} when no cover is attached. */
    private String getCoverId(Direction side) {
        return com.gregtech.gregtech.content.cover.CoverItems.behavior(covers[side.ordinal()]);
    }

    // ── Cover behaviors (ticked each server tick) ───────────────────────

    private void tickCovers(Level level, BlockPos pos) {
        if(panels.stopped())return;
        for (Direction side : Direction.values()) {
            ItemStack stack = covers[side.ordinal()];
            String id = getCoverId(side);
            if (id == null) continue;
            switch (id) {
                case com.gregtech.gregtech.content.cover.CoverItems.PUMP -> tickPumpCover(level, pos, side, stack);
                case com.gregtech.gregtech.content.cover.CoverItems.CONVEYOR -> tickConveyorCover(level, pos, side, stack);
                case com.gregtech.gregtech.content.cover.CoverItems.ROBOT_ARM -> tickRobotArmCover(level, pos, side, stack);
                case com.gregtech.gregtech.content.cover.CoverAttachmentBehaviors.DRAIN -> tickDrainCover(level, pos, side);
                case com.gregtech.gregtech.content.cover.CoverAttachmentBehaviors.AIR_VENT -> tickAirVentCover(level, pos, side);
            }
        }
        // §108: the second batch of cover behaviours (item retriever, pressure valve, filters, ...)
        // is dispatched by tickUtilityCovers below; it walks the six faces itself and has its own
        // panels.stopped() gate, so calling it here is enough to make those covers act.
        tickUtilityCovers(level, pos);
    }

    /**
     * Pump cover: auto-extract fluid from adjacent block into machine input tanks.
     *
     * <p>GT6 {@code CoverPump} moves {@code 250 << (2 * tier)} mB per 20-tick operation
     * ({@code CoverPump.onTickPre}) — the tier comes from the compact electric pump item that carries
     * the cover.</p>
     */
    private void tickPumpCover(Level level, BlockPos pos, Direction side, ItemStack cover) {
        if (level.getGameTime() % 20 != 0) return;
        if (tanksInput == null || tanksInput.length == 0) return;
        int throughput = com.gregtech.gregtech.content.cover.CoverItems.pumpThroughput(cover);
        BlockPos adj = pos.relative(side);
        BlockEntity be = level.getBlockEntity(adj);
        if (be instanceof com.gregtech.gregtech.api.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return;
        if (be == null) return;
        IFluidHandler source = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, be.getBlockPos(), side.getOpposite());
        if (source == null) return;
        FluidStack drained = source.drain(throughput, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) return;
        int filled = fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return;
        FluidStack toDrain = drained.copy();
        toDrain.setAmount(Math.min(filled, throughput));
        FluidStack actuallyDrained = source.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        if (!actuallyDrained.isEmpty()) {
            fill(actuallyDrained, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /**
     * Conveyor cover: auto-pull items from adjacent inventory into input slots.
     *
     * <p>GT6 {@code CoverConveyor(512 >> i)} moves one stack per {@code 512 >> tier} ticks; the port
     * keeps that interval and its own import direction (the original toggles the direction with a
     * screwdriver instead of having two items).</p>
     */
    private void tickConveyorCover(Level level, BlockPos pos, Direction side, ItemStack cover) {
        if (level.getGameTime() % com.gregtech.gregtech.content.cover.CoverItems.itemInterval(cover) != 0) return;
        if (itemHandler == null) return;
        int inputCount = itemHandler.inputCount();
        if (inputCount <= 0) return;
        BlockPos adj = pos.relative(side);
        BlockEntity be = level.getBlockEntity(adj);
        if (be instanceof com.gregtech.gregtech.api.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return;
        if (be == null) return;
        IItemHandler source = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, be.getBlockPos(), side.getOpposite());
        if (source == null) return;
        for (int srcSlot = 0; srcSlot < source.getSlots(); srcSlot++) {
            ItemStack stack = source.getStackInSlot(srcSlot);
            if (stack.isEmpty()) continue;
            if (!acceptsAutomaticInput(stack)) continue;
            for (int i = 0; i < inputCount; i++) {
                ItemStack remaining = itemHandler.insertItem(i, stack.copy(), true);
                int toTake = stack.getCount() - remaining.getCount();
                if (toTake <= 0) continue;
                ItemStack extracted = source.extractItem(srcSlot, toTake, false);
                if (!extracted.isEmpty()) {
                    ItemStack leftover = itemHandler.insertItem(i, extracted, false);
                    returnOrDrop(leftover, source);
                    if (source.getStackInSlot(srcSlot).isEmpty()) break;
                    stack = source.getStackInSlot(srcSlot);
                }
            }
        }
    }

    /**
     * Robot arm cover: auto-push output items into adjacent inventory.
     *
     * <p>GT6 {@code CoverRobotArm(512 >> i)} has the same timing as the conveyor
     * ({@code AbstractCoverAttachment} move on a {@code SERVER_TIME % mTiming == 0} boundary).</p>
     */
    private void tickRobotArmCover(Level level, BlockPos pos, Direction side, ItemStack cover) {
        if (level.getGameTime() % com.gregtech.gregtech.content.cover.CoverItems.itemInterval(cover) != 0) return;
        if (itemHandler == null) return;
        BlockPos adj = pos.relative(side);
        BlockEntity be = level.getBlockEntity(adj);
        if (be instanceof com.gregtech.gregtech.api.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return;
        if (be == null) return;
        IItemHandler target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, be.getBlockPos(), side.getOpposite());
        if (target == null) return;
        pushOutputItems(target);
    }

    /**
     * Drain cover: pull the neighbouring fluid block and the rain into the input tanks.
     *
     * <p>The behaviour is GT6's {@code CoverDrain.onTickPre} ({@code CoverDrain:68-159}); it lives in
     * {@code CoverAttachmentBehaviors} like the other two. Only the fluid block half and the rain
     * half of the original are ported — the XP, sewage and quantum branches are recorded as skipped
     * in that class' javadoc.</p>
     *
     * <p>{@link #coverTicks} is passed instead of {@code level.getGameTime()}: it advances exactly
     * once per server tick, so the original's {@code SERVER_TIME % 20 == 5} cadence is unchanged in a
     * running game while staying observable under the manual {@code serverTick} loop a GameTest
     * uses. The guards of the three covers that came before this one read
     * {@code level.getGameTime()} ({@link #tickPumpCover} and both item covers), which is frozen for
     * the whole duration of such a test body: those branches cannot be observed from a GameTest at
     * all, so a cover written today must not copy that part of them.</p>
     */
    private void tickDrainCover(Level level, BlockPos pos, Direction side) {
        com.gregtech.gregtech.content.cover.CoverAttachmentBehaviors
                .tickDrain(level, pos, side, this, coverTicks);
    }

    /**
     * Air vent cover: GT6 {@code CoverVent.onTickPre} ({@code CoverVent:43-65}) — every 360 ticks, in
     * this face's own slot of the cycle, air from in front of the vent is poured into the input
     * tanks: {@code Air}, {@code Air_Nether} or {@code Air_End} depending on the dimension. The whole
     * behaviour, including the {@code WD.collectable_air} front-face gate and the partial-fill
     * semantics of {@code FL.fill_}, lives in {@code CoverAttachmentBehaviors.tickVent}.
     *
     * <p>{@link #coverTicks} is passed instead of {@code level.getGameTime()} for the same reason as
     * in {@link #tickDrainCover}, and here it matters more: the original fires on a single tick out
     * of every 360, so with a frozen {@code getGameTime()} the branch would not just be hard to
     * observe in a GameTest, it would be unreachable. {@code coverTicks} advances exactly once per
     * server tick, so the cycle is the original's.</p>
     */
    private void tickAirVentCover(Level level, BlockPos pos, Direction side) {
        com.gregtech.gregtech.content.cover.CoverAttachmentBehaviors
                .tickVent(level, pos, side, this, coverTicks);
    }

    // ── Redstone from covers ────────────────────────────────────────────

    public boolean hasRedstoneCover() {
        for (Direction side : Direction.values()) {
            String id = getCoverId(side);
            var spec=MachineCoverSpec.of(covers[side.ordinal()]);
            if(spec!=null&&spec.detector())return true;
            if (com.gregtech.gregtech.content.cover.CoverItems.REDSTONE_TORCH.equals(id)
                    || com.gregtech.gregtech.content.cover.CoverItems.REDSTONE_REPEATER.equals(id)) return true;
        }
        return false;
    }

    public int getRedstoneSignal() {
        int signal=0;for(var side:Direction.values())signal=Math.max(signal,getRedstoneSignal(side));return signal;
    }
    public int getRedstoneSignal(Direction output){return com.gregtech.gregtech.content.cover.PanelCover.of(getCover(output))!=null?panels.signal(output):coverSignals[output.ordinal()];}
    public int getStrongRedstoneSignal(Direction output){return com.gregtech.gregtech.content.cover.PanelCover.of(getCover(output))!=null?panels.strongSignal(output):coverStrong[output.ordinal()]?getRedstoneSignal(output):0;}
    public MachineCoverSpec.State coverState(){return new MachineCoverSpec.State(workPossible||mMaxProgress>0,mRunning,mActive,successful);}
    private void refreshWorkPossible(){
        boolean needed=false;
        for(var stack:covers){var cover=MachineCoverSpec.of(stack);if(cover==MachineCoverSpec.POSSIBLE||cover==MachineCoverSpec.AUTOMATIC||com.gregtech.gregtech.content.cover.PanelCover.of(stack)==com.gregtech.gregtech.content.cover.PanelCover.STATUS){needed=true;break;}}
        if(!needed){workPossible=false;return;}
        if(mMaxProgress>0){workPossible=true;return;}
        if(!mInventoryChanged&&level.getGameTime()%12!=0)return;
        workPossible=false;
        if(recipeMap==null||itemHandler==null)return;
        var recipe=findRecipeWithUsbPort();
        if(recipe!=null){int parallel=computeParallel(recipe);workPossible=parallel>0&&com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(recipe.mEUt,recipe.mDuration,parallel,parallelScalesDuration(),efficiency(),inputMinimum(),inputMaximum(),cheapOverclocking(),usesTimeEnergy())!=null;}
    }
    private void updateCoverSignals(){
        if(level==null||level.isClientSide||isRemoved())return;
        panels.afterTick();
        boolean changed=false;var state=coverState();
        for(var side:Direction.values()){
            var stack=covers[side.ordinal()];var cover=MachineCoverSpec.of(stack);
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
     * §108: GT6's redstone torch cover is an <em>inverter</em> and its repeater a <em>follower</em>
     * ({@code CoverRedstoneTorch:43} / {@code CoverRedstoneRepeater:43}, both through
     * {@code AbstractCoverAttachmentTorch:50-57}: {@code mVisuals == 0 ? 15 : 0}). The wire they hang
     * on is this machine's powered state, which is {@code mRunning}. Until §108 both covers were
     * answered with the same {@code mRunning ? 15 : 0}, so the torch was inverted.
     */
    private int redstoneCoverSignal(Direction side){
        if(coverIs(side,com.gregtech.gregtech.content.cover.CoverItems.REDSTONE_TORCH))
            return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.torchSignal(
                    com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.torchVisual(mRunning));
        if(coverIs(side,com.gregtech.gregtech.content.cover.CoverItems.REDSTONE_REPEATER))
            return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.torchSignal(
                    com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.repeaterVisual(mRunning));
        return 0;
    }

    public boolean configureControlCover(Direction side,boolean cutter){
        var stack=covers[side.ordinal()];var cover=MachineCoverSpec.of(stack);
        if(cover==null||(cutter?!cover.detector():!cover.invertible()))return false;
        if(level!=null&&level.isClientSide)return true;
        String key=cutter?"gt.cover.strong":"gt.cover.inverted";
        var tag=com.gregtech.gregtech.content.cover.CoverStackData.readOrEmpty(stack);tag.putBoolean(key,!tag.getBoolean(key));com.gregtech.gregtech.content.cover.CoverStackData.write(stack,tag);setChanged();
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
            if (!com.gregtech.gregtech.content.cover.CoverItems.TAG_SELECTOR.equals(getCoverId(side))) continue;
            ItemStack cover = covers[side.ordinal()];
            int tag = com.gregtech.gregtech.content.cover.CoverItems.selectorTag(cover);
            if (tag >= 0) return tag;
            CompoundTag nbt = com.gregtech.gregtech.content.cover.CoverStackData.read(cover);
            if (nbt != null && nbt.contains("gt.circuit")) return nbt.getInt("gt.circuit");
            return -1; // selector present but not configured
        }
        return -2; // no selector circuit cover
    }

    /** Shutter covers seal the face against item/fluid IO. */
    public boolean isFaceShuttered(Direction side) {
        return panels.shuttered(side);
    }

    /** Controllers own the switch while attached. Multiple controllers must all allow work. */
    protected boolean switchesAllowRunning() {
        if (level == null) return !controlStopped;
        var state=coverState();
        boolean controlled=false,allowed=true;
        for(var side:Direction.values()){
            var stack=covers[side.ordinal()];var cover=MachineCoverSpec.of(stack);
            if(cover==null||cover.detector())continue;
            controlled=true;
            var neighbor=worldPosition.relative(side);
            int signal=level.hasChunkAt(neighbor)?level.getSignal(neighbor,side):0;
            if(!cover.allows(state,signal,MachineCoverSpec.inverted(stack),coverTicks))allowed=false;
        }
        if(controlled&&controlStopped==allowed){controlStopped=!allowed;setChanged();}
        return !controlStopped;
    }

    /** Multiblock controllers override this to require a valid structure. */
    protected boolean usesTimeEnergy() { return spec!=null&&spec.energyTag()==GregTechTags.Energy.TU; }
    protected long inputMinimum() { return spec.energyInMin(); }
    protected long inputMaximum() { return spec.energyInMax(); }
    protected boolean parallelScalesDuration() {
        var original = spec == null ? null : com.gregtech.gregtech.data.BasicMachineOriginalParams.find(spec.machineName(), spec.tier());
        return original == null || original.parallelDuration();
    }
    // Loader_MultiTileEntities 22010 explicitly enables cheap overclocking for the Melter.
    protected boolean cheapOverclocking() { return spec != null && spec.machineName().equals("melter"); }
    protected boolean requiresConstantEnergy() { return !usesTimeEnergy(); }
    protected int efficiency() { return 10000; }

    protected boolean structureComplete() {
        return true;
    }

    private com.gregtech.gregtech.api.recipe.MachineWorkOutputs pendingOutputs;

    protected void beforeMachineTick() {}
    protected boolean recipeStartAllowed() { return true; }
    protected void onProcessFinished() {}

    private void checkRecipe() {
        if (!recipeStartAllowed()) return;
        if (recipeMap == null) return;
        if (!switchesAllowRunning()) return;
        if (!structureComplete()) return;
        Recipe recipe = findRecipeWithUsbPort();
        if (recipe == null) {
            mMaxProgress = 0;
            mCurrentRecipe = null;
            return;
        }

        int parallel = computeParallel(recipe);
        var cost=com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(recipe.mEUt,recipe.mDuration,parallel,
                parallelScalesDuration(),efficiency(),inputMinimum(),inputMaximum(),cheapOverclocking(),usesTimeEnergy());
        if(cost==null)return;
        mMinEnergy=cost.minimumPower();
        mMaxProgress=cost.totalWork();

        var remaining = com.gregtech.gregtech.api.recipe.RecipeInputs.consume(recipe, recipeInputItems(), recipeInputFluids(), parallel);
        if (remaining == null || parallel < 1) { mMaxProgress=0; mCurrentRecipe=null; return; }
        pendingOutputs = com.gregtech.gregtech.api.recipe.MachineWorkOutputs.roll(recipe,parallel,level.random::nextInt);
        for(int i=0;i<remaining.items().size();i++) itemHandler.setStackInSlot(i,remaining.items().get(i));
        for(int i=0;i<tanksInput.length;i++) tanksInput[i].setFluid(remaining.fluids().get(i));
        setChanged();

        mParallelCount = parallel;
        mCurrentRecipe = recipe;
        mProgress = 0;
    }

    /** GT6's printer and replicator accept a USB cable in place of a retained stick. */
    @Nullable
    private Recipe findRecipeWithUsbPort() {
        if(recipeMap==null||itemHandler==null)return null;
        var items=itemHandler.stacks();var fluids=tanksToList(tanksInput);
        Recipe direct=recipeMap.findRecipe(items,fluids,recipeMap.mNeedsOutputs,recipeMap.mInputItemsCount,recipeMap.mOutputItemsCount, level, this, autocraftingProgram.getStackInSlot(0));
        return direct!=null?direct:com.gregtech.gregtech.content.recipe.MachineContextRecipes.find(this,recipeMap,recipeInputItems(),recipeInputFluids());
    }

    /** GT6 {@code NBT_PARALLEL} batch limits for the large machines. */
    private int parallelLimit() {
        return spec == null ? 1 : spec.parallelLimit();
    }

    /** Batch count limited by the configured parallel cap and available inputs. */
    private java.util.List<ItemStack> recipeInputItems() {
        var items=new java.util.ArrayList<ItemStack>();
        for(int i=0;i<recipeMap.mInputItemsCount;i++) items.add(itemHandler.getStackInSlot(i));
        return items;
    }
    private java.util.List<FluidStack> recipeInputFluids() {
        return java.util.Arrays.stream(tanksInput).map(t -> t.getFluidInTank(0)).toList();
    }
    private int computeParallel(Recipe recipe) {
        int low=0,high=parallelLimit();
        // Original BasicMachine:730/743 caps power-scaled parallel batches at nominal input.
        if(!parallelScalesDuration() && !usesTimeEnergy() && recipe.mEUt>0)
            high=(int)Math.min(high,Math.max(1,spec.energyIn()/recipe.mEUt));
        for(var fluid:recipe.mFluidOutputs) if(fluid!=null && !fluid.isEmpty()) high=Math.min(high,Integer.MAX_VALUE/fluid.getAmount());
        var items=recipeInputItems(); var fluids=recipeInputFluids();
        while(low<high) {
            int mid=low+(high-low+1)/2;
            if(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(recipe,items,fluids,mid)!=null) low=mid; else high=mid-1;
        }
        return Math.min(low, outputLimitedParallel(recipe, low));
    }

    /**
     * GT6 {@code MultiTileEntityBasicMachine.canOutput}: a batch may not exceed the free space in
     * the output slots and output tanks.
     * <p>
     * Without this cap a batch whose products cannot be delivered is started anyway; the machine
     * then retries the flush every tick and stays at 100 % progress forever. That is reachable
     * with the {@code large*} machines, whose parallel limit reaches 256 while most recipe maps
     * declare a single item output slot.
     * </p>
     */
    private int outputLimitedParallel(Recipe recipe, int maxParallel) {
        if (maxParallel <= 0 || recipeMap == null || itemHandler == null) return maxParallel;

        // Items: simulate the output section using the same slot rules as MachineWorkOutputs#flush.
        int firstOutput = recipeMap.mInputItemsCount;
        if (recipe.mOutputs.length > 0 && firstOutput < itemHandler.getSlots()) {
            var present = new net.minecraft.world.item.ItemStack[itemHandler.getSlots()];
            for (int i = firstOutput; i < itemHandler.getSlots(); i++) present[i] = itemHandler.getStackInSlot(i).copy();
            int limit = maxParallel;
            while (limit > 0 && !itemsFit(recipe, limit, firstOutput, present)) limit--;
            maxParallel = Math.min(maxParallel, limit);
        }

        // Fluids: free capacity of the output tanks, filled the same way flush() does.
        if (recipe.mFluidOutputs.length > 0 && tanksOutput != null && tanksOutput.length > 0) {
            int limit = maxParallel;
            while (limit > 0 && !fluidsFit(recipe, limit)) limit--;
            maxParallel = Math.min(maxParallel, limit);
        }
        return maxParallel;
    }

    /** Whether {@code parallel} runs of the recipe's item outputs fit into the output section. */
    private static boolean itemsFit(Recipe recipe, int parallel, int firstOutput,
                                    net.minecraft.world.item.ItemStack[] present) {
        var slots = new net.minecraft.world.item.ItemStack[present.length];
        for (int i = firstOutput; i < present.length; i++) slots[i] = present[i] == null ? net.minecraft.world.item.ItemStack.EMPTY : present[i].copy();
        for (var output : recipe.mOutputs) {
            if (output == null || output.isEmpty()) continue;
            long remaining = (long) output.getCount() * parallel;
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int i = firstOutput; i < slots.length && remaining > 0; i++) {
                    var slot = slots[i];
                    boolean empty = slot == null || slot.isEmpty();
                    if (empty ? pass == 0 : !net.minecraft.world.item.ItemStack.isSameItemSameComponents(slot, output)) continue;
                    int capacity = Math.min(64, output.getMaxStackSize());
                    int space = capacity - (empty ? 0 : slot.getCount());
                    int take = (int) Math.min(Math.max(0, space), remaining);
                    if (take <= 0) continue;
                    slots[i] = empty ? output.copyWithCount(take) : slot.copyWithCount(slot.getCount() + take);
                    remaining -= take;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    /** Whether {@code parallel} runs of the recipe's fluid outputs fit into the output tanks. */
    private boolean fluidsFit(Recipe recipe, int parallel) {
        long[] free = new long[tanksOutput.length];
        for (int i = 0; i < tanksOutput.length; i++) {
            free[i] = Math.max(0, tanksOutput[i].capacity() - tanksOutput[i].getAmount());
        }
        for (var output : recipe.mFluidOutputs) {
            if (output == null || output.isEmpty()) continue;
            long remaining = (long) output.getAmount() * parallel;
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int i = 0; i < tanksOutput.length && remaining > 0; i++) {
                    if (free[i] <= 0) continue;
                    var fluid = tanksOutput[i].getFluid();
                    boolean empty = tanksOutput[i].isEmpty();
                    if (empty ? pass == 0 : !fluid.isFluidEqual(output)) continue;
                    long take = Math.min(free[i], remaining);
                    free[i] -= take;
                    remaining -= take;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }
    private boolean produceOutputs() {
        if(pendingOutputs==null) return true;
        boolean finished=pendingOutputs.flush(itemHandler,recipeMap.mInputItemsCount,tanksOutput);
        if(finished) { pendingOutputs=null; mParallelCount=1; }
        setChanged(); return finished;
    }

    private static Iterable<FluidStack> tanksToList(FluidTankGT[] tanks) {
        if (tanks == null) return Collections.emptyList();
        FluidStack[] list = new FluidStack[tanks.length];
        for (int i = 0; i < tanks.length; i++) list[i] = tanks[i].getFluid();
        return java.util.Arrays.asList(list);
    }

    /** Deduct only deliveries confirmed by the shared transfer engine. */
    private void pushOutputItems(IItemHandler target) {
        for (int slot = itemHandler.inputCount(); slot < itemHandler.getSlots(); slot++) {
            ItemStack offered = itemHandler.getStackInSlot(slot);
            if (offered.isEmpty()) continue;
            var result = com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.transfer(
                    offered.copy(), () -> target);
            if (result.accepted() > 0) itemHandler.extractItem(slot, result.accepted(), false);
        }
    }

    /** A changed source/target must not make an already extracted item disappear. */
    private void returnOrDrop(ItemStack leftover, IItemHandler origin) {
        if (leftover.isEmpty()) return;
        var returned = com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.transfer(
                leftover.copy(), () -> origin);
        ItemStack remainder = leftover.copyWithCount(leftover.getCount() - returned.accepted());
        if (!remainder.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, worldPosition, remainder);
    }

    // ── Auto I/O ─────────────────────────────────────────────────────────

    private void autoIO(Level level, BlockPos pos) {
        if (faceConfig == null) return;

        int autoIn = faceConfig.itemAutoInput();
        if (FaceConfig.autoValid(autoIn) && itemHandler != null) {
            autoInputItems(level, pos, autoIn);
        }

        int autoOut = faceConfig.itemAutoOutput();
        if (FaceConfig.autoValid(autoOut) && itemHandler != null) {
            autoOutputItems(level, pos, autoOut);
        }

        int fluidAutoIn = faceConfig.fluidAutoInput();
        if (FaceConfig.autoValid(fluidAutoIn) && tanksInput != null && tanksInput.length > 0) {
            autoInputFluids(level, pos, fluidAutoIn);
        }

        int fluidAutoOut = faceConfig.fluidAutoOutput();
        if (FaceConfig.autoValid(fluidAutoOut) && tanksOutput != null && tanksOutput.length > 0) {
            autoOutputFluids(level, pos, fluidAutoOut);
        }
    }

    private void autoInputItems(Level level, BlockPos pos, int relDir) {
        Direction absolute = relativeToAbsolute(relDir);
        if(isFaceShuttered(absolute))return;
        BlockPos adj = pos.relative(absolute);
        BlockEntity be = level.getBlockEntity(adj);
        if (be instanceof com.gregtech.gregtech.api.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return;
        if (be == null) return;
        IItemHandler source = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, be.getBlockPos(), absolute.getOpposite());
        if (source == null) return;
        int inputCount = itemHandler.inputCount();
        if (inputCount <= 0) return;
        for (int srcSlot = 0; srcSlot < source.getSlots(); srcSlot++) {
            ItemStack stack = source.getStackInSlot(srcSlot);
            if (stack.isEmpty()) continue;
            if (!acceptsAutomaticInput(stack)) continue;
            for (int i = 0; i < inputCount; i++) {
                ItemStack remaining = itemHandler.insertItem(i, stack.copy(), true);
                int toTake = stack.getCount() - remaining.getCount();
                if (toTake <= 0) continue;
                ItemStack extracted = source.extractItem(srcSlot, toTake, false);
                if (!extracted.isEmpty()) {
                    ItemStack leftover = itemHandler.insertItem(i, extracted, false);
                    returnOrDrop(leftover, source);
                    if (source.getStackInSlot(srcSlot).isEmpty()) break;
                    stack = source.getStackInSlot(srcSlot);
                }
            }
        }
    }

    private void autoOutputItems(Level level, BlockPos pos, int relDir) {
        Direction absolute = relativeToAbsolute(relDir);
        if(isFaceShuttered(absolute))return;
        BlockPos adj = pos.relative(absolute);
        BlockEntity be = level.getBlockEntity(adj);
        if (be instanceof com.gregtech.gregtech.api.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return;
        IItemHandler target = be != null ? level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, be.getBlockPos(), absolute.getOpposite()) : null;

        int firstOutputSlot = itemHandler.inputCount();
        int totalSlots = itemHandler.getSlots();

        if (target != null) {
            pushOutputItems(target);
        } else if (level.isEmptyBlock(adj)) {
            // Drop into air like a hopper
            for (int i = firstOutputSlot; i < totalSlots; i++) {
                ItemStack extracted = itemHandler.extractItem(i, itemHandler.getStackInSlot(i).getCount(), false);
                if (extracted.isEmpty()) continue;
                double x = adj.getX() + 0.5;
                double y = adj.getY() + 0.5;
                double z = adj.getZ() + 0.5;
                double vx = absolute.getStepX() * 0.15;
                double vy = 0.1;
                double vz = absolute.getStepZ() * 0.15;
                ItemEntity entity = new ItemEntity(level, x, y, z, extracted);
                entity.setDeltaMovement(vx, vy, vz);
                entity.setPickUpDelay(10);
                level.addFreshEntity(entity);
            }
        }
    }

    private void autoInputFluids(Level level, BlockPos pos, int relDir) {
        Direction absolute = relativeToAbsolute(relDir);
        if(isFaceShuttered(absolute))return;
        BlockPos adj = pos.relative(absolute);
        BlockEntity be = level.getBlockEntity(adj);
        if (be instanceof com.gregtech.gregtech.api.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return;
        if (be == null) return;
        IFluidHandler source = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, be.getBlockPos(), absolute.getOpposite());
        if (source == null) return;
        FluidStack drained = source.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) return;
        int filled = fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return;
        FluidStack toDrain = drained.copy();
        toDrain.setAmount(filled);
        FluidStack actuallyDrained = source.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        if (!actuallyDrained.isEmpty()) {
            fill(actuallyDrained, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void autoOutputFluids(Level level, BlockPos pos, int relDir) {
        Direction absolute = relativeToAbsolute(relDir);
        if(isFaceShuttered(absolute))return;
        IFluidHandler target = automaticFluidOutputTarget(level,pos,absolute);
        if (target == null) return;
        FluidStack drained = drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) return;
        int filled = target.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return;
        FluidStack offered = drained.copy();
        offered.setAmount(Math.min(filled, drained.getAmount()));
        int accepted = target.fill(offered.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (accepted < 0 || accepted > offered.getAmount())
            throw new IllegalStateException("Fluid handler returned an invalid accepted amount: " + accepted);
        if (accepted > 0) {
            offered.setAmount(accepted);
            drain(offered, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** Dedicated controllers may output beneath their structure rather than the main block. */
    protected IFluidHandler automaticFluidOutputTarget(Level level,BlockPos pos,Direction absolute) {
        BlockPos adj = pos.relative(absolute);
        BlockEntity be = level.getBlockEntity(adj);
        if (be instanceof com.gregtech.gregtech.api.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return null;
        if (be == null) return null;
        IFluidHandler target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, be.getBlockPos(), absolute.getOpposite());
        return target;
    }

    // ── Direction helpers ─────────────────────────────────────────────────

    private int relativeDir(Direction absolute) {
        Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return rotateAbsoluteToRelative(facing, absolute);
    }

    /** Convert absolute side → machine-relative direction (TOP/BOTTOM/LEFT/RIGHT/FRONT/BACK)
     *  using the block facing. */
    private static int rotateAbsoluteToRelative(Direction facing, Direction side) {
        if (side == Direction.DOWN)  return FaceConfig.BOTTOM;
        if (side == Direction.UP)    return FaceConfig.TOP;
        return switch (facing) {
            case NORTH -> switch (side) {
                case NORTH -> FaceConfig.FRONT; case SOUTH -> FaceConfig.BACK;
                case WEST  -> FaceConfig.RIGHT; case EAST  -> FaceConfig.LEFT;
                default -> FaceConfig.LEFT;
            };
            case SOUTH -> switch (side) {
                case NORTH -> FaceConfig.BACK;  case SOUTH -> FaceConfig.FRONT;
                case WEST  -> FaceConfig.LEFT;  case EAST  -> FaceConfig.RIGHT;
                default -> FaceConfig.LEFT;
            };
            case WEST -> switch (side) {
                case NORTH -> FaceConfig.LEFT;  case SOUTH -> FaceConfig.RIGHT;
                case WEST  -> FaceConfig.FRONT; case EAST  -> FaceConfig.BACK;
                default -> FaceConfig.LEFT;
            };
            case EAST -> switch (side) {
                case NORTH -> FaceConfig.RIGHT; case SOUTH -> FaceConfig.LEFT;
                case WEST  -> FaceConfig.BACK;  case EAST  -> FaceConfig.FRONT;
                default -> FaceConfig.LEFT;
            };
            default -> side.get3DDataValue();
        };
    }

    /** Convert machine-relative direction index (TOP/BOTTOM/LEFT/RIGHT/FRONT/BACK)
     *  to the absolute world {@link Direction} based on the block's facing. */
    private Direction relativeToAbsolute(int relDir) {
        if (relDir == FaceConfig.BOTTOM) return Direction.DOWN;
        if (relDir == FaceConfig.TOP)    return Direction.UP;
        Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return switch (facing) {
            case NORTH -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.EAST;
                case FaceConfig.FRONT -> Direction.NORTH;
                case FaceConfig.RIGHT -> Direction.WEST;
                case FaceConfig.BACK  -> Direction.SOUTH;
                default -> Direction.NORTH;
            };
            case SOUTH -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.WEST;
                case FaceConfig.FRONT -> Direction.SOUTH;
                case FaceConfig.RIGHT -> Direction.EAST;
                case FaceConfig.BACK  -> Direction.NORTH;
                default -> Direction.NORTH;
            };
            case WEST -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.NORTH;
                case FaceConfig.FRONT -> Direction.WEST;
                case FaceConfig.RIGHT -> Direction.SOUTH;
                case FaceConfig.BACK  -> Direction.EAST;
                default -> Direction.NORTH;
            };
            case EAST -> switch (relDir) {
                case FaceConfig.LEFT  -> Direction.SOUTH;
                case FaceConfig.FRONT -> Direction.EAST;
                case FaceConfig.RIGHT -> Direction.NORTH;
                case FaceConfig.BACK  -> Direction.WEST;
                default -> Direction.NORTH;
            };
            default -> Direction.NORTH;
        };
    }

    // ── IFluidHandler ────────────────────────────────────────────────────

    @Override
    public int getTanks() { return (tanksInput != null ? tanksInput.length : 0) + (tanksOutput != null ? tanksOutput.length : 0); }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        int inLen = tanksInput != null ? tanksInput.length : 0;
        if (tank < inLen) return tanksInput[tank].getFluid();
        int outIdx = tank - inLen;
        int outLen = tanksOutput != null ? tanksOutput.length : 0;
        if (outIdx < outLen) return tanksOutput[outIdx].getFluid();
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        int inLen = tanksInput != null ? tanksInput.length : 0;
        if (tank < inLen) return (int) tanksInput[tank].getCapacity();
        int outIdx = tank - inLen;
        int outLen = tanksOutput != null ? tanksOutput.length : 0;
        if (outIdx < outLen) return (int) tanksOutput[outIdx].getCapacity();
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        int inLen = tanksInput != null ? tanksInput.length : 0;
        return tank < inLen;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (tanksInput == null) return 0;
        for (int i = 0; i < tanksInput.length; i++) {
            if (tanksInput[i].isFluidValid(resource)) {
                int filled = tanksInput[i].fill(resource, action);
                if (filled > 0) return filled;
            }
        }
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (tanksOutput == null) return FluidStack.EMPTY;
        for (int i = 0; i < tanksOutput.length; i++) {
            FluidStack drained = tanksOutput[i].drain(resource, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (tanksOutput == null) return FluidStack.EMPTY;
        for (int i = 0; i < tanksOutput.length; i++) {
            FluidStack drained = tanksOutput[i].drain(maxDrain, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }

    // ── IEnergyBlock ──────────────────────────────────────────────────────

    private GregTechTags.Tag energyTag() {
        return spec == null ? null : spec.energyTag();
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        if (energyType == null || spec == null) return false;
        return spec.energyType().equalsIgnoreCase(energyType.getShortName());
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        if (spec == null) return Collections.emptyList();
        GregTechTags.Tag tag = energyTag();
        return tag != null ? Collections.singletonList(tag) : Collections.emptyList();
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (!isEnergyType(energyType, side, false) || (!theoretical && controlStopped)) return false;
        if (side == null) return true;
        return FaceConfig.has(faceConfig.energyInputs(), relativeDir(side));
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return false; // basic machines never emit energy
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side,
                         long size, long amount, boolean doInject) {
        if (size==0 || size==Long.MIN_VALUE || amount<=0 || !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        long sizeAbs = Math.abs(size);
        if (sizeAbs > inputMaximum()) {
            // Overvoltage — GT6: the machine blows up
            if (doInject) explodeFromOvervoltage(sizeAbs);
            return amount;
        }
        long consumed = BasicMachineEnergy.accepted(mEnergy, inputMaximum(), size, amount);
        if (doInject && consumed > 0) {
            mEnergy = BasicMachineEnergy.add(mEnergy, inputMaximum(), size, consumed);
            setChanged();
        }
        return consumed;
    }

    /** GT6 overvoltage: destroy the machine in an explosion (configurable). */
    private void explodeFromOvervoltage(long size) {
        if (level == null || level.isClientSide) return;
        if (!com.gregtech.gregtech.GregTechConfig.machineOvervoltageExplosions()) {
            mEnergy = 0;
            return;
        }
        // strength grows with how far the packet exceeds the rated input (GT6 ~4.0 base)
        float over = (float) ((double) size / Math.max(1, inputMaximum()));
        float power = Math.min(8.0f, 3.0f + (float) (Math.log(over) / Math.log(2.0)));
        level.removeBlock(worldPosition, false);
        level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, Math.max(3.0f, power),
                net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public long doExtract(GregTechTags.Tag energyType, @Nullable Direction side,
                           long size, long amount, boolean doExtract) {
        return 0; // basic machines never emit energy
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (size==0||size==Long.MIN_VALUE||!isEnergyAcceptingFrom(energyType, side, false)) return 0;
        return BasicMachineEnergy.demanded(mEnergy, inputMaximum(), size);
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return spec != null ? spec.energyIn() : 1;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return spec != null ? spec.energyInMin() : 1;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return spec != null ? inputMaximum() : 1;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 1;
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 1;
    }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 1;
    }

    // ── Capabilities with side filtering ─────────────────────────────────

    public IItemHandler itemCapability(@Nullable Direction side) {
        if(itemHandler==null)return null;
        if(side==null)return itemHandler;
        int rel=relativeDir(side);
        boolean in=FaceConfig.has(faceConfig.itemInputs(),rel),out=FaceConfig.has(faceConfig.itemOutputs(),rel);
        return in||out?new SidedItemHandler(this,rel,itemHandler,in,out):null;
    }
    public IFluidHandler fluidCapability(@Nullable Direction side) {
        if(tanksInput==null)return null;
        if(side==null)return this;
        int rel=relativeDir(side);
        boolean in=FaceConfig.has(faceConfig.fluidInputs(),rel),out=FaceConfig.has(faceConfig.fluidOutputs(),rel);
        return in||out?new SidedFluidHandler(this,rel,in,out):null;
    }

    // ── MenuProvider ──────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gregtech." + (spec != null ? spec.id() : "basic_machine"));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new BasicMachineContainerMenu(containerId, playerInv, this);
    }

    // ── NBT ──────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putBoolean("gt.control_stopped",controlStopped);
        tag.putLong("gt.cover_ticks",coverTicks);
        if (itemHandler != null)
            tag.put(NBT_INVENTORY, itemHandler.serializeNBT(lookup));
        if (recipeMap == MachineRecipeMaps.Autocrafter || !autocraftingProgram.getStackInSlot(0).isEmpty())
            tag.put("gt.autocrafting.program", autocraftingProgram.serializeNBT(lookup));
        tag.putLong(NBT_ENERGY, mEnergy);
        tag.putLong(NBT_PROGRESS, mProgress);
        tag.putLong(NBT_MAX_PROGRESS, mMaxProgress);
        tag.putInt("gt.parallel", mParallelCount);
        tag.putLong("gt.min_energy",mMinEnergy);
        if(pendingOutputs!=null) tag.put("gt.pending_outputs",pendingOutputs.save(lookup));
        if (tanksInput != null) {
            for (int i = 0; i < tanksInput.length; i++) {
                CompoundTag t = new CompoundTag();
                tanksInput[i].writeToNBT(t,lookup);
                tag.put(NBT_TANKS_IN + i, t);
            }
        }
        if (tanksOutput != null) {
            for (int i = 0; i < tanksOutput.length; i++) {
                CompoundTag t = new CompoundTag();
                tanksOutput[i].writeToNBT(t,lookup);
                tag.put(NBT_TANKS_OUT + i, t);
            }
        }
        saveCovers(tag,lookup);
    }

    private void saveCovers(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        for (int i = 0; i < 6; i++) {
            if (!covers[i].isEmpty()) {
                tag.put("gt_cover_" + i, covers[i].save(lookup));
            }
        }
    }

    private void loadCovers(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        panels.loaded();
        for (int i = 0; i < 6; i++) {
            covers[i] = tag.contains("gt_cover_" + i)
                    ? ItemStack.parseOptional(lookup,tag.getCompound("gt_cover_" + i)) : ItemStack.EMPTY;
        }
    }

    // ── Client sync (covers only — everything else travels via menus) ───────

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        saveCovers(tag,lookup);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        loadCovers(tag,lookup);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,net.minecraft.core.HolderLookup.Provider lookup) {
        if (pkt.getTag() != null) loadCovers(pkt.getTag(),lookup);
    }

    @Override
    public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        if (tag.contains("gt.autocrafting.program", 10)) {
            CompoundTag savedProgram = tag.getCompound("gt.autocrafting.program").copy();
            savedProgram.putInt("Size", 1);
            autocraftingProgram.deserializeNBT(lookup, savedProgram);
        }
        controlStopped=tag.getBoolean("gt.control_stopped");
        coverTicks=Math.max(0,tag.getLong("gt.cover_ticks"));successful=false;workPossible=false;mInventoryChanged=true;
        if (tag.contains(NBT_ENERGY)) mEnergy = tag.getLong(NBT_ENERGY);
        if (tag.contains(NBT_PROGRESS)) mProgress = tag.getLong(NBT_PROGRESS);
        if (tag.contains(NBT_MAX_PROGRESS)) mMaxProgress = tag.getLong(NBT_MAX_PROGRESS);
        if (tag.contains("gt.parallel")) mParallelCount = Math.max(1, tag.getInt("gt.parallel"));
        mMinEnergy=tag.getLong("gt.min_energy");
        pendingOutputs=tag.contains("gt.pending_outputs")?com.gregtech.gregtech.api.recipe.MachineWorkOutputs.load(tag.getCompound("gt.pending_outputs"),lookup):null;
        if(mMaxProgress>0 && pendingOutputs==null) { mProgress=0; mMaxProgress=0; } // Old saves never recorded their consumed job; do not invent outputs.
        if (itemHandler == null) {
            // Called before setSpec — defer
        } else if (tag.contains(NBT_INVENTORY)) {
            itemHandler.deserializeNBT(lookup,tag.getCompound(NBT_INVENTORY));
        }
        if (tanksInput != null) {
            for (int i = 0; i < tanksInput.length; i++)
                if (tag.contains(NBT_TANKS_IN + i))
                    tanksInput[i].readFromNBT(tag.getCompound(NBT_TANKS_IN + i),lookup);
        }
        if (tanksOutput != null) {
            for (int i = 0; i < tanksOutput.length; i++)
                if (tag.contains(NBT_TANKS_OUT + i))
                    tanksOutput[i].readFromNBT(tag.getCompound(NBT_TANKS_OUT + i),lookup);
        }
        loadCovers(tag,lookup);
    }

    // ── Sided wrappers ────────────────────────────────────────────────────

    private record SidedItemHandler(BasicMachineBlockEntity be, int relativeSide, IItemHandler inner, boolean canInsert, boolean canExtract)
            implements IItemHandler {
        @Override public int getSlots() { return inner.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inner.getStackInSlot(slot); }
        // §108: the face's cover is the pipe filter. GT6 asks that from its item handler's
        // interceptItemInsert/interceptItemExtract hooks (CoverFilterItem:115-127), and the retriever
        // cover refuses both outright on its own face (CoverRetrieverItem:138-139, "aCoverSide ==
        // aSide"). A face without such a cover admits everything, which is what keeps every other
        // machine behaving exactly as before this batch.
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return canInsert && be.acceptsAutomaticInput(stack) && !be.isRemoved() && !be.isFaceShuttered(side)
                    && !be.coverBlocksItemTraffic(side) && be.coverFilterPermits(side, stack)
                    ? inner.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            Direction side = be.relativeToAbsolute(relativeSide);
            ItemStack existing = inner.getStackInSlot(slot);
            return canExtract && !be.isRemoved() && !be.isFaceShuttered(side)
                    && !be.coverBlocksItemTraffic(side) && be.coverFilterPermits(side, existing)
                    ? inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inner.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return canInsert && be.acceptsAutomaticInput(stack) && !be.isRemoved() && !be.isFaceShuttered(side)
                    && !be.coverBlocksItemTraffic(side) && be.coverFilterPermits(side, stack)
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
            return canFill && !be.isRemoved() && !be.isFaceShuttered(side)
                    && be.coverFluidFilterPermits(side, stack) && be.isFluidValid(tank, stack);
        }
        // §108: GT6 CoverFilterFluid:117-129 interceptFluidFill / interceptFluidDrain.
        @Override public int fill(FluidStack resource, FluidAction action) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return canFill && !be.isRemoved() && !be.isFaceShuttered(side)
                    && be.coverFluidFilterPermits(side, resource) ? be.fill(resource, action) : 0;
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            Direction side = be.relativeToAbsolute(relativeSide);
            return canDrain && !be.isRemoved() && !be.isFaceShuttered(side)
                    && be.coverFluidFilterPermits(side, resource) ? be.drain(resource, action) : FluidStack.EMPTY;
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            Direction side = be.relativeToAbsolute(relativeSide);
            FluidStack existing = be.drain(maxDrain, FluidAction.SIMULATE);
            return canDrain && !be.isRemoved() && !be.isFaceShuttered(side)
                    && be.coverFluidFilterPermits(side, existing) ? be.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    // ── Machine item handler ──────────────────────────────────────────────

    private boolean acceptsAutomaticInput(ItemStack stack) {
        return recipeMap != MachineRecipeMaps.Autocrafter || level != null
                && (recipeMap.containsInput(stack) || com.gregtech.gregtech.content.recipe.AutocraftingRecipes.containsInput(
                        level, this, autocraftingProgram.getStackInSlot(0), stack));
    }
    public IItemHandlerModifiable inventory() { return itemHandler; }
    public int inputSlots() { return recipeMap != null ? recipeMap.mInputItemsCount : 0; }
    public int outputSlots() { return recipeMap != null ? recipeMap.mOutputItemsCount : 0; }

    public static class MachineItemHandler extends ItemStackHandler {
        private final int inputCount;
        public MachineItemHandler(int totalSlots, int inputCount) {
            super(totalSlots);
            this.inputCount = inputCount;
        }
        public int inputCount() { return inputCount; }

        public Iterable<ItemStack> stacks() {
            return () -> new java.util.Iterator<>() {
                int idx;
                @Override public boolean hasNext() { return idx < getSlots(); }
                @Override public ItemStack next() { return getStackInSlot(idx++); }
            };
        }

        /** Insert into output slots — bypasses the player-facing restriction. */
        public void insertOutput(ItemStack stack) {
            if (stack.isEmpty()) return;
            int outIdx = inputCount;
            // Merge with existing stacks first
            for (int i = outIdx; i < getSlots(); i++) {
                ItemStack existing = getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    int space = Math.min(getSlotLimit(i), existing.getMaxStackSize()) - existing.getCount();
                    if (space > 0) {
                        int toAdd = Math.min(space, stack.getCount());
                        existing.grow(toAdd);
                        stack.shrink(toAdd);
                        onContentsChanged(i);
                        if (stack.isEmpty()) return;
                    }
                }
            }
            // Place in empty slot
            for (int i = outIdx; i < getSlots(); i++) {
                if (getStackInSlot(i).isEmpty()) {
                    setStackInSlot(i, stack);
                    onContentsChanged(i);
                    return;
                }
            }
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot >= inputCount) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
        }
    }
    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        com.gregtech.gregtech.api.inventory.BlockContents.drop(this, itemHandler);
        com.gregtech.gregtech.api.inventory.BlockContents.drop(this, autocraftingProgram);
        for (int i = 0; i < covers.length; i++) { com.gregtech.gregtech.api.inventory.BlockContents.drop(this, covers[i]); covers[i] = ItemStack.EMPTY; }
        setChanged();
    }

    // ── Utility covers (round 108): retriever, pressure valve, filters, texture covers ──
    //
    // The behaviours live in com.gregtech.gregtech.content.cover.CoverUtilityBehaviors; this
    // section only owns the dispatch and the per-face state that dispatch needs, exactly like the
    // drain/vent pair above. Nothing before this line is touched.

    /**
     * Whether this machine's covers were suspended by a controller cover on the previous utility
     * tick, i.e. the {@code !aStopped} edge of GT6 {@code CoverRetrieverItem:55-57}
     * ({@code onStoppedUpdate} sets the retriever's pending value to 1 when it resumes).
     */
    private boolean utilityWasStopped;

    /**
     * GT6's {@code aData.mValues[aSide]} for the retriever ({@code CoverRetrieverItem:56,62}): a
     * per-face "run on the next tick even though the 20-tick clock has not come round" flag, set on
     * resume and cleared once the cover has actually moved something.
     *
     * <p>Not persisted. GT6 saves {@code mValues} only for covers that ask for it
     * ({@code needsVisualsSaved}), and the flag is worth at most one tick: a reload re-arms it on
     * the first tick after the cover stops being suspended anyway.</p>
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
     * once per server tick in a running game and is still observable from a GameTest body.</p>
     */
    public void tickUtilityCovers(Level level, BlockPos pos) {
        if (level == null) return;
        // CoverControllerCovers suspends the other covers; GT6 gates the retriever on !aData.mStopped
        // (CoverRetrieverItem:61) and the valve on the same flag (CoverPressureValve:50).
        if (panels.stopped()) { utilityWasStopped = true; return; }
        boolean resumed = utilityWasStopped;
        utilityWasStopped = false;

        for (Direction side : Direction.values()) {
            String id = getCoverId(side);
            if (id == null) continue;
            ItemStack stack = covers[side.ordinal()];
            int index = side.ordinal();
            switch (id) {
                case com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.RETRIEVER_ITEM -> {
                    if (itemHandler == null) continue;
                    boolean acted = com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.tickRetriever(
                            level, pos, side, itemHandler,
                            com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.itemFilter(stack, level.registryAccess()),
                            com.gregtech.gregtech.content.cover.MachineCoverSpec.inverted(stack),
                            coverTicks, coverPending[index] || resumed);
                    if (acted) coverPending[index] = false;
                    else if (resumed) coverPending[index] = true;
                }
                case com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.PRESSURE_VALVE -> {
                    if (tanksOutput == null || tanksOutput.length == 0) continue;
                    com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.tickPressureValve(
                            level, pos, side, tanksOutput[0], coverTicks);
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
     * exposed here for whatever wiring does own the face — and it is what the GameTest asserts, so
     * the rule is reachable from a real attached cover either way.</p>
     *
     * <p>{@code panels.stopped()} stands in for GT6's {@code aData.mStopped}, which the port tracks
     * once per machine rather than once per cover.</p>
     */
    public boolean coverFilterPermits(Direction side, ItemStack candidate) {
        return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.itemFilterPermits(
                covers[side.ordinal()], panels.stopped(), candidate, level.registryAccess());
    }

    /** GT6 {@code CoverFilterFluid:117-129} asked about a live face. See {@link #coverFilterPermits}. */
    public boolean coverFluidFilterPermits(Direction side, FluidStack candidate) {
        return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.fluidFilterPermits(
                covers[side.ordinal()], panels.stopped(), candidate, level.registryAccess());
    }

    /**
     * §108: GT6 {@code CoverRetrieverItem:138-139} — the retriever refuses <em>every</em> item insert
     * and extract on its own face ({@code return aCoverSide == aSide;}, both hooks), so a pipe can
     * neither push into nor pull out of the face the retriever owns; only the retriever itself moves
     * items there. That is the whole interception rule for that cover — unlike the item filter, whose
     * rule is a whitelist/blacklist test.
     */
    public boolean coverBlocksItemTraffic(Direction side) {
        return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.RETRIEVER_ITEM.equals(
                com.gregtech.gregtech.content.cover.CoverItems.behavior(getCover(side)));
    }

    /** The item filter stored on a face's cover, empty when there is none or it is not a filter. */
    public ItemStack coverItemFilter(Direction side) {
        return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.itemFilter(getCover(side), level.registryAccess());
    }

    /** The fluid filter stored on a face's cover, empty when there is none or it is not a filter. */
    public FluidStack coverFluidFilter(Direction side) {
        return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.fluidFilter(getCover(side), level.registryAccess());
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
        ItemStack stack = covers[side.ordinal()];
        if (com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.FILTER_ITEM.equals(id)
                || com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id)) {
            return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.setItemFilter(stack, held, level.registryAccess());
        }
        if (com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.FILTER_FLUID.equals(id)) {
            var container = held.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            if (container == null) return false;
            FluidStack fluid = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            return com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.setFluidFilter(stack, fluid, level.registryAccess());
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
        boolean filterish = com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.FILTER_ITEM.equals(id)
                || com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.FILTER_FLUID.equals(id)
                || com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id);
        if (!filterish || (!screwdriver && !softHammer)) return false;
        if (level != null && level.isClientSide) return true;
        ItemStack stack = covers[side.ordinal()];
        if (softHammer) com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.clearFilter(stack);
        else com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.toggleFilterMode(stack);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }
}
