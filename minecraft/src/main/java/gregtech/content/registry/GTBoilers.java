package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.BoilerTankBlock;
import gregtech.engine.machine.BoilerSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * GT6 single-block Steam Boiler Tanks ({@code MultiTileEntityBoilerTank}):
 * 13 materials × {normal, strong}. The strong (dense-plate) variants carry the
 * high steam outputs. Values follow {@code Loader_MultiTileEntities} 1200-1262
 * ({@code NBT_OUTPUT_SU = tableValue * STEAM_PER_EU}, STEAM_PER_EU = 2).
 */
public final class GTBoilers {
    private static final List<RegisteredBlock<BoilerTankBlock>> ALL = new ArrayList<>();

    private GTBoilers() {}

    public static List<RegisteredBlock<BoilerTankBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(ContentRegistrar registrar, BoilerSpec spec) {
        RegisteredBlock<BoilerTankBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                spec.id(),
                () -> new BoilerTankBlock(
                        spec,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(spec.hardness(), spec.hardness())
                                .requiresCorrectToolForDrops()),
                bare -> new BlockItem(bare, new Item.Properties()));
        ALL.add(block);
    }

    public static void registerAll(ContentRegistrar registrar) {
        gregtech.content.energy.BoilerCatalog.all().forEach(spec -> add(registrar, spec));
    }
}
