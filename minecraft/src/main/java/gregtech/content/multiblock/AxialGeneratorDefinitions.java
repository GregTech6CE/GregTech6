package gregtech.content.multiblock;

import gregtech.content.registry.GTMultiblocks;
import gregtech.content.registry.RegisteredBlock;
import java.util.List;
import net.minecraft.world.level.block.Block;

/** GT6 main housings 17211–17224. Steam quantities are litres, energy quantities are RU/EU. */
public final class AxialGeneratorDefinitions {

    private AxialGeneratorDefinitions() {}

    public static List<RegisteredBlock<Block>> blocks(boolean steam) {
        int firstOriginalId = steam ? 17211 : 17221;
        return java.util.stream.IntStream.range(firstOriginalId, firstOriginalId + 4)
                .mapToObj(GTMultiblocks::findPart)
                .toList();
    }

    public static Block[] registeredBlocks(boolean steam) {
        return blocks(steam).stream().map(RegisteredBlock::get).toArray(Block[]::new);
    }
}
