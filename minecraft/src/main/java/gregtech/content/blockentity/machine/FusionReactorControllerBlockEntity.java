package gregtech.content.blockentity.machine;

import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.multiblock.*;
import gregtech.engine.multiblock.tracking.Aggregate;
import gregtech.engine.multiblock.tracking.CompiledPattern;
import gregtech.engine.multiblock.tracking.FixedStructures;
import gregtech.engine.multiblock.tracking.PatternMatch;
import gregtech.engine.multiblock.tracking.StructureClaims;
import gregtech.engine.multiblock.tracking.StructureDefinition;
import gregtech.engine.multiblock.tracking.StructureTracking;
import gregtech.engine.recipe.*;
import gregtech.engine.recipe.*;
import gregtech.content.block.machine.*;
import gregtech.content.multiblock.*;
import gregtech.content.data.GregTechTags;
import gregtech.content.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Dedicated GT6 fusion executor: input reservation, LU startup, timed EU output and retained products. */
public class FusionReactorControllerBlockEntity extends LargeRecipeMachineBlockEntity {
    private static final List<Aggregate> AGGREGATES=List.of(
            Aggregate.Count.exactly(Aggregate.part(18200),3),
            Aggregate.Count.exactly(Aggregate.part(18201),12),
            Aggregate.Count.exactly(Aggregate.part(18202),12));
    private final PartBindings<BlockPos,MultiblockLayout.Role> fusionBindings=new PartBindings<>();
    private final StructureDefinition definition=FixedStructures.declarative(this,this::structureCells,AGGREGATES);
    private long chargeRemaining,chargeTotal,progress,duration,outputEU;
    private MachineWorkOutputs products;
    @Override public java.util.List<ItemStack> inspectionPendingItems() {
        return products == null ? java.util.List.of() : products.inspectionItems(24);
    }
    @Override public java.util.List<FluidStack> inspectionPendingFluids() {
        return products == null ? java.util.List.of() : products.inspectionFluids(16);
    }
    @Override public int inspectionPendingItemCount() { return products == null ? 0 : products.pendingItemStacks(); }
    private Recipe continuousRecipe;
    private boolean running, passive, processing, completed;

//? if >=1.21.1 {
    /*public FusionReactorControllerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state);
*///?} else {
    public FusionReactorControllerBlockEntity(BlockPos pos,BlockState state) {
        super(GTBlockEntities.FUSION_REACTOR.get(),pos,state);
//?}
        setSpec(((BasicMachineBlock)state.getBlock()).basicSpec());
    }
    @Override public StructureDefinition structureDefinition() { return definition; }
    @Override protected java.util.List<LargeMachineLayouts.Cell> structureCells() {
        return FusionStructure.CELLS;
    }
    /** CPU positions accept any processor type; the aggregates enforce the 3/12/12 split. */
    private java.util.List<CompiledPattern.FixedCell> structureCells(Direction front) {
        var cells=new java.util.ArrayList<CompiledPattern.FixedCell>(FusionStructure.CELLS.size());
        for(var cell:FusionStructure.CELLS) {
            var spec=cell.part()>=18200&&cell.part()<=18202?processorCell(cell):cell;
            var offset=cell.at(BlockPos.ZERO,front);
            cells.add(new CompiledPattern.FixedCell(offset.getX(),offset.getY(),offset.getZ(),spec));
        }
        return cells;
    }
    private static LargeMachineLayouts.Cell processorCell(LargeMachineLayouts.Cell cell) {
        var alternatives=java.util.stream.IntStream.rangeClosed(18200,18202)
                .filter(id -> id != cell.part()).mapToObj(id -> new SharedLargeMachineLayouts.Choice(id,null)).toList();
        return new LargeMachineLayouts.Cell(cell.right(),cell.up(),cell.back(),cell.part(),cell.role(),cell.plain(),
                cell.design(),alternatives);
    }
    @Override public boolean applyStructure(PatternMatch match) {
        if(level==null||isRemoved())return false;
        var candidates=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(var fixed:match.fixedCells()) {
            var cell=fixed.cell();
            if(cell.role()!=MultiblockLayout.Role.AIR)candidates.put(fixed.pos(),cell.role());
        }
        return StructureClaims.update(this,fusionBindings,candidates,
                p->((MultiblockPortBlockEntity)level.getBlockEntity(p)).canBind(worldPosition),
                (p,r)->((MultiblockPortBlockEntity)level.getBlockEntity(p)).bind(worldPosition,r),
                this::releaseFusionPart)!=StructureClaims.Result.FAILED;
    }
    @Override public void applyIncompleteStructure(PatternMatch match) {
        if (level == null || isRemoved()) return;
        var candidates = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var fixed : match.fixedCells()) {
            if (fixed.cell().role() != MultiblockLayout.Role.AIR
                    && level.getBlockEntity(fixed.pos()) instanceof MultiblockPortBlockEntity part
                    && part.canBind(worldPosition)) candidates.put(fixed.pos(), fixed.cell().role());
        }
        StructureClaims.update(this, fusionBindings, candidates,
                p -> ((MultiblockPortBlockEntity) level.getBlockEntity(p)).canBind(worldPosition),
                (p, r) -> ((MultiblockPortBlockEntity) level.getBlockEntity(p)).bind(worldPosition, r),
                this::releaseFusionPart);
    }
    @Override public void releaseStructureClaims() {
        fusionBindings.clear(this::releaseFusionPart);
        StructureClaims.cleared(this);
    }
    private void releaseFusionPart(BlockPos pos){if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)part.release(worldPosition);}
    @Override public void setRemoved(){
        if (!StructureTracking.unloading(this)) fusionBindings.clear(this::releaseFusionPart);
        super.setRemoved();
    }

    public static void serverTick(Level level,BlockPos pos,BlockState state,FusionReactorControllerBlockEntity be){be.tickFusion();}
    private void tickFusion() {
        if (level == null || level.isClientSide || isRemoved()) return;
        boolean wasPassive = passive, wasProcessing = processing;
        beginCoverTick();
        boolean formed = structureOk();
        // Switches inspect the previous processing state, including blocked completed work.
        boolean allowed = formed && switchesAllowRunning();
        running = passive = processing = completed = false;
        try {
            if (!allowed) { continuousRecipe = null; return; }
            passive = true;
            if (products == null && !startRecipe()) return;
            if (chargeRemaining > 0) return;
            processing = true;
            if (progress < duration) {
                running = true;
                if (outputEU > 0) emitFusionEnergy();
                progress++;
            }
            if (progress >= duration && products.flush(inventory(), inputSlots(), getTanksOutput())) {
                completed = true;
                products = null; duration = 0; progress = 0; outputEU = 0;
                // A continuously supplied, identical reaction retains its startup charge.
                startRecipe();
            }
            setChanged();
        } finally {
            if (passive != wasPassive || processing != wasProcessing) setChanged();
            updateActivity();
            // Basic machines also run component covers while switched off, but not while unformed.
            if (formed) tickCovers(level, worldPosition);
            updateCoverSignals();
        }
    }

    private Recipe availableRecipe() {
        var items = new ArrayList<ItemStack>();
        for (int i = 0; i < inputSlots(); i++) items.add(inventory().getStackInSlot(i));
        var fluids = Arrays.stream(getTanksInput()).map(t -> t.getFluidInTank(0)).toList();
        var recipe = recipeMap().findRecipe(items, fluids, false, inputSlots(), 0);
        return recipe != null && recipe.mEUt <= 0 && recipe.mEUt != Long.MIN_VALUE ? recipe : null;
    }

    @Override public gregtech.content.cover.MachineCoverSpec.State coverState() {
        return new gregtech.content.cover.MachineCoverSpec.State(
                products != null || level != null && availableRecipe() != null, passive, processing, completed);
    }
    @Override protected boolean controlRunning() { return passive; }
    @Override protected boolean controlActive() { return processing; }
    @Override protected long controlProgress() { return completed ? 1 : progress; }
    @Override protected long controlProgressMax() { return completed ? 1 : duration; }
    @Override protected long controlProgressPerTick() { return processing && products != null && chargeRemaining == 0 && progress < duration ? 1 : 0; }
    @Override protected void onControlStopped() {
        running = passive = processing = completed = false;
    }

    private boolean startRecipe() {
        var items=new ArrayList<ItemStack>();
        for(int i=0;i<inputSlots();i++)items.add(inventory().getStackInSlot(i));
        var fluids=Arrays.stream(getTanksInput()).map(t->t.getFluidInTank(0)).toList();
        var recipe=recipeMap().findRecipe(items,fluids,false,inputSlots(),0);
        if(recipe==null||recipe.mEUt>0||recipe.mEUt==Long.MIN_VALUE){continuousRecipe=null;return false;}
        var remaining=RecipeInputs.consume(recipe,items,fluids,1);
        if(remaining==null){continuousRecipe=null;return false;}
        products=MachineWorkOutputs.roll(recipe,1,level.random::nextInt);
        for(int i=0;i<items.size();i++)inventory().setStackInSlot(i,remaining.items().get(i));
        for(int i=0;i<fluids.size();i++)getTanksInput()[i].setFluid(remaining.fluids().get(i));
        chargeTotal=Math.max(0,recipe.mSpecialValue);
        chargeRemaining=continuousRecipe==recipe?0:chargeTotal;
        continuousRecipe=recipe;progress=0;duration=recipe.mDuration;outputEU=-recipe.mEUt;
        setChanged();return true;
    }
    private void emitFusionEnergy() {
        var center=worldPosition.relative(getBlockState().getValue(BasicMachineBlock.FACING).getOpposite(),2);
        for(var direction:Direction.Plane.HORIZONTAL) {
            var target=center.relative(direction,10);
            if(!level.hasChunkAt(target))continue;
            var receiver=level.getBlockEntity(target);
            if(receiver!=null&&EnergyTransfer.insertEnergyInto(GregTechTags.Energy.EU,direction.getOpposite(),outputEU,1,this,receiver)>0)break;
        }
    }
    private void updateActivity() {
        var state=getBlockState();boolean active=products!=null;
        if(state.getValue(BasicMachineBlock.RUNNING)!=active||state.getValue(BasicMachineBlock.LIT)!=running)
            level.setBlock(worldPosition,state.setValue(BasicMachineBlock.RUNNING,active).setValue(BasicMachineBlock.LIT,running),3);
    }
    @Override public boolean isRunning(){return running;}
    @Override public int getProgressPercent(){return duration>0?(int)Math.min(100,100.0*progress/duration):0;}
    public long chargeRemaining(){return chargeRemaining;}
    public long outputEU(){return outputEU;}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return List.of();}
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting){return false;}
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical){return false;}
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type,Direction side,boolean theoretical){return false;}
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute){return 0;}
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role){return role==MultiblockLayout.Role.ENERGY_INPUT?List.of(GregTechTags.Energy.LU):role==MultiblockLayout.Role.ENERGY_OUTPUT?List.of(GregTechTags.Energy.EU):List.of();}
    @Override public boolean emitsPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type){return role==MultiblockLayout.Role.ENERGY_OUTPUT&&type==GregTechTags.Energy.EU;}
    @Override public long portEnergyOutputSize(MultiblockLayout.Role role,GregTechTags.Tag type){return emitsPortEnergy(role,type)?8192:0;}
    /** Recipe acceptance stays on the TU rule. The spec maximum is the port and tooltip cap. */
    @Override protected long inputMinimum() { return 1; }
    @Override protected long inputMaximum() { return 16; }
    @Override public long portEnergyInputRecommended(MultiblockLayout.Role role,GregTechTags.Tag type){return 8192;}
    @Override public long portEnergyInputMin(MultiblockLayout.Role role,GregTechTags.Tag type){return 1;}
    @Override public long portEnergyInputMax(MultiblockLayout.Role role,GregTechTags.Tag type){return 16384;}
    @Override public long portEnergyStored(MultiblockLayout.Role role,GregTechTags.Tag type){return chargeTotal-chargeRemaining;}
    @Override public long portEnergyCapacity(MultiblockLayout.Role role,GregTechTags.Tag type){return chargeTotal;}
    @Override public long portEnergyDemanded(MultiblockLayout.Role role,GregTechTags.Tag type,long size) {
        return role==MultiblockLayout.Role.ENERGY_INPUT&&type==GregTechTags.Energy.LU&&size>0&&size<=16384&&chargeRemaining>0?1+(chargeRemaining-1)/size:0;
    }
    @Override public long injectPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type,long size,long amount,boolean execute) {
        if(role!=MultiblockLayout.Role.ENERGY_INPUT||type!=GregTechTags.Energy.LU||size<1||size>16384||amount<1||chargeRemaining<1||!isStructureOk())return 0;
        long accepted=Math.min(amount,1+(chargeRemaining-1)/size);
        if(execute){chargeRemaining=accepted>chargeRemaining/size?0:chargeRemaining-accepted*size;setChanged();}
        return accepted;
    }
