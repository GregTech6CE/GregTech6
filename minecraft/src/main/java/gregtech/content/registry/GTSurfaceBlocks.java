package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.DiggableBlock;
import gregtech.content.block.wood.FallenLogBlock;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTSurfaceBlocks {
    private GTSurfaceBlocks() {}

    private static void reg(ContentRegistrar registrar, String id, java.util.function.Supplier<Block> create) {
        registrar.block(REGISTRY_NAMESPACE, id, create, block -> new BlockItem(block, new Item.Properties()));
    }

    public static void initializeDiggables(ContentRegistrar registrar) {
        for (var kind : DiggableBlock.Variant.values()) reg(registrar, kind.iconName(), () -> new DiggableBlock(kind));
    }

    public static void initialize(ContentRegistrar registrar) {
        initializeDiggables(registrar);
        for (var kind : FallenLogBlock.Kind.values())
            reg(
                    registrar,
                    "log_" + kind.name().toLowerCase(java.util.Locale.ROOT),
                    () -> new FallenLogBlock(
                            kind,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .strength(1, 3)
                                    .sound(SoundType.WOOD)));
        GTBushes.registerAll(registrar);
    }
}
