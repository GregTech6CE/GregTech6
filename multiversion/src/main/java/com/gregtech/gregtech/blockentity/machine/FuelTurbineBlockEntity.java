package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidPort;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.MultiblockPortOwner;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

import javax.annotation.Nullable;
import java.util.LinkedHashMap;

/** Multiblock fuel generator. The structure is an existing large-machine layout of GregTech parts. */
public class FuelTurbineBlockEntity extends FuelGeneratorBlockEntity implements MultiblockPortOwner {
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();

    public FuelTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        formed = false;
    }

    @Override
    protected boolean refreshStructure() { return isStructureOk(); }

    @Override
    protected boolean acceptsFluid(@Nullable Direction side) { return side == null || side == front(); }

    @Override
    public boolean isStructureOk() {
        if (level == null || isRemoved()) {
            formed = false;
            return false;
        }
        var layout = LargeMachineLayouts.cells(declaration().machineType());
        if (layout == null) {
            bindings.clear(this::release);
            formed = false;
            return false;
        }
        var front = front();
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var cell : layout) {
            BlockPos pos = cell.at(worldPosition, front);
            if (!level.hasChunkAt(pos)) {
                bindings.clear(this::release);
                formed = false;
                return false;
            }
            if (cell.role() == MultiblockLayout.Role.AIR) {
                if (level.isEmptyBlock(pos)) continue;
                bindings.clear(this::release);
                formed = false;
                return false;
            }
            if (level.getBlockState(pos).getBlock() != cell.block()
                    || !(level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity)) {
                bindings.clear(this::release);
                formed = false;
                return false;
            }
            parts.put(pos, cell.role());
        }
        formed = bindings.update(parts,
                pos -> ((com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity) level.getBlockEntity(pos)).canBind(worldPosition),
                (pos, role) -> ((com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::release);
        return formed;
    }

    private void release(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.MultiblockPortBlockEntity part)
            part.release(worldPosition);
    }

    @Override
    public void setRemoved() {
        bindings.clear(this::release);
        super.setRemoved();
    }

    @Override
    public IFluidHandler portFluids(MultiblockLayout.Role role) {
        boolean input = input(role);
        boolean output = output(role);
        if (!input && !output) return null;
        return new FluidPort(this, input, output);
    }

    private static boolean input(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.FLUID_INPUT || role == MultiblockLayout.Role.FLUID_IO
                || role == MultiblockLayout.Role.ITEM_FLUID_IO || role == MultiblockLayout.Role.ITEM_FLUID_INPUT
                || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }

    private static boolean output(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.FLUID_OUTPUT || role == MultiblockLayout.Role.FLUID_IO
                || role == MultiblockLayout.Role.ITEM_FLUID_IO || role == MultiblockLayout.Role.ITEM_FLUID_OUTPUT
                || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }
}
