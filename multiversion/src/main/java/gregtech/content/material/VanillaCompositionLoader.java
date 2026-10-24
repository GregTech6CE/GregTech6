package gregtech.content.material;

import com.google.gson.*;
import gregtech.api.material.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Versioned built-in item data shared by client tooltips and server recovery. */
public final class VanillaCompositionLoader {
    private VanillaCompositionLoader() {}
    public static void register() {
        try (var stream = VanillaCompositionLoader.class.getResourceAsStream("/data/gt6ce/materials/vanilla_compositions.json")) {
            if (stream == null) throw new IllegalStateException("Missing vanilla compositions");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var errors = new ArrayList<String>();
            for (var entry : json.entrySet()) {
//? if >=1.21.1 {
                /*var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(entry.getKey()));
*///?} else {
                var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(entry.getKey()));
//?}
                if (item == null || item == Items.AIR) continue; // Model variants are not registered items.
                var row = entry.getValue().getAsJsonObject();
                var components = new ArrayList<MaterialComponent>();
                for (var part : row.getAsJsonObject("components").entrySet()) {
                    var material = GTMaterialRegistry.get(part.getKey());
                    if (!material.isValid()) { errors.add(entry.getKey() + ": " + part.getKey()); continue; }
                    components.add(MaterialComponent.of(material, part.getValue().getAsLong()));
                }
                var previous = ItemMaterialRegistry.base(item);
                var prefix = previous.map(gregtech.api.material.ItemComposition::prefix).orElse(null);
                ItemMaterialRegistry.register(item, new gregtech.api.material.ItemComposition(prefix, components,
                        row.get("source").getAsString(), row.get("recoverable").getAsBoolean()));
            }
            if (!errors.isEmpty()) throw new IllegalStateException("Unresolved vanilla materials: " + errors);
        } catch (IOException e) { throw new UncheckedIOException(e); }
        // The old generic table describes all planks as Wood. Original WoodDictionary
        // replaces those fallbacks with the species before panel and recipe generation.
        gregtech.content.recipe.VanillaWoodProcessingRecipes.registerMaterials();
    }
}
