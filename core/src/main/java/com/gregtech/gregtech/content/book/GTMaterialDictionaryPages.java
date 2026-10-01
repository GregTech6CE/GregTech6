package com.gregtech.gregtech.content.book;
import com.gregtech.gregtech.api.material.*;import java.util.*;
/** Original material dictionary prose and processing/alloy/enchantment pages shared by both versions. */
public final class GTMaterialDictionaryPages {private GTMaterialDictionaryPages(){}private static final int MAX_PAGE_LENGTH=256;
    /** The pages of one material's dictionary, in GT6's order: identity, composition, alloys, rest. */
    public static List<String> pages(GTMaterial material,java.util.function.Function<GTMaterial,String> formText,java.util.function.Function<GTMaterial,String> oreText) {
        List<String> pages = new ArrayList<>();
        add(pages, identity(material));
        add(pages, composition(material));
        add(pages, formText.apply(material));
        add(pages, toolStats(material));
        add(pages, properties(material));
        add(pages, alloys(material));
        add(pages, enchantments(material));
        add(pages, oreText.apply(material));
        add(pages, processingDataSolidifying(material));
        add(pages, processingDataForming(material));
        add(pages, processingDataWorking(material));
        return List.copyOf(pages);
    }

    /** GT6's "Processing Data" header ({@code UT.Books.addMaterialDictionary:900}). */
    private static final String PROCESSING_HEADER = "Processing Data\n===================\n";

    /**
     * GT6's three "Processing Data" pages ({@code UT.Books.addMaterialDictionary:900-905}), each with
     * its own header: smelting/solidifying/burning/pulverising/crushing, then
     * bending/compressing/cutting/forging/smashing, then working.
     */
    private static final String[] PROCESSING_PAGE_1 = {"Smelting", "Solidifying", "Burning",
            "Pulverising", "Crushing"};
    private static final String[] PROCESSING_PAGE_2 = {"Bending", "Compressing", "Cutting",
            "Forging", "Smashing"};
    private static final String[] PROCESSING_PAGE_3 = {"Working"};

    /**
     * GT6's first "Processing Data" page ({@code UT.Books.addMaterialDictionary:900-905}): what the
     * material becomes when it is smelted, solidified, burnt, pulverised or crushed, in GT6's own
     * amount format ({@code whole.thousandths} plus the target's name).
     *
     * <p>Four of the five come from the material itself ({@code mTargetSmelting}, {@code mTargetBurning},
     * {@code mTargetPulver}, {@code mTargetCrushing}); GT6's {@code mTargetSolidifying} is one of the
     * seven targets the port keeps in {@link MaterialProcessingTargets} instead of on the material, so
     * that line is read from there.</p>
     */
    private static String processingDataSolidifying(GTMaterial material) {
        return processingPage(material, PROCESSING_PAGE_1);
    }

    /** GT6's second "Processing Data" page ({@code :902-904}): all five targets come from the table. */
    private static String processingDataForming(GTMaterial material) {
        return processingPage(material, PROCESSING_PAGE_2);
    }

    /** GT6's third "Processing Data" page ({@code :905}): the single working target. */
    private static String processingDataWorking(GTMaterial material) {
        return processingPage(material, PROCESSING_PAGE_3);
    }

    /** One "Processing Data" page: GT6's header, then one labelled amount line per kind. */
    private static String processingPage(GTMaterial material, String[] kinds) {
        StringBuilder out = new StringBuilder(PROCESSING_HEADER);
        for (String kind : kinds) processingLine(out, kind, material);
        return out.toString();
    }

    /**
     * One labelled line of a processing page. GT6 keeps eleven targets on the material
     * ({@code OreDictMaterial:284-295}); the port models four of them there and the other seven in
     * {@link com.gregtech.gregtech.data.generated.MaterialProcessingTargets}, whose rows are generated
     * from {@code MT.java} and whose default is GT6's own ({@code OreDictMaterial:287-294}: the material
     * itself, one unit).
     */
    private static void processingLine(StringBuilder out, String label, GTMaterial material) {
        out.append(label).append(":\n");
        switch (label) {
            case "Smelting" -> processingValue(out, material.getTargetSmeltingMaterial(),
                    material.getTargetSmeltingAmount(), material);
            case "Burning" -> processingValue(out, material.getTargetBurningMaterial(),
                    material.getTargetBurningAmount(), material);
            case "Pulverising" -> processingValue(out, material.getTargetPulverMaterial(),
                    material.getTargetPulverAmount(), material);
            case "Crushing" -> processingValue(out, material.getTargetCrushingMaterial(),
                    material.getTargetCrushingAmount(), material);
            default -> processingTableValue(out, label, material);
        }
    }

    /** The value line of one of the seven targets the port holds in {@link MaterialProcessingTargets}. */
    private static void processingTableValue(StringBuilder out, String label, GTMaterial material) {
        String[] row = com.gregtech.gregtech.data.generated.MaterialProcessingTargets.of(
                material.getName(), label);
        if (row == null) {
            // GT6's default for those seven: the material itself, one unit.
            processingValue(out, material,
                    com.gregtech.gregtech.data.generated.MaterialProcessingTargets.DEFAULT_AMOUNT,
                    material);
            return;
        }
        long amount = Long.parseLong(row[1]);
        GTMaterial target = material;
        if (!row[0].isEmpty()) {
            String name = com.gregtech.gregtech.loaders.c.GTMaterialFields.materialOf(row[0]);
            target = name == null ? null : GTMaterialRegistry.get(name);
        }
        processingValue(out, target, amount, material);
    }

