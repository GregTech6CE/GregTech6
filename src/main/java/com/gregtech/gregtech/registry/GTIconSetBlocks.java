package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.IconSetBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;

/**
 * Registers placeable icon-set blocks; renderer-only textures are not blocks. Registry ids are the plain
 * icon name ({@code asphalt}, {@code planks_maple}, ...) — {@code ore_*} icons get a
 * {@code special_} prefix because the per-material {@code OreBlock}s own those ids.
 * Block properties follow the matching original GT6 block category.
 * <p>
 * Uses {@link GTBlocks#BLOCKS} and {@link GTBlocks#BLOCK_ITEMS} directly — there
 * must be only one DeferredRegister per registry to avoid ForgeRegistry.sync() conflicts.
 */
public final class GTIconSetBlocks {
    private static final List<RegistryObject<Block>> ALL = new ArrayList<>();
    public static RegistryObject<Block> LOGISTICS_WIRE;

    /**
     * IDs already registered by other registries ({@link GTWoods}, {@link GTDecorBlocks},
     * {@link GTMiscBlocks}) via {@link GTBlocks#BLOCKS}. GTIconSetBlocks must skip these
     * to avoid {@code Duplicate registration} errors from the single shared DeferredRegister.
     */
    private static final Set<String> SKIP_IDS = buildSkipIds();

    private static Set<String> buildSkipIds() {
        Set<String> skip = new HashSet<>();
        // GTDecorBlocks owns these
        skip.addAll(Set.of("asphalt", "cfoam_fresh", "concrete", "concrete_reinforced",
                "glass_clear", "fluid_spring",
                "mud", "turf", "clay_brown", "clay_red", "clay_yellow", "clay_blue", "clay_white"));
        // The original sprites below are used by the real gearbox and lantern
        // renderers. They are textures, not separate placeable blocks.
        skip.addAll(Set.of("gearbox_axle", "greg_o_lantern"));
        // GTMiscBlocks owns this
        skip.add("long_dist_wire");
        // GTWoods species — skip log/planks/leaves/beam/sapling for each
        for (String species : Set.of("rubber", "maple", "rainbowood", "willow",
                "pine", "blue_mahoe", "ebony", "white_mahoe",
                // GT6's remaining trees (hazel/cinnamon/coconut/blue spruce) became real species too
                "hazel", "cinnamon", "coconut", "bluespruce")) {
            for (String prefix : Set.of("log_", "planks_", "leaves_", "beam_", "sapling_")) {
                skip.add(prefix + species);
            }
        }
        return Collections.unmodifiableSet(skip);
    }

    private GTIconSetBlocks() {}

    public static List<RegistryObject<Block>> all() { return Collections.unmodifiableList(ALL); }

