package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.plant.BushBlock;
import gregtech.content.plant.BerryBushCatalog;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Independent food, cotton and material berry identities, plus the unplanted bush. */
public final class GTBushes {
    private GTBushes() {}

    private static final Map<String, RegisteredBlock<BushBlock>> VARIANTS = new LinkedHashMap<>();
    public static RegisteredBlock<BushBlock> BUSH;

    private static RegisteredBlock<BushBlock> register(ContentRegistrar registrar, String path, String berry) {
        var block = registrar.block(
                REGISTRY_NAMESPACE,
                path,
                () -> new BushBlock(
                        berry,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.PLANT)
                                .strength(.2f)
                                .sound(SoundType.GRASS)
                                .noOcclusion()
                                .instabreak()),
                bare -> new gregtech.content.item.BushBlockItem(bare, new Item.Properties()));
        VARIANTS.put(path, block);
        return block;
    }

    public static BushBlock byBerry(String berry) {
        var entry = VARIANTS.get(BerryBushCatalog.blockPath(berry));
        return entry == null ? null : entry.get();
    }

    public static Block[] allBlocks() {
        return VARIANTS.values().stream().map(e -> (Block) e.get()).toArray(Block[]::new);
    }

    public static void registerAll(ContentRegistrar registrar) {
        if (BUSH != null) return;
        BUSH = register(registrar, "bush", "");
        for (var type : BerryBushCatalog.worldgenTypes())
            register(registrar, BerryBushCatalog.blockPath(type.id()), type.id());
    }
}
