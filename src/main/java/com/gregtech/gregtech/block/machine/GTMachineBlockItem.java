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

    @Override
    public Component getName(ItemStack stack) {
        Block block = getBlock();
        if (block instanceof FluidPipeBlock pipe) {
            return Component.literal(capitalize(pipe.spec().size().name()) + " " + spec.materialName() + " Fluid Pipe");
        }
        if (block instanceof ItemPipeBlock pipe) {
            String prefix = pipe.spec().size().restrictive() ? "Restrictive " : "";
            return Component.literal(prefix + capitalize(pipe.spec().size().name()) + " " + spec.materialName() + " Item Pipe");
        }
        if (block instanceof TankBlock) {
            return Component.literal(spec.materialName() + " Tank");
        }
        if (block instanceof SolidBurningBoxBlock) {
            return Component.literal(spec.materialName() + " Burning Box");
        }
        if (block instanceof SmeltingCrucibleBlock) {
            return Component.literal(spec.materialName() + " Crucible");
        }
        if (block instanceof MoldBlock) {
            return Component.literal(spec.materialName() + " Mold");
        }
        if (block instanceof MoldBasinBlock) {
            return Component.literal(spec.materialName() + " Mold Basin");
        }
        if (block instanceof CrucibleCrossingBlock) {
            return Component.literal(spec.materialName() + " Crucible Crossing");
        }
        if (block instanceof CrucibleFaucetBlock) {
            return Component.literal(spec.materialName() + " Crucible Faucet");
        }
        if (block instanceof HopperBlock) {
            return Component.literal(spec.materialName() + " Hopper");
        }
        if (block instanceof QueueHopperBlock) {
            return Component.literal(spec.materialName() + " Queue Hopper");
        }
        return super.getName(stack);
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (getBlock() instanceof BasicMachineBlock && basicSpec != null) {
            MachineTooltips.appendBasicMachine(basicSpec, tooltip);
        } else if (getBlock() instanceof SolidBurningBoxBlock) {
            MachineTooltips.appendSolidBurningBox(spec, tooltip);
        } else if (getBlock() instanceof TankBlock tank) {
            TankTooltips.appendTank(tank.spec(), tooltip);
        } else if (getBlock() instanceof FluidPipeBlock pipe) {
            TankTooltips.appendPipe(pipe.spec(), tooltip);
        } else if (getBlock() instanceof ItemPipeBlock pipe) {
            TankTooltips.appendItemPipe(pipe.spec(), tooltip);
        } else if (getBlock() instanceof EngineBlock engine) {
            EngineTooltips.appendEngine(engine, tooltip);
        }
    }
}
