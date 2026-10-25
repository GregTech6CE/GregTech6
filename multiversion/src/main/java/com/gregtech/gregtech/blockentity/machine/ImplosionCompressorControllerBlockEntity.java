package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
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
        com.gregtech.gregtech.api.multiblock.tracking.TrackedMultiblock {
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    private final com.gregtech.gregtech.api.multiblock.tracking.StructureDefinition definition=
            com.gregtech.gregtech.api.multiblock.tracking.FixedStructures.declarative(this,this::structureCells,List.of());
    public static final MultiblockLayout LAYOUT=MultiblockLayout.fromShared(com.gregtech.gregtech.content.multiblock.SharedImplosionStructure.LAYOUT);
//? if >=1.21.1 {
    /*public ImplosionCompressorControllerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state);
*///?} else {
    public ImplosionCompressorControllerBlockEntity(BlockPos pos,BlockState state) {
        super(GTBlockEntities.IMPLOSION_COMPRESSOR.get(),pos,state);
//?}
        setSpec(com.gregtech.gregtech.content.machine.BasicMachineDefinitions.from(com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.implosionCompressor()));
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,ImplosionCompressorControllerBlockEntity machine) {
        BasicMachineBlockEntity.serverTick(level,pos,state,machine);
    }
    private boolean structureFormed;
    @Override protected boolean structureComplete() {
        return com.gregtech.gregtech.api.multiblock.tracking.StructureTrackingMode.legacy() ? isStructureOk() : structureFormed;
    }
    @Override public void structureFormedChanged(boolean formed) { structureFormed = formed; }
    @Override public com.gregtech.gregtech.api.multiblock.tracking.StructureDefinition structureDefinition() {
        return definition;
    }
    private List<com.gregtech.gregtech.api.multiblock.tracking.CompiledPattern.FixedCell> structureCells(Direction front) {
        var cells=new ArrayList<com.gregtech.gregtech.api.multiblock.tracking.CompiledPattern.FixedCell>(LAYOUT.cells().size());
        for(var cell:LAYOUT.cells()) {
            int part=cell.role()==MultiblockLayout.Role.AIR?0:18023;
            var spec=new com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.Cell(cell.right(),cell.up(),cell.back(),part,cell.role());
            var offset=spec.at(BlockPos.ZERO,front);
            cells.add(new com.gregtech.gregtech.api.multiblock.tracking.CompiledPattern.FixedCell(
                    offset.getX(),offset.getY(),offset.getZ(),spec));
        }
        return cells;
    }
    @Override public boolean applyStructure(com.gregtech.gregtech.api.multiblock.tracking.PatternMatch match) {
        if(level==null||isRemoved())return false;
        var candidates=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        for(var fixed:match.fixedCells()) {
            var cell=fixed.cell();
            if(cell.role()!=MultiblockLayout.Role.AIR)candidates.put(fixed.pos(),cell.role());
        }
        return com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.update(this,bindings,candidates,
                pos->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).canBind(worldPosition),
                (pos,role)->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).bind(worldPosition,role),this::release)
                !=com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.Result.FAILED;
    }
    @Override public void releaseStructureClaims() {
        bindings.clear(this::release);
        com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.cleared(this);
    }
    @Override public boolean isStructureOk() {
        return com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.query(this);
    }
    @Override public boolean scanStructure() {
        if(level==null || isRemoved()) return false;
        var facing=getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        var candidates=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        boolean valid=LAYOUT.matches(worldPosition,facing,level::hasChunkAt,(pos,role)->{
            if(role==MultiblockLayout.Role.AIR) return level.isEmptyBlock(pos);
            if(!level.getBlockState(pos).is(GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get()) || !(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)) return false;
            candidates.put(pos,role);return true;
        });
        if(!valid) { bindings.clear(this::release); com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.cleared(this); return false; }
        return com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.update(this, bindings, candidates,
                pos->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).canBind(worldPosition),
                (pos,role)->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).bind(worldPosition,role),this::release)
                != com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.Result.FAILED;
    }
    private void release(BlockPos pos) { if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part) part.release(worldPosition); }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.attach(this);
    }
    @Override public void onChunkUnloaded() {
        com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }
    @Override public void setRemoved() {
        if (!com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.unloading(this)) bindings.clear(this::release);
        com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.onRemoved(this);
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
