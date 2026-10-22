package com.gregtech.gregtech.integration.kubejs;

import com.google.gson.JsonParser;
import com.gregtech.gregtech.api.kubejs.StructureLayoutDocuments;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeFields;
import com.gregtech.gregtech.api.recipe.data.MachineRecipeScript;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import dev.latvian.mods.kubejs.script.SourceLine;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Global {@code GT6CE} binding. Recipe helpers submit the same JSON a datapack would. */
public final class GT6CEBinding {
    public static final GT6CEBinding INSTANCE = new GT6CEBinding();

    private GT6CEBinding() {}

    public List<String> maps() { return MachineRecipeScript.maps(); }

    public void removeMachine(RecipesKubeEvent event, Object spec) {
        event.parseJson(json(MachineRecipeFields.TYPE_REMOVAL, ScriptValues.removal(spec)), SourceLine.UNKNOWN)
                .getPartialOrThrow(dev.latvian.mods.kubejs.error.KubeRuntimeException::new);
    }

    public void clearMap(RecipesKubeEvent event, String map) {
        event.parseJson(json(MachineRecipeFields.TYPE_REMOVAL, MachineRecipeScript.clear(map)), SourceLine.UNKNOWN)
                .getPartialOrThrow(dev.latvian.mods.kubejs.error.KubeRuntimeException::new);
    }

    /** Writes {@code data/kubejs/gt6ce_layouts/<machine>.json}. The machine must already exist. */
    public void layout(KubeDataGenerator event, String machine, Object cells) {
        var layout = StructureLayoutDocuments.layout(machine, ScriptValues.cells(cells));
        event.json(ResourceLocation.fromNamespaceAndPath("kubejs", "gt6ce_layouts/" + machine),
                JsonParser.parseString(StructureLayoutDocuments.canonical(layout)).getAsJsonObject());
    }

    private static com.google.gson.JsonObject json(String type, String body) {
        return JsonParser.parseString(MachineRecipeScript.typed(type, body)).getAsJsonObject();
    }
}
