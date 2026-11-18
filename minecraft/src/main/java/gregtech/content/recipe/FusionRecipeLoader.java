package gregtech.content.recipe;

import gregtech.bootstrap.InitializationStep;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** GT6 {@code Loader_Recipes_Other} fusion reactions, with fluid quantities in mB and startup energy in LU. */
public final class FusionRecipeLoader implements InitializationStep {
    private enum Phase {
        GAS,
        MOLTEN
    }

    private record FluidAmount(String material, Phase phase, int millibuckets) {}

    private int added;
    private final Set<String> missing = new TreeSet<>();

    @Override
    public void run() {
        add(
                1,
                powerOutput(730, 95_682_560L),
                List.of(gas("D", 2_000)),
                List.of(gas("He_3", 500), gas("T", 500)));
        add(
                1,
                powerOutput(1130, 148_111_360L),
                List.of(gas("T", 2_000)),
                List.of(gas("He", 1_000)));
        add(
                1,
                powerOutput(1290, 169_082_880L),
                List.of(gas("He_3", 2_000)),
                List.of(gas("He", 1_000)));
        add(
                1,
                startupOnly(1890, 247_726_080L),
                List.of(gas("He", 2_000)),
                List.of(molten("Be_8", 144)));
        add(
                1,
                startupOnly(3214, 421_265_408L),
                List.of(molten("Be_8", 288)),
                List.of(gas("O", 1_000)));
        add(
                2,
                powerOutput(546, 1_110_048_768L),
                List.of(gas("H", 1_000), molten("B_11", 144)),
                List.of(gas("He", 3_000)));
        add(
                2,
                powerOutput(315, 41_287_680L),
                List.of(gas("H", 1_000), molten("C", 144)),
                List.of(molten("C_13", 144)));
        add(
                2,
                powerOutput(754, 98_828_288L),
                List.of(gas("H", 1_000), molten("C_13", 144)),
                List.of(gas("N", 1_000)));
        add(
                2,
                powerOutput(1404, 184_025_088L),
                List.of(gas("H", 2_000), gas("N", 1_000)),
                List.of(gas("He", 500), molten("C", 72), gas("O", 500)));
        add(
                2,
                powerOutput(455, 59_637_760L),
                List.of(gas("H", 2_000), gas("O", 1_000)),
                List.of(gas("He", 500), gas("F", 500), gas("N", 500)));
        add(
                2,
                powerOutput(1760, 230_686_720L),
                List.of(gas("D", 1_000), gas("T", 1_000)),
                List.of(gas("He", 1_000)));
        add(
                2,
                powerOutput(1830, 239_861_760L),
                List.of(gas("D", 1_000), gas("He_3", 1_000)),
                List.of(gas("He", 1_000)));
        add(
                2,
                powerOutput(2640, 346_030_080L),
                List.of(gas("T", 1_000), gas("He_3", 1_000)),
                List.of(gas("He", 750), gas("D", 250)));
        add(
                2,
                powerOutput(3336, 437_256_192L),
                List.of(gas("D", 1_000), molten("Li_6", 144)),
                List.of(gas("He", 375), gas("He_3", 125), molten("Li", 18), molten("Be_7", 18)));
        add(
                2,
                powerOutput(1690, 221_511_680L),
                List.of(gas("He_3", 1_000), molten("Li_6", 144)),
                List.of(gas("He", 2_000)));
        add(
                2,
                powerOutput(736, 96_468_992L),
                List.of(gas("He", 1_000), molten("Be_8", 144)),
                List.of(molten("C", 144)));
        add(
                2,
                powerOutput(716, 93_847_552L),
                List.of(gas("He", 1_000), molten("C", 144)),
                List.of(gas("O", 1_000)));
        add(
                2,
                powerOutput(1956, 12_446_072_832L),
                List.of(molten("Ad", 144), molten("Be_7", 144)),
                List.of(molten("W", 144), gas("He", 16_000), gas("He_3", 24_000), gas("T", 24_000)),
                "Vb");

        com.mojang.logging.LogUtils.getLogger().info(
                "[gregtech] Fusion recipes: {} / 18; unresolved: {}", added, missing);
    }

    private static FluidAmount gas(String material, int millibuckets) {
        return new FluidAmount(material, Phase.GAS, millibuckets);
    }

    private static FluidAmount molten(String material, int millibuckets) {
        return new FluidAmount(material, Phase.MOLTEN, millibuckets);
    }

    private static RecipeBuilder powerOutput(int durationTicks, long startupLu) {
        return RecipeBuilder.of(-8192, durationTicks).specialValue(startupLu);
    }

    private static RecipeBuilder startupOnly(int durationTicks, long startupLu) {
        return RecipeBuilder.of(0, durationTicks).specialValue(startupLu);
    }

    private FluidStack fluid(FluidAmount amount) {
        var material = GeneratedRecipeResolver.material(amount.material());
        if (material != null) {
            var stack = switch (amount.phase()) {
                case GAS -> GeneratedRecipeResolver.materialFluid(material.getName(), amount.millibuckets());
                case MOLTEN -> GeneratedRecipeResolver.byRegistryName(
                        "molten." + material.getName().toLowerCase(Locale.ROOT), amount.millibuckets());
            };
            if (stack != null) return stack;
        }
        missing.add(amount.toString());
        return FluidStack.EMPTY;
    }

    private void add(int circuit, RecipeBuilder recipe, List<FluidAmount> inputs, List<FluidAmount> outputs) {
        add(circuit, recipe, inputs, outputs, null);
    }

    private void add(int circuit, RecipeBuilder recipe, List<FluidAmount> inputs, List<FluidAmount> outputs, String dust) {
        var in = inputs.stream().map(this::fluid).toArray(FluidStack[]::new);
        var out = outputs.stream().map(this::fluid).toArray(FluidStack[]::new);
        if (Arrays.stream(in).anyMatch(FluidStack::isEmpty) || Arrays.stream(out).anyMatch(FluidStack::isEmpty)) return;
        if (dust != null) {
            var material = GeneratedRecipeResolver.material(dust);
            var stack = material == null ? ItemStack.EMPTY : MaterialStackItemHelper.mat(MaterialPrefix.dust, material, 1);
            if (stack.isEmpty()) {
                missing.add(dust);
                return;
            }
            recipe.itemOutputs(stack);
        }
        Recipe candidate = recipe
                .itemInputs(new ItemStack(GTTechnological.selectorTag(circuit)))
                .fluidInputs(in)
                .fluidOutputs(out)
                .build();
        if (MachineRecipeMaps.Fusion.addRecipe(candidate) != null) added++;
    }
}
