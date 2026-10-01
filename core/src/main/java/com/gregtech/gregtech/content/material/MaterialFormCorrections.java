package com.gregtech.gregtech.content.material;

import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.content.material.generated.CompoundMaterials;

/** Original form groups lost by the initial import; applied before item registration. */
final class MaterialFormCorrections {
    private MaterialFormCorrections() {}

    static void apply() {
        // GT6 field names the port spells differently, whose specs (`omd:CrO2`, `omd:Flour`) would
        // otherwise not resolve — both are real GT6 chemistry/food rows (§25):
        //   MT.java:8685 Chromium Dioxide ("CrO₂")      ← `Roasting: omd:Cr + Oxygen -> omd:CrO2`
        //   MT.java:9702 Wheat, local name "Flour"      ← `Mixer: omd:Flour + water -> food_dough`
        com.gregtech.gregtech.api.material.GTMaterialRegistry.registerAlias("CrO2", "ChromiumDioxide");
        com.gregtech.gregtech.api.material.GTMaterialRegistry.registerAlias("Flour", "Wheat");

        // Every source capsule material has INGOTS and DUSTS (MT.wax or G_INGOT).
        // OP.nugget follows OP.ingot. Restore forms before Forge registration so
        // high-temperature forming and empty-shell recycling use real material items.
        for(var spec:com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec.values())if(spec.shapeId().equals("cell")) {
            spec.material().put(MaterialProperty.DUST,MaterialProperty.GENERATE_DUST,
                    MaterialProperty.INGOT,MaterialProperty.GENERATE_INGOT,MaterialProperty.GENERATE_NUGGET);
        }
        // MT.Ke is Trinium (1260), an element with G_INGOT_MACHINE_ORES and SMITHABLE.
        // It is not the unrelated Kreknorite alloy (8759).
        Materials.Trinium.put(MaterialProperty.INGOT,MaterialProperty.GENERATE_INGOT,MaterialProperty.GENERATE_NUGGET,
                MaterialProperty.GENERATE_DUST,MaterialProperty.GENERATE_PLATE,MaterialProperty.GENERATE_STICKS,
                MaterialProperty.GENERATE_PARTS,MaterialProperty.SMITHABLE);
        // GT6 iodine() uses G_CRYSTAL_ORES; the original import retained only its gas form.
        // Restore the solid dust needed for MultiItemFood's iodine radiation medicine.
        Materials.Iodine.put(MaterialProperty.GENERATE_DUST);
        // Original canner registrations explicitly consume rods/bolts of these materials.
        for(var rod:com.gregtech.gregtech.content.nuclear.ReactorRodCatalog.ALL) {
            if(switch(rod.kind()){case NUCLEAR,BREEDER,ABSORBER,REFLECTOR,MODERATOR->true;default->false;}) {
                var material=com.gregtech.gregtech.api.material.GTMaterialRegistry.get(rod.material());
                if(!material.isValid())throw new IllegalStateException("Missing reactor material: "+rod.material());
                material.put(MaterialProperty.GENERATE_STICKS);
            }
        }

        // MT's gas/lquid acid factories set ACID even for bases such as ammonia.
        // Preserve original classifications rather than guessing from an English suffix.
        for(String name:new String[]{"Hydrochloric Acid","Hydrogen Fluoride","Ammonia","Nitric Acid",
                "Hydrosulfuric Acid","Sulfuric Acid","Disulfuric Acid","Hexafluorosilicic Acid",
                "Titanium Tetrachloride","Chloroauric Acid","Chloroplatinic Acid","Stannic Chloride",
                "Black Vitriol","Blue Vitriol","Green Vitriol","Red Vitriol","Pink Vitriol","Cyan Vitriol",
                "White Vitriol","Gray Vitriol","Martian Vitriol","Vitriol Of Clay","Aqua Regia"}){
            var material=com.gregtech.gregtech.api.material.GTMaterialRegistry.get(name.replace(" ",""));
            if(material.isValid())material.put(MaterialProperty.ACID);
        }

        // MT.Graphite explicitly has STICKS; its rod is used in reactor moderators.
        com.gregtech.gregtech.content.material.generated.OreMaterials.Graphite.put(MaterialProperty.GENERATE_STICKS);

        // MT.Ad is element(... G_INGOT_MACHINE_ORES, SMITHABLE ...), not a dust-only element.
        // Restore the sheet-metal chain needed by its original dense structural wall.
        com.gregtech.gregtech.content.material.generated.ElementMaterials.Adamantium.put(
                MaterialProperty.INGOT, MaterialProperty.GENERATE_INGOT, MaterialProperty.GENERATE_PLATE,
                MaterialProperty.GENERATE_MULTIINGOT, MaterialProperty.GENERATE_MULTIPLATE,
                MaterialProperty.GENERATE_DENSEPLATE, MaterialProperty.GENERATE_STICKS, MaterialProperty.GENERATE_PARTS, MaterialProperty.SMITHABLE);
        // MT.Rubber / Plastic: G_INGOT_MACHINE; these are formable, not smithable metals.
        for (var material : new com.gregtech.gregtech.api.material.GTMaterial[]{CompoundMaterials.Rubber, CompoundMaterials.Plastic}) {
            material.put(MaterialProperty.INGOT, MaterialProperty.GENERATE_INGOT, MaterialProperty.GENERATE_NUGGET,
                    MaterialProperty.GENERATE_PLATE, MaterialProperty.GENERATE_PARTS, MaterialProperty.GENERATE_STICKS,
                    MaterialProperty.GENERATE_FOIL, MaterialProperty.GENERATE_WIRE);
        }
        // OP.bouleGt.forceItemGeneration; crystal plates do not imply ordinary gems.
        for (String name : new String[]{"Silicon", "Germanium", "RedstoneAlloy", "NikolineAlloy"}) {
            var material = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(name);
            if (!material.isValid()) throw new IllegalStateException("Missing semiconductor: " + name);
            material.put(MaterialProperty.BOULE, MaterialProperty.GENERATE_PLATE, MaterialProperty.DUST, MaterialProperty.GENERATE_DUST);
        }
        // MT.java:3833 Blackstone is a *knappable* stone — `brick(9223, "Blackstone", …).qual(1, 5.0, 64, 1)`
        // — so like Stone (MT.java:1631) and Basalt it carries tool stats; the port declared it as a
        // dust-only supplemental material, which left its tool heads (and the recipes knapping them) out.
        var blackstone = com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Blackstone");
        if (blackstone != null && blackstone.isValid()) blackstone.setToolStats(1, 5.0F, 64, 1);

        // MT.Ice uses G_GEM_TRANSPARENT and SET_CUBE_SHINY, not a dust-only group.
        CompoundMaterials.Ice.put(MaterialProperty.GEM, MaterialProperty.DUST, MaterialProperty.GENERATE_DUST,
                MaterialProperty.GENERATE_PLATE, MaterialProperty.GENERATE_STICKS,
                MaterialProperty.GENERATE_PLANT, MaterialProperty.GENERATE_PROJECTILE,
                MaterialProperty.GENERATE_LENS).setTextureSet(MaterialTextureSet.CUBE_SHINY);

        // GT6's quartz() helper uses TD.G_QUARTZ (TD.java:604 =
        // PROJECTILES, DUSTS, PLANTS, PLATES, STICKS, GEMS), so Nether Quartz, Certus Quartz,
        // Charged Certus Quartz and Fluix all have a GEM form in the original; the import kept the
        // dust only, which is why gem-shaped recipes (e.g. the charged-certus chain) had no item.
        for (String name : new String[]{"NetherQuartz", "CertusQuartz", "ChargedCertusQuartz", "Fluix"}) {
            var material = com.gregtech.gregtech.api.material.GTMaterialRegistry.get(name);
            if (material == null || !material.resolve().isValid()) continue;
            material.resolve().put(MaterialProperty.GEM, MaterialProperty.GENERATE_PLATE,
                    MaterialProperty.GENERATE_STICKS, MaterialProperty.GENERATE_PLANT,
                    MaterialProperty.GENERATE_PROJECTILE);
        }

        // GT6 TD.G_INGOT_MACHINE / G_INGOT_MACHINE_ORES both include DUSTS (TD.java:609-610), so
        // every metal and alloy created with them has a dust. Materials created through other
        // helpers in this port may still lack it — sweep them so alloy dusts (Bronze, Steel,
        // Brass, Invar, …) exist everywhere, as they do in the original.
        for (var raw : com.gregtech.gregtech.api.material.GTMaterialRegistry.allMaterials()) {
            var material = raw.resolve();
            if (!material.isValid() || material.getId() <= 0) continue;
            if (!material.has(MaterialProperty.METAL) && !material.has(MaterialProperty.ALLOY)) continue;
            if (!material.has(MaterialProperty.INGOT) && !material.has(MaterialProperty.GENERATE_INGOT)) continue;
            if (material.has(MaterialProperty.DUST) || material.has(MaterialProperty.GENERATE_DUST)) continue;
            // GT6's *nd* helpers (metalmachnd / alloymachnd) are deliberately dust-free:
            // Wrought Iron (8655), Annealed Copper (8656), IronCompressed (8644), Cast Iron (8803).
            if (NO_DUST_MATERIALS.contains(material.getName())) continue;
            material.put(MaterialProperty.DUST, MaterialProperty.GENERATE_DUST);
        }
    }

    /**
     * GT6 materials whose factory is one of the {@code *nd} helpers, i.e. intentionally without a
     * dust form ({@code MT.java}: metalmachnd 8655/8656, alloymachnd 8644/8803).
     */
    private static final java.util.Set<String> NO_DUST_MATERIALS = java.util.Set.of(
            "WroughtIron", "AnnealedCopper", "IronCompressed", "CastIron");
}
