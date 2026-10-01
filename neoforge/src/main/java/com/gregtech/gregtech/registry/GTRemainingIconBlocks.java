package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.IconSetBlock;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.neoforged.neoforge.registries.DeferredHolder;import net.neoforged.neoforge.registries.DeferredRegister;import java.util.*;
/** Complete remaining original icon identities. Existing typed registrations retain ownership. */
public final class GTRemainingIconBlocks {private GTRemainingIconBlocks(){}private static final List<DeferredHolder<Block,Block>> ALL=new ArrayList<>();public static List<DeferredHolder<Block,Block>> all(){return List.copyOf(ALL);}
private static void queued(Set<String> ids,DeferredRegister<Block> registry){for(var h:registry.getEntries())ids.add(h.getId().getPath());}
public static void initialize(){Set<String> seen=new HashSet<>(buildSkipIds());
 queued(seen,GTBlocks.BLOCKS);queued(seen,GTEnergyNodes.BLOCKS);queued(seen,GTAxles.BLOCKS);queued(seen,GTGearboxes.BLOCKS);queued(seen,GTToolBlocks.BLOCKS);queued(seen,GTItemPipes.BLOCKS);queued(seen,com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.BLOCKS);queued(seen,com.gregtech.gregtech.content.transport.HopperRegistries.BLOCKS);queued(seen,com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries.BLOCKS);queued(seen,com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries.BLOCKS);queued(seen,com.gregtech.gregtech.platform.neoforge.logistics.StorageRegistries.BLOCKS);seen.addAll(com.gregtech.gregtech.platform.neoforge.logistics.LogisticsRegistries.queuedBlockIds());
 for(String icon:com.gregtech.gregtech.content.plant.IconPlantCatalog.ICON_NAMES){String id=blockId(icon);if(seen.add(id))add(id,()->createBlock(icon));}
 for(var v:com.gregtech.gregtech.content.plant.IconColumnCatalog.all()){String id=v[0];if(!seen.add(id))continue;if(id.startsWith("bale_")){var stage=switch(id){case "bale_grass"->com.gregtech.gregtech.block.plant.BaleBlock.Stage.FRESH;case "bale_grass_dry"->com.gregtech.gregtech.block.plant.BaleBlock.Stage.DRY;case "bale_grass_moldy"->com.gregtech.gregtech.block.plant.BaleBlock.Stage.MOLDY;case "bale_grass_rotten"->com.gregtech.gregtech.block.plant.BaleBlock.Stage.ROTTEN;default->com.gregtech.gregtech.block.plant.BaleBlock.Stage.CROP;};add(id,()->new com.gregtech.gregtech.block.plant.BaleBlock(stage,BlockBehaviour.Properties.of().strength(.5f).sound(SoundType.GRASS)));}else add(id,()->new RotatedPillarBlock(propertiesFor(v[1])));}
 com.mojang.logging.LogUtils.getLogger().info("[gregtech] Remaining original icon/column blocks registered: {} ({})",ALL.size(),ALL.stream().map(h->h.getId().getPath()).toList());
}
private static void add(String id,java.util.function.Supplier<Block> factory){var b=GTBlocks.BLOCKS.register(id,factory);GTBlocks.BLOCK_ITEMS.register(id,()->new net.minecraft.world.item.BlockItem(b.get(),new net.minecraft.world.item.Item.Properties().stacksTo(64)));ALL.add(b);}
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
            return new com.gregtech.gregtech.block.misc.LongDistPipeBlock(kind, propertiesFor(iconName));
        }
        if(iconName.startsWith("long_dist_wire_")) {
            long maximum=com.gregtech.gregtech.content.logistics.LongDistanceCatalog.voltage(iconName);
            return new com.gregtech.gregtech.block.misc.LongDistPipeBlock(true,maximum,propertiesFor(iconName));
        }
        if (isLily(iconName)) {
            if (iconName.equals("flower_hexalily"))
                return new com.gregtech.gregtech.block.plant.BedrockHexalilyBlock(
                        BlockBehaviour.Properties.of().instabreak().noCollission().sound(SoundType.LILY_PAD),
                        iconName);
            return new com.gregtech.gregtech.block.IconSetLilyBlock(
                    BlockBehaviour.Properties.of().instabreak().noCollission().sound(SoundType.LILY_PAD)
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

}
