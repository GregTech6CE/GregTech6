package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.*;
import gregtech.content.block.tool.BumbliaryBlock;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** One set of flattened wild hives and apiary factories for both loaders. */
public final class GTBumbleBlocks {
    private GTBumbleBlocks() {}

    private static final Map<DyeColor, RegisteredBlock<BumbleHiveBlock>> HIVES = new EnumMap<>(DyeColor.class);
    public static RegisteredBlock<BumbliaryBlock> BUMBLIARY, ADVANCED_BUMBLIARY;

    public static BumbleHiveBlock hive(DyeColor color) {
        return HIVES.get(color).get();
    }

    public static Block[] allHives() {
        return HIVES.values().stream().map(RegisteredBlock::get).toArray(Block[]::new);
    }

    public static void initializeHives(ContentRegistrar registrar) {
        if (!HIVES.isEmpty()) throw new IllegalStateException("Hives registered twice");
        for (var color : DyeColor.values())
            HIVES.put(
                    color,
                    registrar.block(
                            REGISTRY_NAMESPACE,
                            "bumble_hive_" + color.getName(),
                            () -> new FixedBumbleHiveBlock(BumbleHiveBlock.defaultProperties(), color)));
    }

    public static void initializeApiaries(ContentRegistrar registrar) {
        if (BUMBLIARY != null) return;
        BUMBLIARY = apiary(registrar, "bumbliary", false, MapColor.WOOD, 5);
        ADVANCED_BUMBLIARY = apiary(registrar, "advanced_bumbliary", true, MapColor.METAL, 6);
    }

    private static RegisteredBlock<BumbliaryBlock> apiary(
            ContentRegistrar registrar, String id, boolean advanced, MapColor color, float hardness) {
        return registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new BumbliaryBlock(
                        id,
                        advanced,
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .strength(hardness, hardness)
                                .sound(SoundType.WOOD)),
                block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
    }

    public static void initialize(ContentRegistrar registrar) {
        initializeHives(registrar);
        initializeApiaries(registrar);
    }
}
