package gregtech.content.recipe.generated;

import static gregtech.engine.recipe.chem.FluidPhase.GAS;
import static gregtech.engine.recipe.chem.FluidPhase.LIQUID;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import com.mojang.logging.LogUtils;
import gregtech.api.material.GTValues;
import gregtech.engine.recipe.chem.ChemFluid;
import gregtech.engine.recipe.chem.ChemIngredient;
import gregtech.engine.recipe.chem.ChemOutcome;
import gregtech.engine.recipe.chem.ChemRecipe;
import gregtech.engine.recipe.chem.ChemResult;
import gregtech.engine.recipe.chem.MaterialAmount;
import gregtech.engine.recipe.chem.Rounding;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * Typed row builder shared by the hand-maintained source tables transposed from the fixed
 * {@code 3703e403} GT6 tree.
 *
 * <p>Amounts are material units, never amount-thousandths or stack counts. A source row is
 * transcribed with its own unit, phase and rounding; the native values are produced only when
 * {@link ChemRecipeResolver} resolves the binding.</p>
 */
final class GeneratedRows {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final int CERTAIN = 10_000;

    private GeneratedRows() {}

    static Row row(String source, String map, long eut, long ticks) {
        return new Row(source, map, false, eut, ticks);
    }

    static Row row(String source, String map, boolean optimize, long eut, long ticks) {
        return new Row(source, map, optimize, eut, ticks);
    }

    static Row row(String source, String map, long eut, long ticks, int line) {
        return new Row(source + line, map, false, eut, ticks);
    }

    static Row row(String source, String map, boolean optimize, long eut, long ticks, int line) {
        return new Row(source + line, map, optimize, eut, ticks);
    }

    static Row row(String source, String map, long eut, long ticks, String detail) {
        return new Row(source + detail, map, false, eut, ticks);
    }

    static Row row(String source, String map, boolean optimize, long eut, long ticks, String detail) {
        return new Row(source + detail, map, optimize, eut, ticks);
    }

    static final class Row {
        private final String source;
        private final String map;
        private final boolean optimize;
        private final long eut;
        private final long ticks;
        private final List<ChemRecipe.ChemInput> inputs = new ArrayList<>();
        private final List<ChemRecipe.ChemFluidInput> fluidInputs = new ArrayList<>();
        private final List<ChemRecipe.ChemFluidOutput> fluidOutputs = new ArrayList<>();
        private final List<ChemRecipe.ChemOutput> outputs = new ArrayList<>();

        private Row(String source, String map, boolean optimize, long eut, long ticks) {
            this.source = source;
            this.map = map;
            this.optimize = optimize;
            this.eut = eut;
            this.ticks = ticks;
        }

        Row item(ChemIngredient ingredient) {
            inputs.add(new ChemRecipe.ChemInput(ingredient, ChemRecipe.Role.CONSUMED));
            return this;
        }

        /** An input that gates the row without being consumed. */
        Row kept(ChemIngredient ingredient) {
            inputs.add(new ChemRecipe.ChemInput(ingredient, ChemRecipe.Role.KEPT));
            return this;
        }

        Row fluid(ChemFluid fluid) {
            fluidInputs.add(new ChemRecipe.ChemFluidInput(fluid, ChemRecipe.Role.CONSUMED));
            return this;
        }

        Row keptFluid(ChemFluid fluid) {
            fluidInputs.add(new ChemRecipe.ChemFluidInput(fluid, ChemRecipe.Role.KEPT));
            return this;
        }

        Row presence(ChemFluid fluid) {
            fluidInputs.add(new ChemRecipe.ChemFluidInput(fluid, ChemRecipe.Role.PRESENCE));
            return this;
        }

        Row out(ChemIngredient ingredient) {
            outputs.add(new ChemRecipe.ChemOutput(ingredient, CERTAIN));
            return this;
        }

        Row out(ChemIngredient ingredient, int chance) {
            outputs.add(new ChemRecipe.ChemOutput(ingredient, chance));
            return this;
        }

        Row out(ChemFluid fluid) {
            fluidOutputs.add(new ChemRecipe.ChemFluidOutput(fluid));
            return this;
        }

        void add(List<ChemRecipe> rows) {
            rows.add(new ChemRecipe(map, optimize, eut, ticks, inputs, fluidInputs, fluidOutputs, outputs, source));
        }
    }

    static MaterialAmount U(long whole) {
        return MaterialAmount.units(Math.multiplyExact(whole, GTValues.U));
    }

    static MaterialAmount U(long numerator, long denominator) {
        return MaterialAmount.fraction(numerator, denominator);
    }

    /** Source {@code factor * (U / denominator)} with the original integer division. */
    static MaterialAmount src(long factor, long denominator) {
        return MaterialAmount.units(Math.multiplyExact(factor, GTValues.U / denominator));
    }

    /** Source thousandths of a unit, as written by {@code OM.dust(material, mU * U / 1000)}. */
    static MaterialAmount thousandths(long amount) {
        return MaterialAmount.fraction(amount, 1000);
    }

