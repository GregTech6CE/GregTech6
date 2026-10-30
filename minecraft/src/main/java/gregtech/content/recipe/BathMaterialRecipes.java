package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialGroups;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * GT6's Bath conversions that are not tied to untreated wood:
 *
 * <ul>
 *   <li>{@code Loader_Recipes_Other.java:201-214} turns wood, bark and paper dust into paper in
 *       the four water types.</li>
 *   <li>{@code Loader_Recipes_Handlers.java:696-738} converts iron/steel/diamond families with
 *       fiery tears, fiery blood, molten metals, Midasium and Holy Water.</li>
 * </ul>
 *
 * <p>Both use the same {@code RecipeMapHandlerMaterial} rules: one row per prefix that passes the
 * handler condition and has an item for the input and output material. Fluid amount and duration
 * scale with the prefix's material amount (fluid {@code perUnit * amount / U}, duration
 * {@code 144 * amount / U}, rounded up). The prefix condition is identical to the Polarizer
 * handlers', so its reviewed source-prefix set is reused rather than re-listed.</p>
 */
public final class BathMaterialRecipes extends OriginalRecipeBatch {
    public static final BathMaterialRecipes INSTANCE = new BathMaterialRecipes();

    /** FL.waters(125, 100): water, mineral water and spectral dew at 125, distilled water at 100. */
    private static final String[] WATER_REGISTRY_NAMES = {
        "water", "potion.mineralwater", "ic2distilledwater", "spectral_dew"
    };
    private static final int[] WATER_AMOUNTS = {125, 125, 100, 125};
    private static final String[] DUST_FORMS = {"dust", "dustSmall", "dustTiny"};
    private static final int[] DUST_COUNTS = {1, 4, 9};
    private static final long PAPER_TICKS = 16;
    /** Wood-property materials the source ANY.Wood does not contain. */
    private static final Set<String> NOT_ANY_WOOD_DUST = Set.of("PetrifiedWood");
    private static final long CONVERSION_TICKS = 144;

    private final Set<String> used = new HashSet<>();

    private BathMaterialRecipes() {}

    public static int register() {
        INSTANCE.paper();
        INSTANCE.conversions();
        return INSTANCE.entries().size();
    }

