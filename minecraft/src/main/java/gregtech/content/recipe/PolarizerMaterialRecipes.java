package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialGroups;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** GT6 Loader_Recipes_Handlers:655,658,712-722 and RecipeMapHandlerMaterial.
 * Only registered input/output pairs of the same source prefix are converted.
 */
public final class PolarizerMaterialRecipes {
    public record Entry(String prefix, GTMaterial input, GTMaterial output, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean registered;

    // OP.java's positive-weight prefixes satisfying conditionP. An explicit source set avoids
    // accidentally admitting a port-only prefix or treating ores, dust, plants, crates or hot ingots
    // as ordinary parts. The source casingSmall spelling is resolved by PrefixRegistry.
    private static final Set<String> SOURCE_PREFIXES = Set.of(
            "ingotQuintuple",
            "ingotQuadruple",
            "ingotTriple",
            "ingotDouble",
            "ingot",
            "billet",
            "chunkGt",
            "nugget",
            "gem",
            "gemChipped",
            "gemFlawed",
            "gemFlawless",
            "gemExquisite",
            "gemLegendary",
            "gemOre",
            "gemRaw",
            "gemUncut",
            "gemPolished",
            "bouleGt",
            "crystalPure",
            "crystal",
            "lens",
            "plateSteamcraft",
            "plateDense",
            "plateQuintuple",
            "plateQuadruple",
            "plateTriple",
            "plateDouble",
            "plate",
            "plateGem",
            "plateTiny",
            "plateGemTiny",
            "plateCurved",
            "compressed",
            "sheetGt",
            "foil",
            "stick",
            "stickLong",
            "bolt",
            "screw",
            "round",
            "ring",
            "chain",
            "spring",
            "springSmall",
            "wireFine",
            "minecartWheels",
            "gearGt",
            "gearGtSmall",
            "railGt",
            "casingSmall",
            "casingMachine",
            "casingMachineDouble",
            "casingMachineQuadruple",
            "casingMachineDense",
            "rotor",
            "toolHeadSaw",
            "toolHeadFile",
            "toolHeadChisel",
            "toolHeadBuzzSaw",
            "toolHeadChainsaw",
            "toolHeadWrench",
            "toolHeadDrill",
            "toolHeadSword",
            "toolHeadPickaxe",
            "toolHeadShovel",
            "toolHeadSpade",
            "toolHeadAxe",
            "toolHeadHoe",
            "toolHeadSense",
            "toolHeadPlow",
            "toolHeadHammer",
            "toolHeadScrewdriver",
            "toolHeadBuilderwand",
            "toolHeadConstructionPickaxe",
            "toolHeadPickaxeGem",
            "toolHeadAxeDouble",
            "toolHeadUniversalSpade",
            "toolHeadArrow",
            "toolHeadRawSaw",
            "toolHeadRawChisel",
            "toolHeadRawSword",
            "toolHeadRawPickaxe",
            "toolHeadRawShovel",
            "toolHeadRawSpade",
            "toolHeadRawUniversalSpade",
            "toolHeadRawAxe",
            "toolHeadRawAxeDouble",
            "toolHeadRawHoe",
            "toolHeadRawSense",
            "toolHeadRawPlow",
            "toolHeadRawArrow",
            "toolSword",
            "toolPickaxe",
            "toolShovel",
            "toolAxe",
            "toolHoe",
            "toolShears",
            "bulletGtSmall",
            "bulletGtMedium",
            "bulletGtLarge",
            "arrowGtWood",
            "arrowGtPlastic",
            "armorHelmet",
            "armorChestplate",
            "armorLeggings",
            "armorBoots",
            "frameGt",
            "capcellcon",
            "pipeTiny",
            "pipeSmall",
            "pipeMedium",
            "pipeLarge",
            "pipeHuge",
            "pipeQuadruple",
            "pipeNonuple",
            "pipeRestrictiveTiny",
            "pipeRestrictiveSmall",
            "pipeRestrictiveMedium",
            "pipeRestrictiveLarge",
            "pipeRestrictiveHuge",
            "wireGt16",
            "wireGt15",
            "wireGt14",
            "wireGt13",
            "wireGt12",
            "wireGt11",
            "wireGt10",
            "wireGt09",
            "wireGt08",
            "wireGt07",
            "wireGt06",
            "wireGt05",
            "wireGt04",
            "wireGt03",
            "wireGt02",
            "wireGt01",
            "cableGt12",
            "cableGt08",
            "cableGt04",
            "cableGt02",
            "cableGt01",
            "blockGem",
            "blockIngot",
            "blockPlate",
            "blockPlateGem",
            "blockSolid");

    private PolarizerMaterialRecipes() {}

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static boolean allowsPrefix(String name) {
        return SOURCE_PREFIXES.contains(PrefixRegistry.sourceName(name));
    }

    public static List<Recipe> register() {
        if (registered) throw new IllegalStateException("Polarizer material recipes registered twice");
        registered = true;
        addMaterial(GTMaterialRegistry.get("Neodymium"), GTMaterialRegistry.get("NeodymiumMagnetic"), 128);
        addGroup(
                MaterialGroups.Fe,
                "IronMagnetic",
                "Iron",
                "WroughtIron",
                "IronCast",
                "IronCompressed",
                "PigIron",
                "MeteoricIron",
                "Meteorite",
                "Enori");
        addGroup(MaterialGroups.Steel, "SteelMagnetic", "Steel", "Knightmetal", "MeteoricSteel");
        return ENTRIES.stream().map(Entry::recipe).toList();
    }

    private static void addGroup(GTMaterial group, String outputName, String... sourceMembers) {
        Set<GTMaterial> materials = new LinkedHashSet<>();
        for (GTMaterial material : group.getReRegistrations())
            if (material != null && material.isValid()) materials.add(material.resolve());
        // ANY.java:118/120 includes Enori; use its registered form if available, never invent an item.
        for (String name : sourceMembers) {
            GTMaterial material = GTMaterialRegistry.get(name);
            if (material != null && material.isValid()) materials.add(material.resolve());
        }
        GTMaterial output = GTMaterialRegistry.get(outputName);
        for (GTMaterial input : materials) addMaterial(input, output, 16);
    }

    private static void addMaterial(GTMaterial inputMaterial, GTMaterial outputMaterial, long power) {
        if (inputMaterial == null || outputMaterial == null || !inputMaterial.isValid() || !outputMaterial.isValid())
            return;
        GTMaterial input = inputMaterial.resolve(), output = outputMaterial.resolve();
        for (var prefix : PrefixRegistry.all()) {
            if (!allowsPrefix(prefix.getName())) continue;
            add(
                    prefix.getName(),
                    prefix.getMaterialWeight(),
                    input,
                    output,
                    power,
                    GTItems.getStack(prefix, input, 1),
                    GTItems.getStack(prefix, output, 1));
        }
        // Storage blocks and machine casings are separate registries in the port, but ordinary
        // prefixes in GT6. Crates and powder/ore blocks are deliberately not in SOURCE_PREFIXES.
        for (var prefix : BlockPrefixRegistry.all()) {
            if (!allowsPrefix(prefix.getName())) continue;
            add(
                    prefix.getName(),
                    prefix.getMaterialWeight(),
                    input,
                    output,
                    power,
                    GTBlocks.getStack(prefix, input),
                    GTBlocks.getStack(prefix, output));
        }
    }

    private static void add(
            String prefix,
            long amount,
            GTMaterial inputMaterial,
            GTMaterial outputMaterial,
            long power,
            ItemStack input,
            ItemStack output) {
        if (amount <= 0 || input.isEmpty() || output.isEmpty()) return;
        long ticks = Math.max(1, (amount * 144L + GTValues.U - 1) / GTValues.U);
        Recipe recipe =
                new Recipe(new ItemStack[] {input}, new ItemStack[] {output}, null, null, null, null, ticks, power, 0);
        if (MachineRecipeMaps.Polarizer.addRecipe(recipe) == recipe)
            ENTRIES.add(new Entry(prefix, inputMaterial, outputMaterial, recipe));
    }
}
