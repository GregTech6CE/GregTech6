/* Gregorius Techneticies / GregTech-6 Team source recipes, LGPL-3.0-or-later. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.data.*;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;

/** Explicit MultiItem rows do not pass through the material-prefix recipe listeners. */
public final class SingleItemSourceRecipes extends OriginalRecipeBatch {
    public static final SingleItemSourceRecipes INSTANCE = new SingleItemSourceRecipes();

    private SingleItemSourceRecipes() {}

    private static final List<String> SMELTING = new ArrayList<>();
    private static final Map<Item, FurnaceSmeltingRecipes.CookingData> COOKING = new IdentityHashMap<>();

    public static Optional<FurnaceSmeltingRecipes.CookingData> cooking(Item item) {
        return Optional.ofNullable(COOKING.get(item));
    }

    public static List<String> smeltingRows() {
        return List.copyOf(SMELTING);
    }

    public static int register() {
        if (!INSTANCE.entries().isEmpty())
            throw new IllegalStateException("Single-item source recipes registered twice");
        INSTANCE.compounds();
        ItemMaterialRegistry.register(
                own("dry_bark", 1).getItem(), null, GTMaterialRegistry.get("Bark"), GTValues.U / 2);
        INSTANCE.animalParts();
        INSTANCE.meat();
        INSTANCE.remains();
        INSTANCE.chum();
        return INSTANCE.entries().size() + SMELTING.size();
    }

    private void compounds() {
        for (var row : OriginalSingleItemCatalog.COMPOUNDS) {
            var input = own(row.item(), 1);
            var material = GTMaterialRegistry.get(row.material());
            // MultiItemRandomTools:63-73: each compound contains 1/9 U and fires into one nugget.
            ItemMaterialRegistry.register(input.getItem(), null, material, GTValues.U / 9);
            smelt(row.item(), mat(MaterialPrefix.nugget, row.material(), 1), false);
            var target = material.getTargetSmeltingMaterial();
            var output = gregtech.util.OM.ingotOrDust(target, material.getTargetSmeltingAmount() / 9);
            if (!output.isEmpty())
                MachineRecipeMaps.CrucibleSmelting.addFakeRecipe(
                        false, items(input), items(output), null, null, null, null, 0, 0, material.getMeltingPoint());
        }
    }

    private void animalParts() {
        // Loader_Recipes_Food:325-346: hooves, horns, antlers and ivory have the same two uses.
        for (String id : OriginalSingleItemCatalog.ANIMAL_PARTS) {
            smelt(id, own("sticky_goo", 1), true);
            add(
                    id + "/grind",
                    MachineRecipeMaps.Mortar,
                    16,
                    16,
                    items(own(id, 1)),
                    items(new ItemStack(Items.BONE_MEAL)),
                    null,
                    null);
            add(
                    id + "/shred",
                    MachineRecipeMaps.Shredder,
                    16,
                    16,
                    items(own(id, 1)),
                    items(new ItemStack(Items.BONE_MEAL)),
                    null,
                    null);
            var output = id.contains("hoof")
                    ? "hoof"
                    : id.contains("tusk") ? "tusk" : id.contains("antler") ? "deer_antler" : "horn";
            if (!id.equals(output))
                add(
                        id + "/generify",
                        MachineRecipeMaps.Generifier,
                        1,
                        0,
                        items(own(id, 1)),
                        items(own(output, 1)),
                        null,
                        null);
        }
    }

    private void meat() {
        // MultiItemFood:581 and Loader_Recipes_Food:350-365 preserve the 2U scrap-meat payload.
        ItemMaterialRegistry.register(
                own("scrap_meat", 1).getItem(), null, GTMaterialRegistry.get("MeatRaw"), 2 * GTValues.U);
        add(
                "scrap_meat/grind",
                MachineRecipeMaps.Mortar,
                16,
                16,
                items(own("scrap_meat", 1)),
                items(mat(MaterialPrefix.dust, "MeatRaw", 2)),
                null,
                null);
        add(
                "scrap_meat/rot",
                MachineRecipeMaps.Fermenter,
                288,
                16,
                items(own("scrap_meat", 1)),
                items(new ItemStack(Items.ROTTEN_FLESH)),
                null,
                null);
        // MultiItemFood:927-928 are the original ingot forms, not decorative food copies.
        for (String state : List.of("raw", "cooked")) {
            String material = state.equals("raw") ? "MeatRaw" : "MeatCooked";
            String id = state + "_meat_bar";
            ItemMaterialRegistry.register(
                    own(id, 1).getItem(), MaterialPrefix.ingot, GTMaterialRegistry.get(material), GTValues.U);
            add(
                    id + "/compress",
                    MachineRecipeMaps.Compressor,
                    16,
                    16,
                    items(mat(MaterialPrefix.dust, material, 1)),
                    items(own(id, 1)),
                    null,
                    null);
            add(
                    id + "/grind",
                    MachineRecipeMaps.Mortar,
                    16,
                    16,
                    items(own(id, 1)),
                    items(mat(MaterialPrefix.dust, material, 1)),
                    null,
                    null);
            add(
                    id + "/rot",
                    MachineRecipeMaps.Fermenter,
                    144,
                    16,
                    items(own(id, 1)),
                    items(mat(MaterialPrefix.ingot, "MeatRotten", 1)),
                    null,
                    null);
        }
        smelt("raw_meat_bar", own("cooked_meat_bar", 1), true);
        // Loader_Recipes_Food:97-103: all seven raw meat forms can be cooked.
        for (var prefix : List.of(
                MaterialPrefix.ingot,
                MaterialPrefix.billet,
                MaterialPrefix.chunkGt,
                MaterialPrefix.nugget,
                MaterialPrefix.dust,
                MaterialPrefix.dustSmall,
                MaterialPrefix.dustTiny)) {
            var input = GTItems.getStack(prefix, GTMaterialRegistry.get("MeatRaw"), 1);
            var output = GTItems.getStack(prefix, GTMaterialRegistry.get("MeatCooked"), 1);
            if (!input.isEmpty()
                    && !output.isEmpty()
                    && MachineRecipeMaps.add_smelting(input, output, false, true, false)) {
                SMELTING.add(prefix.getName() + "/MeatRaw");
                COOKING.put(input.getItem(), new FurnaceSmeltingRecipes.CookingData(0, true));
            }
        }
    }