    private void paper() {
        Set<GTMaterial> sources = new LinkedHashSet<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            GTMaterial resolved = material.resolve();
            if (!resolved.isValid() || !resolved.has(MaterialProperty.WOOD)) continue;
            if (NOT_ANY_WOOD_DUST.contains(resolved.getName())) continue;
            sources.add(resolved);
        }
        for (String name : new String[] {"Bark", "Paper"}) {
            GTMaterial material = GTMaterialRegistry.get(name);
            if (material.isValid()) sources.add(material);
        }
        ItemStack paper = new ItemStack(Items.PAPER, 1);
        for (GTMaterial material : sources) {
            for (int form = 0; form < DUST_FORMS.length; form++) {
                MaterialPrefix prefix = PrefixRegistry.byName(DUST_FORMS[form]);
                if (prefix == null) continue;
                ItemStack input = GTItems.getStack(prefix, material, DUST_COUNTS[form]);
                if (input.isEmpty()) continue;
                for (int water = 0; water < WATER_REGISTRY_NAMES.length; water++) {
                    FluidStack fluid = GeneratedRecipeResolver.byRegistryName(
                            WATER_REGISTRY_NAMES[water], WATER_AMOUNTS[water]);
                    if (fluid == null || fluid.isEmpty()) continue;
                    add(
                            "paper/" + material.getName() + "/" + DUST_FORMS[form] + "/"
                                    + WATER_REGISTRY_NAMES[water],
                            MachineRecipeMaps.Bath,
                            PAPER_TICKS,
                            0,
                            items(input.copy()),
                            items(paper.copy()),
                            fluids(fluid),
                            null);
                }
            }
        }
    }

    private void conversions() {
        for (GTMaterial material : group(MaterialGroups.Fe, "Enori")) {
            convert(material, "GildedIron", "molten.gold", 16);
            if (!material.getName().equals("MeteoricSteel")) fiery(material, "FierySteel");
        }
        for (GTMaterial material : group(MaterialGroups.Steel)) {
            convert(material, "SteelGalvanized", "molten.zinc", 16);
            if (!material.getName().equals("MeteoricSteel")) fiery(material, "FierySteel");
        }
        for (GTMaterial material : group(MaterialGroups.Diamond))
            convert(material, "NetherizedDiamond", "molten.netherite", 36);

        fiery("Steeleaf", "Fireleaf");
        fiery("MeteoricSteel", "MeteoflameSteel");
        fiery("MeteoricBlackSteel", "MeteoflameBlackSteel");
        fiery("MeteoricBlueSteel", "MeteoflameBlueSteel");
        fiery("MeteoricRedSteel", "MeteoflameRedSteel");
        fiery("DamascusSteel", "FlamascusSteel");

        convert("Lead", "Gold", "molten.midasium", 18);
        convert("Oak", "Skyroot", "holywater", 50);
        convert("Amber", "GoldenAmber", "holywater", 250);
        convert("Tanzanite", "Zanite", "holywater", 250);
    }

    /**
     * The shared handler condition plus scrap and hot ingots: GT6's Bath dump contains both, even
     * though its {@code tConditionP} lists them as excluded.
     */
    private static boolean allowsPrefix(String name) {
        String source = PrefixRegistry.sourceName(name);
        return PolarizerMaterialRecipes.allowsPrefix(name)
                || source.equals("scrapGt")
                || source.equals("ingotHot");
    }

    private static Set<GTMaterial> group(GTMaterial group, String... extraMembers) {
        Set<GTMaterial> materials = new LinkedHashSet<>();
        for (GTMaterial material : group.getReRegistrations()) if (material != null && material.isValid()) materials.add(material.resolve());
        for (String name : extraMembers) {
            GTMaterial material = GTMaterialRegistry.get(name);
            if (material.isValid()) materials.add(material);
        }
        return materials;
    }

    private void fiery(GTMaterial input, String outputName) {
        convert(input, outputName, "fieryblood", 144);
        convert(input, outputName, "fierytears", 144);
    }

    private void fiery(String inputName, String outputName) {
        GTMaterial input = GTMaterialRegistry.get(inputName);
        if (input.isValid()) fiery(input, outputName);
    }

    private void convert(String inputName, String outputName, String fluidName, int fluidPerUnit) {
        GTMaterial input = GTMaterialRegistry.get(inputName);
        GTMaterial output = GTMaterialRegistry.get(outputName);
        if (input.isValid() && output.isValid()) convert(input, output, fluidName, fluidPerUnit);
    }

    private void convert(GTMaterial input, String outputName, String fluidName, int fluidPerUnit) {
        GTMaterial output = GTMaterialRegistry.get(outputName);
        if (output.isValid()) convert(input, output, fluidName, fluidPerUnit);
    }

    private void convert(GTMaterial input, GTMaterial output, String fluidName, int fluidPerUnit) {
        for (MaterialPrefix prefix : PrefixRegistry.all())
            addRow(
                    prefix.getName(),
                    prefix.getMaterialWeight(),
                    input,
                    output,
                    fluidName,
                    fluidPerUnit,
                    GTItems.getStack(prefix, input, 1),
                    GTItems.getStack(prefix, output, 1));
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all())
            addRow(
                    prefix.getName(),
                    prefix.getMaterialWeight(),
                    input,
                    output,
                    fluidName,
                    fluidPerUnit,
                    GTBlocks.getStack(prefix, input),
                    GTBlocks.getStack(prefix, output));
    }

    private void addRow(
            String prefix,
            long weight,
            GTMaterial input,
            GTMaterial output,
            String fluidName,
            int fluidPerUnit,
            ItemStack source,
            ItemStack target) {
        if (!allowsPrefix(prefix) || weight <= 0) return;
        if (source.isEmpty() || target.isEmpty()) return;
        // Unified source items can represent more than one material; the first row wins like the
        // other material handlers, and the duplicate must not abort registration.
        if (!used.add(source + "|" + target + "|" + fluidName)) return;
        long ticks = Math.max(1, (CONVERSION_TICKS * weight + GTValues.U - 1) / GTValues.U);
        long scaled = ((long) fluidPerUnit * weight + GTValues.U - 1) / GTValues.U;
        int mb = (int) Math.max(1, scaled);
        // Resolve the source registry name; a material's generated field can differ from the
        // GenMolten_<name> spelling, and the declared fluid is what the original row names.
        FluidStack fluid = GeneratedRecipeResolver.byRegistryName(fluidName, mb);
        if (fluid == null || fluid.isEmpty()) return;
        // The port unifies some block forms (blockIngot/blockSolid on minecraft:iron_block), so two
        // source rows can share one item input. GT6 keeps both and findRecipe answers with the first;
        // the port rejects the later row, which is the same effective behaviour.
        Recipe probe = new Recipe(
                items(MaterialUnification.canonical(source.copy())),
                items(target.copy()),
                null,
                null,
                fluids(fluid),
                null,
                ticks,
                0,
                0);
        if (MachineRecipeMaps.Bath.findCollision(probe) != null) return;
        add(
                "convert/" + input.getName() + "/" + output.getName() + "/" + prefix + "/" + fluidName,
                MachineRecipeMaps.Bath,
                ticks,
                0,
                items(source.copy()),
                items(target.copy()),
                fluids(fluid),
                null);
    }
}
