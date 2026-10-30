package gregtech.content.blockentity.machine;

import gregtech.engine.multiblock.*;
import gregtech.engine.multiblock.tracking.LayoutStructures;
import gregtech.engine.multiblock.tracking.PatternMatch;
import gregtech.engine.multiblock.tracking.StructureClaims;
import gregtech.engine.multiblock.tracking.StructureDefinition;
import gregtech.engine.multiblock.tracking.StructureTracking;
import gregtech.engine.multiblock.tracking.StructureScanProbe;
import gregtech.engine.multiblock.tracking.TrackedMultiblock;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.data.GregTechTags;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

import java.util.*;

/** Shared execution, part ownership and tool routing for original large recipe-machine layouts. */
public class LargeRecipeMachineBlockEntity extends BasicMachineBlockEntity implements MultiblockPortOwner, TrackedMultiblock, MultiblockToolTarget {
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    private boolean structureFormed;
    private java.util.List<AddonEnergyPartBlockEntity> conditionParts = java.util.List.of();
    public LargeRecipeMachineBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    private boolean originalProcessingController() {return spec()!=null && spec().suppressSideAutoInput();}
    private boolean sourceUnlimitedOutputs() {return spec()!=null && spec().unlimitedOutputTanks();}
    private BlockPos sourcePosition(int right,int up,int back) {
        var front=getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        return worldPosition.relative(front.getClockWise(),right).above(up).relative(front.getOpposite(),back);
    }
    private Direction declaredFace(gregtech.api.machine.MachineFace face) {
        return relativeToAbsolute(face.index());
    }
    private record Placed(BlockPos pos, Direction face) {}
    private Placed place(int right, int up, int back, Direction face) { return new Placed(sourcePosition(right, up, back), face); }
    private Placed declared(gregtech.api.machine.IoTarget target) {
        return target == null ? null : place(target.right(), target.up(), target.back(), declaredFace(target.receiver()));
    }
    private record OutputCell(int right, int up, int back, Direction face, String label) {}
    private OutputCell outputCell(boolean items) {
        if (spec() == null) return null;
        var declared = items ? spec().itemOutputTarget() : spec().fluidOutputTarget();
        if (declared == null) return null;
        return new OutputCell(declared.right(), declared.up(), declared.back(),
                declaredFace(declared.receiver()),
                declared.right() + "," + declared.up() + "," + declared.back() + "," + outputLabelFace(declared.receiver()));
    }
    /** The snapshot still prints the original UP and DOWN tokens. TOP and BOTTOM are those directions. */
    private static String outputLabelFace(gregtech.api.machine.MachineFace face) {
        return switch (face) {
            case TOP -> "UP";
            case BOTTOM -> "DOWN";
            default -> face.name();
        };
    }
    /** Relative cell the processing path pushes to. Empty when this controller does not name one. */
    protected String outputCellLabel(boolean items) {
        var cell = outputCell(items);
        return cell == null ? "" : cell.label();
    }
    private Placed itemOutput() {
        var cell = outputCell(true);
        return cell == null ? null : place(cell.right(), cell.up(), cell.back(), cell.face());
    }
    private Placed fluidOutput() {
        var cell = outputCell(false);
        return cell == null ? null : place(cell.right(), cell.up(), cell.back(), cell.face());
    }
    private IItemHandler itemsAt(BlockPos pos, Direction face) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        var be = level.getBlockEntity(pos);
//? if >=1.21.1 {
/*        if (be instanceof gregtech.engine.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return null;
        if (be == null) return null;
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, face);
*///?} else {
        if (be instanceof MultiblockPortBlockEntity part && part.isBoundTo(worldPosition)) return null;
        if (be == null) return null;
        return be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, face).resolve().orElse(null);
//?}
    }
    private IFluidHandler fluidsAt(BlockPos pos, Direction face) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        var be = level.getBlockEntity(pos);
