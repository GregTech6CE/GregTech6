package com.gregtech.gregtech.content.book;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GT6's <b>Material Dictionary</b> ({@code MultiItemBooks:59-60}, metas 32002/32003; loot variant
 * {@code 32766} "Dusty Material Dictionary").
 *
 * <p>GT6 builds one of these per material and stores it on the material
 * ({@code OreDictMaterial.mDictionaryBook}, created by {@code UT.java:762} as
 * {@code ST.book("Material_Dictionary_<name>")}); the pages come from
 * {@code UT.Books.addMaterialDictionary}, which walks the material's data: chemical components,
 * alloy recipes it takes part in, byproducts, tool stats, the enchantments its tools accept, its
 * properties, its machine and ore flags, and the materials that smelt/solidify/burn/pulverize into
 * it. The loot table {@code gt.matdicts} hands out one row per material
 * ({@code Loader_Loot:359-364}) and the Dusty Material Dictionary rolls that table
 * ({@code MultiItemBooks:68} + {@code Behavior_Drop_Loot}).</p>
 *
 * <p>The port generates the same kind of page from its own material data. This is the first slice —
 * identity, composition, the forms the material actually has, tool stats, properties and ore/byproduct
 * links; GT6's alloy/enchantment/target matrices are the documented remainder (see §31 of the porting
 * notes). Pages are plain strings inside a vanilla written book, exactly like {@link GTBooks}, so the
 * player opens them with the vanilla book screen.</p>
 */
public final class GTMaterialDictionary {

    /** GT6's title mapping prefix: {@code Material_Dictionary_<mNameInternal>}. */
    public static final String MAPPING_PREFIX = "Material_Dictionary_";

    /** Author GT6 writes into every book ({@code Loader_Books}). */
    private static final String AUTHOR = "Gregorius Techneticies";

    /** GT6 drops pages of 256 characters or more; the port keeps the same rule. */
    private static final int MAX_PAGE_LENGTH = 256;

    private GTMaterialDictionary() {}

    /** The material a dictionary stack belongs to, or null. */
    public static GTMaterial materialOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        String mapping = tag.contains("gt.material") ? tag.getString("gt.material")
                : tag.contains("book") ? tag.getString("book") : "";
        if (mapping.startsWith(MAPPING_PREFIX)) mapping = mapping.substring(MAPPING_PREFIX.length());
        return mapping.isEmpty() ? null : GTMaterialRegistry.get(mapping);
    }

    /** GT6's mapping name for a material, e.g. {@code Material_Dictionary_Iron}. */
    public static String mapping(GTMaterial material) {
        return MAPPING_PREFIX + material.getName();
    }

    /** The dictionary of a material as a written book, or an empty stack when it has no pages. */
    public static ItemStack bookStack(GTMaterial material) {
        if (material == null || !material.isValid()) return ItemStack.EMPTY;
        List<String> pages = pages(material);
        if (pages.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", MaterialPresentation.name(material) + " - Material Dictionary");
        tag.putString("author", AUTHOR);
        tag.putString("book", mapping(material));
        tag.putString("gt.material", mapping(material));
        ListTag list = new ListTag();
        for (String page : pages) list.add(StringTag.valueOf(page));
        tag.put("pages", list);
        stack.setHoverName(Component.literal(MaterialPresentation.name(material) + " - Material Dictionary"));
        return stack;
    }

    /** The dictionary for a GT6 mapping name ({@code Material_Dictionary_Xxx}), or an empty stack. */
    public static ItemStack bookStack(String mapping) {
        if (mapping == null || !mapping.startsWith(MAPPING_PREFIX)) return ItemStack.EMPTY;
        return bookStack(GTMaterialRegistry.get(mapping.substring(MAPPING_PREFIX.length())));
    }

    /** Every material that has a dictionary, in registry order. */
    public static List<GTMaterial> materials() {
        List<GTMaterial> out = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.isValid() && !pages(material).isEmpty()) out.add(material);
        }
        return out;
    }

    // ── pages ─────────────────────────────────────────────────────────────

    /** The pages of one material's dictionary, in GT6's order: identity, composition, alloys, rest. */
    public static List<String> pages(GTMaterial material) {
        List<String> pages = new ArrayList<>();
        add(pages, identity(material));
        add(pages, composition(material));
        add(pages, forms(material));
        add(pages, toolStats(material));
        add(pages, properties(material));
        add(pages, alloys(material));
        add(pages, enchantments(material));
        add(pages, ore(material));
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
    private static String forms(GTMaterial material) {
        List<String> names = new ArrayList<>();
        for (MaterialPrefix form : com.gregtech.gregtech.api.prefix.PrefixRegistry.all()) {
            if (!form.isValidFor(material)) continue;
            if (GTItems.getStack(form, material, 1).isEmpty()) continue;
            names.add(form.getName());
        }
        if (names.isEmpty()) return null;
        StringBuilder out = new StringBuilder("Forms available (" + names.size() + "):\n");
        for (int i = 0; i < names.size(); i++) {
            out.append(names.get(i));
            out.append((i + 1) % 3 == 0 ? '\n' : ", ");
        }
        return out.toString();
    }

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
    private static String ore(GTMaterial material) {
        // The port's ore blocks are blocks (BlockMaterialPrefix.ore), the raw ore is an item.
        ItemStack ore = com.gregtech.gregtech.registry.GTBlocks.getStack(
                com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.ore, material);
        ItemStack raw = GTItems.getStack(MaterialPrefix.oreRaw, material, 1);
        if (ore.isEmpty() && raw.isEmpty()) return null;
        StringBuilder out = new StringBuilder();
        if (!ore.isEmpty()) out.append("This material generates ore.\n");
        if (!raw.isEmpty()) out.append("Raw ore: ").append(raw.getHoverName().getString()).append('\n');
        if (!material.getByProducts().isEmpty()) {
            out.append("Byproducts: ");
            int i = 0;
            for (GTMaterial byproduct : material.getByProducts()) {
                out.append(i++ > 0 ? ", " : "").append(byproduct.getName());
            }
        }
        return out.toString();
    }

    private static String state(GTMaterial material) {
        if (material.has(MaterialProperty.GAS)) return "gas";
        if (material.has(MaterialProperty.LIQUID)) return "liquid";
        return "solid";
    }
}
