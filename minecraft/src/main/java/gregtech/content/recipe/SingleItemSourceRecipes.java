/* Gregorius Techneticies / GregTech-6 Team source recipes, LGPL-3.0-or-later. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.data.*;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.*;
import gregtech.engine.recipe.RecipeBuilder;
import gregtech.engine.recipe.RecipeMap;
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
        INSTANCE.animalParts();
        INSTANCE.meat();
        INSTANCE.remains();
        return INSTANCE.entries().size() + SMELTING.size();
    }

    private void compounds() {
        for (var row : OriginalSingleItemCatalog.COMPOUNDS) {
            var input = own(row.item(), 1);
            var material = GTMaterialRegistry.get(row.material());
            smelt(row.item(), mat(MaterialPrefix.nugget, row.material(), 1), false);
            var target = material.getTargetSmeltingMaterial();
            var output = gregtech.util.OM.ingotOrDust(target, material.getTargetSmeltingAmount() / 9);
            if (!output.isEmpty())
                MachineRecipeMaps.CrucibleSmelting.register(
                        RecipeBuilder.of(0, 0)
                                .itemInputs(items(input))
                                .itemOutputs(items(output))
                                .specialValue(material.getMeltingPoint()),
                        RecipeMap.Registration.DISPLAY);
        }
    }

    private void animalParts() {
        // Loader_Recipes_Food: hooves, horns, antlers and ivory have the same two uses.
        for (String id : OriginalSingleItemCatalog.ANIMAL_PARTS) {
            smelt(id, own("sticky_goo", 1), true);
            add(
                    id + "/grind",
                    MachineRecipeMaps.Mortar,
                    16,
                    16,
                    items(own(id, 1)),
                    items(new ItemStack(Items.BONE_MEAL)));
            add(
                    id + "/shred",
                    MachineRecipeMaps.Shredder,
                    16,
                    16,
                    items(own(id, 1)),
                    items(new ItemStack(Items.BONE_MEAL)));
            var output = id.contains("hoof")
                    ? "hoof"
                    : id.contains("tusk") ? "tusk" : id.contains("antler") ? "deer_antler" : "horn";
            if (!id.equals(output))
                add(id + "/generify", MachineRecipeMaps.Generifier, 1, 0, items(own(id, 1)), items(own(output, 1)));
        }
    }

    private void meat() {
        add(
                "scrap_meat/grind",
                MachineRecipeMaps.Mortar,
                16,
                16,
                items(own("scrap_meat", 1)),
                items(mat(MaterialPrefix.dust, "MeatRaw", 2)));
        add(
                "scrap_meat/rot",
                MachineRecipeMaps.Fermenter,
                288,
                16,
                items(own("scrap_meat", 1)),
                items(new ItemStack(Items.ROTTEN_FLESH)));
        // MultiItemFood are the original ingot forms, not decorative food copies.
        for (String state : List.of("raw", "cooked")) {
            String material = state.equals("raw") ? "MeatRaw" : "MeatCooked";
            String id = state + "_meat_bar";
            add(
                    id + "/compress",
                    MachineRecipeMaps.Compressor,
                    16,
                    16,
                    items(mat(MaterialPrefix.dust, material, 1)),
                    items(own(id, 1)));
            add(
                    id + "/grind",
                    MachineRecipeMaps.Mortar,
                    16,
                    16,
                    items(own(id, 1)),
                    items(mat(MaterialPrefix.dust, material, 1)));
            add(
                    id + "/rot",
                    MachineRecipeMaps.Fermenter,
                    144,
                    16,
                    items(own(id, 1)),
                    items(mat(MaterialPrefix.ingot, "MeatRotten", 1)));
        }
        smelt("raw_meat_bar", own("cooked_meat_bar", 1), true);
        // Loader_Recipes_Food: all seven raw meat forms can be cooked.
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
            if (!input.isEmpty() && !output.isEmpty() && MachineRecipeMaps.addSmelting(input, output)) {
                SMELTING.add(prefix.getName() + "/MeatRaw");
                COOKING.put(input.getItem(), new FurnaceSmeltingRecipes.CookingData(0, true));
            }
        }
    }

    private void remains() {
        // Loader_Recipes_Crops delegates all plant remains to RM.biomass(8).
        for (String id : OriginalSingleItemCatalog.PLANT_REMAINS) {
            Set<String> seen = new HashSet<>();
            biomass(id, "Water", 135, 32, seen);
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
                        items(own("plant_remains", 1)));
        }
    }

    private void biomass(String id, String field, int output, int ticks, Set<String> seen) {
        var input = GTFluids.stack(field, 135);
        if (input == null || input.isEmpty()) throw new IllegalStateException("Missing source biomass fluid " + field);
        String key = BuiltInRegistries.FLUID.getKey(input.getFluid()).toString();
        if (!seen.add(key)) return;
        var result = GTFluids.stack("Biomass", output);
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

    private static ItemStack own(String id, int count) {
        return item("gt6ce:" + id, count);
    }

    private static void smelt(String id, ItemStack output, boolean food) {
        if (!MachineRecipeMaps.addSmelting(own(id, 1), output))
            throw new IllegalStateException("Rejected single-item smelting: " + id);
        SMELTING.add(id);
        COOKING.put(own(id, 1).getItem(), new FurnaceSmeltingRecipes.CookingData(0, food));
    }
}
