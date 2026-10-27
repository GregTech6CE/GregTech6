package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Bound native Magnetic Separator rows; expectations transcribed from Ores 54/59/64 and 454-455. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MagneticRegressionTests {
    private MagneticRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void sourceMagneticAfterStartup(GameTestHelper helper) {
        require(GTMaterialRegistry.registrationPhase() == GTMaterialRegistry.RegistrationPhase.READY
                && MachineRecipeOverlay.applications() > 0, "native registration and server overlay must finish");
        physicalAndOrdinaryDust();
        // MT ordered byproducts, filtered by opposite magnetic class, then padded with self.
        generic("Copper", new String[]{"Cobaltite", "Nickel", "Copper", "Copper", "Copper"});
        generic("Cobaltite", new String[]{"Realgar", "OrangeFluorite", "Cobaltite", "Cobaltite", "Cobaltite"});
        for (Recipe recipe : SharedOreMagneticRows.owned()) {
            if (recipe.mInputs[0].getItem() instanceof BlockItem) continue;
            var input = MaterialEquivalence.form(recipe.mInputs[0]);
            require(input != null, "generic accepted input has an actual material binding");
            GTMaterial material = input.material().resolve();
            require(!material.has(MaterialProperty.ANTIMATTER) && material != material("Bedrock"),
                    "generic antimatter and Bedrock exclusions");
            for (var prefix : List.of(MaterialPrefix.dust, MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny))
                require(!GTItems.getStack(prefix, material, 1).isEmpty(), "generic gate requires each actual registered form");
        }
        helper.succeed();
    }

    private static void physicalAndOrdinaryDust() {
        check(row(block("SluiceSand", 8102), true), 16, 1296,
                new long[]{9640, 5832, 5832, 5832, 5832, 5832}, sluiceOutputs(MaterialPrefix.dust, 9));
        check(row(block("Bedrock", 8599), true), 64, 1296,
                new long[]{7000, 3000, 3000, 3000, 3000, 3000}, bedrockOutputs(9));
        check(row(block("MoonTurf", 8514), true), 16, 1296,
                new long[]{3000, 972, 972, 972, 972, 972}, moonOutputs(MaterialPrefix.dust, 9));
        for (String material : List.of("SluiceSand", "Bedrock", "MoonTurf")) {
            ItemStack dust = form(MaterialPrefix.dust, material, 1);
            require(MachineRecipeMaps.MagneticSeparator.mRecipeList.stream().noneMatch(r -> r.mInputs.length == 1
                    && r.mInputs[0].getItem() == dust.getItem() && r.mInputs[0].getCount() == 9),
                    material + " physical block never substitutes nine loose dusts");
        }
        MaterialPrefix[] prefixes = {MaterialPrefix.dustTiny, MaterialPrefix.dustSmall, MaterialPrefix.dust};
        int[] ticks = {16, 36, 144}, sluiceChance = {72, 162, 648}, moonChance = {12, 27, 108};
        for (int shape = 0; shape < prefixes.length; shape++) {
            int s = sluiceChance[shape], m = moonChance[shape];
            check(row(form(prefixes[shape], "SluiceSand", 1), false), 16, ticks[shape],
                    new long[]{9640, s, s, s, s, s}, sluiceOutputs(prefixes[shape], 1));
            check(row(form(prefixes[shape], "MoonTurf", 1), false), 16, ticks[shape],
                    new long[]{3000, m, m, m, m, m}, moonOutputs(prefixes[shape], 1));
        }
        check(row(form(MaterialPrefix.dust, "Bedrock", 1), false), 64, 144,
                new long[]{7000, 3000, 3000, 3000, 3000, 3000}, bedrockOutputs(1));
    }

    private static ItemStack[] sluiceOutputs(MaterialPrefix primary, int count) {
        return new ItemStack[]{form(primary, "Stone", count), form(MaterialPrefix.dustTiny, "Iron", 2),
                form(MaterialPrefix.dustTiny, "Neodymium", 1), form(MaterialPrefix.dustTiny, "Nickel", 1),
                form(MaterialPrefix.dustTiny, "Cobalt", 1), form(MaterialPrefix.dustTiny, "Manganese", 1)};
    }
    private static ItemStack[] bedrockOutputs(int count) {
        return new ItemStack[]{form(MaterialPrefix.dust, "Deepslate", count), form(MaterialPrefix.dustTiny, "Adamantine", count),
                form(MaterialPrefix.dustTiny, "Atlarus", count), form(MaterialPrefix.dustTiny, "RareEarth", 2 * count),
                form(MaterialPrefix.dustTiny, "Neodymium", 3 * count), form(MaterialPrefix.dustTiny, "VanadiumPentoxide", 3 * count)};
    }
    private static ItemStack[] moonOutputs(MaterialPrefix primary, int count) {
        return new ItemStack[]{form(primary, "Basalt", count), form(MaterialPrefix.dustTiny, "MeteoricIron", 9),
                form(MaterialPrefix.dustTiny, "Neodymium", 1), form(MaterialPrefix.dustTiny, "Nickel", 1),
                form(MaterialPrefix.dustTiny, "Cobalt", 1), form(MaterialPrefix.dustTiny, "Manganese", 1)};
    }
    private static void generic(String material, String[] bonuses) {
        for (boolean tiny : new boolean[]{false, true}) {
            var outputs = new ItemStack[6];
            outputs[0] = form(tiny ? MaterialPrefix.crushedCentrifugedTiny : MaterialPrefix.crushedCentrifuged, material, 1);
            for (int slot = 0; slot < bonuses.length; slot++)
                outputs[slot + 1] = form(MaterialPrefix.crushedCentrifugedTiny, bonuses[slot], tiny ? 2 : 18);
            check(row(form(tiny ? MaterialPrefix.crushedPurifiedTiny : MaterialPrefix.crushedPurified, material, 1), true),
                    16, tiny ? 16 : 144, new long[]{10000, 600, 600, 600, 600, 600}, outputs);
        }
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void currentFormsAndOwnOnlyReload(GameTestHelper helper) {
        independentForms();
        Recipe foreign = null;
        Recipe original = row(form(MaterialPrefix.crushedPurified, "Copper", 1), true);
        Recipe ordinary = row(form(MaterialPrefix.dust, "SluiceSand", 1), false);
        int count = SharedOreMagneticRows.owned().size();
        try {
            require(MachineRecipeMaps.MagneticSeparator.unlink(original), "isolate one native collision fixture");
            foreign = new Recipe(new ItemStack[]{original.mInputs[0].copy()},
                    new ItemStack[]{form(MaterialPrefix.dust, "Gold", 1)}, null, new long[]{10000},
                    new FluidStack[0], new FluidStack[0], 17, 7, 0);
            require(MachineRecipeMaps.MagneticSeparator.addRecipe(foreign) == foreign, "foreign fixture admitted normally");
            for (int reload = 0; reload < 2; reload++) {
                var oldOwned = SharedOreMagneticRows.owned();
                SharedOreMagneticRows.rebuild();
                require(MachineRecipeMaps.MagneticSeparator.mRecipeList.contains(foreign), "foreign collision identity survives");
                require(!SharedOreMagneticRows.owned().contains(foreign), "collision is not adopted as owned");
                require(SharedOreMagneticRows.owned().size() == count - 1, "collision suppresses only the one owned row");
                require(SharedOreMagneticRows.owned().stream().noneMatch(oldOwned::contains), "rebuild replaces own identities only");
                require(MachineRecipeMaps.MagneticSeparator.mRecipeList.contains(ordinary), "ordinary dust identity untouched");
            }
        } finally {
            if (foreign != null) MachineRecipeMaps.MagneticSeparator.unlink(foreign);
            SharedOreMagneticRows.rebuild();
        }
        var beforeOverlay = SharedOreMagneticRows.owned();
        MachineRecipeOverlay.apply(helper.getLevel().getServer().getRecipeManager());
        require(SharedOreMagneticRows.owned().size() == count
                && SharedOreMagneticRows.owned().stream().noneMatch(beforeOverlay::contains), "real overlay rebuilds fresh owned rows");
        require(MachineRecipeMaps.MagneticSeparator.mRecipeList.contains(ordinary), "real overlay preserves static dust baseline");
        physicalAndOrdinaryDust();
        helper.succeed();
    }

    private static void independentForms() {
        var copper = material("Copper");
        for (var absent : List.of(MaterialPrefix.dust, MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny)) {
            var snapshot = new SharedOreMagneticRows.Snapshot() {
                @Override SharedOreWashingRows.NativeForm direct(MaterialPrefix prefix, GTMaterial material, int count) {
                    return material == copper && prefix == absent ? SharedOreWashingRows.NativeForm.absent()
                            : super.direct(prefix, material, count);
                }
            };
            require(SharedOreMagneticRows.definitions(List.of(copper), snapshot).isEmpty(),
                    "actual missing gate suppresses both generic sizes: " + absent.getName());
        }
        for (boolean tiny : new boolean[]{false, true}) {
            var absent = tiny ? MaterialPrefix.crushedCentrifugedTiny : MaterialPrefix.crushedCentrifuged;
            var snapshot = new SharedOreMagneticRows.Snapshot() {
                @Override SharedOreWashingRows.NativeForm output(MaterialPrefix prefix, GTMaterial material, int count) {
                    return material == copper && prefix == absent ? SharedOreWashingRows.NativeForm.absent()
                            : super.output(prefix, material, count);
                }
            };
            var definitions = SharedOreMagneticRows.definitions(List.of(copper), snapshot);
            require(definitions.size() == 2, "one missing refined form cannot suppress either independently resolved size");
            for (var definition : definitions) {
                boolean tinyInput = definition.input().getItem() == form(MaterialPrefix.crushedPurifiedTiny, "Copper", 1).getItem();
                var expected = tinyInput == tiny ? (tiny ? MaterialPrefix.dustTiny : MaterialPrefix.dust)
                        : (tinyInput ? MaterialPrefix.crushedCentrifugedTiny : MaterialPrefix.crushedCentrifuged);
                require(ItemStack.matches(definition.outputs()[0], form(expected, "Copper", 1)), "direct same-count refined→dust fallback");
                if (tiny) for (int slot = 3; slot < 6; slot++)
                    require(ItemStack.matches(definition.outputs()[slot], form(MaterialPrefix.dustTiny, "Copper", tinyInput ? 2 : 18)),
                            "independent byproduct fallback retains raw counts and repeated slots");
            }
        }
        var noBlock = new SharedOreMagneticRows.Snapshot() {
            @Override SharedOreWashingRows.NativeForm blockDust(GTMaterial material, int count) {
                return SharedOreWashingRows.NativeForm.absent();
            }
        };
        require(SharedOreMagneticRows.definitions(List.of(material("Bedrock")), noBlock).isEmpty(),
                "missing physical block neither fabricates dust9 nor creates generic Bedrock rows");
    }

    private static ItemStack block(String name, int id) {
        GTMaterial material = material(name);
        require(material.getId() == id, "strict source material ID: " + name);
        ItemStack stack = item("gt6ce:block_dust_" + name.toLowerCase(Locale.ROOT), 1);
        require(stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof MaterialBlockLike,
                name + " actual physical material block");
        var block = ((BlockItem) stack.getItem()).getBlock();
        var form = (MaterialBlockLike) block;
        require(form.prefix() == BlockMaterialPrefix.blockDust && form.prefix().getMaterialWeight() == 9 * GTValues.U
                && form.material().resolve() == material, name + " exact blockDust/9U/material identity");
        require(block.asItem() == stack.getItem() && BuiltInRegistries.BLOCK.getKey(block).equals(
                BuiltInRegistries.ITEM.getKey(stack.getItem())), name + " bound block/item holder identity");
        return stack;
    }
    private static Recipe row(ItemStack input, boolean owned) {
        var rows = MachineRecipeMaps.MagneticSeparator.mRecipeList.stream().filter(r -> r.mInputs.length == 1
                && r.mInputs[0].getItem() == input.getItem()).toList();
        require(rows.size() == 1 && ItemStack.matches(rows.get(0).mInputs[0], input), "one exact Magnetic row: " + input);
        Recipe recipe = rows.get(0);
        require(SharedOreMagneticRows.owned().contains(recipe) == owned, "exact producer ownership: " + input);
        require(MachineRecipeMaps.MagneticSeparator.findRecipe(List.of(input), List.of(), false, 1, 6) == recipe,
                "native indexed lookup finds registered identity: " + input);
        return recipe;
    }
    private static void check(Recipe recipe, long power, long ticks, long[] chances, ItemStack[] outputs) {
        require(recipe.mEUt == power && recipe.mDuration == ticks, "source native power/duration");
        require(recipe.mSpecialItems == null && recipe.mSpecialValue == 0 && recipe.mEnabled && !recipe.mFakeRecipe
                && !recipe.mHidden && recipe.mCanBeBuffered, "ordinary enabled recipe flags");
        require(recipe.mFluidInputs.length == 0 && recipe.mFluidOutputs.length == 0, "source no-fluid row");
        require(recipe.explicitCatalystInputs().length == 0 && recipe.explicitCatalystFluidInputs().length == 0
                && recipe.explicitPresenceFluidInputs().length == 0, "no fabricated retained gate");
        require(Arrays.equals(recipe.mChances, chances) && recipe.mMaxChances.length == chances.length
                && Arrays.stream(recipe.mMaxChances).allMatch(c -> c == 10000), "raw ordered chances/denominators");
        require(recipe.mOutputs.length == outputs.length, "ordered output slot count");
        for (int slot = 0; slot < outputs.length; slot++)
            require(ItemStack.matches(recipe.mOutputs[slot], outputs[slot]), "exact output slot " + slot);
    }
    private static GTMaterial material(String name) {
        GTMaterial value = GTMaterialRegistry.get(name);
        require(value != null && value.isValid(), "missing expected material " + name);
        return value.resolve();
    }
    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        return item("gt6ce:" + prefix.getItemId(material(name)), count);
    }
    private static ItemStack item(String id, int count) {
        var key = new ResourceLocation(id);
        require(BuiltInRegistries.ITEM.containsKey(key), "missing actual holder " + id);
        return new ItemStack(BuiltInRegistries.ITEM.get(key), count);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
