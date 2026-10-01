package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6 {@code BlockStones:255-468}: what the machines make of every masonry variant of every rock type.
 *
 * <p>GT6 iterates {@code mEqualBlocks[<variant>]} — the blocks equal to one variant of one rock — and
 * gives each group its own hammer, crusher, shredder, smelting, {@code generify} and moss rows. The
 * port's equivalent of an equal-block set is the {@code _<variant>} block of a {@link StoneType}, the
 * same approximation {@code StoneAndToolSurvivalRecipes.registerStoneExtrusions()} uses for the
 * extruder rows (see §27 of the porting notes).</p>
 *
 * <table>
 *   <caption>GT6's group table</caption>
 *   <tr><th>group</th><th>hammer</th><th>crusher</th><th>shredder</th><th>smelting</th><th>generify</th></tr>
 *   <tr><td>STONE</td><td>cobble</td><td>cobble</td><td>9 dust</td><td>smooth</td><td>vanilla stone</td></tr>
 *   <tr><td>COBBLE / COBBLE_MOSSY</td><td>4 rocks (80%)</td><td>4 rocks</td><td>9 dust</td><td>stone</td><td>cobblestone / mossy</td></tr>
 *   <tr><td>BRICKS</td><td>cracked</td><td>cracked</td><td>9 dust</td><td>stone</td><td>stone bricks</td></tr>
 *   <tr><td>BRICKS_CRACKED / BRICKS_MOSSY</td><td>4 rocks (70%)</td><td>cobble</td><td>9 dust</td><td>stone</td><td>cracked / mossy bricks</td></tr>
 *   <tr><td>BRICKS_CHISELED / SMOOTH</td><td>cobble</td><td>cobble</td><td>9 dust</td><td>stone</td><td>chiseled bricks / smooth slab</td></tr>
 *   <tr><td>BRICKS_REINFORCED</td><td>4 rocks (70%)</td><td>cobble + iron dust</td><td>9 dust + iron dust</td><td>—</td><td>—</td></tr>
 *   <tr><td>BRICKS_REDSTONE</td><td>4 rocks (70%)</td><td>cracked + redstone</td><td>9 dust + redstone</td><td>—</td><td>—</td></tr>
 *   <tr><td>TILES…SQUARE_BRICKS</td><td>cracked</td><td>cracked</td><td>9 dust</td><td>stone</td><td>stone bricks</td></tr>
 * </table>
 *
 * <p>Two details worth keeping: the reinforced and redstone brick groups carry their extra material
 * into the crusher/shredder byproducts (and therefore are never smelted back to plain stone, GT6
 * {@code BlockStones:426-438}), and the sawing rows exist for every variant GT6 marks
 * {@code JUSTSTONE} — i.e. all but those same two (GT6 {@code BlockStones:271-274}).</p>
 */
public final class StoneVariantRecipes {

    private static final List<Recipe> ENTRIES = new ArrayList<>();
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static boolean registered;

    private StoneVariantRecipes() {}

    /** Every registered row, for tests and reports. */
    public static List<Recipe> entries() { return Collections.unmodifiableList(ENTRIES); }

    /** Rows per family ({@code hammer}, {@code crusher}, …), for the start-up log and tests. */
    public static Map<String, Integer> counts() { return Collections.unmodifiableMap(COUNTS); }

    /** Rows GT6 has but the port cannot express, with the reason. */
    public static List<String> skipped() { return Collections.unmodifiableList(SKIPPED); }

    public static int register() {
        if (registered) throw new IllegalStateException("Stone variant recipes registered twice");
        registered = true;
        for (StoneType type : StoneType.values()) {
            items(type);
            sawing(type);
        }
        // GT6 BlockStones:262 — one dust block smelts back into the plain stone block. The port has no
        // OP.blockDust block, so nine dusts stand in for it; where that input already smelts into the
        // material's metal (FurnaceSmeltingRecipes) the row is rejected and recorded instead.
        for (StoneType type : StoneType.values()) {
            ItemStack dust = GTItems.getStack(MaterialPrefix.dust, type.material(), 9);
            ItemStack stone = block(type, StoneVariant.STONE);
            if (!smelt("dust", dust, stone)) {
                SKIPPED.add(type.registryId() + ": 9 dust already smelt into the material's metal"
                        + " (the port has no blockDust block, so GT6's row shares its input)");
            }
        }
        report();
        return ENTRIES.size();
    }

