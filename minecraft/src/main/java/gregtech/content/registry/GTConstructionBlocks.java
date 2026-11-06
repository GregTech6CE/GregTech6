package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Retain palette registrations for saved worlds, then publish default flat color handles. */
public final class GTConstructionBlocks {
    private GTConstructionBlocks() {}

    private static final List<RegisteredBlock<? extends Block>> LEGACY = new ArrayList<>();
    public static RegisteredBlock<AsphaltBlock> ASPHALT;
    public static RegisteredBlock<CFoamSlabBlock> CFOAM_SLAB;
    public static RegisteredBlock<ConcreteBlock> CONCRETE, CONCRETE_REINFORCED;
    public static RegisteredBlock<CFoamBlock> CFOAM, CFOAM_FRESH;
    public static RegisteredBlock<ColoredGlassBlock> GLASS_CLEAR, GLASS_GLOW;

    public static List<RegisteredBlock<? extends Block>> all() {
        var result = new ArrayList<RegisteredBlock<? extends Block>>(LEGACY);
        result.addAll(GTConstructionVariants.all());
        return List.copyOf(result);
    }

    public static void initialize(ContentRegistrar registrar) {
        if (!LEGACY.isEmpty()) throw new IllegalStateException("Construction blocks registered twice");
        var clearSlab = registrar.block(
                REGISTRY_NAMESPACE,
                "glass_clear_slab",
                () -> new ColoredGlassSlabBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.NONE)
                        .strength(0.5f)
                        .sound(SoundType.GLASS)
                        .noOcclusion()),
                block -> new ColoredConstructionBlockItem(block, new Item.Properties()));
        item(clearSlab);
        var glowSlab = registrar.block(
                REGISTRY_NAMESPACE,
                "glass_glow_slab",
                () -> new ColoredGlassSlabBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.NONE)
                        .strength(0.5f)
                        .sound(SoundType.GLASS)
                        .noOcclusion()
                        .lightLevel(state -> 15)),
                block -> new ColoredConstructionBlockItem(block, new Item.Properties()));
        item(glowSlab);
        CFOAM_SLAB = registrar.block(
                REGISTRY_NAMESPACE,
                "cfoam_slab",
                () -> new CFoamSlabBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOL)
                        .strength(1, 1)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.WOOL)),
                block -> new ColoredConstructionBlockItem(block, new Item.Properties()));
        item(CFOAM_SLAB);
        ASPHALT = registrar.block(
                REGISTRY_NAMESPACE,
                "asphalt",
                () -> new AsphaltBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_BLACK)
                        .strength(3, 3)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE)),
                block -> new ColoredConstructionBlockItem(block, new Item.Properties()));
        item(ASPHALT);
        CONCRETE = concrete(registrar, "concrete", false, 1.5f, 20);
        CONCRETE_REINFORCED = concrete(registrar, "concrete_reinforced", true, 6, 80);
        CFOAM = registrar.block(
                REGISTRY_NAMESPACE,
                "cfoam",
                () -> new CFoamBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOL)
                        .strength(1, 1)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.WOOL)),
                block -> new ColoredConstructionBlockItem(block, new Item.Properties()));
        item(CFOAM);
        CFOAM_FRESH = registrar.block(
                REGISTRY_NAMESPACE,
                "cfoam_fresh",
                () -> new FreshCFoamBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOL)
                        .strength(.5f, .5f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.WOOL)),
                block -> new ColoredConstructionBlockItem(block, new Item.Properties()));
        item(CFOAM_FRESH);
        GLASS_CLEAR = glass(registrar, "glass_clear", false);
        GLASS_GLOW = glass(registrar, "glass_glow", true);

        GTConstructionVariants.initialize(registrar, List.copyOf(LEGACY));
        ASPHALT = GTConstructionVariants.defaultBlock("asphalt");
        CONCRETE = GTConstructionVariants.defaultBlock("concrete");
        CONCRETE_REINFORCED = GTConstructionVariants.defaultBlock("concrete_reinforced");
        CFOAM = GTConstructionVariants.defaultBlock("cfoam");
        CFOAM_FRESH = GTConstructionVariants.defaultBlock("cfoam_fresh");
        CFOAM_SLAB = GTConstructionVariants.defaultBlock("cfoam_slab");
        GLASS_CLEAR = GTConstructionVariants.defaultBlock("glass_clear");
        GLASS_GLOW = GTConstructionVariants.defaultBlock("glass_glow");
    }

    private static RegisteredBlock<ConcreteBlock> concrete(
            ContentRegistrar registrar, String id, boolean reinforced, float hardness, float resistance) {
        var block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new ConcreteBlock(
                        reinforced,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.STONE)
                                .strength(hardness, resistance)
                                .requiresCorrectToolForDrops()
                                .sound(SoundType.STONE)),
                bare -> new ConcreteBlockItem(bare, new Item.Properties()));
        LEGACY.add(block);
        return block;
    }

    private static RegisteredBlock<ColoredGlassBlock> glass(ContentRegistrar registrar, String id, boolean glow) {
        var block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new ColoredGlassBlock(
                        glow,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.NONE)
                                .strength(.75f, 5)
                                .sound(SoundType.GLASS)
                                .lightLevel(state -> glow ? 15 : 0)
                                .noOcclusion()
                                .isValidSpawn((s, l, p, e) -> false)),
                bare -> new ColoredGlassBlockItem(bare, new Item.Properties()));
        LEGACY.add(block);
        return block;
    }

    private static void item(RegisteredBlock<? extends Block> block) {
        LEGACY.add(block);
    }
}