    /** An amount already expressed in GT material units. */
    static MaterialAmount units(long amount) {
        return MaterialAmount.units(amount);
    }

    /** A water volume expressed in millibuckets at the source's 1000 mB/U water unit. */
    static MaterialAmount waterUnits(long millibuckets) {
        return MaterialAmount.fraction(millibuckets, 1000);
    }

    static ChemIngredient dust(String material, MaterialAmount amount) {
        return new ChemIngredient.DustUnits(material, amount);
    }

    static ChemIngredient ingot(String material, MaterialAmount amount) {
        return new ChemIngredient.IngotUnits(material, amount);
    }

    static ChemIngredient ingotOrDust(String material, MaterialAmount amount) {
        return new ChemIngredient.IngotOrDustUnits(material, amount);
    }

    static ChemIngredient gem(String material, MaterialAmount amount) {
        return new ChemIngredient.GemUnits(material, amount);
    }

    static ChemIngredient form(String prefix, String material, int count) {
        return new ChemIngredient.ItemForm(prefix, material, count);
    }

    static ChemIngredient block(String prefix, String material, int count) {
        return new ChemIngredient.BlockForm(prefix, material, count);
    }

    static ChemIngredient vanilla(String id, int count) {
        return new ChemIngredient.RegistryItem(id.contains(":") ? id : "minecraft:" + id, count);
    }

    static ChemIngredient tech(String name, int count) {
        return new ChemIngredient.TechnologyItem(name, count);
    }

    static ChemIngredient cell(String fluidKey, int count) {
        return new ChemIngredient.FluidItem(fluidKey, count);
    }

    static ChemIngredient book(String name) {
        return new ChemIngredient.Book(name);
    }

    static ChemIngredient selector(int index) {
        return new ChemIngredient.Selector(index);
    }

    static ChemIngredient blockDust(String material, int count) {
        return new ChemIngredient.CentrifugeBlockDust(material, count);
    }

    static ChemIngredient sourceItem(String material, String prefix, int count) {
        return new ChemIngredient.SourceItem(material, prefix, count);
    }

    static ChemIngredient optional(String plan) {
        return new ChemIngredient.OptionalOutput(plan);
    }

    static ChemFluid liquid(String material, MaterialAmount amount) {
        return new ChemFluid.MaterialFluid(material, LIQUID, amount, FLOOR);
    }

    static ChemFluid liquid(String material, MaterialAmount amount, Rounding rounding) {
        return new ChemFluid.MaterialFluid(material, LIQUID, amount, rounding);
    }

    static ChemFluid gas(String material, MaterialAmount amount) {
        return new ChemFluid.MaterialFluid(material, GAS, amount, FLOOR);
    }

    static ChemFluid gas(String material, MaterialAmount amount, Rounding rounding) {
        return new ChemFluid.MaterialFluid(material, GAS, amount, rounding);
    }

    static ChemFluid nativeFluid(String key, int millibuckets) {
        return new ChemFluid.NativeFluid(key, millibuckets);
    }

    static ChemFluid registryFluid(String registryName, int millibuckets) {
        return new ChemFluid.RegistryFluid(registryName, millibuckets);
    }

    static ChemFluid water(MaterialAmount amount, Rounding rounding) {
        return new ChemFluid.Water(amount, rounding);
    }

    /**
     * A source group and its re-registrations. A group is never reduced to one representative;
     * members without a registered material resolve as missing content instead.
     */
    static java.util.Set<gregtech.api.material.GTMaterial> group(gregtech.api.material.GTMaterial group) {
        var out = new java.util.LinkedHashSet<gregtech.api.material.GTMaterial>();
        if (group != null && group.isValid()) out.add(group.resolve());
        if (group != null) for (var material : group.getReRegistrations())
            if (material != null && material.isValid()) out.add(material.resolve());
        return out;
    }

    /** Publishes one family and reports the disposition of every source row. */
    static void publish(String family, List<ChemRecipe> rows) {
        int[] counts = new int[ChemOutcome.values().length];
        for (ChemRecipe row : rows) {
            ChemResult result = ChemRecipeResolver.publish(row);
            counts[result.outcome().ordinal()]++;
            if (result.outcome() != ChemOutcome.PUBLISHED && result.outcome() != ChemOutcome.SUBSTITUTED)
                LOGGER.warn("[gregtech] {}: {} ({})", result.outcome(), result.source(), result.detail());
        }
        LOGGER.info("[gregtech] {}: {} source rows -> {} published, {} substituted, {} excluded, "
                        + "{} missing, {} optional absent, {} collisions, {} invalid", family, rows.size(),
                counts[ChemOutcome.PUBLISHED.ordinal()], counts[ChemOutcome.SUBSTITUTED.ordinal()],
                counts[ChemOutcome.EXCLUDED.ordinal()], counts[ChemOutcome.MISSING_CONTENT.ordinal()],
                counts[ChemOutcome.OPTIONAL_ABSENT.ordinal()], counts[ChemOutcome.COLLISION.ordinal()],
                counts[ChemOutcome.VALIDATION_FAILED.ordinal()]);
    }
}
