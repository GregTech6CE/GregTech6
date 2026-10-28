package gregtech.content.multiblock;

import gregtech.content.block.machine.LargeGasTurbineControllerBlock;
import gregtech.content.registry.*;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.*;
import net.minecraft.world.level.block.Block;

/** GT6 Loader_MultiTileEntities 17231–17234. Values are normal HU/RU packet sizes. */
public final class GasTurbineDefinitions {
    public record Grade(
            String id, String rotor, String casingMaterial, int input, int output, int wallId, float hardness) {
        public int inputMaximum() {
            return input * 2;
        }

        public int outputMaximum() {
            return output * 2;
        }

        public int tint() {
            return GTMaterialRegistry.get(casingMaterial).getColor();
        }

        public Block wall() {
            return wallId == 18022 ? GTMultiblocks.TANK_WALL_DENSE.get() : LargeMachineParts.block(wallId);
        }

        public boolean accepts(Block block) {
            return block == wall();
        }
    }

    public static final List<Grade> GRADES = OriginalGeneratorParameters.GRADES.stream()
            .map(p -> new Grade(p.id(), p.rotor(), p.casingMaterial(), p.input(), p.output(), p.wallId(), p.hardness()))
            .toList();

    private GasTurbineDefinitions() {}

    public static List<RegisteredBlock<Block>> blocks() {
        return java.util.stream.IntStream.range(17231, 17235)
                .mapToObj(GTMultiblocks::findPart)
                .toList();
    }

    public static Block[] registeredBlocks() {
        return blocks().stream().map(RegisteredBlock::get).toArray(Block[]::new);
    }

    public static Grade grade(Block block) {
        return ((LargeGasTurbineControllerBlock) block).grade();
    }

    public static boolean isController(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof LargeGasTurbineControllerBlock;
    }
}
