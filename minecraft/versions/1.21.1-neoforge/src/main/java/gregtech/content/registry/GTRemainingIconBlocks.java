package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.IconSetBlock;
import java.util.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Complete remaining original icon identities. Existing typed registrations retain ownership. */
public final class GTRemainingIconBlocks {
    private GTRemainingIconBlocks() {}

    private static final List<RegisteredBlock<Block>> ALL = new ArrayList<>();

    public static List<RegisteredBlock<Block>> all() {
        return List.copyOf(ALL);
    }

    private static void queued(Set<String> ids, DeferredRegister<Block> registry) {
        for (var h : registry.getEntries()) ids.add(h.getId().getPath());
    }

    public static void initialize(ContentRegistrar registrar) {
        Set<String> seen = new HashSet<>(buildSkipIds());
        queued(seen, GTBlocks.BLOCKS);
        for (String icon : gregtech.content.plant.IconPlantCatalog.BLOCK_ICONS) {
            String id = blockId(icon);
            if (seen.add(id)) add(registrar, id, () -> createBlock(icon));
        }
        for (var v : gregtech.content.plant.IconColumnCatalog.all()) {
            String id = v[0];
            if (!seen.add(id)) continue;
            if (id.startsWith("bale_")) {
                var stage = switch (id) {
                    case "bale_grass" -> gregtech.content.block.plant.BaleBlock.Stage.FRESH;
                    case "bale_grass_dry" -> gregtech.content.block.plant.BaleBlock.Stage.DRY;
                    case "bale_grass_moldy" -> gregtech.content.block.plant.BaleBlock.Stage.MOLDY;
                    case "bale_grass_rotten" -> gregtech.content.block.plant.BaleBlock.Stage.ROTTEN;
                    default -> gregtech.content.block.plant.BaleBlock.Stage.CROP;
                };
                add(
                        registrar,
                        id,
                        () -> new gregtech.content.block.plant.BaleBlock(
                                stage,
                                BlockBehaviour.Properties.of().strength(.5f).sound(SoundType.GRASS)));
            } else if (java.util.Set.of("log_dry", "log_rotten", "log_mossy", "log_frozen")
                    .contains(id)) {
                var kind = gregtech.content.block.wood.FallenLogBlock.Kind.valueOf(
                        id.substring(4).toUpperCase(java.util.Locale.ROOT));
                add(
                        registrar,
                        id,
                        () -> new gregtech.content.block.wood.FallenLogBlock(
                                kind, propertiesFor(v[1]).strength(1f, 3f)));
            } else add(registrar, id, () -> new RotatedPillarBlock(propertiesFor(v[1])));
        }
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Remaining original icon/column blocks registered: {} ({})",
                        ALL.size(),
                        ALL.stream().map(h -> h.getId().getPath()).toList());
    }

    private static void add(ContentRegistrar registrar, String id, java.util.function.Supplier<Block> factory) {
        var b = registrar.block(
                REGISTRY_NAMESPACE, id, factory, bare -> new BlockItem(bare, new Item.Properties().stacksTo(64)));
        ALL.add(b);
    }

    private static Set<String> buildSkipIds() {
        Set<String> skip = new HashSet<>();
        // GTDecorBlocks owns these
        skip.addAll(Set.of(
                "asphalt",
                "cfoam_fresh",
                "concrete",
                "concrete_reinforced",
                "glass_clear",
                "fluid_spring",
                "mud",
                "turf",
                "clay_brown",
                "clay_red",
                "clay_yellow",
                "clay_blue",
                "clay_white"));
        // The original sprites below are used by the real gearbox and lantern
        // renderers. They are textures, not separate placeable blocks.
        skip.addAll(Set.of("gearbox_axle", "greg_o_lantern"));
        // GTLongDistance owns the whole catalog. The old single wire id is not in that catalog.
        skip.add("long_dist_wire");
        // GTLogisticsWire owns the logistics wire block and item.
        skip.add("logistics_wire");
        for (var spec : gregtech.content.logistics.LongDistanceCatalog.ALL) skip.add(spec.id());
        // GTWoods species — skip log/planks/leaves/sapling for each
        for (String species : Set.of(
                "rubber",
                "maple",
                "rainbowood",
                "willow",
                "blue_mahoe",
                // GT6's remaining trees (hazel/cinnamon/coconut/blue spruce) became real species too
                "hazel",
                "cinnamon",
                "coconut",
                "bluespruce")) {
            for (String prefix : Set.of("log_", "planks_", "leaves_", "sapling_")) {
                skip.add(prefix + species);
            }
        }
        return Collections.unmodifiableSet(skip);
    }

    /** Registry id for an icon name; {@code ore_*} would collide with OreBlock ids. */
    public static String blockId(String iconName) {
        return iconName.startsWith("ore_") ? "block_" + iconName : iconName;
    }

    /** True for cross-rendered plants (vanilla bush rules, no collision). */
    public static boolean isPlant(String n) {
        return (n.startsWith("flower_") && !n.equals("flower_hexalily"))
                || n.startsWith("sapling_")
                || n.equals("fluid_spring");
    }

    /** Glowtus behaves like a vanilla lily pad (water-placed flat plant). */
    public static boolean isLily(String n) {
        return gregtech.content.plant.IconPlantCatalog.lily(n);
    }

    private static Block createBlock(String iconName) {
        gregtech.content.block.BlackSandBlock.Spec blackSand = gregtech.content.block.BlackSandBlock.spec(iconName);
        if (blackSand != null) {
            return new gregtech.content.block.BlackSandBlock(propertiesFor(iconName), blackSand);
        }
        gregtech.content.block.RockOreBlock.Spec rockOre = gregtech.content.block.RockOreBlock.spec(iconName);
        if (rockOre != null) {
            return new gregtech.content.block.RockOreBlock(propertiesFor(iconName), iconName, rockOre);
        }
        gregtech.content.block.VanillaOreBlock.Spec vanillaOre = gregtech.content.block.VanillaOreBlock.spec(iconName);
        if (vanillaOre != null) {
            return new gregtech.content.block.VanillaOreBlock(propertiesFor(iconName), iconName, vanillaOre);
        }
        if (iconName.startsWith("crystal_ore_")) {
            String slug = iconName.substring("crystal_ore_".length());
            String materialName = Character.toUpperCase(slug.charAt(0)) + slug.substring(1);
            return new gregtech.content.block.CrystalOreBlock(propertiesFor(iconName), iconName, materialName);
        }
        if (iconName.equals("long_dist_pipe_item") || iconName.equals("long_dist_pipe_fluid")) {
            var kind = iconName.equals("long_dist_pipe_item")
                    ? gregtech.content.block.misc.LongDistPipeBlock.Kind.ITEM_PIPE
                    : gregtech.content.block.misc.LongDistPipeBlock.Kind.FLUID_PIPE;
            return new gregtech.content.block.misc.LongDistPipeBlock(
                    gregtech.content.logistics.LongDistanceCatalog.get(iconName), longDistanceProperties(iconName));
        }
        if (iconName.startsWith("long_dist_wire_")) {
            long maximum = gregtech.content.logistics.LongDistanceCatalog.voltage(iconName);
            return new gregtech.content.block.misc.LongDistPipeBlock(
                    gregtech.content.logistics.LongDistanceCatalog.get(iconName), longDistanceProperties(iconName));
        }
        if (isLily(iconName)) {
            if (iconName.equals("flower_hexalily"))
                return new gregtech.content.block.plant.BedrockHexalilyBlock(
                        BlockBehaviour.Properties.of()
                                .instabreak()
                                .noCollission()
                                .sound(SoundType.LILY_PAD),
                        iconName);
            return new gregtech.content.block.IconSetLilyBlock(
                    BlockBehaviour.Properties.of()
                            .instabreak()
                            .noCollission()
                            .sound(SoundType.LILY_PAD)
                            .lightLevel(state -> iconName.startsWith("glowtus_") ? 15 : 0),
                    iconName);
        }
        if (isPlant(iconName)) {
            var bedrockFlower = gregtech.content.plant.BedrockFlowers.byId(iconName);
            if (bedrockFlower != null) {
                return new gregtech.content.block.plant.BedrockFlowerBlock(
                        BlockBehaviour.Properties.of()
                                .instabreak()
                                .noCollission()
                                .sound(SoundType.GRASS),
                        iconName,
                        bedrockFlower.desert());
            }
            return new gregtech.content.block.IconSetPlantBlock(
                    BlockBehaviour.Properties.of().instabreak().noCollission().sound(SoundType.GRASS), iconName);
        }
        if (iconName.startsWith("leaves_"))
            return new gregtech.content.block.wood.DecorativeLeavesBlock(propertiesFor(iconName));
        return new IconSetBlock(propertiesFor(iconName), iconName);
    }

    /**
     * GT6-equivalent block properties by icon category (original blocks:
     * BlockBaseWood / BlockConcrete / BlockAsphalt / BlockRail / machine casings etc.).
     */
    private static BlockBehaviour.Properties longDistanceProperties(String id) {
        var spec = gregtech.content.logistics.LongDistanceCatalog.get(id);
        return BlockBehaviour.Properties.of()
                .strength(spec.hardness(), spec.resistance())
                .requiresCorrectToolForDrops()
                .sound(spec.kind().equals("WIRE") ? SoundType.WOOL : SoundType.METAL);
    }

    private static BlockBehaviour.Properties propertiesFor(String n) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of();

        // Plants — instant break, grass sounds (GT6 BlockBaseFlower)
        if (n.startsWith("sapling_")
                || n.startsWith("flower_")
                || n.startsWith("glowtus_")
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
        if (n.startsWith("planks_")
                || n.startsWith("beam_")
                || n.startsWith("log_")
                || n.equals("crate")
                || n.startsWith("bottlecrate_")) {
            return p.strength(2.0F, 3.0F).sound(SoundType.WOOD);
        }
        // Soils (GT6 turf/mud/grass paths)
        if (n.startsWith("grass_")
                || n.startsWith("grassblock_")
                || n.startsWith("path_")
                || n.equals("turf")
                || n.equals("mud")) {
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
        gregtech.content.block.RockOreBlock.Spec rockOre = gregtech.content.block.RockOreBlock.spec(n);
        if (rockOre != null) {
            return p.strength(1.5F * rockOre.hardnessMultiplier(), 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE);
        }
        gregtech.content.block.VanillaOreBlock.Spec vanillaOre = gregtech.content.block.VanillaOreBlock.spec(n);
        if (vanillaOre != null) {
            return p.strength(1.5F * vanillaOre.hardnessMultiplier(), 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE);
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
}
