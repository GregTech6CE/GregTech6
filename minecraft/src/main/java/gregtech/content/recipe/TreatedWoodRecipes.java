package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.wood.WoodSpecies;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTWoods;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * GT6's untreated-wood Bath handler and plank ore-dictionary recipes:
 * {@code Loader_Recipes_Handlers.java:660-667} and {@code Loader_Recipes_OreDict.java:257-263}.
 * The {@code plate/WoodTreated} material form resolves to the placeable
 * {@code planks_treated} block, as in GT6 {@code Loader_Woods.java:85-87}.
 *
 * <p>GT6 registers one {@code RecipeMapHandlerMaterial} per untreated wood material and maps every
 * prefix that has an item for both the wood and the treated/polished material. The oil amount and
 * the duration scale with the prefix's material amount ({@code 100 mB x amount / U} and
 * {@code 144 ticks x amount / U}, both rounded up); the polished oils use 1000 mB (fish) and 500 mB
 * (whale) per unit.</p>
 */
public final class TreatedWoodRecipes extends OriginalRecipeBatch {
    public static final TreatedWoodRecipes INSTANCE = new TreatedWoodRecipes();
    private static final String[] VANILLA_PLANKS = {
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"
    };
    /** Loader_Recipes_Handlers:661-667. */
    private static final String[] TREATED_OILS = {
        "Oil_Seed", "Oil_Lin", "Oil_Hemp", "Oil_Nut", "Oil_Olive", "Oil_Sunflower", "Oil_Creosote"
    };
    /** Loader_Recipes_Handlers:668-669 with their per-unit amounts. */
    private static final String[] POLISHED_OILS = {"Oil_Fish", "Oil_Whale"};

    private static final int[] POLISHED_UNIT_MB = {1000, 500};
    private static final long TREATED_UNIT_MB = 100;
    private static final long TREATMENT_TICKS = 144;
    /**
     * The forms GT6's Bath handler actually converts: its prefix condition excludes unused, plant,
     * container, dust, ore, ore-processing, scrap and hot-ingot forms, leaving these item prefixes
     * (verified against the original dump's 44 input items; the remaining two are multi-tile pipes
     * and the unified vanilla stick, which are not prefix items here).
     */
    private static final String[] WOOD_FORM_PREFIXES = {
        "arrowGtPlastic",
        "arrowGtWood",
        "bolt",
        "bulletGtLarge",
        "bulletGtMedium",
        "bulletGtSmall",
        "itemCasing",
        "chain",
        "foil",
        "gearGt",
        "gearGtSmall",
        "plateCurved",
        "plateTiny",
        "ring",
        "rotor",
        "round",
        "scrapGt",
        "screw",
        "spring",
        "springSmall",
        "stick",
        "stickLong",
        "blockPlate",
        "toolHeadArrow",
        "toolHeadAxe",
        "toolHeadHammer",
        "toolHeadHoe",
        "toolHeadPickaxe",
        "toolHeadPlow",
        "toolHeadRawArrow",
        "toolHeadRawAxe",
        "toolHeadRawHoe",
        "toolHeadRawPickaxe",
        "toolHeadRawPlow",
        "toolHeadRawSense",
        "toolHeadRawShovel",
        "toolHeadRawSpade",
        "toolHeadRawSword",
        "toolHeadSense",
        "toolHeadShovel",
        "toolHeadSpade",
        "toolHeadSword",
        // GT6 keeps these as multi-tile pipe items rather than prefix items; the port's pipe
        // prefixes are block items, which the same handler also treats.
        "pipeTiny",
        "pipeSmall",
        "pipeMedium",
        "pipeLarge",
        "pipeHuge",
        "pipeQuadruple",
        "pipeNonuple"
    };
    /** Wood-property materials that GT6's {@code ANY.WoodUntreated} does not contain. */
    private static final Set<String> NOT_UNTREATED_WOOD = Set.of("Marshmallow", "PetrifiedWood");

    private TreatedWoodRecipes() {}

    public static int register() {
        for (String wood : VANILLA_PLANKS) {
            INSTANCE.treat(
                    "planks/vanilla/" + wood,
                    item("minecraft:" + wood + "_planks", 1),
                    mat(MaterialPrefix.plate, "WoodTreated", 1));
        }
        for (WoodSpecies species : WoodSpecies.values()) {
            INSTANCE.treat(
                    "planks/gregtech/" + species.id(),
                    new ItemStack(GTWoods.planks(species)),
                    mat(MaterialPrefix.plate, "WoodTreated", 1));
        }
        INSTANCE.materialBath();
        return INSTANCE.entries().size();
    }

