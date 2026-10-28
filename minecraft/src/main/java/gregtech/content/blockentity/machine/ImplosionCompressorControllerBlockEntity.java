package gregtech.content.blockentity.machine;

import gregtech.engine.multiblock.*;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.energy.FaceConfig;
import gregtech.content.material.Materials;
import gregtech.content.data.*;
import gregtech.content.registry.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import java.util.*;

/** GT6 implosion: 25 dense tungstensteel walls, one air cell, timed explosive recipes. */
public class ImplosionCompressorControllerBlockEntity extends BasicMachineBlockEntity implements MultiblockPortOwner,
        gregtech.engine.multiblock.tracking.TrackedMultiblock {
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    private final gregtech.engine.multiblock.tracking.StructureDefinition definition=
            gregtech.engine.multiblock.tracking.FixedStructures.declarative(this,this::structureCells,List.of());
    public static final MultiblockLayout LAYOUT=MultiblockLayout.fromShared(gregtech.content.multiblock.SharedImplosionStructure.LAYOUT);
//? if >=1.21.1 {
    /*public ImplosionCompressorControllerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state);
*///?} else {
    public ImplosionCompressorControllerBlockEntity(BlockPos pos,BlockState state) {
        super(GTBlockEntities.IMPLOSION_COMPRESSOR.get(),pos,state);
//?}
        setSpec(gregtech.content.machine.BasicMachineDefinitions.from(gregtech.content.multiblock.OriginalMultiblockMachineParameters.implosionCompressor()));
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,ImplosionCompressorControllerBlockEntity machine) {
        BasicMachineBlockEntity.serverTick(level,pos,state,machine);
    }
    private boolean structureFormed;
    @Override protected boolean structureComplete() {
        return structureFormed;
    }
    @Override public void structureFormedChanged(boolean formed) { structureFormed = formed; }
    @Override public gregtech.engine.multiblock.tracking.StructureDefinition structureDefinition() {
        return definition;
    }
    private List<gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell> structureCells(Direction front) {
        var cells=new ArrayList<gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell>(LAYOUT.cells().size());
        for(var cell:LAYOUT.cells()) {
            int part=cell.role()==MultiblockLayout.Role.AIR?0:18023;
            var spec=new gregtech.content.multiblock.LargeMachineLayouts.Cell(cell.right(),cell.up(),cell.back(),part,cell.role());
            var offset=spec.at(BlockPos.ZERO,front);
            cells.add(new gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell(
                    offset.getX(),offset.getY(),offset.getZ(),spec));
        }
        return cells;
    }
    @Override public boolean applyStructure(gregtech.engine.multiblock.tracking.PatternMatch match) {
        if(level==null||isRemoved())return false;
        var candidates=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(var fixed:match.fixedCells()) {
            var cell=fixed.cell();
            if(cell.role()!=MultiblockLayout.Role.AIR)candidates.put(fixed.pos(),cell.role());
        }
        return gregtech.engine.multiblock.tracking.StructureClaims.update(this,bindings,candidates,
                pos->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).canBind(worldPosition),
                (pos,role)->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).bind(worldPosition,role),this::release)
                !=gregtech.engine.multiblock.tracking.StructureClaims.Result.FAILED;
    }
    @Override public void applyIncompleteStructure(gregtech.engine.multiblock.tracking.PatternMatch match) {
        if(level==null||isRemoved())return;
        gregtech.engine.multiblock.tracking.StructureClaims.bindMatched(this,bindings,match,false,this::release);
    }
    @Override public void releaseStructureClaims() {
        bindings.clear(this::release);
        gregtech.engine.multiblock.tracking.StructureClaims.cleared(this);
    }
    @Override public boolean isStructureOk() {
        return gregtech.engine.multiblock.tracking.StructureTracking.query(this);
    }
    private void release(BlockPos pos) { if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part) part.release(worldPosition); }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) gregtech.engine.multiblock.tracking.StructureTracking.attach(this);
    }
    @Override public void onChunkUnloaded() {
        gregtech.engine.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }
    @Override public void setRemoved() {
        if (!gregtech.engine.multiblock.tracking.StructureTracking.unloading(this)) bindings.clear(this::release);
        gregtech.engine.multiblock.tracking.StructureTracking.onRemoved(this);
        super.setRemoved();
    }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) { return null; }
    @Override public IItemHandler portItems(MultiblockLayout.Role role) {
//? if >=1.21.1 {
        /*return role==MultiblockLayout.Role.ITEM_IO?itemCapability(null):null;
*///?} else {
        return role==MultiblockLayout.Role.ITEM_IO?getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElse(null):null;
//?}
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) { return false; }
}
