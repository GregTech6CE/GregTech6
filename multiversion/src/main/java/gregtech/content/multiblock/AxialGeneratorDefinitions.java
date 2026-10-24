package gregtech.content.multiblock;

import gregtech.content.block.machine.*;
import gregtech.content.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import java.util.*;

/** GT6 main housings 17211–17224. Steam quantities are litres, energy quantities are RU/EU. */
public final class AxialGeneratorDefinitions {

    public record Grade(String id, GasTurbineDefinitions.Grade materials, boolean steam, int input, int output) {

        public int inputMaximum() {
            return input * 2;
        }

        public int outputMaximum() {
            return output * 2;
        }

        public Block wall() {
            return materials.wall();
        }

        public int tint() {
            return materials.tint();
        }

        public Map<BlockPos, Block> cells() {
            return LAYOUTS.computeIfAbsent(id, key -> createCells());
        }

        private Map<BlockPos, Block> createCells() {
            var cells = new LinkedHashMap<BlockPos, Block>();
            for (var cell : SharedTurbineStructure.LAYOUT.cells()) {
                var cellPos = new BlockPos(cell.right(), cell.up(), cell.back());
                cells.put(cellPos, OriginalGeneratorParameters.usesCopperCoil(steam, cell.back())
                        ? LargeMachineParts.block(18040) : wall());
            }
            return Collections.unmodifiableMap(cells);
        }

        public boolean accepts(BlockPos cell, Block block) {
            if (OriginalGeneratorParameters.usesCopperCoil(steam, cell.getZ())) return (block == LargeMachineParts.block(18040));
            return materials.accepts(block);
        }
    }
    public static final List<Grade> STEAM = grades(true), DYNAMO = grades(false);

    private static List<Grade> grades(boolean steam) {
        return (steam ? OriginalGeneratorParameters.STEAM : OriginalGeneratorParameters.DYNAMO).stream()
                .map(parameters -> new Grade(parameters.id(), GasTurbineDefinitions.GRADES.get(parameters.materialIndex()),
                        parameters.steam(), parameters.input(), parameters.output()))
                .toList();
    }

    private static final Map<String, Map<BlockPos, Block>> LAYOUTS = new java.util.concurrent.ConcurrentHashMap<>();

    private AxialGeneratorDefinitions() {
    }

    public static List<RegisteredBlock<Block>> blocks(boolean steam) {
        int firstOriginalId = steam ? 17211 : 17221;
        return java.util.stream.IntStream.range(firstOriginalId, firstOriginalId + 4).mapToObj(GTMultiblocks::findPart).toList();
    }

    public static Block[] registeredBlocks(boolean steam) {
        return blocks(steam).stream().map(RegisteredBlock::get).toArray(Block[]::new);
    }

    public static Grade grade(Block block) {
        return ((AxialGeneratorBlock) block).grade();
    }
}
