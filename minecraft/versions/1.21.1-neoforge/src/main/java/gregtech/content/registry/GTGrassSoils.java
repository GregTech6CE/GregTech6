package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class GTGrassSoils {
    private GTGrassSoils() {}

    public static void initialize(ContentRegistrar registrar) {
        for (var spec : GrassSoilCatalog.SPECS) {
            registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> {
                        var p = BlockBehaviour.Properties.of().strength(.6f).sound(SoundType.GRAVEL);
                        return spec.plantSoil()
                                ? new GTGrassBlock(p.isValidSpawn((state, level, pos, entity) -> false), spec.side())
                                : new IconSetBlock(p, spec.side());
                    },
                    GTBlocks::materialBlockItem);
        }
    }
}
