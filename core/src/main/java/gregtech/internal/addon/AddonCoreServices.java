package gregtech.internal.addon;

import gregtech.api.addon.AddonRuntime;
import gregtech.api.addon.GregTechMaterials;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialSentinels;
import gregtech.internal.prefix.PrefixRegistry;

import java.util.ArrayList;
import java.util.List;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Installs the core addon entry. Call this before material initialization. */
public final class AddonCoreServices {
    private static final GregTechMaterials.Catalog MATERIALS = new Materials();

    private AddonCoreServices() {}

    public static void bind() {
        AddonRuntime.bind(new Services());
    }

    private static final class Services implements AddonRuntime.Services {
        @Override public GregTechMaterials.Catalog materials() { return MATERIALS; }

        @Override public boolean prefixExists(String name) {
            return name != null && PrefixRegistry.byName(name) != null;
        }
    }

    private static final class Materials implements GregTechMaterials.Catalog {
        @Override public GregTechMaterials.Material find(String name) {
            return new Handle(GTMaterialRegistry.get(name));
        }

        @Override public GregTechMaterials.Material find(int id) {
            return new Handle(GTMaterialRegistry.get(id));
        }

        @Override public GregTechMaterials.Material invalid() {
            return new Handle(MaterialSentinels.Invalid);
        }

        @Override public Iterable<GregTechMaterials.Material> all() {
            return GTMaterialRegistry.allMaterials().stream().<GregTechMaterials.Material>map(Handle::new).toList();
        }
    }

    private static final class Handle implements GregTechMaterials.Material {
        private final GTMaterial material;

        private Handle(GTMaterial material) {
            this.material = material == null ? MaterialSentinels.Invalid : material;
        }

        @Override public String name() { return material.getName(); }
        @Override public int id() { return material.getId(); }
        @Override public boolean valid() { return material.isValid(); }
        @Override public int color() { return material.getColor(); }
        @Override public int meltingPoint() { return material.getMeltingPoint(); }
        @Override public int boilingPoint() { return material.getBoilingPoint(); }
        @Override public float density() { return material.getDensity(); }
        @Override public String formula() { return material.getResolvedTooltipChemical(); }
        @Override public String translationKey() { return material.getTranslationKey(); }
        @Override public long mass() { return material.getMass(); }
        @Override public int toolQuality() { return material.getToolQuality(); }
        @Override public float toolSpeed() { return material.getToolSpeed(); }
        @Override public long toolDurability() { return material.getToolDurability(); }

        @Override public List<GregTechMaterials.Component> composition() {
            var components = new ArrayList<GregTechMaterials.Component>();
            for (var component : material.getCompositionComponents()) {
                components.add(new GregTechMaterials.Component(component.material().getName(), component.amount()));
            }
            return List.copyOf(components);
        }

        @Override public List<String> byproducts() {
            var names = new ArrayList<String>();
            for (GTMaterial byproduct : material.getByProducts()) {
                if (byproduct == null) continue;
                names.add(byproduct.resolve().getName());
            }
            return List.copyOf(names);
        }

        @Override public GregTechMaterials.Material resolve() {
            GTMaterial resolved = material.resolve();
            return resolved == material ? this : new Handle(resolved);
        }
    }
}
