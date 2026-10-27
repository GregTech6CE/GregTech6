package gregtech.content.material;

import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/** Explicit modern vanilla compositions, applied after the imported item-data table. */
final class VanillaMaterialCorrections {
    private static final long U = GTValues.U;
    private VanillaMaterialCorrections() {}

    static void register() {
        family("Stone", new String[]{"stone", "smooth_stone", "cobblestone", "mossy_cobblestone",
                "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks"}, 9 * U);
        family("Stone", new String[]{"stone_slab", "smooth_stone_slab", "cobblestone_slab",
                "mossy_cobblestone_slab", "stone_brick_slab", "mossy_stone_brick_slab"}, 9 * U / 2);
        family("Stone", new String[]{"stone_stairs", "cobblestone_stairs", "mossy_cobblestone_stairs",
                "stone_brick_stairs", "mossy_stone_brick_stairs"}, 27 * U / 4);
        family("Stone", new String[]{"cobblestone_wall", "mossy_cobblestone_wall", "stone_brick_wall",
                "mossy_stone_brick_wall"}, 9 * U);
        for (String[] rock : new String[][]{{"granite", "Granite"}, {"diorite", "Diorite"},
                {"andesite", "Andesite"}}) {
            family(rock[1], new String[]{rock[0], "polished_" + rock[0]}, 9 * U);
            family(rock[1], new String[]{rock[0] + "_slab", "polished_" + rock[0] + "_slab"}, 9 * U / 2);
            family(rock[1], new String[]{rock[0] + "_stairs", "polished_" + rock[0] + "_stairs"}, 27 * U / 4);
            family(rock[1], new String[]{rock[0] + "_wall"}, 9 * U);
        }
        stoneFamily("Deepslate", new String[]{"deepslate", "cobbled_deepslate", "polished_deepslate",
                "deepslate_bricks", "cracked_deepslate_bricks", "deepslate_tiles", "cracked_deepslate_tiles",
                "chiseled_deepslate"}, new String[]{"cobbled_deepslate", "polished_deepslate", "deepslate_brick", "deepslate_tile"});
        stoneFamily("Tuff", new String[]{"tuff", "polished_tuff", "tuff_bricks", "chiseled_tuff", "chiseled_tuff_bricks"},
                new String[]{"tuff", "polished_tuff", "tuff_brick"});
        stoneFamily("MudBrick", new String[]{"mud_bricks"}, new String[]{"mud_brick"});
        stoneFamily("RedNetherBrick", new String[]{"red_nether_bricks"}, new String[]{"red_nether_brick"});
        stoneFamily("Purpur", new String[]{"purpur_block", "purpur_pillar"}, new String[]{"purpur"});
        stoneFamily("PrismarineDark", new String[]{"dark_prismarine"}, new String[]{"dark_prismarine"});
        family("Sand", new String[]{"sandstone_stairs"}, 27 * U / 4);
        family("RedSand", new String[]{"red_sandstone_stairs"}, 27 * U / 4);
        family("Prismarine", new String[]{"prismarine_stairs", "prismarine_brick_stairs"}, 27 * U / 4);
        family("NetherQuartz", new String[]{"smooth_quartz", "quartz_stairs"}, 3 * U);
//? if >=1.21.1 {
        /*copper();
*///?}
    }

    private static void stoneFamily(String material, String[] blocks, String[] shapes) {
        family(material, blocks, 9 * U);
        for (String shape : shapes) {
            family(material, new String[]{shape + "_slab"}, 9 * U / 2);
            family(material, new String[]{shape + "_stairs"}, 27 * U / 4);
            family(material, new String[]{shape + "_wall"}, 9 * U);
        }
    }

    private static void family(String material, String[] ids, long amount) {
        for (String id : ids) bind(id, material, amount, false, false);
    }

    private static void copper() {
        for (String wax : new String[]{"", "waxed_"}) for (String oxidation : new String[]{"", "exposed_", "weathered_", "oxidized_"}) {
            boolean waxed = !wax.isEmpty();
            String base = wax + (oxidation.isEmpty() ? "copper_block" : oxidation + "copper");
            bind(base, "Copper", 9 * U, waxed, false);
            for (String block : new String[]{"chiseled_copper", "copper_grate", "cut_copper"})
                bind(wax + oxidation + block, "Copper", 9 * U / 4, waxed, false);
            bind(wax + oxidation + "cut_copper_stairs", "Copper", 3 * U / 2, waxed, false);
            bind(wax + oxidation + "cut_copper_slab", "Copper", 9 * U / 8, waxed, false);
            for (String block : new String[]{"copper_door", "copper_trapdoor"})
                bind(wax + oxidation + block, "Copper", 2 * U, waxed, false);
            bind(wax + oxidation + "copper_bulb", "Copper", 27 * U / 4, waxed, true);
        }
    }

    private static void bind(String path, String name, long amount, boolean waxed, boolean bulb) {
        var item = BuiltInRegistries.ITEM.get(path.contains(":") ? ResourceLocation.parse(path) : ResourceLocation.withDefaultNamespace(path));
        if (item == Items.AIR) return; // Some modern variants exist only on the newer platform.
        List<MaterialComponent> components = new ArrayList<>();
        components.add(MaterialComponent.of(GTMaterialRegistry.get(name), amount));
        if (bulb) {
            components.add(MaterialComponent.of(GTMaterialRegistry.get("Blaze"), U / 16)); // 1/8 blaze rod
            components.add(MaterialComponent.of(GTMaterialRegistry.get("Redstone"), U / 4));
        }
        if (waxed) components.add(MaterialComponent.of(GTMaterialRegistry.get("WaxBee"), U));
        ItemMaterialRegistry.register(item, new ItemComposition(null, components, "GT6CE modern vanilla materials", true));
    }
}
