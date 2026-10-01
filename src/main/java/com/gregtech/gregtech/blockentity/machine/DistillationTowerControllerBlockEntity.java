package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** GT6 17101: heat-only base, bottom item/fluid IO, upper fluid outputs. */
public class DistillationTowerControllerBlockEntity extends LargeRecipeMachineBlockEntity {
    public DistillationTowerControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    @Override protected long inputMaximum() { return 1024; }
    @Override protected List<LargeMachineLayouts.Cell> structureCells() {
        return Layout.CELLS;
    }
    private static final class Layout {
        static final List<LargeMachineLayouts.Cell> CELLS = ControllerStructureLayouts.cells(
                com.gregtech.gregtech.registry.GTMultiblocks.DISTILLATION_TOWER_MAIN.get()).keySet().stream()
            .map(p -> new LargeMachineLayouts.Cell(p.getX(),p.getY(),p.getZ(),p.getY()<0?18101:18102,
                p.getY()<0?Role.ENERGY_INPUT:p.getY()==0?Role.ITEM_FLUID_IO:Role.FLUID_OUTPUT)).toList();
    }
}