//? if >=1.21.1 {
/*        if (be instanceof gregtech.engine.multiblock.BoundMachinePort part && part.isBoundTo(worldPosition)) return null;
        if (be == null) return null;
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, face);
*///?} else {
        if (be instanceof MultiblockPortBlockEntity part && part.isBoundTo(worldPosition)) return null;
        if (be == null) return null;
        return be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, face).resolve().orElse(null);
//?}
    }
    private void pushItems(BlockPos pos, Direction face) {
        var target = itemsAt(pos, face);
        if (target == null) return;
        for (int slot = inputSlots(); slot < inventory().getSlots(); slot++) {
            var offer = inventory().getStackInSlot(slot);
            if (offer.isEmpty()) continue;
            var result = gregtech.content.transport.ItemPipeTransferAdapter.transfer(offer.copy(), () -> target);
            if (result.accepted() > 0) inventory().extractItem(slot, result.accepted(), false);
        }
    }
    private void pushFluids(BlockPos pos, Direction face) {
        var target = fluidsAt(pos, face);
        var tanks = getTanksOutput();
        if (target == null || tanks == null) return;
        for (var tank : tanks) {
            var offer = tank.getFluid();
            if (offer.isEmpty()) continue;
            int simulated = target.fill(offer.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (simulated <= 0) continue;
            offer.setAmount(Math.min(simulated, offer.getAmount()));
            int accepted = target.fill(offer.copy(), IFluidHandler.FluidAction.EXECUTE);
            if (accepted < 0 || accepted > offer.getAmount())
                throw new IllegalStateException("Invalid original controller fluid receiver amount: " + accepted);
            if (accepted > 0) tank.remove(accepted);
        }
    }
    private void pullItems(BlockPos pos, Direction face) {
        var source = itemsAt(pos, face);
        if (source == null) return;
        int inputCount = inputSlots();
        if (inputCount <= 0) return;
        for (int srcSlot = 0; srcSlot < source.getSlots(); srcSlot++) {
            ItemStack stack = source.getStackInSlot(srcSlot);
            if (stack.isEmpty() || !acceptsAutomaticInput(stack)) continue;
            for (int i = 0; i < inputCount; i++) {
                ItemStack remaining = inventory().insertItem(i, stack.copy(), true);
                int toTake = stack.getCount() - remaining.getCount();
                if (toTake <= 0) continue;
                ItemStack extracted = source.extractItem(srcSlot, toTake, false);
                if (!extracted.isEmpty()) {
                    ItemStack leftover = inventory().insertItem(i, extracted, false);
                    returnOrDrop(leftover, source);
                    if (source.getStackInSlot(srcSlot).isEmpty()) break;
                    stack = source.getStackInSlot(srcSlot);
                }
            }
        }
    }
    private void pullFluids(BlockPos pos, Direction face) {
        var source = fluidsAt(pos, face);
        if (source == null) return;
        FluidStack drained = source.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) return;
        int filled = fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return;
        FluidStack toDrain = drained.copy();
        toDrain.setAmount(filled);
        FluidStack actuallyDrained = source.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        if (!actuallyDrained.isEmpty()) fill(actuallyDrained, IFluidHandler.FluidAction.EXECUTE);
    }
    // The source multiblock parent suppresses ordinary six-neighbor requests; only explicit layouts opt in.
    protected record EnergySourceCell(int right, int up, int back, String face) {}
    /** Cells whose neighbor is toggled with this controller. Empty means the six neighbors are left alone. */
    protected java.util.List<EnergySourceCell> energySourceCells() {
        if (spec() == null) return java.util.List.of();
        var list = new java.util.ArrayList<EnergySourceCell>();
        for (var source : spec().energySources())
            list.add(new EnergySourceCell(source.right(), source.up(), source.back(), source.face()));
        return list;
    }
    protected String energySourceText() {
        var text = new StringBuilder();
        for (var source : energySourceCells()) {
            if (!text.isEmpty()) text.append(';');
            text.append(source.right()).append(',').append(source.up()).append(',').append(source.back()).append(',').append(source.face());
        }
        return text.toString();
    }
    private Direction energySourceFace(String face) {
        var front = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        return switch (face) {
            case "UP" -> Direction.UP;
            case "DOWN" -> Direction.DOWN;
            case "LEFT" -> front.getCounterClockWise();
            case "RIGHT" -> front.getClockWise();
            case "FRONT" -> front;
            default -> throw new IllegalStateException("Unknown energy source face: " + face);
        };
    }
    @Override protected void updateAdjacentToggleableEnergySources() {
        if (level == null || level.isClientSide || spec() == null || isRemoved()) return;
        for (var source : energySourceCells()) {
            var pos = sourcePosition(source.right(), source.up(), source.back());
            if (level.hasChunkAt(pos)) gregtech.engine.machine.AdjacentEnergyControl.update(level.getBlockEntity(pos),
                    spec().energyTag(), energySourceFace(source.face()), machineControl(null).enabled());
        }
    }
    @Override public void setSpec(gregtech.engine.machine.BasicMachineSpec spec) {
        super.setSpec(spec);
        if(sourceUnlimitedOutputs())for(var tank:getTanksOutput())tank.setCapacity(Long.MAX_VALUE);
    }
    @Override protected void autoInputItems(net.minecraft.world.level.Level level,BlockPos pos,int side) {
        if (declared(spec() == null ? null : spec().itemInputSource()) != null || originalProcessingController()) return;
        super.autoInputItems(level,pos,side); // Source getItemInputTarget returns null.
    }
    @Override protected void autoInputFluids(net.minecraft.world.level.Level level,BlockPos pos,int side) {
        if (declared(spec() == null ? null : spec().fluidInputSource()) != null || originalProcessingController()) return;
        super.autoInputFluids(level,pos,side); // Source getFluidInputTarget returns null.
    }
    @Override protected void positionedItemInput() {
        var source = declared(spec() == null ? null : spec().itemInputSource());
        if (source != null) pullItems(source.pos(), source.face());
    }
    @Override protected void positionedFluidInput() {
        var source = declared(spec() == null ? null : spec().fluidInputSource());
        if (source != null) pullFluids(source.pos(), source.face());
    }
    @Override protected void positionedItemOutput() {
        var output = itemOutput();
        if (output != null) pushItems(output.pos(), output.face());
    }
    @Override protected void beforeMachineTick() {
        super.beforeMachineTick();
        if (!automaticTransferEnabled(gregtech.engine.machine.MachineToolSettings.Transfer.FLUID_OUTPUT)) return;
        var output = fluidOutput();
        if (output == null) return;
        if (spec().fluidOutputTarget() == null && faceConfig().fluidAutoOutput() < 0) return;
        pushFluids(output.pos(), output.face());
    }
    @Override protected void autoOutputFluids(net.minecraft.world.level.Level level,BlockPos pos,int side) {
        if (fluidOutput() != null) return; // Already moved before work, including while stopped/unformed.
        super.autoOutputFluids(level,pos,side);
    }
    @Override protected void autoOutputItems(net.minecraft.world.level.Level level,BlockPos pos,int side) {
        var output = itemOutput();
        if (output == null) { super.autoOutputItems(level,pos,side); return; }
        pushItems(output.pos(), output.face());
    }

