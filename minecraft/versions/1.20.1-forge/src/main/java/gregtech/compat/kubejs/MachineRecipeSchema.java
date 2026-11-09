package gregtech.compat.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import gregtech.api.addon.recipe.FluidComponent;
import gregtech.api.addon.recipe.ItemComponent;
import gregtech.api.addon.recipe.ItemResult;

/** Typed {@code gt6ce:machine} schema. The JSON names are the datapack field names. */
final class MachineRecipeSchema {
    static final RecipeKey<String> MAP = StringComponent.ANY.key("map");
    static final RecipeKey<ItemComponent[]> ITEM_INPUTS = MachineItemInputComponent.INSTANCE
            .asArray()
            .key("item_inputs")
            .preferred("itemInputs")
            .optional(new ItemComponent[0])
            .alwaysWrite();
    static final RecipeKey<FluidComponent[]> FLUID_INPUTS = MachineFluidInputComponent.INSTANCE
            .asArray()
            .key("fluid_inputs")
            .preferred("fluidInputs")
            .optional(new FluidComponent[0])
            .alwaysWrite();
    static final RecipeKey<ItemResult[]> ITEM_OUTPUTS = MachineItemOutputComponent.INSTANCE
            .asArray()
            .key("item_outputs")
            .preferred("itemOutputs")
            .optional(new ItemResult[0])
            .alwaysWrite();
    static final RecipeKey<FluidComponent[]> FLUID_OUTPUTS = MachineFluidInputComponent.INSTANCE
            .asArray()
            .key("fluid_outputs")
            .preferred("fluidOutputs")
            .optional(new FluidComponent[0])
            .alwaysWrite();
    static final RecipeKey<Long> EU =
            NumberComponent.LONG.key("eu_per_tick").preferred("eu").optional(0L).alwaysWrite();
    static final RecipeKey<Long> DURATION = NumberComponent.longRange(1L, Long.MAX_VALUE)
            .key("duration")
            .preferred("duration")
            .optional(1L)
            .alwaysWrite();
    static final RecipeKey<Long> SPECIAL = NumberComponent.LONG
            .key("special")
            .preferred("special")
            .optional(0L)
            .alwaysWrite();
    static final RecipeKey<gregtech.api.addon.recipe.MaterialUnitIngredient> MATERIAL_INPUT =
            MachineMaterialInputComponent.INSTANCE
                    .key("material_input")
                    .preferred("materialInput")
                    .optional((gregtech.api.addon.recipe.MaterialUnitIngredient) null);
    static final RecipeSchema SCHEMA = new RecipeSchema(
                    MachineRecipeJS.class,
                    MachineRecipeJS::new,
                    MAP,
                    ITEM_INPUTS,
                    FLUID_INPUTS,
                    ITEM_OUTPUTS,
                    FLUID_OUTPUTS,
                    EU,
                    DURATION,
                    SPECIAL,
                    MATERIAL_INPUT)
            .constructor(MAP);

    private MachineRecipeSchema() {}
}
