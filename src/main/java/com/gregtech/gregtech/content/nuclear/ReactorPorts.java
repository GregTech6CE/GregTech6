package com.gregtech.gregtech.content.nuclear;

import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.block.machine.MachineRotationType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** GT6 independent primary (hot) and secondary (excess cold) fluid outlets. */
public final class ReactorPorts {
    public static final DirectionProperty HOT = DirectionProperty.create("hot_outlet");
    public static final DirectionProperty COLD = DirectionProperty.create("cold_outlet");
    private ReactorPorts() {}
    public static ToolInteractionSpec tool(ItemStack stack) {
        if (GTToolHelper.matchesTool(stack, GTToolType.MONKEY_WRENCH))
            return ToolInteractionSpec.facing(COLD, MachineRotationType.ALL);
        if (GTToolHelper.matchesTool(stack, GTToolType.WRENCH))
            return ToolInteractionSpec.facing(HOT, MachineRotationType.ALL);
        return null;
    }
    public static void saveItemState(ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getOrCreateTagElement("BlockStateTag");
        tag.putString(HOT.getName(), state.getValue(HOT).getSerializedName());
        tag.putString(COLD.getName(), state.getValue(COLD).getSerializedName());
    }
}