    /** GT6's {@code <whole>.<thousandths> <name>}, with its "nothing" and "itself" wording. */
    private static void processingValue(StringBuilder out, GTMaterial target, long amount,
                                        GTMaterial self) {
        if (target == null || !target.isValid() || amount <= 0) {
            out.append("0.000 nothing\n");
            return;
        }
        long units = amount / GTValues.U;
        long thousandths = (long) (((double) (amount % GTValues.U) / (double) GTValues.U) * 1000.0D);
        out.append(units).append('.').append(String.format(Locale.ROOT, "%03d", thousandths)).append(' ')
                .append(target == self ? "itself" : target.getName()).append('\n');
    }

    /**
     * GT6's enchantment page (§55): what a tool, weapon, ammo, ranged weapon, fishing rod or piece
     * of armour made of this material can be enchanted with, from {@link GTMaterialEnchants}.
     */
    private static String enchantments(GTMaterial material) {
        java.util.Map<String, java.util.List<String>> enchants =
                GTMaterialEnchants.enchantmentsOf(material);
        if (enchants.isEmpty()) return null;
        StringBuilder out = new StringBuilder();
        out.append("Enchantments of ").append(material.getName()).append('\n');
        for (String kind : GTMaterialEnchants.KINDS) {
            java.util.List<String> values = enchants.get(kind);
            if (values == null || values.isEmpty()) continue;
            out.append("\n").append(kind).append(": ").append(String.join(", ", values)).append('\n');
        }
        return out.toString();
    }

    /**
     * GT6's alloy page (§54): which alloys this material takes part in and which ones it is made
     * from, taken from the port's own alloying table ({@link GTMaterialRecipes}).
     */
    private static String alloys(GTMaterial material) {
        List<String> into = GTMaterialRecipes.alloyedInto(material);
        List<String> from = GTMaterialRecipes.madeFrom(material);
        if (into.isEmpty() && from.isEmpty()) return null;
        StringBuilder out = new StringBuilder();
        out.append("Alloys of ").append(material.getName()).append('\n');
        if (!from.isEmpty()) {
            out.append("\nIs an alloy of:\n");
            for (String line : from) out.append("  ").append(line).append('\n');
        }
        if (!into.isEmpty()) {
            out.append("\nAlloys into:\n");
            for (String line : into) out.append("  ").append(line).append('\n');
        }
        out.append("\n(Amounts are material units, 1 unit = 144 mB.)");
        return out.toString();
    }

    private static void add(List<String> pages, String page) {
        if (page == null || page.isBlank()) return;
        pages.add(page.length() < MAX_PAGE_LENGTH ? page : page.substring(0, MAX_PAGE_LENGTH - 1));
    }

    /** GT6's first page: what the material is. */
    private static String identity(GTMaterial material) {
        StringBuilder out = new StringBuilder();
        out.append("Material: ").append(material.getName()).append('\n');
        if (material.getLocalName() != null && !material.getLocalName().isBlank()
                && !material.getLocalName().equals(material.getName())) {
            out.append("Also known as: ").append(material.getLocalName()).append('\n');
        }
        out.append("Melting point: ").append(material.getMeltingPoint()).append(" K\n");
        out.append("Density: ").append(material.getDensity()).append(" kg/m3\n");
        out.append("Mass: ").append(material.getMass()).append('\n');
        out.append("State: ").append(state(material));
        return out.toString();
    }

    /** GT6's {@code mComponents} page: what the material is made of (or its element). */
    private static String composition(GTMaterial material) {
        if (!material.hasComposition()) {
            return "Element: " + material.getName()
                    + "\nProtons: " + material.getProtons()
                    + "\nNeutrons: " + material.getNeutrons()
                    + "\nMass: " + material.getMass();
        }
        return "Compound, made of:\n" + material.getTooltipChemical()
                + "\n\nGrind it in a Mortar or a Macerator,\nor dissolve it to separate\nthe elements.";
    }

    /** The forms the material actually has in this port, like GT6's "what can be made of it". */


    /** GT6's tool page: the quality and durability of the tools made of this material. */
    private static String toolStats(GTMaterial material) {
        if (!material.hasToolStats()) return null;
        return "Tools:\nQuality: " + material.getToolQuality()
                + "\nDurability: " + material.getToolDurability()
                + "\nSpeed: " + material.getToolSpeed()
                + "\nTool types: " + material.getToolTypes();
    }

    /** GT6's property page: the processing/behaviour flags the material carries. */
    private static String properties(GTMaterial material) {
        List<String> flags = new ArrayList<>();
        for (MaterialProperty property : MaterialProperty.values()) {
            if (property == MaterialProperty.HIDDEN) continue;
            if (material.has(property)) flags.add(property.name().toLowerCase(Locale.ROOT));
        }
        if (flags.isEmpty()) return null;
        StringBuilder out = new StringBuilder("Properties:\n");
        for (int i = 0; i < flags.size(); i++) {
            out.append(flags.get(i));
            out.append((i + 1) % 3 == 0 ? '\n' : ", ");
        }
        return out.toString();
    }

    /** GT6's ore page: whether the material generates ore and what comes out of it. */


    private static String state(GTMaterial material) {
        if (material.has(MaterialProperty.GAS)) return "gas";
        if (material.has(MaterialProperty.LIQUID)) return "liquid";
        return "solid";
    }
}
