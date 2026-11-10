package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.inventory.LogisticsMassStorageBlock;
import gregtech.content.block.inventory.MassStorageBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** GT6's ordinary and logistics mass storages, one block per storage metal. */
public final class GTMassStorages {
    public static final List<RegisteredBlock<MassStorageBlock>> MASS_STORAGES = new ArrayList<>();
    public static final List<RegisteredBlock<LogisticsMassStorageBlock>> LOGISTICS_MASS_STORAGES = new ArrayList<>();
    public static RegisteredBlock<MassStorageBlock> MASS_STORAGE;

    private GTMassStorages() {}

    public static List<RegisteredBlock<? extends Block>> all() {
        var out = new ArrayList<RegisteredBlock<? extends Block>>();
        out.addAll(MASS_STORAGES);
        out.addAll(LOGISTICS_MASS_STORAGES);
        return out;
    }

    public static void initialize(ContentRegistrar registrar) {
        for (var spec : GTStorageMetals.ALL) {
            var storage = registrar.block(
                    REGISTRY_NAMESPACE,
                    "mass_storage_" + spec.suffix(),
                    () -> new MassStorageBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
            MASS_STORAGES.add(storage);
            if (spec.suffix().equals("stainless_steel")) MASS_STORAGE = storage;
        }
        for (var spec : GTStorageMetals.ALL) {
            var storage = registrar.block(
                    REGISTRY_NAMESPACE,
                    "logistics_mass_storage_" + spec.suffix(),
                    () -> new LogisticsMassStorageBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
            LOGISTICS_MASS_STORAGES.add(storage);
        }
    }
}
