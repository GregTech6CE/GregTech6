package gregtech.content.material;

import gregtech.api.material.MaterialComponent;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.VanillaUnificationDefinitions;
import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Minecraft item binding for the single shared vanilla identity/weight table. */
public final class VanillaUnificationLoader {
    private VanillaUnificationLoader() {}

    public static void register() {
        for (var definition : VanillaUnificationDefinitions.entries()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(definition.itemPath()));
            var material = GTMaterialRegistry.get(definition.materialName());
            if (item == Items.AIR || !material.isValid()) continue;
            if (definition.extraComponents().isEmpty()) {
                ItemMaterialRegistry.register(item, definition.prefix(), material, definition.amount());
                continue;
            }
            var components = new ArrayList<MaterialComponent>();
            components.add(MaterialComponent.of(material, definition.amount()));
            for (var extra : definition.extraComponents()) {
                var extraMaterial = GTMaterialRegistry.get(extra.materialName());
                if (extraMaterial.isValid()) components.add(MaterialComponent.of(extraMaterial, extra.amount()));
            }
            ItemMaterialRegistry.register(
                    item, new ItemComposition(definition.prefix(), components, "GT6 unification", true));
        }
    }
}
