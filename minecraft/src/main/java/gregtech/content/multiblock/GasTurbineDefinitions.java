package gregtech.content.multiblock;

import gregtech.content.block.machine.LargeGasTurbineControllerBlock;
import gregtech.content.multiblock.OriginalGeneratorParameters.GasGrade;
import gregtech.content.registry.GTMultiblocks;
import gregtech.content.registry.RegisteredBlock;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.List;
import net.minecraft.world.level.block.Block;

/** GT6 Loader_MultiTileEntities 17231–17234. Values are normal HU/RU packet sizes. */
public final class GasTurbineDefinitions {
    private GasTurbineDefinitions() {}

    public static int tint(GasGrade grade) {
        return GTMaterialRegistry.get(grade.casingMaterial()).getColor();
    }

    public static List<RegisteredBlock<Block>> blocks() {
        return java.util.stream.IntStream.range(17231, 17235)
                .mapToObj(GTMultiblocks::findPart)
                .toList();
    }

    public static Block[] registeredBlocks() {
        return blocks().stream().map(RegisteredBlock::get).toArray(Block[]::new);
    }

    public static boolean isController(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof LargeGasTurbineControllerBlock;
    }
}
