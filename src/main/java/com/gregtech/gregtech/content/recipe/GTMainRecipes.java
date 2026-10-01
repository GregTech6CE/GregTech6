package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.loaders.c.GTGeneratedChem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The rows GT6 adds from its own main class — {@code GT6_Main.java:326-403} — which the transpilers do
 * not see because they are neither a {@code Loader_Recipes_*} file nor a registration pattern:
 *
 * <ul>
 *   <li><b>Printer</b> ({@code :352}) — GT6's only real printer recipe: a plain book plus black dye
 *       fluid prints the "Scanner &amp; Printer Manual" ({@code ST.book("Manual_Printer", …)}), which is
 *       how the original hands that manual out. The port's printer had an <em>empty</em> recipe map, so
 *       the machine was unusable and the manual unobtainable outside loot.</li>
 *   <li><b>Boxinator</b> ({@code :351}) — 8 paper + a compass fold into a map.</li>
 *   <li><b>Scanner (Visuals)</b> ({@code :326-347}) — display-only rows ({@code addFakeRecipe}) showing
 *       what a scanner writes onto a USB stick; the port's map was empty.</li>
 *   <li><b>Material dictionary</b> ({@code :388}) — the printer's dictionary row needs the material a
 *       scanner wrote onto a USB stick, so {@link GTMaterialDataRecipes} builds it from the stick's
 *       NBT; the molecular scanner that writes that data is GT6's
 *       {@code RecipeMapScannerMolecular}.</li> *   <li><b>Unboxinator</b> ({@code :386-393}) — display-only rows showing what each GT loot bag, the
 *       guide book and the loot bottle hand out; the port has those items (§21, §25), so the chains are
 *       now visible in JEI.</li>
 * </ul>
 *
 * <p>Rows whose content the port does not register (printed pages, blueprints, Twilight Forest and
 * Galacticraft maps, Thaumcraft loot bags) are recorded in {@link #skipped()} with their reason
 * instead of being invented. The material dictionary book ({@code GT6_Main:388}) also needs the NBT
 * of a scanned USB stick, so it is printed by {@link GTMaterialDataRecipes} rather than from a static
 * row; it stays listed here as a checklist entry.</p>
 */
public final class GTMainRecipes {

    /** {@code map|fake|eu|ticks|inputs|outputs|fluids in|fluids out}; specs use the chem spec grammar. */
    private record Row(String map, boolean fake, int eu, int ticks, String inputs, String outputs,
                       String fluidIn, String fluidOut, String source) {}

    private static final List<Row> ROWS = List.of(
            // ---- real recipes (the machine consumes them) ----
            new Row("Printer", false, 16, 256, "v:book:1", "book:Manual_Printer",
                    "f:Dye_Chemical_Black:144", "", "GT6_Main:352"),
            new Row("Boxinator", false, 16, 16, "v:paper:8;v:compass:1", "v:map:1", "", "",
                    "GT6_Main:351"),

            // ---- GT6's "Did you know...?" hint pages (Loader_Recipes_Hints) ----
            // The labels name the GT6 machines; the shown stacks are the materials, books and
            // fluids the port registers (several GT6 machine blocks have different ids here).
            new Row("DidYouKnow", true, 0, 0,
                    "i:dust:Cinnabar:3|Throw three Units of Cinnabar into the Crucible;"
                            + "tech:clay_crucible:1|Wait until it melts into Mercury;"
                            + "v:glass_bottle:1|Rightclick the Crucible with an Empty Bottle;"
                            + "v:redstone_ore:1|Mine Vanilla Redstone Ore with a Club for Cinnabar",
                    "tech:mercury_bottle:1|A Bottle of Mercury", "", "", "Loader_Recipes_Hints:38"),
            new Row("DidYouKnow", true, 0, 0,
                    "i:ingot:Fe:1|Throw some Iron into the Crucible (leave room for Air);"
                            + "tech:clay_crucible:1|Blow Air in with a running Engine;"
                            + "i:ingot:WroughtIron:1|Wrought Iron works too",
                    "i:ingot:Steel:1|Wait until it all turns into Steel;i:dust:Steel:1;i:plate:Steel:1;"
                            + "i:stick:Steel:1;i:gearGt:Steel:1",
                    "", "", "Loader_Recipes_Hints:44"),
            new Row("DidYouKnow", true, 0, 0,
                    "i:ingot:Zn:1|Dump some Zinc into the Crucible;"
                            + "tech:clay_faucet:1|Pour the Zinc with a Faucet on the Crucible;"
                            + "i:plate:Steel:1|Put your Steel object into the Bathing Pot below it",
                    "i:plate:SteelGalvanized:1|Galvanized Steel Plate;"
                            + "i:stick:SteelGalvanized:1;i:screw:SteelGalvanized:1",
                    "m:Zn:144", "m:Zn:144", "Loader_Recipes_Hints:50"),
            new Row("DidYouKnow", true, 0, 0,
                    "v:book:1|Insert a basic empty Book into the Printer to get a Manual",
                    "book:Manual_Printer", "f:Dye_Chemical_Black:144", "", "Loader_Recipes_Hints:56"),
            new Row("ScannerVisuals", true, 16, 64, "v:filled_map:1;tech:usb1_stick:1",
                    "v:filled_map:1;tech:usb1_stick:1", "", "", "GT6_Main:332"),
            new Row("ScannerVisuals", true, 16, 64, "v:crafting_table:1;tech:usb1_stick:1",
                    "v:crafting_table:1;tech:usb1_stick:1", "", "", "GT6_Main:331"),

            // ---- display rows: the printer reproduces what a scanned USB stick carries ----
            // GT6 prints a map of one dye unit per colour divided by nine (16 mB per colour) and one
            // unit of black for a scanned block (GT6_Main:361 / :355).
            new Row("Printer", true, 16, 64, "v:map:1;tech:usb1_stick:1", "v:filled_map:1",
                    "f:Dye_Chemical_Yellow:16;f:Dye_Chemical_Magenta:16;f:Dye_Chemical_Cyan:16;"
                            + "f:Dye_Chemical_Black:16", "", "GT6_Main:361"),
            new Row("Printer", true, 16, 64, "v:crafting_table:1;tech:usb1_stick:1",
                    "v:crafting_table:1",
                    "f:Dye_Chemical_Yellow:16;f:Dye_Chemical_Magenta:16;f:Dye_Chemical_Cyan:16;"
                            + "f:Dye_Chemical_Black:16",
                    "", "GT6_Main:355"),

            // ---- display rows: opening a GT loot bag / book / bottle ----
            new Row("Unboxinator", true, 16, 16, "tech:dusty_guide_book:1",
                    "book:Manual_Steam;book:Manual_Random;book:Manual_Printer;book:Manual_Enchantments",
                    "", "", "GT6_Main:387"),
            new Row("Unboxinator", true, 16, 16, "tech:loot_bottle:1",
                    "v:experience_bottle:1;tech:holy_water:1;tech:green_slime_bottle:1;tech:purple_drink:1;"
                            + "tech:ink_bottle:1",
                    "", "", "GT6_Main:389"),
            new Row("Unboxinator", true, 16, 16, "tech:gem_pouch:1",
                    "i:gemFlawless:Diamond:1;i:gem:Emerald:1", "", "", "GT6_Main:392"),
            new Row("Unboxinator", true, 16, 16, "tech:bagged_sapling:1",
                    "v:oak_sapling:1;v:spruce_sapling:1;v:birch_sapling:1;v:jungle_sapling:1", "", "",
                    "GT6_Main:390"),
            new Row("Unboxinator", true, 16, 16, "tech:seed_pouch:1",
                    "v:wheat_seeds:1;v:pumpkin_seeds:1;v:melon_seeds:1", "", "", "GT6_Main:391"),
            new Row("Unboxinator", true, 16, 16, "tech:loot_pouch:1",
                    "tech:dynamite:1;tech:modeled_porcelain_cup:1;i:rockGt:MeteoricIron:1;"
                            + "i:dust:Zeolite:1",
                    "", "", "GT6_Main:393"));

    /** Rows GT6 has that the port cannot show (their content is not registered), with the reason. */
    private static final List<String> SKIPPED = new ArrayList<>();
    private static final List<String> ENTRIES = new ArrayList<>();

    private static boolean registered;

    private GTMainRecipes() {}

    public static List<String> entries() { return List.copyOf(ENTRIES); }

    public static List<String> skipped() { return List.copyOf(SKIPPED); }

    private static RecipeMap map(String name) {
        return switch (name) {
            case "Printer" -> MachineRecipeMaps.Printer;
            case "Boxinator" -> MachineRecipeMaps.Boxinator;
            case "Unboxinator" -> MachineRecipeMaps.Unboxinator;
            case "ScannerVisuals" -> MachineRecipeMaps.ScannerVisuals;
            case "DidYouKnow" -> MachineRecipeMaps.DidYouKnow;
            default -> null;
        };
    }

    /** Registers the portable rows; idempotent. */
    public static int register() {
        if (registered) return ENTRIES.size();
        registered = true;
        for (Row row : ROWS) {
            RecipeMap target = map(row.map());
            if (target == null) {
                SKIPPED.add(row.map() + " (" + row.source() + "): recipe map is not ported");
                continue;
            }
            ItemStack[] inputs = items(row.inputs());
            ItemStack[] outputs = items(row.outputs());
            FluidStack[] fluidsIn = fluids(row.fluidIn());
            FluidStack[] fluidsOut = fluids(row.fluidOut());
            if (inputs == null || outputs == null || fluidsIn == null || fluidsOut == null) {
                SKIPPED.add(row.source() + ": content not registered in the port ("
                        + firstMissing(row) + ")");
                continue;
            }
            Recipe recipe = row.fake()
                    ? target.addFakeRecipe(true, inputs, outputs, null, fluidsIn, fluidsOut, row.ticks(), row.eu(), 0)
                    // addRecipe(optimize, in, out, special, outputChances, fluidIn, fluidOut, duration, eu, special)
                    : target.addRecipe(true, inputs, outputs, null, new long[outputs.length], fluidsIn,
                            fluidsOut, row.ticks(), row.eu(), 0);
            if (recipe == null) {
                SKIPPED.add(row.source() + ": rejected by " + row.map());
                continue;
            }
            ENTRIES.add(row.map() + "|" + (row.fake() ? "display" : "recipe") + "|" + row.source());
        }
        // GT6_Main rows whose content the port does not register, kept as a checklist.
        for (String reason : new String[]{
                "GT6_Main:327-328 printed pages (the port registers no IL.Paper_Printed_Pages items)",
                "GT6_Main:339/357-358 blueprints (no IL.Paper_Blueprint_* items)",
                "GT6_Main:334-338/161 Twilight Forest maps (another mod)",
                "GT6_Main:341-345/369-371 Galacticraft schematics (another mod)",
                "GT6_Main:347/372 IndustrialCraft blueprint (another mod)",
                "GT6_Main:388 material dictionary book (printed by GTMaterialDataRecipes from a scanned USB stick)",
                "GT6_Main:395-403 Thaumcraft / LootBags mod bags (other mods)"}) {
            SKIPPED.add(reason);
        }
        com.gregtech.gregtech.GregTech.LOGGER.info("GT6 main-class rows: {} registered, {} skipped",
                ENTRIES.size(), SKIPPED.size());
        for (String skip : SKIPPED) com.gregtech.gregtech.GregTech.LOGGER.info("  skipped: {}", skip);
        return ENTRIES.size();
    }

    private static ItemStack[] items(String specs) {
        if (specs == null || specs.isEmpty()) return new ItemStack[0];
        List<ItemStack> out = new ArrayList<>();
        for (String spec : specs.split(";")) {
            ItemStack stack = GTGeneratedChem.resolveSpec(stripLabel(spec));
            if (stack == null || stack.isEmpty()) return null;
            String label = labelOf(spec);
            if (label != null) {
                // GT6 labels hint ingredients with ST.make(stack, "text") for the recipe viewer.
                stack.setHoverName(net.minecraft.network.chat.Component.literal(label));
            }
            out.add(stack);
        }
        return out.toArray(ItemStack[]::new);
    }

    /** A spec may carry a display label after a {@code |} (the port's form of GT6's named stacks). */
    private static String stripLabel(String spec) {
        int pipe = spec.indexOf('|');
        return pipe < 0 ? spec : spec.substring(0, pipe);
    }

    private static String labelOf(String spec) {
        int pipe = spec.indexOf('|');
        return pipe < 0 ? null : spec.substring(pipe + 1);
    }

    /** The first spec of a row that does not resolve — the reason a row is skipped. */
    private static String firstMissing(Row row) {
        for (String spec : row.inputs().split(";")) {
            if (!spec.isEmpty() && GTGeneratedChem.resolveSpec(stripLabel(spec)) == null) return "input " + spec;
        }
        for (String spec : row.outputs().split(";")) {
            if (!spec.isEmpty() && GTGeneratedChem.resolveSpec(stripLabel(spec)) == null) return "output " + spec;
        }
        for (String spec : row.fluidIn().split(";")) {
            if (!spec.isEmpty() && GTGeneratedChem.resolveFluidSpec(stripLabel(spec)) == null) return "fluid " + spec;
        }
        for (String spec : row.fluidOut().split(";")) {
            if (!spec.isEmpty() && GTGeneratedChem.resolveFluidSpec(stripLabel(spec)) == null) {
                return "fluid out " + spec;
            }
        }
        return "unknown";
    }

    private static FluidStack[] fluids(String specs) {
        if (specs == null || specs.isEmpty()) return new FluidStack[0];
        List<FluidStack> out = new ArrayList<>();
        for (String spec : specs.split(";")) {
            FluidStack stack = GTGeneratedChem.resolveFluidSpec(spec);
            if (stack == null || stack.isEmpty()) return null;
            out.add(stack);
        }
        return out.toArray(FluidStack[]::new);
    }
}