    private void remains() {
        // Loader_Recipes_Crops:142-152 delegates all plant remains to RM.biomass(8).
        for (String id : OriginalSingleItemCatalog.PLANT_REMAINS) {
            Set<String> seen = new HashSet<>();
            biomass(id, "Rotten_Drink", 405, 16, seen);
            biomass(id, "Soup_Mushroom", 405, 16, seen);
            var fields = new TreeSet<>(RegisteredFluids.all().keySet());
            for (String field : fields) {
                var definition = RegisteredFluids.get(field);
                if (definition.hasFlag(RegisteredFluids.FluidFlags.WATER)) biomass(id, field, 135, 32, seen);
                else if (definition.hasFlag(RegisteredFluids.FluidFlags.MILK)) biomass(id, field, 270, 24, seen);
                else if (definition.hasFlag(RegisteredFluids.FluidFlags.JUICE)
                        || definition.hasFlag(RegisteredFluids.FluidFlags.HONEY)) biomass(id, field, 405, 24, seen);
            }
            biomass(id, "Honeydew", 405, 16, seen);
            biomass(id, "RoyalJelly", 1570, 16, seen);
            if (!id.equals("plant_remains"))
                add(
                        id + "/generify",
                        MachineRecipeMaps.Generifier,
                        1,
                        0,
                        items(own(id, 1)),
                        items(own("plant_remains", 1)),
                        null,
                        null);
        }
    }

    private void biomass(String id, String field, int output, int ticks, Set<String> seen) {
        var input = GTFluids.stack(field, 135);
        if (input == null || input.isEmpty()) throw new IllegalStateException("Missing source biomass fluid " + field);
        String key = BuiltInRegistries.FLUID.getKey(input.getFluid()).toString();
        if (!seen.add(key)) return;
        var result = GTFluids.stack("BiomassIC2", output);
        if (result == null || result.isEmpty()) throw new IllegalStateException("Missing source biomass output");
        add(
                id + "/biomass/" + field,
                MachineRecipeMaps.Fermenter,
                ticks,
                16,
                items(own(id, 1)),
                null,
                fluids(input),
                fluids(result));
    }

    private void chum() {
        // Loader_Recipes_Food:392-398: vanilla fallback of the original scrap-meat recipe.
        for (String rotten : List.of("MeatRotten", "FishRotten")) {
            for (String oil : List.of("Oil_Fish", "Oil_Whale")) {
                add(
                        "chum/" + rotten + "/" + oil,
                        MachineRecipeMaps.Mixer,
                        256,
                        16,
                        items(
                                own("scrap_meat", 1),
                                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                                mat(MaterialPrefix.dust, rotten, 1),
                                new ItemStack(Items.BONE_MEAL),
                                new ItemStack(Items.POISONOUS_POTATO),
                                new ItemStack(Items.RED_MUSHROOM)),
                        items(own("chum", 8)),
                        fluids(fluid("Purple_Drink", 1000), fluid(oil, oil.equals("Oil_Whale") ? 500 : 1000)),
                        fluids(fluid("Sludge", 1000)));
            }
        }
    }

    private static ItemStack own(String id, int count) {
        return item("gt6ce:" + id, count);
    }

    private static void smelt(String id, ItemStack output, boolean food) {
        if (!MachineRecipeMaps.add_smelting(own(id, 1), output, false, food, !food))
            throw new IllegalStateException("Rejected single-item smelting: " + id);
        SMELTING.add(id);
        COOKING.put(own(id, 1).getItem(), new FurnaceSmeltingRecipes.CookingData(0, food));
    }
}
