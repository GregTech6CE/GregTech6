package gregtech.content.recipe;

import gregtech.api.material.GTValues;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Modern vanilla stone items retain their own rock material in GT6 machine processing. */
public final class VanillaStoneMachineRecipes {
    private static int added;
    private VanillaStoneMachineRecipes() {}

    public static int register() {
        added = 0;
        engrave();
        meltStone();
        for (String[] row : new String[][]{
                {"granite", "Granite", "stone_granite_cobble"},
                {"diorite", "Diorite", "stone_diorite_cobble"},
                {"andesite", "Andesite", "stone_andesite_cobble"},
                {"deepslate", "Deepslate", "minecraft:cobbled_deepslate"},
                {"tuff", "Tuff", ""}}) {
            rock(row[0], row[1], row[2]);
            if (row[0].equals("granite") || row[0].equals("diorite") || row[0].equals("andesite"))
                rock("polished_" + row[0], row[1], row[2]);
        }
        return added;
    }

    private static void engrave() {
        for (String name : new String[]{"Glass", "Zirconium", "Dilithium", "HexoriumWhite"}) {
            var material = GTMaterialRegistry.get(name);
            if (!material.isValid()) continue;
            var lens = GTItems.getStack(MaterialPrefix.lens, material, 1);
            if (lens.isEmpty()) continue;
            var recipe = new Recipe(new ItemStack[]{vanilla("stone"), lens},
                    new ItemStack[]{vanilla("chiseled_stone_bricks")}, null, null, null, null, 64, 16, 0)
                    .withCatalystInputs(1);
            add(MachineRecipeMaps.LaserEngraver, recipe);
        }
    }

    private static void meltStone() {
        for (String path : new String[]{"stone", "smooth_stone", "stone_slab", "smooth_stone_slab",
                "stone_stairs", "stone_button", "stone_pressure_plate", "cobblestone", "cobblestone_slab",
                "cobblestone_stairs", "cobblestone_wall", "mossy_cobblestone", "mossy_cobblestone_slab",
                "mossy_cobblestone_stairs", "mossy_cobblestone_wall", "stone_bricks", "stone_brick_slab",
                "stone_brick_stairs", "stone_brick_wall", "mossy_stone_bricks", "mossy_stone_brick_slab",
                "mossy_stone_brick_stairs", "mossy_stone_brick_wall", "cracked_stone_bricks", "chiseled_stone_bricks"}) {
            var input = vanilla(path);
            if (input.isEmpty()) continue;
            var data = ItemMaterialRegistry.base(input.getItem()).orElse(null);
            if (data == null || data.components().size() != 1 || !data.material().getName().equals("Stone")) continue;
            var stone = GTMaterialRegistry.get("Stone");
            var result = gregtech.engine.recipe.SourceRecyclingMath.singleLiquid(data.amount(), GTValues.U,
                    GTValues.U, 144, stone.getDensity(), stone.getMeltingPoint(), stone.getMeltingPoint(), 293);
            if (result.millibuckets() <= 0) continue;
            var output = GTFluids.stack("GenMolten_Stone", result.millibuckets());
            if (output == null || output.isEmpty()) continue;
            add(MachineRecipeMaps.Smelter, new Recipe(new ItemStack[]{input}, new ItemStack[0], null, null,
                    null, new FluidStack[]{output}, result.ticks(), 16, 0));
        }
    }

    private static void rock(String path, String materialName, String cobble) {
        ItemStack input = vanilla(path);
        var material = GTMaterialRegistry.get(materialName);
        if (input.isEmpty() || !material.isValid()) return;
        add(MachineRecipeMaps.Lathe, one(input, GTItems.getStack(MaterialPrefix.stickLong, material, 9), 16));
        var dustBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce",
                BlockMaterialPrefix.blockDust.getBlockId(material)));
        ItemStack dust = dustBlock == Blocks.AIR ? GTItems.getStack(MaterialPrefix.dust, material, 9) : new ItemStack(dustBlock);
        add(MachineRecipeMaps.Shredder, one(input, dust, 32));
        if (!cobble.isEmpty()) add(MachineRecipeMaps.Crusher, one(input, item(cobble), 32));
        for (String moldFamily : new String[]{"extruder_shape_", "low_heat_extruder_shape_"}) {
            for (String[] shape : new String[][]{{"plate", "plate", "9"}, {"curvedplate", "plateCurved", "9"},
                    {"rod", "stick", "18"}, {"longrod", "stickLong", "9"}, {"bolt", "bolt", "64"},
                    {"shovelhead", "toolHeadRawShovel", "9"}, {"swordblade", "toolHeadRawSword", "4"},
                    {"hoehead", "toolHeadRawHoe", "4"}, {"pickaxehead", "toolHeadRawPickaxe", "3"},
                    {"axehead", "toolHeadRawAxe", "3"}, {"gear", "gearGt", "2"},
                    {"smallgear", "gearGtSmall", "9"}, {"hammerhead", "toolHeadHammer", "1"}}) {
                var prefix = gregtech.engine.prefix.PrefixRegistry.byName(shape[1]);
                if (prefix != null) extrude(input, moldFamily + shape[0], GTItems.getStack(prefix, material, Integer.parseInt(shape[2])));
            }
            extrude(input, moldFamily + "block", vanilla(path.startsWith("polished_") ? path.substring(9) : path));
            String base = path.startsWith("polished_") ? path.substring(9) : path;
            String brick = switch (base) {
                case "granite", "diorite", "andesite" -> "stone_" + base + "_bricks";
                case "deepslate" -> "minecraft:deepslate_bricks";
                case "tuff" -> "minecraft:tuff_bricks";
                default -> "";
            };
            if (!brick.isEmpty()) extrude(input, moldFamily + "ingot", item(brick));
        }
    }

    private static void extrude(ItemStack input, String mold, ItemStack output) {
        var shape = GTTechnological.get(mold);
        if (shape == null || output.isEmpty()) return;
        add(MachineRecipeMaps.Extruder, new Recipe(new ItemStack[]{input.copy(), new ItemStack(shape)},
                new ItemStack[]{output}, null, null, null, null, 32, 16, 0).withCatalystInputs(1));
    }

    private static Recipe one(ItemStack input, ItemStack output, long ticks) {
        return new Recipe(new ItemStack[]{input.copy()}, new ItemStack[]{output}, null, null, null, null, ticks, 16, 0);
    }

    private static void add(RecipeMap map, Recipe recipe) {
        for (var input : recipe.mInputs) if (input.isEmpty()) return;
        for (var output : recipe.mOutputs) if (output.isEmpty()) return;
        if (map.addRecipe(recipe, true, false, false, false) != null) added++;
    }

    private static ItemStack vanilla(String path) { return item("minecraft:" + path); }
    private static ItemStack item(String path) {
        var id = path.contains(":") ? path : "gt6ce:" + path;
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
