package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.*;
import gregtech.content.logistics.ExtenderSpec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTRelaysFilters {
    private GTRelaysFilters() {}

    public static final java.util.List<RegisteredBlock<? extends Block>> EXTENDERS = new java.util.ArrayList<>();
    public static final java.util.List<RegisteredBlock<AutoHammerBlock>> HAMMERS = new java.util.ArrayList<>();
    public static final java.util.List<RegisteredBlock<AutoIgniterBlock>> IGNITERS = new java.util.ArrayList<>();
    public static final java.util.List<RegisteredBlock<FilterBlock>> FILTERS = new java.util.ArrayList<>();

    private static BlockBehaviour.Properties props(float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(hardness, hardness)
                .requiresCorrectToolForDrops();
    }

    public static void initialize(ContentRegistrar registrar) {
        for (var spec : gregtech.content.tool.AutomaticToolRules.ALL) {
            if (spec.igniter() != true) continue;
            var b = registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new AutoIgniterBlock(props(spec.hardness()), spec.input(), spec.quality()),
                    bare -> new BlockItem(bare, new Item.Properties()));
            IGNITERS.add(b);
        }
        for (var spec : gregtech.content.tool.AutomaticToolRules.ALL) {
            if (spec.igniter() != false) continue;
            var b = registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new AutoHammerBlock(props(spec.hardness()), spec.input(), spec.quality()),
                    bare -> new BlockItem(bare, new Item.Properties()));
            HAMMERS.add(b);
        }
        for (var spec : ExtenderSpec.values()) {
            var b = registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id,
                    () -> new SourceExtenderBlock(spec, props(6)),
                    bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
            EXTENDERS.add(b);
        }
        for (String kind : new String[] {"items", "fluids", "items_fluids", "oredict"}) {
            String id = "filter_" + kind;
            var b = registrar.block(
                    REGISTRY_NAMESPACE,
                    id,
                    () -> new FilterBlock(kind, props(6)),
                    bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
            FILTERS.add(b);
        }
    }
}
