package com.gregtech.gregtech.integration.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Typed {@code gt6ce:machine} schema. The JSON names are the datapack field names. */
final class MachineRecipeSchema {
    static final RecipeKey<String> MAP = StringComponent.STRING.instance().otherKey("map");
    static final RecipeKey<List<MachineRecipeComponents.MachineItem>> ITEM_INPUTS = MachineRecipeComponents.ITEMS
            .otherKey("item_inputs").functionNames("itemInputs").optional(List.of()).alwaysWrite();
    static final RecipeKey<List<MachineRecipeComponents.MachineFluid>> FLUID_INPUTS = MachineRecipeComponents.FLUIDS
            .otherKey("fluid_inputs").functionNames("fluidInputs").optional(List.of()).alwaysWrite();
    static final RecipeKey<List<MachineRecipeComponents.MachineResult>> ITEM_OUTPUTS = MachineRecipeComponents.RESULTS
            .otherKey("item_outputs").functionNames("itemOutputs").optional(List.of()).alwaysWrite();
    static final RecipeKey<List<MachineRecipeComponents.MachineFluid>> FLUID_OUTPUTS = MachineRecipeComponents.FLUIDS
            .otherKey("fluid_outputs").functionNames("fluidOutputs").optional(List.of()).alwaysWrite();
    static final RecipeKey<Long> EU = NumberComponent.longRange(0, Long.MAX_VALUE)
            .otherKey("eu_per_tick").functionNames("eu").optional(0L).alwaysWrite();
    static final RecipeKey<Long> DURATION = NumberComponent.longRange(1, Long.MAX_VALUE)
            .otherKey("duration").functionNames("duration").optional(1L).alwaysWrite();
    static final RecipeKey<Long> SPECIAL = NumberComponent.longRange(0, Long.MAX_VALUE)
            .otherKey("special").functionNames("special").optional(0L).alwaysWrite();
    static final RecipeSchema SCHEMA = new RecipeSchema(
            MAP, ITEM_INPUTS, FLUID_INPUTS, ITEM_OUTPUTS, FLUID_OUTPUTS, EU, DURATION, SPECIAL)
            .factory(new KubeRecipeFactory(ResourceLocation.fromNamespaceAndPath("gt6ce", "machine"),
                    MachineKubeRecipe.class, MachineKubeRecipe::new))
            .constructor(MAP);

    private MachineRecipeSchema() {}
}
