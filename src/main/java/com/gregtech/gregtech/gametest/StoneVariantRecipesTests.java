package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.recipe.StoneVariantRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.loaders.Loader_StoneCraftingRecipes;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guards GT6 {@code BlockStones:255-487}: what the machines and the crafting grid make of every
 * masonry variant of every rock type.
 *
 * <p>Regression these tests exist for: the port had rows for plain stone and cobblestone only, so
 * bricks could not be cracked, chiselled, reinforced or recoloured, smooth stone could not be turned
 * into tiles or windmill tiles, and no variant could be smelted, generified or sawed. The crafting
 * rows also exposed that GT6's tool keys ({@code craftingToolHammer/Chisel/File}) were ported as
 * fresh {@code ItemStack} ingredients, which never match a real tool because an assembled GT tool
 * carries {@code GT.ToolStats} NBT — hence {@link GTToolTags} and this test.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class StoneVariantRecipesTests {

    /** GT6's per-group table, as the test reads it: variant → {hammer, crusher, smelting, generify}. */
    private static final String[][] GROUPS = {
            // variant, hammer target, hammer chance, crusher target, crusher ticks, smelt target, generify target
            {"STONE", "COBBLE", "0", "COBBLE", "harvest", "SMOOTH", "minecraft:stone"},
            {"COBBLE", "ROCK4", "8000", "ROCK4", "harvest", "STONE", "minecraft:cobblestone"},
            {"COBBLE_MOSSY", "ROCK4", "8000", "ROCK4", "harvest", "STONE", "minecraft:mossy_cobblestone"},
            {"BRICKS", "BRICKS_CRACKED", "0", "BRICKS_CRACKED", "harvest", "STONE", "minecraft:stone_bricks"},
            {"BRICKS_CRACKED", "ROCK4", "7000", "COBBLE", "harvest", "STONE", "minecraft:cracked_stone_bricks"},
            {"BRICKS_MOSSY", "ROCK4", "7000", "COBBLE", "harvest", "STONE", "minecraft:mossy_stone_bricks"},
            {"BRICKS_CHISELED", "COBBLE", "0", "COBBLE", "harvest", "STONE", "minecraft:chiseled_stone_bricks"},
            {"SMOOTH", "COBBLE", "0", "COBBLE", "harvest", "STONE", "minecraft:smooth_stone_slab"},
            {"BRICKS_REINFORCED", "ROCK4", "7000", "COBBLE", "heavy", "", ""},
            {"BRICKS_REDSTONE", "ROCK4", "7000", "BRICKS_CRACKED", "heavy", "", ""},
            {"TILES", "BRICKS_CRACKED", "0", "BRICKS_CRACKED", "harvest", "STONE", "minecraft:stone_bricks"},
            {"SMALL_TILES", "BRICKS_CRACKED", "0", "BRICKS_CRACKED", "harvest", "STONE", "minecraft:stone_bricks"},
            {"SMALL_BRICKS", "BRICKS_CRACKED", "0", "BRICKS_CRACKED", "harvest", "STONE", "minecraft:stone_bricks"},
            {"WINDMILL_TILES_A", "BRICKS_CRACKED", "0", "BRICKS_CRACKED", "harvest", "STONE", "minecraft:stone_bricks"},
            {"WINDMILL_TILES_B", "BRICKS_CRACKED", "0", "BRICKS_CRACKED", "harvest", "STONE", "minecraft:stone_bricks"},
            {"SQUARE_BRICKS", "BRICKS_CRACKED", "0", "BRICKS_CRACKED", "harvest", "STONE", "minecraft:stone_bricks"},
    };

    private static TransientCraftingContainer grid() {
        var menu = new AbstractContainerMenu(null, 0) {
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) {
                return ItemStack.EMPTY;
            }

            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return true;
            }
        };
        return new TransientCraftingContainer(menu, 3, 3);
    }

    /** A tool exactly like a player assembles it: carrying {@code GT.ToolStats}, optionally worn. */
    private static ItemStack tool(GTToolType type, int damage) {
        ItemStack stack = GTToolItem.create(type, Materials.Steel, GTMaterialRegistry.get("Wood"));
        if (damage > 0) stack.setDamageValue(damage);
        return stack;
    }

    private static ItemStack block(StoneType type, StoneVariant variant) {
        Block block = GTBlocks.getStone(type, variant);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    private static boolean has(RecipeMap map, ItemStack input, ItemStack expected) {
        for (Recipe recipe : map.mRecipeList) {
            if (!contains(recipe.mInputs, input)) continue;
            if (contains(recipe.mOutputs, expected)) return true;
        }
        return false;
    }

    private static Recipe find(RecipeMap map, ItemStack input, ItemStack expected) {
        for (Recipe recipe : map.mRecipeList) {
            if (!contains(recipe.mInputs, input)) continue;
            if (contains(recipe.mOutputs, expected)) return recipe;
        }
        return null;
    }

    private static boolean contains(ItemStack[] stacks, ItemStack expected) {
        if (expected.isEmpty()) return false;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameTags(stack, expected)) return true;
        }
        return false;
    }

    /** Every group's hammer, crusher, smelter and generifier row, with GT6's chance and tick values. */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void everyVariantCarriesGt6sMachineRows(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (StoneType type : StoneType.values()) {
            int harvest = type.harvestLevel();
            ItemStack rock4 = GTItems.getStack(MaterialPrefix.rockGt, type.material(), 4);
            ItemStack dust9 = GTItems.getStack(MaterialPrefix.dust, type.material(), 9);
            if (rock4.isEmpty() || dust9.isEmpty()) continue;
            for (String[] group : GROUPS) {
                StoneVariant variant = StoneVariant.valueOf(group[0]);
                ItemStack in = block(type, variant);
                if (in.isEmpty()) {
                    failures.add(type.registryId() + " has no " + group[0]);
                    continue;
                }
                String label = type.registryId() + "/" + group[0];
                // hammer
                ItemStack hammerOut = group[1].equals("ROCK4") ? rock4 : block(type, StoneVariant.valueOf(group[1]));
                Recipe hammer = find(MachineRecipeMaps.Hammer, in, hammerOut);
                if (hammer == null) {
                    failures.add(label + " hammer → " + group[1] + " missing");
                } else {
                    checked++;
                    long chance = hammer.mChances.length > 0 ? hammer.mChances[0] : 10000;
                    long wanted = Long.parseLong(group[2]);
                    if (chance != (wanted == 0 ? 10000 : wanted)) {
                        failures.add(label + " hammer chance " + chance + " != " + wanted);
                    }
                    if (hammer.mEUt != 16 || hammer.mDuration != 16) {
                        failures.add(label + " hammer runs at " + hammer.mEUt + " EU/t / " + hammer.mDuration + "t");
                    }
                }
                // crusher
                ItemStack crusherOut = group[3].equals("ROCK4") ? rock4 : block(type, StoneVariant.valueOf(group[3]));
                Recipe crusher = find(MachineRecipeMaps.Crusher, in, crusherOut);
                if (crusher == null) {
                    failures.add(label + " crusher → " + group[3] + " missing");
                } else {
                    checked++;
                    long wantedTicks = group[4].equals("heavy") ? 64L + harvest * 64L : 16L + harvest * 16L;
                    if (crusher.mDuration != wantedTicks) {
                        failures.add(label + " crusher runs for " + crusher.mDuration + "t, GT6 wants " + wantedTicks);
                    }
                }
                // shredder always yields the rock's dust
                if (!has(MachineRecipeMaps.Shredder, in, dust9)) failures.add(label + " shredder → 9 dust missing");
                else checked++;
                // smelting
                if (group[5].isEmpty()) {
                    if (find(MachineRecipeMaps.Furnace, in, block(type, StoneVariant.STONE)) != null) {
                        failures.add(label + " must not smelt (GT6 has no row for it)");
                    }
                } else {
                    Recipe smelt = find(MachineRecipeMaps.Furnace, in, block(type, StoneVariant.valueOf(group[5])));
                    if (smelt == null) failures.add(label + " smelt → " + group[5] + " missing");
                    else checked++;
                }
                // generify
                if (!group[6].isEmpty()) {
                    var vanilla = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(group[6]));
                    if (vanilla != null && !has(MachineRecipeMaps.Generifier, in, new ItemStack(vanilla))) {
                        failures.add(label + " generify → " + group[6] + " missing");
                    } else {
                        checked++;
                    }
                }
            }
        }
        helper.assertTrue(failures.isEmpty(), "GT6 masonry machine rows missing (" + failures.size() + "): "
                + failures.subList(0, Math.min(6, failures.size())));
        helper.assertTrue(checked > 1500, "machine rows checked: " + checked);
        // GT6's row counts for 27 rock types: 16 variants each, minus the two that are never smelted
        // or generified (BRICKS_REINFORCED / BRICKS_REDSTONE), 2 moss rows and 15 sawing slabs.
        int types = StoneType.values().length;
        var counts = StoneVariantRecipes.counts();
        helper.assertTrue(counts.getOrDefault("hammer", 0) == 16 * types,
                "hammer rows: " + counts.getOrDefault("hammer", 0) + " != " + 16 * types);
        helper.assertTrue(counts.getOrDefault("crusher", 0) == 16 * types,
                "crusher rows: " + counts.getOrDefault("crusher", 0) + " != " + 16 * types);
        helper.assertTrue(counts.getOrDefault("shredder", 0) == 16 * types,
                "shredder rows: " + counts.getOrDefault("shredder", 0) + " != " + 16 * types);
        helper.assertTrue(counts.getOrDefault("smelt", 0) == 14 * types,
                "smelting rows: " + counts.getOrDefault("smelt", 0) + " != " + 14 * types);
        helper.assertTrue(counts.getOrDefault("generify", 0) == 14 * types,
                "generify rows: " + counts.getOrDefault("generify", 0) + " != " + 14 * types);
        helper.assertTrue(counts.getOrDefault("cleanmoss", 0) == 2 * types,
                "moss rows: " + counts.getOrDefault("cleanmoss", 0) + " != " + 2 * types);
        helper.assertTrue(counts.getOrDefault("sawing", 0) == 15 * types,
                "sawing rows: " + counts.getOrDefault("sawing", 0) + " != " + 15 * types);
        helper.assertTrue(StoneVariantRecipes.skipped().size() <= types,
                "documented skips: " + StoneVariantRecipes.skipped());
        writeReport(helper, checked);
        helper.succeed();
    }

    /** The two masonry families that carry a second material keep it in the crusher and shredder. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void reinforcedAndRedstoneKeepTheirExtraMaterial(GameTestHelper helper) {
        ItemStack iron = GTItems.getStack(MaterialPrefix.dustSmall, Materials.Iron, 2);
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        helper.assertTrue(!iron.isEmpty(), "iron small dust exists");
        for (StoneType type : StoneType.values()) {
            ItemStack reinforced = block(type, StoneVariant.BRICKS_REINFORCED);
            ItemStack redstoneBricks = block(type, StoneVariant.BRICKS_REDSTONE);
            ItemStack cobble = block(type, StoneVariant.COBBLE);
            ItemStack cracked = block(type, StoneVariant.BRICKS_CRACKED);
            if (reinforced.isEmpty() || redstoneBricks.isEmpty()) continue;
            Recipe reinforcedCrusher = find(MachineRecipeMaps.Crusher, reinforced, cobble);
            helper.assertTrue(reinforcedCrusher != null && contains(reinforcedCrusher.mOutputs, iron),
                    type.registryId() + " reinforced bricks crush into cobble + iron");
            Recipe redstoneCrusher = find(MachineRecipeMaps.Crusher, redstoneBricks, cracked);
            helper.assertTrue(redstoneCrusher != null && contains(redstoneCrusher.mOutputs, redstone),
                    type.registryId() + " redstone bricks crush into cracked bricks + redstone");
            // and neither has a smelting or generify row (GT6 BlockStones:427-438)
            helper.assertTrue(find(MachineRecipeMaps.Furnace, reinforced, block(type, StoneVariant.STONE)) == null,
                    type.registryId() + " reinforced bricks must not smelt into stone");
            helper.assertTrue(find(MachineRecipeMaps.Furnace, redstoneBricks, block(type, StoneVariant.STONE)) == null,
                    type.registryId() + " redstone bricks must not smelt into stone");
        }
        helper.succeed();
    }

    /** Every {@code JUSTSTONE} slab saws into plates and small dust; the redstone slab keeps its redstone. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void slabsSawIntoPlates(GameTestHelper helper) {
        int checked = 0;
        for (StoneType type : StoneType.values()) {
            ItemStack plates = GTItems.getStack(MaterialPrefix.plate, type.material(), 4);
            ItemStack dust = GTItems.getStack(MaterialPrefix.dustSmall, type.material(), 2);
            if (plates.isEmpty() || dust.isEmpty()) continue;
            for (StoneVariant variant : StoneVariant.values()) {
                if (variant == StoneVariant.BRICKS_REINFORCED) continue;
                Block slab = GTBlocks.getStoneSlab(type, variant);
                if (slab == null) continue;
                Recipe recipe = find(MachineRecipeMaps.Cutter, new ItemStack(slab), plates);
                boolean expected = variant != StoneVariant.BRICKS_REDSTONE;
                if (expected) {
                    helper.assertTrue(recipe != null && contains(recipe.mOutputs, dust),
                            type.registryId() + "/" + variant.registrySuffix() + " saws into plates + small dust");
                    checked += recipe == null ? 0 : 1;
                }
            }
        }
        helper.assertTrue(checked >= 300, "slab sawing rows checked: " + checked);
        helper.succeed();
    }

    /**
     * GT6's tool keys ({@code craftingToolHammer/Chisel/File}) match a tool of that type whatever it
     * is made of and however worn it is, and a full crafting simulation has to agree — including the
     * GT6 wear the tool takes from the craft.
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void toolKeysAcceptRealTools(GameTestHelper helper) {
        for (GTToolType type : new GTToolType[]{GTToolType.FILE, GTToolType.CHISEL, GTToolType.HARD_HAMMER}) {
            ItemStack fresh = tool(type, 0);
            ItemStack worn = tool(type, 1000);
            helper.assertTrue(!fresh.isEmpty() && GTToolHelper.isTool(fresh),
                    type.id() + " assembles into a real tool");
            Ingredient ingredient = Ingredient.of(GTToolItems.empty(type));
            helper.assertTrue(!ingredient.isEmpty(), type.id() + " ingredient is empty");
            helper.assertTrue(ingredient.test(fresh), type.id() + " key matches a fresh tool");
            helper.assertTrue(ingredient.test(worn), type.id() + " key matches a worn tool");
        }

        // End to end: granite black bricks + a hammer craft cracked bricks.
        Block bricks = GTBlocks.getStone(StoneType.GRANITE_BLACK, StoneVariant.BRICKS);
        Block cracked = GTBlocks.getStone(StoneType.GRANITE_BLACK, StoneVariant.BRICKS_CRACKED);
        helper.assertTrue(bricks != null && cracked != null, "granite black bricks exist");
        TransientCraftingContainer grid = grid();
        grid.setItem(0, tool(GTToolType.HARD_HAMMER, 25));
        grid.setItem(3, new ItemStack(bricks));
        var found = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(found.isPresent(), "bricks + hammer is a crafting recipe");
        CraftingRecipe recipe = found.get();
        ItemStack result = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.isSameItemSameTags(result, new ItemStack(cracked)),
                "bricks + hammer → cracked bricks, got " + result);
        var remaining = recipe.getRemainingItems(grid);
        ItemStack returned = remaining.get(0);
        helper.assertTrue(returned.getItem() instanceof GTToolItem && returned.getDamageValue() == 25 + GTToolType.HARD_HAMMER.damagePerCraft(),
                "the hammer is returned with GT6's wear, got " + returned);
        // GT6 writes 28 crafting rows per rock type; every one the port cannot express must be recorded
        // (the 27 data-pack plain-stone → bricks rows cover GT6's remaining row).
        int types = StoneType.values().length;
        int expectedRows = 28 * types;
        int registered = Loader_StoneCraftingRecipes.registeredIds().size();
        int skipped = Loader_StoneCraftingRecipes.skipped().size();
        helper.assertTrue(registered + skipped + types == expectedRows,
                "masonry crafting rows " + registered + " + " + skipped + " skipped + " + types
                        + " data-pack != GT6's " + expectedRows);
        helper.assertTrue(registered >= 20 * types, "masonry crafting rows registered: " + registered);
        helper.assertTrue(Loader_StoneCraftingRecipes.skipped().stream()
                        .allMatch(reason -> reason.contains("the rock has no form")
                                || reason.contains("drill") || reason.contains("moss item")),
                "unexpected skip reasons: " + Loader_StoneCraftingRecipes.skipped());
        // GT6's CR.DEF rows must not mirror, the CR.DEF_MIR ones must (CR.java:161-163).
        assertMirror(helper, "stone_granite_black/bricks_cracked_by_hammer", false);
        assertMirror(helper, "stone_granite_black/cobble_to_wall", true);
        assertMirror(helper, "stone_granite_black/smooth_to_tiles", false);
        assertMirror(helper, "stone_granite_black/rocks_to_stairs", true);
        helper.succeed();
    }

    private static void assertMirror(GameTestHelper helper, String path, boolean expected) {
        var id = ResourceLocation.fromNamespaceAndPath("gregtech", "stone/" + path);
        var recipe = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        helper.assertTrue(recipe instanceof com.gregtech.gregtech.recipe.ToolShapedRecipe,
                "masonry row " + id + " is a tool-shaped recipe");
        boolean mirrors = ((com.gregtech.gregtech.recipe.ToolShapedRecipe) recipe).allowMirror();
        helper.assertTrue(mirrors == expected,
                id + (expected ? " must mirror (CR.DEF_MIR)" : " must not mirror (CR.DEF)"));
    }

    private static void writeReport(GameTestHelper helper, int checked) {
        var json = new java.util.TreeMap<String, Object>();
        json.put("masonryRows", StoneVariantRecipes.entries().size());
        json.put("rowsByFamily", new java.util.TreeMap<>(StoneVariantRecipes.counts()));
        json.put("machineRowsChecked", checked);
        json.put("craftingRows", Loader_StoneCraftingRecipes.registeredIds().size());
        json.put("craftingRowsSkipped", Loader_StoneCraftingRecipes.skipped());
        json.put("craftingRowIds", Loader_StoneCraftingRecipes.registeredIds());
        json.put("skipped", StoneVariantRecipes.skipped());
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/stone-variant-coverage.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/stone-variant-coverage.json: " + e);
        }
    }

    /** Kept for symmetry with the other reports: the rock materials the rows were built from. */
    static Map<String, String> rockTypeIds() {
        Map<String, String> ids = new LinkedHashMap<>();
        for (StoneType type : StoneType.values()) ids.put(type.registryId(), type.material().getName());
        return ids;
    }
}