    /** Every untreated wood form GT6's Bath handler converts, scaled by the prefix's material amount. */
    private void materialBath() {
        GTMaterial treated = GTMaterialRegistry.get("WoodTreated").resolve();
        GTMaterial polished = GTMaterialRegistry.get("WoodPolished").resolve();
        // GT6 stamps one recipe per (input stack, fluid); shared items such as vanilla sticks are
        // reached through several wood identities and must not repeat.
        Set<String> used = new HashSet<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid() || !material.has(MaterialProperty.WOOD)) continue;
            if (material == treated || material == polished) continue;
            if (NOT_UNTREATED_WOOD.contains(material.getName())) continue;
            for (String form : WOOD_FORM_PREFIXES) {
                MaterialPrefix prefix = PrefixRegistry.byName(form);
                if (prefix != null)
                    bathForm(
                            used,
                            material,
                            form,
                            prefix.getMaterialWeight(),
                            GTItems.getStack(prefix, material, 1),
                            GTItems.getStack(prefix, treated, 1),
                            GTItems.getStack(prefix, polished, 1));
                BlockMaterialPrefix blockPrefix = BlockPrefixRegistry.byName(form);
                if (blockPrefix != null)
                    bathForm(
                            used,
                            material,
                            form,
                            blockPrefix.getMaterialWeight(),
                            GTBlocks.getStack(blockPrefix, material),
                            GTBlocks.getStack(blockPrefix, treated),
                            GTBlocks.getStack(blockPrefix, polished));
            }
        }
    }

    /** One prefix for one material: treated rows for the seven oils, polished rows for fish/whale. */
    private void bathForm(
            Set<String> used,
            GTMaterial material,
            String form,
            long weight,
            ItemStack input,
            ItemStack treatedStack,
            ItemStack polishedStack) {
        if (weight <= 0 || input.isEmpty()) return;
        long ticks = Math.max(1, (TREATMENT_TICKS * weight + GTValues.U - 1) / GTValues.U);
        if (!treatedStack.isEmpty()) {
            int mb = (int) Math.max(1, (TREATED_UNIT_MB * weight + GTValues.U - 1) / GTValues.U);
            for (String oil : TREATED_OILS)
                if (used.add(itemKey(input) + "|" + oil))
                    add(
                            "treated/" + material.getName() + "/" + form + "/" + oil,
                            MachineRecipeMaps.Bath,
                            ticks,
                            0,
                            items(input.copy()),
                            items(treatedStack.copy()),
                            fluids(fluid(oil, mb)),
                            null);
        }
        if (!polishedStack.isEmpty()) {
            for (int i = 0; i < POLISHED_OILS.length; i++) {
                int mb = (int) Math.max(1, (POLISHED_UNIT_MB[i] * weight + GTValues.U - 1) / GTValues.U);
                if (used.add(itemKey(input) + "|" + POLISHED_OILS[i]))
                    add(
                            "polished/" + material.getName() + "/" + form + "/" + POLISHED_OILS[i],
                            MachineRecipeMaps.Bath,
                            ticks,
                            0,
                            items(input.copy()),
                            items(polishedStack.copy()),
                            fluids(fluid(POLISHED_OILS[i], mb)),
                            null);
            }
        }
    }

    private static String itemKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "#" + stack.getCount();
    }

    private void treat(String source, ItemStack input, ItemStack output) {
        long weight = MaterialPrefix.plate.getMaterialWeight();
        int mb = (int) Math.max(1, (TREATED_UNIT_MB * weight + GTValues.U - 1) / GTValues.U);
        long ticks = Math.max(1, (TREATMENT_TICKS * weight + GTValues.U - 1) / GTValues.U);
        for (String oil : TREATED_OILS) {
            add(
                    "treated/" + source + "/" + oil,
                    MachineRecipeMaps.Bath,
                    ticks,
                    0,
                    items(input.copy()),
                    items(output.copy()),
                    fluids(fluid(oil, mb)),
                    null);
        }
    }

    public static int expectedRows() {
        return (VANILLA_PLANKS.length + WoodSpecies.values().length) * TREATED_OILS.length;
    }
}