//? if >=1.21.1 {
    /*@Override public void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
*///?} else {
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        var job=new CompoundTag();job.putLong("charge",chargeRemaining);job.putLong("totalCharge",chargeTotal);
        job.putLong("progress",progress);job.putLong("duration",duration);job.putLong("outputEU",outputEU);
        job.putBoolean("passive",passive);job.putBoolean("processing",processing);
//? if >=1.21.1 {
        /*if(products!=null)job.put("products",products.save(lookup));
*///?} else {
        if(products!=null)job.put("products",products.save());
//?}
        tag.put("gt.fusion",job);
    }
//? if >=1.21.1 {
    /*@Override public void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);var job=tag.getCompound("gt.fusion");
*///?} else {
    @Override public void load(CompoundTag tag) {
        super.load(tag);var job=tag.getCompound("gt.fusion");
//?}
        chargeTotal=Math.max(0,job.getLong("totalCharge"));chargeRemaining=Math.max(0,Math.min(chargeTotal,job.getLong("charge")));
        duration=Math.max(0,job.getLong("duration"));progress=Math.max(0,Math.min(duration,job.getLong("progress")));
//? if >=1.21.1 {
        /*outputEU=Math.max(0,job.getLong("outputEU"));products=job.contains("products")?MachineWorkOutputs.load(job.getCompound("products"),lookup):null;
*///?} else {
        outputEU=Math.max(0,job.getLong("outputEU"));products=job.contains("products")?MachineWorkOutputs.load(job.getCompound("products")):null;
//?}
        continuousRecipe=null;running=completed=false;
        // Restore only an explicit saved latch, never infer processing from a waiting job.
        // tickFusion validates the real structure before this latch can permit any work.
        passive=machineControl(null).enabled() && products!=null && duration>0 && job.getBoolean("passive");
        processing=passive && products!=null && duration>0 && chargeRemaining==0 && job.getBoolean("processing");
    }
}
