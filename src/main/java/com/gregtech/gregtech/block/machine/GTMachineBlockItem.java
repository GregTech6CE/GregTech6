package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.gregtech.gregtech.client.EngineTooltips;
import com.gregtech.gregtech.client.MachineTooltips;
import com.gregtech.gregtech.client.TankTooltips;

import javax.annotation.Nullable;
import java.util.List;

public class GTMachineBlockItem extends BlockItem {
    private final MachineSpec spec;
    @Nullable
    private final BasicMachineSpec basicSpec;

    public GTMachineBlockItem(Block block, Properties properties, MachineSpec spec) {
        this(block, properties, spec, null);
    }

    public GTMachineBlockItem(Block block, Properties properties, MachineSpec spec,
                              @Nullable BasicMachineSpec basicSpec) {
        super(block, properties);
        this.spec = spec;
        this.basicSpec = basicSpec;
    }

    public MachineSpec spec() { return spec; }
    @Nullable
    public BasicMachineSpec basicSpec() { return basicSpec; }

    @Override public int getMaxStackSize(ItemStack stack) {
        var tag = stack.getTagElement("BlockEntityTag");
        if (getBlock() instanceof TankBlock && tag != null && tag.getCompound("gt.tank").getLong("Amount") > 0) return 1;
        return super.getMaxStackSize(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (getBlock() instanceof BasicMachineBlock && basicSpec != null) {
            MachineTooltips.appendBasicMachine(basicSpec, tooltip);
        } else if (getBlock() instanceof SolidBurningBoxBlock) {
            MachineTooltips.appendSolidBurningBox(spec, tooltip);
        } else if (getBlock() instanceof TankBlock tank) {
            TankTooltips.appendTank(tank.spec(), stack, tooltip);
        } else if (getBlock() instanceof FluidPipeBlock pipe) {
            TankTooltips.appendPipe(pipe.spec(), tooltip);
        } else if (getBlock() instanceof ItemPipeBlock pipe) {
            TankTooltips.appendItemPipe(pipe.spec(), tooltip);
        } else if (getBlock() instanceof EngineBlock engine) {
            EngineTooltips.appendEngine(engine, tooltip);
        }
    }
}
