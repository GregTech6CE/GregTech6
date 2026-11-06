package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.BookShelfBlock;
import gregtech.content.book.BookShelfVariants;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTBookShelves {
    private GTBookShelves() {}

    private static final java.util.Map<String, RegisteredBlock<BookShelfBlock>> ALL = new java.util.LinkedHashMap<>();

    public static java.util.Collection<RegisteredBlock<BookShelfBlock>> all() {
        return java.util.List.copyOf(ALL.values());
    }

    public static BookShelfBlock get(String id) {
        return ALL.get(id).get();
    }

    public static void initialize(ContentRegistrar registrar) {
        for (var v : BookShelfVariants.all()) {
            var b = registrar.block(
                    REGISTRY_NAMESPACE,
                    v.path(),
                    () -> new BookShelfBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(v.metal() ? MapColor.METAL : MapColor.WOOD)
                                    .strength(v.hardness(), v.resistance())
                                    .requiresCorrectToolForDrops()
                                    .sound(v.metal() ? SoundType.METAL : SoundType.WOOD),
                            v),
                    block -> new gregtech.content.item.BookShelfBlockItem(block, new Item.Properties().stacksTo(16)));
            ALL.put(v.path(), b);
        }
    }
}