    /** GT6 {@code BlockStones:260-468} — one pass over the 16 variants of one rock type. */
    private static void items(StoneType type) {
        int harvest = type.harvestLevel();
        ItemStack rock4 = GTItems.getStack(MaterialPrefix.rockGt, type.material(), 4);
        ItemStack dust9 = GTItems.getStack(MaterialPrefix.dust, type.material(), 9);
        for (StoneVariant variant : StoneVariant.values()) {
            ItemStack in = block(type, variant);
            if (in.isEmpty()) {
                SKIPPED.add(type.registryId() + " has no " + variant.registrySuffix() + " block");
                continue;
            }
            switch (variant) {
                // :276-285 — plain stone breaks to cobble, shatters into 9 dust and fires into smooth stone.
                case STONE -> {
                    hammer(in, block(type, StoneVariant.COBBLE), 0);
                    crusher(in, block(type, StoneVariant.COBBLE), 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.SMOOTH));
                    generify(in, "stone");
                }
                // :318-330 — cobblestone exposes the rock and grows moss.
                case COBBLE -> {
                    hammer(in, rock4, 8000);
                    crusher(in, rock4, 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.STONE));
                    generify(in, "cobblestone");
                }
                case COBBLE_MOSSY -> {
                    hammer(in, rock4, 8000);
                    crusher(in, rock4, 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.STONE));
                    generify(in, "mossy_cobblestone");
                    cleanmoss(in, block(type, StoneVariant.COBBLE));
                }
                // :372-385 — bricks crack under a hammer and take iron or redstone.
                case BRICKS -> {
                    hammer(in, block(type, StoneVariant.BRICKS_CRACKED), 0);
                    crusher(in, block(type, StoneVariant.BRICKS_CRACKED), 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.STONE));
                    generify(in, "stone_bricks");
                }
                case BRICKS_CRACKED -> {
                    hammer(in, rock4, 7000);
                    crusher(in, block(type, StoneVariant.COBBLE), 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.STONE));
                    generify(in, "cracked_stone_bricks");
                }
                case BRICKS_MOSSY -> {
                    hammer(in, rock4, 7000);
                    crusher(in, block(type, StoneVariant.COBBLE), 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.STONE));
                    generify(in, "mossy_stone_bricks");
                    cleanmoss(in, block(type, StoneVariant.BRICKS));
                }
                // :405-411 — chiselled and smooth masonry is brittle again.
                case BRICKS_CHISELED -> {
                    hammer(in, block(type, StoneVariant.COBBLE), 0);
                    crusher(in, block(type, StoneVariant.COBBLE), 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.STONE));
                    generify(in, "chiseled_stone_bricks");
                }
                case SMOOTH -> {
                    hammer(in, block(type, StoneVariant.COBBLE), 0);
                    crusher(in, block(type, StoneVariant.COBBLE), 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.STONE));
                    generify(in, "smooth_stone_slab");
                }
                // :427-438 — reinforced and redstone masonry carry their extra material through.
                case BRICKS_REINFORCED -> {
                    ItemStack iron = GTItems.getStack(MaterialPrefix.dustSmall, com.gregtech.gregtech.content.material.Materials.Iron, 2);
                    hammer(in, rock4, 7000);
                    crusher(in, block(type, StoneVariant.COBBLE), 64L + harvest * 64L, iron);
                    shred("shredder", in, dust9, 64L + harvest * 64L, iron);
                }
                case BRICKS_REDSTONE -> {
                    ItemStack redstone = new ItemStack(Items.REDSTONE);
                    hammer(in, rock4, 7000);
                    crusher(in, block(type, StoneVariant.BRICKS_CRACKED), 64L + harvest * 64L, redstone);
                    shred("shredder", in, dust9, 64L + harvest * 64L, redstone);
                }
                // :439-487 — the decorative families shatter like cracked bricks.
                default -> {
                    hammer(in, block(type, StoneVariant.BRICKS_CRACKED), 0);
                    crusher(in, block(type, StoneVariant.BRICKS_CRACKED), 16L + harvest * 16L, null);
                    shred("shredder", in, dust9, 16L + harvest * 16L, null);
                    smelt("smelt", in, block(type, StoneVariant.STONE));
                    generify(in, "stone_bricks");
                }
            }
        }
    }

    /** GT6 {@code BlockStones:271-274}: every {@code JUSTSTONE} slab saws into plates and small dust. */
    private static void sawing(StoneType type) {
        for (StoneVariant variant : StoneVariant.values()) {
            if (variant == StoneVariant.BRICKS_REINFORCED || variant == StoneVariant.BRICKS_REDSTONE) continue;
            ItemStack slab = slab(type, variant);
            if (slab.isEmpty()) {
                SKIPPED.add(type.registryId() + " has no " + variant.registrySuffix() + " slab");
                continue;
            }
            ItemStack plates = GTItems.getStack(MaterialPrefix.plate, type.material(), 4);
            ItemStack dust = GTItems.getStack(MaterialPrefix.dustSmall, type.material(), 2);
            saw(slab, plates, dust);
        }
        // :274 — the redstone brick slab keeps its redstone.
        ItemStack redstoneSlab = slab(type, StoneVariant.BRICKS_REDSTONE);
        if (!redstoneSlab.isEmpty()) {
            saw(redstoneSlab, GTItems.getStack(MaterialPrefix.plate, type.material(), 4),
                    GTItems.getStack(MaterialPrefix.dustSmall, type.material(), 2),
                    GTItems.getStack(MaterialPrefix.dustSmall, com.gregtech.gregtech.content.material.Materials.Redstone, 2));
        }
    }

    // ── rows ──────────────────────────────────────────────────────────────

    /** GT6 {@code RM.Hammer.addRecipe1(T, 16, 16[, chance], …)}. */
    private static void hammer(ItemStack in, ItemStack out, int chance) {
        if (in.isEmpty() || out.isEmpty() || out.getCount() <= 0) return;
        Recipe recipe = chance <= 0
                ? MachineRecipeMaps.Hammer.addRecipe1(true, 16, 16, in, out)
                : MachineRecipeMaps.Hammer.addRecipe1(true, 16, 16, new long[]{chance}, in, out);
        count("hammer", recipe);
    }

    /** GT6 {@code RM.Crusher.addRecipe1(T, 16, 16+mHarvestLevel*16, …)} with an optional byproduct. */
    private static void crusher(ItemStack in, ItemStack out, long ticks, ItemStack extra) {
        if (in.isEmpty() || out.isEmpty()) return;
        Recipe recipe = extra == null || extra.isEmpty()
                ? MachineRecipeMaps.Crusher.addRecipe1(true, 16, ticks, in, out)
                : MachineRecipeMaps.Crusher.addRecipe1(true, 16, ticks, in, out, extra);
        count("crusher", recipe);
    }

    /** GT6 {@code RM.Shredder.addRecipe1(…)}: the port's stand-in for GT6's {@code OP.blockDust} block. */
    private static void shred(String family, ItemStack in, ItemStack dust, long ticks, ItemStack extra) {
        if (in.isEmpty() || dust.isEmpty()) return;
        Recipe recipe = extra == null || extra.isEmpty()
                ? MachineRecipeMaps.Shredder.addRecipe1(true, 16, ticks, in, dust)
                : MachineRecipeMaps.Shredder.addRecipe1(true, 16, ticks, in, dust, extra);
        count(family, recipe);
    }

    /** GT6 {@code RM.add_smelting(aInput, aOutput, F, F, F)} — the Oven's table, mirrored to vanilla. */
    private static boolean smelt(String family, ItemStack in, ItemStack out) {
        if (in.isEmpty() || out.isEmpty()) return false;
        if (!MachineRecipeMaps.add_smelting(in, out, 0, false, false, true)) return false;
        COUNTS.merge(family, 1, Integer::sum);
        return true;
    }

    /** GT6 {@code RM.generify(block, vanillaBlock)} — lets GT machines treat the rock as the vanilla block. */
    private static void generify(ItemStack in, String vanillaPath) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(vanillaPath));
        if (item == null || item == Items.AIR) {
            SKIPPED.add("generify target missing: " + vanillaPath);
            return;
        }
        if (MachineRecipeMaps.generify(in, new ItemStack(item))) COUNTS.merge("generify", 1, Integer::sum);
    }

    /** GT6 {@code RM.cleanmoss(aClean, aMossy)} → {@code RM.pressurewash(aMossy, aClean)}. */
    private static void cleanmoss(ItemStack mossy, ItemStack clean) {
        if (mossy.isEmpty() || clean.isEmpty()) return;
        if (MachineRecipeMaps.pressurewash(mossy, clean)) COUNTS.merge("cleanmoss", 1, Integer::sum);
    }

    /** GT6 {@code RM.sawing(16, 16, F, 50, slab, …)} — one row per coolant in this port. */
    private static void saw(ItemStack in, ItemStack... outputs) {
        if (in.isEmpty() || outputs.length == 0) return;
        if (MachineRecipeMaps.sawing(16, 16, false, 50, in, outputs)) {
            COUNTS.merge("sawing", 1, Integer::sum);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static void count(String family, Recipe recipe) {
        if (recipe == null) return;
        ENTRIES.add(recipe);
        COUNTS.merge(family, 1, Integer::sum);
    }

    private static ItemStack block(StoneType type, StoneVariant variant) {
        Block block = GTBlocks.getStone(type, variant);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    private static ItemStack slab(StoneType type, StoneVariant variant) {
        Block block = GTBlocks.getStoneSlab(type, variant);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    private static void report() {
        com.gregtech.gregtech.GregTech.LOGGER.info("Registered {} GT6 masonry rows {} ({} skipped: {})",
                ENTRIES.size(), COUNTS, SKIPPED.size(), SKIPPED);
    }
}
