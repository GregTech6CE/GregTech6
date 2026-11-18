package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.recipe.generated.ChemRecipeResolver;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.engine.recipe.RecipeBuilder;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.chem.ChemFluid.NativeFluid;
import gregtech.engine.recipe.chem.ChemIngredient;
import gregtech.platform.Mc;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * The rows GT6 adds from its own main class — {@code GT6_Main} — which the transpilers do
 * not see because they are neither a {@code Loader_Recipes_*} file nor a registration pattern:
 *
 * <ul>
 *   <li><b>Printer</b> — GT6's only real printer recipe: a plain book plus black dye
 *       fluid prints the "Scanner &amp; Printer Manual" ({@code ST.book("Manual_Printer", …)}), which is
 *       how the original hands that manual out. The port's printer had an <em>empty</em> recipe map, so
 *       the machine was unusable and the manual unobtainable outside loot.</li>
 *   <li><b>Boxinator</b> — 8 paper + a compass fold into a map.</li>
 *   <li><b>Scanner (Visuals)</b> — display-only rows ({@code addFakeRecipe}) showing
 *       what a scanner writes onto a USB stick; the port's map was empty.</li>
 *   <li><b>Material dictionary</b> — the printer's dictionary row needs the material a
 *       scanner wrote onto a USB stick, so {@link GTMaterialDataRecipes} builds it from the stick's
 *       NBT; the molecular scanner that writes that data is GT6's
 *       {@code RecipeMapScannerMolecular}.</li>
 *   <li><b>Unboxinator</b> — display-only rows showing what each GT loot bag, the
 *       guide book and the loot bottle hand out; the port has those items, so the chains are
 *       now visible in JEI.</li>
 * </ul>
 *
 * <p>Rows whose content the port does not register (Twilight Forest and
 * Galacticraft maps, Thaumcraft loot bags) are recorded in {@link #skipped()} with their reason
 * instead of being invented. The material dictionary book ({@code GT6_Main}) also needs the NBT
 * of a scanned USB stick, so it is printed by {@link GTMaterialDataRecipes} rather than from a static
 * row; it stays listed here as a checklist entry.</p>
 */
public final class GTMainRecipes {

    private record Target(String name, RecipeMap map) {}

    private record StackSpec(ChemIngredient ingredient, String label, boolean catalyst) {
        private StackSpec labelled(String text) {
            return new StackSpec(ingredient, text, catalyst);
        }

        private StackSpec kept() {
            return new StackSpec(ingredient, label, true);
        }
    }

    private record Row(
            Target target,
            String source,
            int energyPerTick,
            int duration,
            boolean displayOnly,
            List<StackSpec> itemInputs,
            List<StackSpec> itemOutputs,
            List<NativeFluid> fluidInputs,
            List<NativeFluid> fluidOutputs) {}

    private static final Target BATH = new Target("Bath", MachineRecipeMaps.Bath);
    private static final Target PRINTER = new Target("Printer", MachineRecipeMaps.Printer);
    private static final Target BOXINATOR = new Target("Boxinator", MachineRecipeMaps.Boxinator);
    private static final Target UNBOXINATOR = new Target("Unboxinator", MachineRecipeMaps.Unboxinator);
    private static final Target SCANNER_VISUALS = new Target("ScannerVisuals", MachineRecipeMaps.ScannerVisuals);
    private static final Target DID_YOU_KNOW = new Target("DidYouKnow", MachineRecipeMaps.DidYouKnow);

    private static final List<Row> ROWS = List.of(
            // ---- real recipes (the machine consumes them) ----

            recipe(BATH, "MultiItemRandomTools:436-437", 0, 16)
                    .itemInputs(vanilla("paper", 1))
                    .itemOutputs(tech("empty_blueprint", 1))
                    .fluidInputs(fluid("Dye_Water_Blue", 144))
                    .build(),
            recipe(BATH, "MultiItemRandomTools:436-437", 0, 16)
                    .itemInputs(vanilla("paper", 1))
                    .itemOutputs(tech("empty_blueprint", 1))
                    .fluidInputs(fluid("Dye_Flower_Blue", 144))
                    .build(),
            recipe(BATH, "MultiItemRandomTools:436-437", 0, 16)
                    .itemInputs(vanilla("paper", 1))
                    .itemOutputs(tech("empty_blueprint", 1))
                    .fluidInputs(fluid("Dye_Chemical_Blue", 144))
                    .build(),
            display(SCANNER_VISUALS, "GT6_Main:339", 16, 64)
                    .itemInputs(
                            tech("blueprint", 1),
                            tech("usb1_stick", 1))
                    .itemOutputs(
                            tech("usb1_stick", 1).labelled("Containing scanned Blueprint"),
                            tech("blueprint", 1))
                    .build(),
            display(PRINTER, "GT6_Main:357", 16, 32)
                    .itemInputs(
                            tech("empty_blueprint", 1),
                            tech("usb1_stick", 1).kept())
                    .itemOutputs(tech("blueprint", 1))
                    .fluidInputs(fluid("Dye_Chemical_White", 16))
                    .build(),
            display(PRINTER, "GT6_Main:358", 16, 128)
                    .itemInputs(
                            vanilla("paper", 1),
                            tech("usb1_stick", 1).kept())
                    .itemOutputs(tech("blueprint", 1))
                    .fluidInputs(fluid("Dye_Chemical_Blue", 144))
                    .build(),
            recipe(PRINTER, "GT6_Main:352", 16, 256)
                    .itemInputs(vanilla("book", 1))
                    .itemOutputs(book("Manual_Printer"))
                    .fluidInputs(fluid("Dye_Chemical_Black", 144))
                    .build(),
            recipe(BOXINATOR, "GT6_Main:351", 16, 16)
                    .itemInputs(
                            vanilla("paper", 8),
                            vanilla("compass", 1))
                    .itemOutputs(vanilla("map", 1))
                    .build(),

            // ---- GT6's "Did you know...?" hint pages (Loader_Recipes_Hints) ----
            // The labels name the GT6 machines; the shown stacks are the materials, books and
            // fluids the port registers (several GT6 machine blocks have different ids here).
            display(DID_YOU_KNOW, "Loader_Recipes_Hints:38", 0, 0)
                    .itemInputs(
                            form("dust", "Cinnabar", 3).labelled("Throw three Units of Cinnabar into the Crucible"),
                            tech("clay_crucible", 1).labelled("Wait until it melts into Mercury"),
                            vanilla("glass_bottle", 1).labelled("Rightclick the Crucible with an Empty Bottle"),
                            vanilla("redstone_ore", 1).labelled("Mine Vanilla Redstone Ore with a Club for Cinnabar"))
                    .itemOutputs(tech("mercury_bottle", 1).labelled("A Bottle of Mercury"))
                    .build(),
            display(DID_YOU_KNOW, "Loader_Recipes_Hints:44", 0, 0)
                    .itemInputs(
                            form("ingot", "Fe", 1).labelled("Throw some Iron into the Crucible (leave room for Air)"),
                            tech("clay_crucible", 1).labelled("Blow Air in with a running Engine"),
                            form("ingot", "WroughtIron", 1).labelled("Wrought Iron works too"))
                    .itemOutputs(
                            form("ingot", "Steel", 1).labelled("Wait until it all turns into Steel"),
                            form("dust", "Steel", 1),
                            form("plate", "Steel", 1),
                            form("stick", "Steel", 1),
                            form("gearGt", "Steel", 1))
                    .build(),
            display(DID_YOU_KNOW, "Loader_Recipes_Hints:50", 0, 0)
                    .itemInputs(
                            form("ingot", "Zn", 1).labelled("Dump some Zinc into the Crucible"),
                            tech("clay_faucet", 1).labelled("Pour the Zinc with a Faucet on the Crucible"),
                            form("plate", "Steel", 1).labelled("Put your Steel object into the Bathing Pot below it"))
                    .itemOutputs(
                            form("plate", "SteelGalvanized", 1).labelled("Galvanized Steel Plate"),
                            form("stick", "SteelGalvanized", 1),
                            form("screw", "SteelGalvanized", 1))
                    .fluidInputs(fluid("GenMolten_Zinc", 144))
                    .fluidOutputs(fluid("GenMolten_Zinc", 144))
                    .build(),
            display(DID_YOU_KNOW, "Loader_Recipes_Hints:56", 0, 0)
                    .itemInputs(vanilla("book", 1).labelled("Insert a basic empty Book into the Printer to get a Manual"))
                    .itemOutputs(book("Manual_Printer"))
                    .fluidInputs(fluid("Dye_Chemical_Black", 144))
                    .build(),
            display(SCANNER_VISUALS, "GT6_Main:332", 16, 64)
                    .itemInputs(
                            vanilla("filled_map", 1),
                            tech("usb1_stick", 1))
                    .itemOutputs(
                            vanilla("filled_map", 1),
                            tech("usb1_stick", 1))
                    .build(),
            display(SCANNER_VISUALS, "GT6_Main:331", 16, 64)
                    .itemInputs(
                            vanilla("crafting_table", 1),
                            tech("usb1_stick", 1))
                    .itemOutputs(
                            vanilla("crafting_table", 1),
                            tech("usb1_stick", 1))
                    .build(),

            display(SCANNER_VISUALS, "GT6_Main:326", 16, 512)
                    .itemInputs(
                            vanilla("written_book", 1),
                            tech("usb1_stick", 1))
                    .itemOutputs(
                            tech("usb1_stick", 1).labelled("Containing scanned Book"),
                            vanilla("written_book", 1))
                    .build(),
            display(SCANNER_VISUALS, "GT6_Main:327", 16, 512)
                    .itemInputs(
                            tech("printed_pages", 1),
                            tech("usb1_stick", 1))
                    .itemOutputs(
                            tech("usb1_stick", 1).labelled("Containing scanned Book"),
                            tech("printed_pages", 1))
                    .build(),
            display(SCANNER_VISUALS, "GT6_Main:328", 16, 512)
                    .itemInputs(
                            tech("many_printed_pages", 1),
                            tech("usb1_stick", 1))
                    .itemOutputs(
                            tech("usb1_stick", 1).labelled("Containing large scanned Book"),
                            tech("many_printed_pages", 1))
                    .build(),
            display(PRINTER, "GT6_Main:359", 16, 512)
                    .itemInputs(
                            vanilla("paper", 3),
                            tech("usb1_stick", 1).labelled("Containing scanned Book").kept())
                    .itemOutputs(tech("printed_pages", 1))
                    .fluidInputs(fluid("Dye_Chemical_Black", 72))
                    .build(),
            display(PRINTER, "GT6_Main:360", 16, 1024)
                    .itemInputs(
                            vanilla("paper", 6),
                            tech("usb1_stick", 1).labelled("Containing large scanned Book").kept())
                    .itemOutputs(tech("many_printed_pages", 1))
                    .fluidInputs(fluid("Dye_Chemical_Black", 144))
                    .build(),

            // ---- display rows: the printer reproduces what a scanned USB stick carries ----
            // GT6 prints a map of one dye unit per colour divided by nine (16 mB per colour) and one
            // unit of black for a scanned block (GT6_Main /).
            display(PRINTER, "GT6_Main:361", 16, 64)
                    .itemInputs(
                            vanilla("map", 1),
                            tech("usb1_stick", 1).kept())
                    .itemOutputs(vanilla("filled_map", 1))
                    .fluidInputs(
                            fluid("Dye_Chemical_Yellow", 16),
                            fluid("Dye_Chemical_Magenta", 16),
                            fluid("Dye_Chemical_Cyan", 16),
                            fluid("Dye_Chemical_Black", 16))
                    .build(),
            display(PRINTER, "GT6_Main:355", 16, 64)
                    .itemInputs(
                            vanilla("crafting_table", 1),
                            tech("usb1_stick", 1))
                    .itemOutputs(vanilla("crafting_table", 1))
                    .fluidInputs(
                            fluid("Dye_Chemical_Yellow", 16),
                            fluid("Dye_Chemical_Magenta", 16),
                            fluid("Dye_Chemical_Cyan", 16),
                            fluid("Dye_Chemical_Black", 16))
                    .build(),

            // ---- display rows: opening a GT loot bag / book / bottle ----
            display(UNBOXINATOR, "GT6_Main:387", 16, 16)
                    .itemInputs(tech("dusty_guide_book", 1))
                    .itemOutputs(
                            book("Manual_Steam"),
                            book("Manual_Random"),
                            book("Manual_Printer"),
                            book("Manual_Enchantments"))
                    .build(),
            display(UNBOXINATOR, "GT6_Main:389", 16, 16)
                    .itemInputs(tech("loot_bottle", 1))
                    .itemOutputs(
                            vanilla("experience_bottle", 1),
                            tech("holy_water", 1),
                            tech("green_slime_bottle", 1),
                            tech("purple_drink", 1),
                            tech("ink_bottle", 1))
                    .build(),
            display(UNBOXINATOR, "GT6_Main:392", 16, 16)
                    .itemInputs(tech("gem_pouch", 1))
                    .itemOutputs(
                            form("gemFlawless", "Diamond", 1),
                            form("gem", "Emerald", 1))
                    .build(),
            display(UNBOXINATOR, "GT6_Main:390", 16, 16)
                    .itemInputs(tech("bagged_sapling", 1))
                    .itemOutputs(
                            vanilla("oak_sapling", 1),
                            vanilla("spruce_sapling", 1),
                            vanilla("birch_sapling", 1),
                            vanilla("jungle_sapling", 1))
                    .build(),
            display(UNBOXINATOR, "GT6_Main:391", 16, 16)
                    .itemInputs(tech("seed_pouch", 1))
                    .itemOutputs(
                            vanilla("wheat_seeds", 1),
                            vanilla("pumpkin_seeds", 1),
                            vanilla("melon_seeds", 1))
                    .build(),
            display(UNBOXINATOR, "GT6_Main:393", 16, 16)
                    .itemInputs(tech("loot_pouch", 1))
                    .itemOutputs(
                            tech("dynamite", 1),
                            tech("modeled_porcelain_cup", 1),
                            form("dust", "Zeolite", 1))
                    .build(),
            display(UNBOXINATOR, "GT6_Main:386", 16, 16)
                    .itemInputs(registered("loot_crate", 1))
                    .itemOutputs(
                            registered("loot_crate", 1).labelled("1 Vanilla Loot Table Stack"),
                            registered("crate", 1))
                    .build());

    /** Rows GT6 has that the port cannot show (their content is not registered), with the reason. */
    private static final List<String> SKIPPED = new ArrayList<>();
    private static final List<String> ENTRIES = new ArrayList<>();

    private static boolean registered;

    private GTMainRecipes() {}

    public static List<String> entries() { return List.copyOf(ENTRIES); }

    public static List<String> skipped() { return List.copyOf(SKIPPED); }

    /** Registers the portable rows; idempotent. */
    public static int register() {
        if (registered) return ENTRIES.size();
        registered = true;
        for (Row row : ROWS) {
            try {
                register(row);
            } catch (MissingContent missing) {
                SKIPPED.add(row.source() + ": content not registered in the port (" + missing.getMessage() + ")");
            }
        }
        // GT6_Main rows whose content the port does not register, kept as a checklist.
        for (String reason : new String[]{
                "GT6_Main:334-338/161 Twilight Forest maps (another mod)",
                "GT6_Main:341-345/369-371 Galacticraft schematics (another mod)",
                "GT6_Main:347/372 IndustrialCraft blueprint (another mod)",
                "GT6_Main:388 material dictionary book (printed by GTMaterialDataRecipes from a scanned USB stick)",
                "GT6_Main:395-403 Thaumcraft / LootBags mod bags (other mods)"}) {
            SKIPPED.add(reason);
        }
        Mc.logger().info("GT6 main-class rows: {} registered, {} skipped",
                ENTRIES.size(), SKIPPED.size());
        for (String skip : SKIPPED) Mc.logger().info("  skipped: {}", skip);
        return ENTRIES.size();
    }

    private static void register(Row row) {
        ItemStack[] inputs = items(row.itemInputs(), "input");
        ItemStack[] outputs = items(row.itemOutputs(), "output");
        var builder = RecipeBuilder.of(row.energyPerTick(), row.duration())
                .itemInputs(inputs)
                .fluidInputs(fluids(row.fluidInputs(), "fluid input"))
                .itemOutputs(outputs)
                .fluidOutputs(fluids(row.fluidOutputs(), "fluid output"));

        var target = row.target();
        var recipe = row.displayOnly()
                ? target.map().register(builder, RecipeMap.Registration.DISPLAY_CHECKED)
                : target.map().register(builder.optimize(true).chances(new long[outputs.length]));
        if (recipe == null) {
            SKIPPED.add(row.source() + ": rejected by " + target.name());
            return;
        }

        for (int slot = 0; slot < row.itemInputs().size(); slot++) {
            if (row.itemInputs().get(slot).catalyst()) recipe.withCatalystInputs(slot);
        }
        ENTRIES.add(target.name() + "|" + (row.displayOnly() ? "display" : "recipe") + "|" + row.source());
    }

    private static ItemStack[] items(List<StackSpec> specs, String side) {
        var stacks = new ItemStack[specs.size()];
        for (int index = 0; index < stacks.length; index++) {
            var spec = specs.get(index);
            ItemStack stack = ChemRecipeResolver.resolveItem(spec.ingredient());
            if (stack == null || stack.isEmpty()) throw new MissingContent(side + " " + spec.ingredient());
            if (spec.label() != null) {
                Component name = Component.literal(spec.label());
//? if >=1.21.1 {
                /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
*///?} else {
                stack.setHoverName(name);
//?}
            }
            stacks[index] = stack;
        }
        return stacks;
    }

    private static FluidStack[] fluids(List<NativeFluid> specs, String side) {
        var stacks = new FluidStack[specs.size()];
        for (int index = 0; index < stacks.length; index++) {
            var spec = specs.get(index);
            FluidStack stack = GeneratedRecipeResolver.namedFluid(spec.key(), spec.millibuckets());
            if (stack == null || stack.isEmpty()) throw new MissingContent(side + " " + spec.key());
            stacks[index] = stack;
        }
        return stacks;
    }

    private static StackSpec vanilla(String path, int count) {
        return new StackSpec(new ChemIngredient.RegistryItem("minecraft:" + path, count), null, false);
    }

    private static StackSpec registered(String path, int count) {
        return new StackSpec(new ChemIngredient.RegistryItem("gt6ce:" + path, count), null, false);
    }

    private static StackSpec tech(String name, int count) {
        return new StackSpec(new ChemIngredient.TechnologyItem(name, count), null, false);
    }

    private static StackSpec form(String prefix, String material, int count) {
        return new StackSpec(new ChemIngredient.ItemForm(prefix, material, count), null, false);
    }

    private static StackSpec book(String name) {
        return new StackSpec(new ChemIngredient.Book(name), null, false);
    }

    private static NativeFluid fluid(String key, int millibuckets) {
        return new NativeFluid(key, millibuckets);
    }

    private static RowBuilder recipe(Target target, String source, int energyPerTick, int duration) {
        return new RowBuilder(target, source, energyPerTick, duration, false);
    }

    private static RowBuilder display(Target target, String source, int energyPerTick, int duration) {
        return new RowBuilder(target, source, energyPerTick, duration, true);
    }

    private static final class RowBuilder {
        private final Target target;
        private final String source;
        private final int energyPerTick;
        private final int duration;
        private final boolean displayOnly;
        private List<StackSpec> itemInputs = List.of();
        private List<StackSpec> itemOutputs = List.of();
        private List<NativeFluid> fluidInputs = List.of();
        private List<NativeFluid> fluidOutputs = List.of();

        private RowBuilder(Target target, String source, int energyPerTick, int duration, boolean displayOnly) {
            this.target = target;
            this.source = source;
            this.energyPerTick = energyPerTick;
            this.duration = duration;
            this.displayOnly = displayOnly;
        }

        private RowBuilder itemInputs(StackSpec... inputs) {
            itemInputs = List.of(inputs);
            return this;
        }

        private RowBuilder itemOutputs(StackSpec... outputs) {
            itemOutputs = List.of(outputs);
            return this;
        }

        private RowBuilder fluidInputs(NativeFluid... inputs) {
            fluidInputs = List.of(inputs);
            return this;
        }

        private RowBuilder fluidOutputs(NativeFluid... outputs) {
            fluidOutputs = List.of(outputs);
            return this;
        }

        private Row build() {
            return new Row(target, source, energyPerTick, duration, displayOnly, itemInputs, itemOutputs,
                    fluidInputs, fluidOutputs);
        }
    }

    private static final class MissingContent extends RuntimeException {
        private MissingContent(String message) {
            super(message);
        }
    }
}
