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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.*;

/** GT6 implosion: 25 dense tungstensteel walls, one air cell, timed explosive recipes. */
public class ImplosionCompressorControllerBlockEntity extends BasicMachineBlockEntity implements MultiblockPortOwner {
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    public static final MultiblockLayout LAYOUT=MultiblockLayout.fromShared(com.gregtech.gregtech.content.multiblock.SharedImplosionStructure.LAYOUT);
    public ImplosionCompressorControllerBlockEntity(BlockPos pos,BlockState state) {
        super(GTBlockEntities.IMPLOSION_COMPRESSOR.get(),pos,state);
        setSpec(com.gregtech.gregtech.content.machine.BasicMachineDefinitions.from(com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.implosionCompressor()));
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,ImplosionCompressorControllerBlockEntity machine) {
        if(machine.isStructureOk()) BasicMachineBlockEntity.serverTick(level,pos,state,machine);
    }
    @Override protected boolean usesTimeEnergy() { return true; }
    @Override protected long inputMinimum() { return 1; }
    @Override protected long inputMaximum() { return 16; }
    @Override protected boolean parallelScalesDuration() { return false; }
    @Override protected boolean structureComplete() { return isStructureOk(); }
    @Override public boolean isStructureOk() {
        if(level==null || isRemoved()) return false;
        var facing=getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        var candidates=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        boolean valid=LAYOUT.matches(worldPosition,facing,level::hasChunkAt,(pos,role)->{
            if(role==MultiblockLayout.Role.AIR) return level.isEmptyBlock(pos);
            if(!level.getBlockState(pos).is(GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get()) || !(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)) return false;
            candidates.put(pos,role);return true;
        });
        if(!valid) { bindings.clear(this::release);return false; }
        return bindings.update(candidates,pos->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).canBind(worldPosition),
                (pos,role)->((MultiblockPortBlockEntity)level.getBlockEntity(pos)).bind(worldPosition,role),this::release);
    }
    private void release(BlockPos pos) { if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part) part.release(worldPosition); }
    @Override public void setRemoved() { bindings.clear(this::release);super.setRemoved(); }
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) { return null; }
    @Override public IItemHandler portItems(MultiblockLayout.Role role) {
        return role==MultiblockLayout.Role.ITEM_IO?getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElse(null):null;
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,Direction side,boolean theoretical) { return false; }
}
