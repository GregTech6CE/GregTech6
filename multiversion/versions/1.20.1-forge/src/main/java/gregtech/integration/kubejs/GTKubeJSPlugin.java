package gregtech.integration.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;

/** Loaded from {@code kubejs.plugins.txt} only when the {@code kubejs} mod is present. */
public final class GTKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        GT6CEStartupEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("GT6CE", GT6CEBinding.INSTANCE);
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.namespace("gt6ce").register("machine", MachineRecipeSchema.SCHEMA);
    }
}