    public static void registerAll() {
        for (String iconName : ICON_NAMES) {
            String blockId = blockId(iconName);
            if (SKIP_IDS.contains(blockId)) continue;
            RegistryObject<Block> block = GTBlocks.BLOCKS.register(blockId, () -> createBlock(iconName));
            if (iconName.equals("logistics_wire")) LOGISTICS_WIRE = block;
            var sandSpec = com.gregtech.gregtech.block.BlackSandBlock.spec(iconName);
            if (sandSpec != null) GTBlocks.bind(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.blockDust,
                    com.gregtech.gregtech.api.material.GTMaterialRegistry.get(sandSpec.material()), block);
            RegistryObject<Item> item = GTBlocks.BLOCK_ITEMS.register(blockId,
                    () -> com.gregtech.gregtech.content.logistics.LongDistanceCatalog.find(blockId)!=null
                            ? new com.gregtech.gregtech.item.LongDistanceBlockItem(block.get(),new Item.Properties(),com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get(blockId))
                            : block.get() instanceof net.minecraft.world.level.block.WaterlilyBlock
                            ? new net.minecraft.world.item.PlaceOnWaterBlockItem(block.get(), new Item.Properties())
                            : new BlockItem(block.get(), new Item.Properties().stacksTo(64)));
            if (iconName.equals("planks_treated")) {
                // GT6 Loader_Woods:85-87: the treated plank block is also the
                // canonical OP.plate/WoodTreated, rather than a separate flat item.
                GTItems.bind(com.gregtech.gregtech.data.MaterialPrefix.plate,
                        com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated, item);
            }
            ALL.add(block);
        }
        // Composite blocks assembled from side/top texture pairs (GT6 BlockBaseLog /
        // BlockBaseBeam / BlockGrass / BlockBaleCrop etc. were single pillar blocks,
        // not one block per texture).
        for (String[] spec : COLUMN_BLOCKS) {
            String blockId = spec[0];
            if (SKIP_IDS.contains(blockId)) continue;
            String sideTex = spec[1];
            RegistryObject<Block> block = GTBlocks.BLOCKS.register(blockId, () ->
                    switch (blockId) {
                        case "log_dry" -> new com.gregtech.gregtech.block.wood.FallenLogBlock(
                                com.gregtech.gregtech.block.wood.FallenLogBlock.Kind.DRY,
                                propertiesFor(sideTex).strength(1.0F, 3.0F));
                        case "log_rotten" -> new com.gregtech.gregtech.block.wood.FallenLogBlock(
                                com.gregtech.gregtech.block.wood.FallenLogBlock.Kind.ROTTEN,
                                propertiesFor(sideTex).strength(1.0F, 3.0F));
                        case "log_mossy" -> new com.gregtech.gregtech.block.wood.FallenLogBlock(
                                com.gregtech.gregtech.block.wood.FallenLogBlock.Kind.MOSSY,
                                propertiesFor(sideTex).strength(1.0F, 3.0F));
                        case "log_frozen" -> new com.gregtech.gregtech.block.wood.FallenLogBlock(
                                com.gregtech.gregtech.block.wood.FallenLogBlock.Kind.FROZEN,
                                propertiesFor(sideTex).strength(1.0F, 3.0F));
                        default -> blockId.startsWith("bale_")
                                ? new com.gregtech.gregtech.block.plant.BaleBlock(
                                        switch (blockId) {
                                            case "bale_grass" -> com.gregtech.gregtech.block.plant.BaleBlock.Stage.FRESH;
                                            case "bale_grass_dry" -> com.gregtech.gregtech.block.plant.BaleBlock.Stage.DRY;
                                            case "bale_grass_moldy" -> com.gregtech.gregtech.block.plant.BaleBlock.Stage.MOLDY;
                                            case "bale_grass_rotten" -> com.gregtech.gregtech.block.plant.BaleBlock.Stage.ROTTEN;
                                            default -> com.gregtech.gregtech.block.plant.BaleBlock.Stage.CROP;
                                        }, BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GRASS))
                                : new net.minecraft.world.level.block.RotatedPillarBlock(propertiesFor(sideTex));
                    });
            GTBlocks.BLOCK_ITEMS.register(blockId, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(64)));
            ALL.add(block);
        }
        for (String[] spec : BOTTOM_TOP_BLOCKS) {
            String blockId = spec[0];
            if (SKIP_IDS.contains(blockId)) continue;
            String sideTex = spec[1];
            RegistryObject<Block> block = GTBlocks.BLOCKS.register(blockId, () -> {
                var properties = propertiesFor(sideTex);
                if (blockId.equals("grass") || blockId.startsWith("grassblock_"))
                    return new com.gregtech.gregtech.block.GTGrassBlock(
                            properties.isValidSpawn((state, level, pos, entity) -> false), sideTex);
                return new IconSetBlock(properties, sideTex);
            });
            GTBlocks.BLOCK_ITEMS.register(blockId, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(64)));
            ALL.add(block);
        }
    }

    /** {id, side texture, end/top texture} — pillar blocks (axis-rotatable). */
    public static final String[][] COLUMN_BLOCKS = buildColumnBlocks();

    /** {id, side texture, top texture, bottom texture} — grass-style blocks. */
    public static final String[][] BOTTOM_TOP_BLOCKS = buildBottomTopBlocks();

    private static String[][] buildColumnBlocks(){return com.gregtech.gregtech.content.plant.IconColumnCatalog.all();}

    private static String[][] buildBottomTopBlocks() {
        return com.gregtech.gregtech.block.GrassSoilCatalog.SPECS.stream().map(v->new String[]{v.id(),v.side(),v.top(),"minecraft:block/dirt"}).toArray(String[][]::new);
    }

    /** Registry id for an icon name; {@code ore_*} would collide with OreBlock ids. */
    public static String blockId(String iconName) {
        return iconName.startsWith("ore_") ? "block_" + iconName : iconName;
    }

    /** True for cross-rendered plants (vanilla bush rules, no collision). */
    public static boolean isPlant(String n) {
        return (n.startsWith("flower_") && !n.equals("flower_hexalily"))
                || n.startsWith("sapling_") || n.equals("fluid_spring");
    }

    /** Glowtus behaves like a vanilla lily pad (water-placed flat plant). */
    public static boolean isLily(String n) {
        return com.gregtech.gregtech.content.plant.IconPlantCatalog.lily(n);
    }

    private static Block createBlock(String iconName) {
        com.gregtech.gregtech.block.BlackSandBlock.Spec blackSand =
                com.gregtech.gregtech.block.BlackSandBlock.spec(iconName);
        if (blackSand != null) {
            return new com.gregtech.gregtech.block.BlackSandBlock(propertiesFor(iconName), blackSand);
        }
        com.gregtech.gregtech.block.RockOreBlock.Spec rockOre =
                com.gregtech.gregtech.block.RockOreBlock.spec(iconName);
        if (rockOre != null) {
            return new com.gregtech.gregtech.block.RockOreBlock(propertiesFor(iconName), iconName, rockOre);
        }
        com.gregtech.gregtech.block.VanillaOreBlock.Spec vanillaOre =
                com.gregtech.gregtech.block.VanillaOreBlock.spec(iconName);
        if (vanillaOre != null) {
            return new com.gregtech.gregtech.block.VanillaOreBlock(propertiesFor(iconName), iconName, vanillaOre);
        }
        if (iconName.startsWith("crystal_ore_")) {
            String slug = iconName.substring("crystal_ore_".length());
            String materialName = Character.toUpperCase(slug.charAt(0)) + slug.substring(1);
            return new com.gregtech.gregtech.block.CrystalOreBlock(
                    propertiesFor(iconName), iconName, materialName);
        }
        if (iconName.equals("logistics_wire")) {
            return new com.gregtech.gregtech.block.machine.LogisticsWireBlock(
                    BlockBehaviour.Properties.of().strength(1.0F, 2.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL));
        }
        if (iconName.equals("long_dist_pipe_item") || iconName.equals("long_dist_pipe_fluid")) {
            var kind = iconName.equals("long_dist_pipe_item")
                    ? com.gregtech.gregtech.block.misc.LongDistPipeBlock.Kind.ITEM_PIPE
                    : com.gregtech.gregtech.block.misc.LongDistPipeBlock.Kind.FLUID_PIPE;
            return new com.gregtech.gregtech.block.misc.LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get(iconName), longDistanceProperties(iconName));
        }
        if(iconName.startsWith("long_dist_wire_")) {
            long maximum=com.gregtech.gregtech.content.logistics.LongDistanceCatalog.voltage(iconName);
            return new com.gregtech.gregtech.block.misc.LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get(iconName),longDistanceProperties(iconName));
        }
        if (isLily(iconName)) {
            if (iconName.equals("flower_hexalily"))
                return new com.gregtech.gregtech.block.plant.BedrockHexalilyBlock(
                        BlockBehaviour.Properties.of().instabreak().sound(SoundType.LILY_PAD),
                        iconName);
            return new com.gregtech.gregtech.block.IconSetLilyBlock(
                    BlockBehaviour.Properties.of().instabreak().sound(SoundType.LILY_PAD)
                            .lightLevel(state -> iconName.startsWith("glowtus_") ? 15 : 0), iconName);
        }
        if (isPlant(iconName)) {
            var bedrockFlower = com.gregtech.gregtech.content.plant.BedrockFlowers.byId(iconName);
            if (bedrockFlower != null) {
                return new com.gregtech.gregtech.block.plant.BedrockFlowerBlock(
                        BlockBehaviour.Properties.of().instabreak().noCollission().sound(SoundType.GRASS),
                        iconName, bedrockFlower.desert());
            }
            return new com.gregtech.gregtech.block.IconSetPlantBlock(
                    BlockBehaviour.Properties.of().instabreak().noCollission().sound(SoundType.GRASS), iconName);
        }
        return new IconSetBlock(propertiesFor(iconName), iconName);
    }

    /**
     * GT6-equivalent block properties by icon category (original blocks:
     * BlockBaseWood / BlockConcrete / BlockAsphalt / BlockRail / machine casings etc.).
     */
    private static BlockBehaviour.Properties longDistanceProperties(String id) {
        var spec=com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get(id);
        return BlockBehaviour.Properties.of().strength(spec.hardness(),spec.resistance()).requiresCorrectToolForDrops()
                .sound(spec.kind().equals("WIRE")?SoundType.WOOL:SoundType.METAL);
    }
    private static BlockBehaviour.Properties propertiesFor(String n) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of();

        // Plants — instant break, grass sounds (GT6 BlockBaseFlower)
        if (n.startsWith("sapling_") || n.startsWith("flower_") || n.startsWith("glowtus_")
                || n.equals("fluid_spring")) {
            return p.instabreak().sound(SoundType.GRASS);
        }
        // Crop bales (GT6 BlockBaleCrop — hay-bale-like)
        if (n.startsWith("barley_") || n.startsWith("oat_") || n.startsWith("rice_") || n.startsWith("rye_")) {
            return p.strength(0.5F).sound(SoundType.GRASS);
        }
        // Leaves
        if (n.startsWith("leaves_")) {
            return p.strength(0.2F).sound(SoundType.GRASS);
        }
        // Wood family (GT6 BlockBaseWood: 2.0 hardness, flammable, axe)
        if (n.startsWith("planks_") || n.startsWith("beam_") || n.startsWith("log_")
                || n.equals("crate") || n.startsWith("bottlecrate_")) {
            return p.strength(2.0F, 3.0F).sound(SoundType.WOOD);
        }
        // Soils (GT6 turf/mud/grass paths)
        if (n.startsWith("grass_") || n.startsWith("grassblock_") || n.startsWith("path_")
                || n.equals("turf") || n.equals("mud")) {
            return p.strength(0.6F).sound(SoundType.GRAVEL);
        }
        if (n.startsWith("clay_")) {
            return p.strength(0.6F).sound(SoundType.GRAVEL);
        }
        if (n.startsWith("sand_")) {
            return p.strength(0.5F, 0.5F).requiresCorrectToolForDrops().sound(SoundType.SAND);
        }
        // Construction (GT6 BlockConcrete: very blast-resistant; reinforced even more)
        if (n.equals("concrete")) {
            return p.strength(5.0F, 60.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
        }
        if (n.equals("concrete_reinforced")) {
            return p.strength(25.0F, 300.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
        }
        if (n.equals("asphalt")) {
            return p.strength(1.5F, 15.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
        }
        if (n.startsWith("cfoam_fresh")) {
            return p.strength(0.3F).sound(SoundType.SNOW);
        }
        if (n.startsWith("cfoam_hardened")) {
            return p.strength(2.5F, 15.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
        }
        // GT6 BlockCrystalOres delegates hardness/resistance to glowstone and needs a pickaxe.
        if (n.startsWith("crystal_ore_")) {
            return p.strength(0.3F, 0.3F).requiresCorrectToolForDrops().sound(SoundType.GLASS);
        }
        com.gregtech.gregtech.block.RockOreBlock.Spec rockOre =
                com.gregtech.gregtech.block.RockOreBlock.spec(n);
        if (rockOre != null) {
            return p.strength(1.5F * rockOre.hardnessMultiplier(), 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE);
        }
        com.gregtech.gregtech.block.VanillaOreBlock.Spec vanillaOre =
                com.gregtech.gregtech.block.VanillaOreBlock.spec(n);
        if (vanillaOre != null) {
            return p.strength(1.5F * vanillaOre.hardnessMultiplier(), 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE);
        }
        // Special ore textures without a GT6 BlockRockOres mining variant.
        if (n.startsWith("ore_")) {
            return p.strength(3.0F, 5.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
        }
        // Glass
        if (n.startsWith("glass_")) {
            return p.strength(0.3F).sound(SoundType.GLASS);
        }
        // Pumpkin
        if (n.equals("greg_o_lantern")) {
            return p.strength(1.0F).sound(SoundType.WOOD);
        }
        // Machine-ish metal blocks (casings, gearboxes, axles, wires, insulation, ZPM, ...)
        return p.strength(3.5F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    private static final String[] ICON_NAMES = buildIconNames();

    private static String[] buildIconNames(){return com.gregtech.gregtech.content.plant.IconPlantCatalog.BLOCK_ICONS.toArray(String[]::new);}

    static {
        // Validation: every icon name must have a corresponding texture
        for (String name : ICON_NAMES) {
            if (name.isEmpty()) throw new IllegalStateException("Empty icon set name");
        }
    }
}
