package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.MaterialWorkability;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * GT6's bulk material form conversions — dust &harr; dust block, small/tiny dust piles &rarr; dust,
 * ingot &harr; ingot block, plate &rarr; dense plate, and friends.
 * <p>
 * GT6 registers these generically per material instead of listing them one by one:
 * </p>
 * <ul>
 *   <li>{@code Loader_Recipes_Handlers} — {@code RM.Compressor} handlers
 *       ({@code RecipeMapHandlerPrefix}), e.g. {@code plate 9 -> plateDense 1},
 *       {@code dust 1 -> plateGem 1}, {@code blockPlate 1 -> plateDense 1}.</li>
 *   <li>{@code Loader_Recipes_Handlers} — {@code RM.Unboxinator} handlers,
 *       e.g. {@code blockIngot 1 -> ingot 9}, {@code ingot 1 -> nugget 9},
 *       {@code dust 1 -> dustTiny 9}, {@code dustSmall 1 -> dustDiv72 18},
 *       {@code toolHeadPickaxeGem 1 -> gemFlawed 2 + empty head}.</li>
 *   <li>{@code Loader_Recipes_Handlers} — {@code RM.Boxinator} handlers; the packing
 *       direction needs a circuit ({@code ST.tag(n)}, port: {@link GTTechnological#selectorTag(int)})
 *       so the machine knows which compression is meant, exactly like GT6.</li>
 * </ul>
 *
 * <p>One source condition still has no port equivalent: {@code EXPLODES_IN_NONVANILLA_CRAFTING_GRID}
 * (on the block, nugget and crushed rows) is not linked to any material property, so the port applies
 * those rows to every non-antimatter material. No built-in material carries that property, so the
 * registered set matches; an addon that does would gain rows the original excludes.</p>
 * <p>
 * Duration/EU rules are GT6's: every handler runs at 16 EU/t. {@code RecipeMapHandlerPrefix} uses a
 * fixed duration when the handler passes one (the packing/unpacking handlers all pass 16 ticks) and
 * otherwise {@code getCosts()}, which is {@code ceil(units / U * multiplier * (1 + toolQuality))}.
 * GT6 gates the two compressor passes on {@code tEasyWorkable = FURNACE || SOFT}
 * ({@code Loader_Recipes_Handlers}): easy materials compress in a flat 16/144 ticks, everything
 * else pays the quality-scaled multiplier 256. The port has no such material property, so the flags
 * are imported by {@code tools/extract_gt6_workability.py} into {@link MaterialWorkability}.
 * <p>
 * Pairs GT6 has but the port cannot express yet are recorded in {@link #skipped()} instead of being
 * guessed: the port has no {@code compressed} or {@code plateSteamcraft} prefix. GT6's
 * {@code casingSmall} is the port's {@link MaterialPrefix#itemCasing}, so the
 * {@code plateTiny}/{@code plateGemTiny} boxinator rows are registered under that identity.
 */
public final class MaterialFormConversionRecipes {
    /** One registered conversion: the GT6 handler family, its GT6 prefix pair, and the recipe. */
    public record Entry(String kind, String route, RecipeMap map, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static boolean registered;

    private MaterialFormConversionRecipes() {}

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<String> skipped() {
        return Collections.unmodifiableList(SKIPPED);
    }

    public static int register() {
        if (registered) throw new IllegalStateException("Material form conversions registered twice");
        registered = true;
        skippedOnce("compressor", "ingot 1 -> compressed 1", "MaterialPrefix.compressed is missing");
        skippedOnce("compressor", "billet 1 -> plateSteamcraft 1", "MaterialPrefix.plateSteamcraft is missing");
        skippedOnce("compressor", "compressed 9 -> plateDense 1", "MaterialPrefix.compressed is missing");

        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue; // GT6: ANTIMATTER.NOT on all of these handlers
            boolean easy = MaterialWorkability.isEasyWorkable(material);
            int quality = material.getToolQuality();
            compressor(material, easy, quality);
            gemPickaxeHead(material);
            unboxinator(material);
            boxinator(material);
        }
        Mc.logger()
                .info(
                        "Registered {} GT6 material form conversions ({} GT6 pairs skipped: {})",
                        ENTRIES.size(),
                        SKIPPED.size(),
                        SKIPPED);
        return ENTRIES.size();
    }

    /** GT6's compressor rows, dust to plate or gem ({@code Loader_Recipes_Handlers}). */
    private static void compressor(GTMaterial material, boolean easy, int quality) {
        // dust -> plateGem. GT6 excludes materials that already have the fancy gem forms (and Ice).
        boolean gemLike = has(MaterialPrefix.gemLegendary, material)
                || has(MaterialPrefix.gemExquisite, material)
                || has(MaterialPrefix.gemFlawless, material)
                || has(MaterialPrefix.bouleGt, material);
        if (!gemLike && !"Ice".equals(material.getName())) {
            compressing(material, easy ? 16 : 256L * (quality + 1), MaterialPrefix.dust, 1, MaterialPrefix.plateGem, 1);
        }
        // 9 material units of plate -> 1 dense plate. plate 9, plateTriple 3, blockPlate 1 and
        // blockSolid 1 are all 9U, which is why GT6 lists them as four routes to the same result.
        long denseTicks = easy ? 144 : 9L * 256 * (quality + 1);
        compressing(material, denseTicks, MaterialPrefix.plate, 9, MaterialPrefix.plateDense, 1);
        compressing(material, denseTicks, MaterialPrefix.plateTriple, 3, MaterialPrefix.plateDense, 1);
        compressingBlock(material, denseTicks, BlockMaterialPrefix.blockPlate, 1, MaterialPrefix.plateDense, 1);
        compressingBlock(material, denseTicks, BlockMaterialPrefix.blockSolid, 1, MaterialPrefix.plateDense, 1);
    }

    /** GT6 {@code Loader_Recipes_Handlers}: the gem pickaxe head is a cast that returns two flawed gems. */
    private static void gemPickaxeHead(GTMaterial material) {
        ItemStack input = GTItems.getStack(MaterialPrefix.toolHeadPickaxeGem, material, 1);
        ItemStack gems = GTItems.getStack(MaterialPrefix.gemFlawed, material, 2);
        ItemStack empty = GTItems.getStack(MaterialPrefix.toolHeadPickaxeGem, Materials.Empty, 1);
        if (input.isEmpty() || gems.isEmpty() || empty.isEmpty()) return;
        unit(
                "unboxinator",
                "toolHeadPickaxeGem 1 -> gemFlawed 2 + empty head",
                MachineRecipeMaps.Unboxinator,
                16,
                material,
                input,
                MaterialPrefix.toolHeadPickaxeGem.getMaterialWeight(),
                null,
                gems,
                empty);
    }

    /** GT6 {@code Loader_Recipes_Handlers}: 1 block / big form -> 9 (or 6/18/8) smaller forms. */
    private static void unboxinator(GTMaterial material) {
        unbox("blockRaw 1 -> oreRaw 9", BlockMaterialPrefix.blockRaw, 1, MaterialPrefix.oreRaw, 9, material);
        unbox("blockDust 1 -> dust 9", BlockMaterialPrefix.blockDust, 1, MaterialPrefix.dust, 9, material);
        unbox("blockGem 1 -> gem 9", BlockMaterialPrefix.blockGem, 1, MaterialPrefix.gem, 9, material);
        unbox("blockIngot 1 -> ingot 9", BlockMaterialPrefix.blockIngot, 1, MaterialPrefix.ingot, 9, material);
        unbox("blockPlate 1 -> plate 9", BlockMaterialPrefix.blockPlate, 1, MaterialPrefix.plate, 9, material);
        unbox(
                "blockPlateGem 1 -> plateGem 9",
                BlockMaterialPrefix.blockPlateGem,
                1,
                MaterialPrefix.plateGem,
                9,
                material);

        unpack("crushed 1 -> crushedTiny 9", MaterialPrefix.crushed, 1, MaterialPrefix.crushedTiny, 9, material);
        unpack(
                "crushedPurified 1 -> crushedPurifiedTiny 9",
                MaterialPrefix.crushedPurified,
                1,
                MaterialPrefix.crushedPurifiedTiny,
                9,
                material);
        unpack(
                "crushedCentrifuged 1 -> crushedCentrifugedTiny 9",
                MaterialPrefix.crushedCentrifuged,
                1,
                MaterialPrefix.crushedCentrifugedTiny,
                9,
                material);
        unpack("ingot 1 -> nugget 9", MaterialPrefix.ingot, 1, MaterialPrefix.nugget, 9, material);
        unpack("billet 1 -> nugget 6", MaterialPrefix.billet, 1, MaterialPrefix.nugget, 6, material);
        unpack("dust 1 -> dustTiny 9", MaterialPrefix.dust, 1, MaterialPrefix.dustTiny, 9, material);
        unpack("dustSmall 1 -> dustDiv72 18", MaterialPrefix.dustSmall, 1, MaterialPrefix.dustDiv72, 18, material);
        unpack("dustTiny 1 -> dustDiv72 8", MaterialPrefix.dustTiny, 1, MaterialPrefix.dustDiv72, 8, material);
        // Loader_Recipes_Handlers: a projectile gives back its head/dust and the empty
        // carrier. MT.Empty itself is excluded by the source's MT.Empty.NOT condition.
        if (material != Materials.Empty) {
            projectile(
                    "arrowGtWood 1 -> toolHeadArrow 1 + empty arrow",
                    MaterialPrefix.arrowGtWood,
                    MaterialPrefix.toolHeadArrow,
                    1,
                    material);
            projectile(
                    "arrowGtPlastic 1 -> toolHeadArrow 1 + empty arrow",
                    MaterialPrefix.arrowGtPlastic,
                    MaterialPrefix.toolHeadArrow,
                    1,
                    material);
            projectile(
                    "bulletGtSmall 1 -> dustTiny 1 + empty bullet",
                    MaterialPrefix.bulletGtSmall,
                    MaterialPrefix.dustTiny,
                    1,
                    material);
            projectile(
                    "bulletGtMedium 1 -> dustTiny 2 + empty bullet",
                    MaterialPrefix.bulletGtMedium,
                    MaterialPrefix.dustTiny,
                    2,
                    material);
            projectile(
                    "bulletGtLarge 1 -> dustTiny 3 + empty bullet",
                    MaterialPrefix.bulletGtLarge,
                    MaterialPrefix.dustTiny,
                    3,
                    material);
        }
    }

    /** GT6 {@code Loader_Recipes_Handlers}: pack smaller forms into blocks / bigger forms. */
    private static void boxinator(GTMaterial material) {
        box("oreRaw 9 -> blockRaw 1", MaterialPrefix.oreRaw, 9, 9, BlockMaterialPrefix.blockRaw, 1, material);
        box("dust 9 -> blockDust 1", MaterialPrefix.dust, 9, 9, BlockMaterialPrefix.blockDust, 1, material);
        box("gem 9 -> blockGem 1", MaterialPrefix.gem, 9, 9, BlockMaterialPrefix.blockGem, 1, material);
        box("ingot 9 -> blockIngot 1", MaterialPrefix.ingot, 9, 9, BlockMaterialPrefix.blockIngot, 1, material);
        box("plate 9 -> blockPlate 1", MaterialPrefix.plate, 9, 9, BlockMaterialPrefix.blockPlate, 1, material);
        box(
                "plateGem 9 -> blockPlateGem 1",
                MaterialPrefix.plateGem,
                9,
                9,
                BlockMaterialPrefix.blockPlateGem,
                1,
                material);
        box("dustSmall 36 -> blockDust 1", MaterialPrefix.dustSmall, 36, 9, BlockMaterialPrefix.blockDust, 1, material);
        box("chunkGt 36 -> blockIngot 1", MaterialPrefix.chunkGt, 36, 9, BlockMaterialPrefix.blockIngot, 1, material);
        box("billet 27 -> blockIngot 2", MaterialPrefix.billet, 27, 9, BlockMaterialPrefix.blockIngot, 2, material);

        pack("dustTiny 9 -> dust 1", MaterialPrefix.dustTiny, 9, 9, MaterialPrefix.dust, 1, material);
        pack("dustSmall 4 -> dust 1", MaterialPrefix.dustSmall, 4, 4, MaterialPrefix.dust, 1, material);
        pack("dustDiv72 8 -> dustTiny 1", MaterialPrefix.dustDiv72, 8, 9, MaterialPrefix.dustTiny, 1, material);
        pack("dustDiv72 18 -> dustSmall 1", MaterialPrefix.dustDiv72, 18, 4, MaterialPrefix.dustSmall, 1, material);
        pack("nugget 9 -> ingot 1", MaterialPrefix.nugget, 9, 9, MaterialPrefix.ingot, 1, material);
        pack("nugget 6 -> billet 1", MaterialPrefix.nugget, 6, 6, MaterialPrefix.billet, 1, material);
        pack("ingot 2 -> billet 3", MaterialPrefix.ingot, 2, 6, MaterialPrefix.billet, 3, material);
        pack("chunkGt 4 -> ingot 1", MaterialPrefix.chunkGt, 4, 4, MaterialPrefix.ingot, 1, material);
        pack("billet 3 -> ingot 2", MaterialPrefix.billet, 3, 3, MaterialPrefix.ingot, 2, material);
        pack("crushedTiny 9 -> crushed 1", MaterialPrefix.crushedTiny, 9, 9, MaterialPrefix.crushed, 1, material);
        pack(
                "crushedPurifiedTiny 9 -> crushedPurified 1",
                MaterialPrefix.crushedPurifiedTiny,
                9,
                9,
                MaterialPrefix.crushedPurified,
                1,
                material);
        pack(
                "crushedCentrifugedTiny 9 -> crushedCentrifuged 1",
                MaterialPrefix.crushedCentrifugedTiny,
                9,
                9,
                MaterialPrefix.crushedCentrifuged,
                1,
                material);
        // GT6 casingSmall is the port's itemCasing (PrefixRegistry alias).
        pack("plateTiny 9 -> casingSmall 2", MaterialPrefix.plateTiny, 9, 9, MaterialPrefix.itemCasing, 2, material);
        pack(
                "plateGemTiny 9 -> casingSmall 2",
                MaterialPrefix.plateGemTiny,
                9,
                9,
                MaterialPrefix.itemCasing,
                2,
                material);
    }

    private static void addLaminator(String route, ItemStack wax, ItemStack empty, ItemStack fireproof) {
        if (wax.isEmpty()) return;
        Recipe recipe = MachineRecipeMaps.Laminator.addRecipe2(false, 16, 32, item(empty, 1), wax, fireproof.copy());
        if (recipe != null) ENTRIES.add(new Entry("laminator", route, MachineRecipeMaps.Laminator, recipe));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Compressor route with a block input ({@code blockPlate 1 -> plateDense 1}). */
    private static void compressingBlock(
            GTMaterial material, long ticks, BlockMaterialPrefix from, int fromCount, MaterialPrefix to, int toCount) {
        ItemStack input = GTBlocks.getStack(from, material);
        ItemStack output = GTItems.getStack(to, material, toCount);
        if (input.isEmpty() || output.isEmpty()) return;
        unit(
                "compressor",
                from.getName() + " " + fromCount + " -> " + to.getName() + " " + toCount,
                MachineRecipeMaps.Compressor,
                ticks,
                material,
                item(input, fromCount),
                from.getMaterialWeight(),
                null,
                output);
    }

    private static void unbox(
            String route,
            BlockMaterialPrefix from,
            int fromCount,
            MaterialPrefix to,
            int toCount,
            GTMaterial material) {
        ItemStack input = blockStack(from, material);
        ItemStack output = GTItems.getStack(to, material, toCount);
        if (input.isEmpty() || output.isEmpty()) return;
        unit(
                "unboxinator",
                route,
                MachineRecipeMaps.Unboxinator,
                16,
                material,
                item(input, fromCount),
                from.getMaterialWeight(),
                null,
                output);
    }

    /** A vanilla block target replaces the GT stack of that form; other forms keep the GT block. */
    private static ItemStack blockStack(BlockMaterialPrefix prefix, GTMaterial material) {
        if (prefix == BlockMaterialPrefix.blockDust) {
            var vanilla = MaterialUnification.vanillaFormStack(prefix.getName(), material);
            if (!vanilla.isEmpty()) return vanilla;
        }
        return GTBlocks.getStack(prefix, material);
    }

    /** Unboxinator row that returns the empty projectile carrier as the second output. */
    private static void projectile(
            String route, MaterialPrefix from, MaterialPrefix content, int contentCount, GTMaterial material) {
        ItemStack input = GTItems.getStack(from, material, 1);
        ItemStack output = GTItems.getStack(content, material, contentCount);
        ItemStack empty = GTItems.getStack(from, Materials.Empty, 1);
        if (input.isEmpty() || output.isEmpty() || empty.isEmpty()) return;
        unit(
                "unboxinator",
                route,
                MachineRecipeMaps.Unboxinator,
                16,
                material,
                input,
                from.getMaterialWeight(),
                null,
                output,
                empty);
    }

    private static void unpack(
            String route, MaterialPrefix from, int fromCount, MaterialPrefix to, int toCount, GTMaterial material) {
        ItemStack input = GTItems.getStack(from, material, fromCount);
        ItemStack output = GTItems.getStack(to, material, toCount);
        if (input.isEmpty() || output.isEmpty()) return;
        unit(
                "unboxinator",
                route,
                MachineRecipeMaps.Unboxinator,
                16,
                material,
                input,
                from.getMaterialWeight(),
                null,
                output);
    }

    private static void box(
            String route,
            MaterialPrefix from,
            int fromCount,
            int circuit,
            BlockMaterialPrefix to,
            int toCount,
            GTMaterial material) {
        ItemStack input = GTItems.getStack(from, material, fromCount);
        ItemStack output = blockStack(to, material);
        if (input.isEmpty() || output.isEmpty()) return;
        unit(
                "boxinator",
                route,
                MachineRecipeMaps.Boxinator,
                16,
                material,
                input,
                from.getMaterialWeight(),
                circuit,
                item(output, toCount));
    }

    private static void pack(
            String route,
            MaterialPrefix from,
            int fromCount,
            int circuit,
            MaterialPrefix to,
            int toCount,
            GTMaterial material) {
        ItemStack input = GTItems.getStack(from, material, fromCount);
        ItemStack output = GTItems.getStack(to, material, toCount);
        if (input.isEmpty() || output.isEmpty()) return;
        unit(
                "boxinator",
                route,
                MachineRecipeMaps.Boxinator,
                16,
                material,
                input,
                from.getMaterialWeight(),
                circuit,
                output);
    }

    /** Single-input conversion used by the compressor table ({@code plate 9 -> plateDense 1}). */
    private static void compressing(
            GTMaterial material, long ticks, MaterialPrefix from, int fromCount, MaterialPrefix to, int toCount) {
        ItemStack input = GTItems.getStack(from, material, fromCount);
        ItemStack output = GTItems.getStack(to, material, toCount);
        if (input.isEmpty() || output.isEmpty()) return;
        unit(
                "compressor",
                from.getName() + " " + fromCount + " -> " + to.getName() + " " + toCount,
                MachineRecipeMaps.Compressor,
                ticks,
                material,
                input,
                from.getMaterialWeight(),
                null,
                output);
    }

    /** GT6 checks prefix membership through the material's registered items; the port does it directly. */
    private static boolean has(MaterialPrefix prefix, GTMaterial material) {
        return !GTItems.getStack(prefix, material, 1).isEmpty();
    }

    private static ItemStack item(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    /**
     * Publishes one fixed-form conversion in material units. The template keeps the row's printed
     * counts; the rule rebuilds the same batch from the offered form's unit weight, so any
     * equivalent item of the form is admitted.
     */
    private static void unit(
            String kind,
            String route,
            RecipeMap map,
            long ticks,
            GTMaterial material,
            ItemStack input,
            long unitsPerItem,
            Integer circuit,
            ItemStack... outputs) {
        if (input.isEmpty() || outputs.length == 0) return;
        ItemStack[] inputs = circuit == null
                ? new ItemStack[] {input}
                : new ItemStack[] {input, new ItemStack(GTTechnological.selectorTag(circuit))};
        var template = RecipeMap.make(false, inputs, outputs, null, null, null, null, ticks, 16, 0);
        if (template == null) return;
        long required = Math.multiplyExact(unitsPerItem, input.getCount());
        var rule = new gregtech.engine.recipe.MaterialUnitRecipe(
                template,
                0,
                material,
                required,
                gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding.WHOLE_OUTPUTS);
        rule.source(input, unitsPerItem);
        ENTRIES.add(new Entry(kind, route, map, rule.publish(map)));
    }

    private static void skippedOnce(String kind, String route, String reason) {
        SKIPPED.add(kind + ": " + route + " (" + reason + ")");
    }
}