//? if >=1.21.1 {
/*    @Override public void loadAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
*///?} else {
    @Override public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
//?}

        if(sourceUnlimitedOutputs())for(var tank:getTanksOutput())tank.setCapacity(Long.MAX_VALUE);
    }
    @Override protected boolean usesTimeEnergy() { return spec()!=null&&spec().energyTag()==GregTechTags.Energy.TU; }
    @Override protected int efficiency() { return spec().processingEfficiency(); }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side) { return usesTimeEnergy()?1:512; }
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type,Direction side) { return inputMinimum(); }
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type,Direction side) { return inputMaximum(); }
    @Override protected boolean structureComplete() { return structureOk() && conditionPartsMet(); }
    protected boolean structureOk() {
        return structureFormed;
    }
    @Override public void structureFormedChanged(boolean formed) { structureFormed = formed; }
    /** Only the edge rebuilds this list; a formed tick never walks the shell. */
    @Override public void onStructureFormed() {
        java.util.ArrayList<AddonEnergyPartBlockEntity> found = new java.util.ArrayList<>();
        if (level != null) for (BlockPos pos : bindings.positions())
            if (level.getBlockEntity(pos) instanceof AddonEnergyPartBlockEntity part && part.condition()) found.add(part);
        conditionParts = java.util.List.copyOf(found);
    }
    @Override public void onStructureBroken() { conditionParts = java.util.List.of(); }
    /** The layout a structure check reads. Controllers with hand-built cells override {@link #structureCells()}. */
    protected LargeMachineLayouts.Layout structureLayout() {
        var layout=LargeMachineLayouts.layout(spec().machineName());
        if(layout!=null)return layout;
        var cells=structureCells();
        return cells==null?null:new LargeMachineLayouts.Layout(cells,java.util.List.of());
    }
    /** Hand-built cells for a controller without a shared layout. Read on every check. */
    protected java.util.List<LargeMachineLayouts.Cell> structureCells() { return null; }
    protected boolean validStructureParts() {return true;}
    protected boolean matchesPart(LargeMachineLayouts.Cell cell,net.minecraft.world.level.block.Block expected,net.minecraft.world.level.block.Block actual) {return (actual == expected);}
    public LargeMachineLayouts.Layout trackedLayout() { return structureLayout(); }
    @Override public boolean acceptsTrackedPart(LargeMachineLayouts.Cell cell, net.minecraft.world.level.block.Block expected, net.minecraft.world.level.block.Block actual) {
        if (cell.part() == 18042 && spec() != null && "largeoven".equals(spec().machineName()))
            return (actual == expected || actual == gregtech.content.multiblock.LargeMachineParts.block(18043))
                    && matchesPart(cell, actual, actual);
        return matchesPart(cell, expected, actual);
    }
    @Override public boolean structurePartsValid() { return validStructureParts(); }
    @Override public StructureDefinition structureDefinition() {
        return spec() != null && "largeoven".equals(spec().machineName())
                ? gregtech.content.multiblock.SpecialStructures.oven(this) : LayoutStructures.recipe(this);
    }
    @Override public boolean isStructureOk() { return StructureTracking.query(this); }
    @Override public boolean containsToolPosition(BlockPos pos) {
        return worldPosition.equals(pos) || structureDefinition().declaredFootprint(this).contains(pos);
    }
    @Override public long useMultiblockTool(UseOnContext context, List<Component> messages) {
        if (level == null || level.isClientSide || isRemoved() || spec() == null
                || !containsToolPosition(context.getClickedPos())) return 0;
        if (GTToolHelper.matchesTool(context.getItemInHand(), GTToolType.BUILDER_WAND)) {
            var layout = structureLayout();
            if (layout == null) return 0;
            var front = LayoutStructures.facing(this);
            boolean wholeStructure = MultiblockTools.buildsWholeStructure(context, worldPosition);
            for (var cell : layout.cells()) buildCell(context, cell, cell.at(worldPosition, front), wholeStructure);
            // Repeat layouts build their minimum valid size, as shown in the structure preview.
            for (var repeat : layout.repeats()) for (int copy = 0; copy < repeat.min(); copy++)
                for (var cell : repeat.cells())
                    buildCell(context, cell, cell.at(worldPosition, front, repeat.absoluteUp(cell, copy)), wholeStructure);
            StructureTracking.invalidate(this);
            return MultiblockToolRules.BUILDER_COST;
        }
        if (GTToolHelper.matchesTool(context.getItemInHand(), GTToolType.MAGNIFYING_GLASS)) {
            appendMachineInspection(messages);
            messages.add(Component.literal(MultiblockToolRules.structureMessage(structureFormed, isStructureOk())));
            if (!structureFormed) MultiblockTools.appendDiagnosis(this, messages);
            return MultiblockToolRules.MAGNIFIER_COST;
        }
        return 0;
    }
    private void buildCell(
            UseOnContext context, LargeMachineLayouts.Cell cell, BlockPos pos, boolean wholeStructure) {
        // GT6 builds registered structure parts, not required air, fluids or arbitrary terrain.
        if (pos.equals(worldPosition)) return;
        MultiblockTools.buildCell(context, pos, cell, wholeStructure);
    }
    @Override public boolean scanStructure() {
        if(level==null||spec()==null||isRemoved()) return false;
        var layout=structureLayout();if(layout==null)return false;
        int[] matchedLayers=new int[layout.repeats().size()];
        try {
            boolean complete = validStructureParts();
            var front=getBlockState().getValue(HorizontalDirectionalBlock.FACING);
            var matchedCells=new java.util.ArrayList<LargeMachineLayouts.Cell>();
            var matchedParts=new java.util.ArrayList<LargeMachineLayouts.Placed>();
            for(var cell:layout.cells()) {
                var pos=cell.at(worldPosition,front);
                var matched=matchingCell(cell,pos);
                if(matched==null) {complete=false;continue;}
                matchedCells.add(matched);
                matchedParts.add(new LargeMachineLayouts.Placed(matched,pos));
            }
            var repeatPlaced=new java.util.ArrayList<LargeMachineLayouts.Placed>();
            for(int group=0;group<layout.repeats().size();group++) {
                var repeat=layout.repeats().get(group);
                var matches=new java.util.HashMap<BlockPos,LargeMachineLayouts.Cell>();
                int[] found=new int[1];
                var placed=LargeMachineLayouts.repeatCells(level,worldPosition,front,repeat,(cell,pos)->{
                    var matched=matchingCell(cell,pos);
                    if(matched==null)return false;
                    matches.put(pos,matched);
                    return true;
                },found);
                matchedLayers[group]=found[0];
                if(placed==null){complete=false;continue;}
                // Only complete repeat copies are committed, not the matched prefix of a failed copy.
                for(var cell:placed) {
                    var matched=matches.get(cell.pos());
                    repeatPlaced.add(new LargeMachineLayouts.Placed(matched,cell.pos()));
                }
            }
            // The retained full scan checks the whole coil group explicitly; matchers have no scan-local state.
            if ("largeoven".equals(spec().machineName()) && !ovenCoilsAgree(front, matchedCells, repeatPlaced)) {
                complete = false;
            }
            matchedParts.addAll(repeatPlaced);
            return updateStructureBindings(matchedParts, complete);
        } finally {
            StructureTracking.noteMatchedLayers(this, matchedLayers);
        }
    }
    /** Claims a match the incremental evaluator already built, without reading the layout again. */
    @Override public boolean applyStructure(PatternMatch match) {
        return updateStructureBindings(matchedParts(match), true);
    }
    @Override public void applyIncompleteStructure(PatternMatch match) {
        // Custom subclasses with their own claim table retain their existing release policy.
        if (structureLayout() == null) { releaseStructureClaims(); return; }
        updateStructureBindings(matchedParts(match), false);
    }
    private List<LargeMachineLayouts.Placed> matchedParts(PatternMatch match) {
        var parts = new ArrayList<LargeMachineLayouts.Placed>();
        for (var fixed : match.fixedCells()) parts.add(new LargeMachineLayouts.Placed(fixed.cell(), fixed.pos()));
        parts.addAll(match.repeatPlaced());
        return parts;
    }
    /** Incomplete shells keep tool routing; port capabilities still require a formed controller. */
    private boolean updateStructureBindings(List<LargeMachineLayouts.Placed> matched, boolean complete) {
        if (level == null || spec() == null || isRemoved()) return false;
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        var designs = new ArrayList<LargeMachineLayouts.Placed>();
        for (var placed : matched) {
            var cell = placed.cell();
            if (cell.role() == MultiblockLayout.Role.AIR || cell.plainBlock()) continue;
            if (level.getBlockEntity(placed.pos()) instanceof MultiblockPortBlockEntity port && port.canBind(worldPosition)) {
                parts.put(placed.pos(), cell.role());
                designs.add(placed);
            } else complete = false;
        }
        var claimed = StructureClaims.update(this, bindings, parts,
                pos -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).canBind(worldPosition),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::release);
        if (claimed == StructureClaims.Result.FAILED) return false;
        if (claimed == StructureClaims.Result.APPLIED) gregtech.content.multiblock.StructureDesigns.apply(level, designs);
        return complete;
    }

    @Override public void releaseStructureClaims() {
        bindings.clear(this::release);
        StructureClaims.cleared(this);
    }

    private boolean ovenCoilsAgree(Direction front, List<LargeMachineLayouts.Cell> fixed, List<LargeMachineLayouts.Placed> repeated) {
        return java.util.stream.Stream.concat(
                fixed.stream().filter(cell -> cell.part() == 18042).map(cell -> cell.at(worldPosition, front)),
                repeated.stream().filter(placed -> placed.cell().part() == 18042).map(LargeMachineLayouts.Placed::pos))
                .map(pos -> level.getBlockState(pos).getBlock()).distinct().limit(2).count() <= 1;
    }
    private LargeMachineLayouts.Cell matchingCell(LargeMachineLayouts.Cell cell,BlockPos pos) {
        StructureScanProbe.matcher();
        if(!level.hasChunkAt(pos))return null;
        if(cell.role()==MultiblockLayout.Role.AIR)return level.isEmptyBlock(pos)?cell:null;
        if(matchesChoice(cell,cell.part(),cell.plain(),pos))return cell;
        for(var alternative:cell.alternatives()) {
            var choice = new LargeMachineLayouts.Cell(cell.right(),cell.up(),cell.back(),alternative.part(),
                    cell.role(),alternative.plain(),cell.design(),java.util.List.of());
            if(matchesChoice(choice,alternative.part(),alternative.plain(),pos))return choice;
        }
        return null;
    }
    private boolean matchesChoice(LargeMachineLayouts.Cell cell,int part,String plain,BlockPos pos) {
        if(plain!=null)return LargeMachineLayouts.matchesPlain(level,pos,plain);
        var expected=gregtech.content.multiblock.LargeMachineParts.block(part);
        if(part==18042 && "largeoven".equals(spec().machineName())) {
            var actual = level.getBlockState(pos).getBlock();
            return (actual == expected || actual == gregtech.content.multiblock.LargeMachineParts.block(18043))
                    && matchesPart(cell, actual, actual)
                    && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity;
        }
        return matchesPart(cell,expected,level.getBlockState(pos).getBlock())
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity;
    }
    /** Bound condition parts must all be satisfied. A structure with none works as before. */
    private boolean conditionPartsMet() {
        if (level == null) return true;
        for (AddonEnergyPartBlockEntity part : conditionParts) if (!part.satisfied()) return false;
        return true;
    }
    private void release(BlockPos pos) { if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part) part.release(worldPosition); }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) StructureTracking.attach(this);
    }
    @Override public void onChunkUnloaded() {
        StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }
    @Override public void setRemoved() {
        if (!StructureTracking.unloading(this)) bindings.clear(this::release);
        StructureTracking.onRemoved(this);
        super.setRemoved();
    }
    private static boolean input(MultiblockLayout.Role role) { return role==MultiblockLayout.Role.ITEM_FLUID_IO||role==MultiblockLayout.Role.ITEM_FLUID_INPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY; }
    private static boolean output(MultiblockLayout.Role role) { return role==MultiblockLayout.Role.FLUID_OUTPUT||role==MultiblockLayout.Role.ITEM_FLUID_IO||role==MultiblockLayout.Role.ITEM_FLUID_OUTPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY; }
    private static boolean energy(MultiblockLayout.Role role) {return role==MultiblockLayout.Role.ENERGY_INPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT||role==MultiblockLayout.Role.ITEM_FLUID_ENERGY;}
    private final Map<MultiblockLayout.Role,IItemHandler> itemPorts=new EnumMap<>(MultiblockLayout.Role.class);
    private final Map<MultiblockLayout.Role,IFluidHandler> fluidPorts=new EnumMap<>(MultiblockLayout.Role.class);
    @Override public IItemHandler portItems(MultiblockLayout.Role role) {
        return role==MultiblockLayout.Role.FLUID_OUTPUT||(!input(role)&&!output(role))?null:itemPorts.computeIfAbsent(role,this::createItemPort);
    }
    private IItemHandler createItemPort(MultiblockLayout.Role role) {
        return new IItemHandler() {
            public int getSlots() {return inventory().getSlots();}
            public ItemStack getStackInSlot(int slot) {return inventory().getStackInSlot(slot);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {return input(role)?inventory().insertItem(slot,stack,simulate):stack;}
            public ItemStack extractItem(int slot,int amount,boolean simulate) {return output(role)&&slot>=inputSlots()?inventory().extractItem(slot,amount,simulate):ItemStack.EMPTY;}
            public int getSlotLimit(int slot) {return inventory().getSlotLimit(slot);}
            public boolean isItemValid(int slot,ItemStack stack) {return input(role)&&slot<inputSlots();}
        };
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return !input(role)&&!output(role)?null:fluidPorts.computeIfAbsent(role,this::createFluidPort);
    }
    private IFluidHandler createFluidPort(MultiblockLayout.Role role) {
        return new IFluidHandler() {
            public int getTanks(){return LargeRecipeMachineBlockEntity.this.getTanks();}
            public FluidStack getFluidInTank(int tank){return LargeRecipeMachineBlockEntity.this.getFluidInTank(tank);}
            public int getTankCapacity(int tank){return LargeRecipeMachineBlockEntity.this.getTankCapacity(tank);}
            public boolean isFluidValid(int tank,FluidStack stack){return input(role)&&LargeRecipeMachineBlockEntity.this.isFluidValid(tank,stack);}
            public int fill(FluidStack stack,FluidAction action){return input(role)?LargeRecipeMachineBlockEntity.this.fill(stack,action):0;}
            public FluidStack drain(FluidStack stack,FluidAction action){return output(role)?LargeRecipeMachineBlockEntity.this.drain(stack,action):FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){return output(role)?LargeRecipeMachineBlockEntity.this.drain(amount,action):FluidStack.EMPTY;}
        };
    }
    @Override public long portEnergyInputMin(MultiblockLayout.Role role,GregTechTags.Tag type) {return inputMinimum();}
    @Override public long portEnergyInputMax(MultiblockLayout.Role role,GregTechTags.Tag type) {return inputMaximum();}
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role){return !usesTimeEnergy()&&energy(role)?List.of(spec().energyTag()):List.of();}
    @Override public long portEnergyStored(MultiblockLayout.Role role,GregTechTags.Tag type){return portEnergyTypes(role).contains(type)?getEnergyTick():0;}
    @Override public long portEnergyCapacity(MultiblockLayout.Role role,GregTechTags.Tag type){return portEnergyTypes(role).contains(type)?inputMaximum():0;}
    @Override public long injectPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type,long size,long amount,boolean execute){return portEnergyTypes(role).contains(type)&&isStructureOk()?doEnergyInjection(type,null,size,amount,execute):0;}
}
