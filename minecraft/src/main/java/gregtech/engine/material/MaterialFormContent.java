package gregtech.engine.material;

import gregtech.api.addon.material.FormKey;
import gregtech.api.addon.material.MaterialFormBinding;
import gregtech.api.addon.material.MaterialFormKind;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.material.GTMaterial;
import gregtech.content.data.FluidCatalog;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;

/** Projects registered native content into the definition catalog without adding another registry. */
public final class MaterialFormContent {
    private MaterialFormContent() {}

    public static synchronized void publish() {
        var bindings = new ArrayList<MaterialFormBinding>();
        for (var definition : MaterialItemDefinitions.all()) {
            var registered = GTItems.getObject(definition.prefix(), definition.material());
            if (registered == null || !registered.isPresent()) continue;
            GTMaterial material = definition.material().resolve();
            if (!material.isValid()) continue;
            bindings.add(new MaterialFormBinding(
                    new MaterialKey(material.getName()),
                    new FormKey(definition.prefix().getName()),
                    MaterialFormKind.ITEM,
                    registered.getId().toString(),
                    definition.prefix().getMaterialWeight()));
        }
        for (var definition : MaterialBlockDefinitions.all()) {
            var registered = GTBlocks.getObject(definition.prefix(), definition.material());
            if (registered == null || !registered.isPresent()) continue;
            GTMaterial material = definition.material().resolve();
            if (!material.isValid()) continue;
            bindings.add(new MaterialFormBinding(
                    new MaterialKey(material.getName()),
                    new FormKey(definition.prefix().getName()),
                    MaterialFormKind.BLOCK,
                    registered.getId().toString(),
                    definition.prefix().getMaterialWeight()));
        }
        for (var entry : ItemMaterialRegistry.entries().entrySet()) {
            var composition = entry.getValue();
            if (composition.prefix() == null
                    || composition.components().size() != 1
                    || composition.amount() != composition.prefix().getMaterialWeight()) continue;
            GTMaterial material = composition.material().resolve();
            if (!material.isValid()) continue;
            bindings.add(new MaterialFormBinding(
                    new MaterialKey(material.getName()),
                    new FormKey(composition.prefix().getName()),
                    MaterialFormKind.ITEM,
                    BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(),
                    composition.amount()));
        }
        for (var entry : GTFluids.entries().entrySet()) {
            FluidCatalog.FluidEntry definition = entry.getValue();
            String materialName = definition.materialKey() == null
                    ? FluidCatalog.boundMaterial(definition.registryName())
                    : definition.materialKey();
            if (materialName == null) continue;
            GTMaterial material = GTMaterialRegistry.get(materialName);
            if (!material.isValid()) continue;
            String form = definition.textureMode() == FluidCatalog.FluidTextureMode.GENERIC_MOLTEN
                    ? "molten"
                    : definition.gas() ? "gas" : "liquid";
            bindings.add(new MaterialFormBinding(
                    new MaterialKey(material.getName()),
                    new FormKey(form),
                    MaterialFormKind.FLUID,
                    entry.getKey().toString(),
                    0));
        }
        MaterialViews.publishContent(List.copyOf(bindings));
    }
}
