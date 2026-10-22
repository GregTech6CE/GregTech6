package com.gregtech.gregtech.integration.kubejs;

import dev.latvian.mods.kubejs.fluid.InputFluid;
import dev.latvian.mods.kubejs.fluid.OutputFluid;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.FluidComponents;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

/** Typed {@code gt6ce:machine} schema. The JSON names are the datapack field names. */
final class MachineRecipeSchema {
    static final RecipeKey<String> MAP = StringComponent.ANY.key("map");
    static final RecipeKey<InputItem[]> ITEM_INPUTS = ItemComponents.INPUT_ARRAY.key("item_inputs")
            .preferred("itemInputs").optional(new InputItem[0]).alwaysWrite();
    static final RecipeKey<InputFluid[]> FLUID_INPUTS = FluidComponents.INPUT_ARRAY.key("fluid_inputs")
            .preferred("fluidInputs").optional(new InputFluid[0]).alwaysWrite();
    static final RecipeKey<OutputItem[]> ITEM_OUTPUTS = ItemComponents.OUTPUT_ARRAY.key("item_outputs")
            .preferred("itemOutputs").optional(new OutputItem[0]).alwaysWrite();
    static final RecipeKey<OutputFluid[]> FLUID_OUTPUTS = FluidComponents.OUTPUT_ARRAY.key("fluid_outputs")
            .preferred("fluidOutputs").optional(new OutputFluid[0]).alwaysWrite();
    static final RecipeKey<Long> EU = NumberComponent.LONG.key("eu_per_tick").preferred("eu").optional(0L).alwaysWrite();
    static final RecipeKey<Long> DURATION = NumberComponent.longRange(1L, Long.MAX_VALUE).key("duration").preferred("duration").optional(1L).alwaysWrite();
    static final RecipeKey<Long> SPECIAL = NumberComponent.LONG.key("special").preferred("special").optional(0L).alwaysWrite();
    static final RecipeSchema SCHEMA = new RecipeSchema(MachineRecipeJS.class, MachineRecipeJS::new,
            MAP, ITEM_INPUTS, FLUID_INPUTS, ITEM_OUTPUTS, FLUID_OUTPUTS, EU, DURATION, SPECIAL)
            .constructor(MAP);

    private MachineRecipeSchema() {}
}
