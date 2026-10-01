package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ensures every material referenced by the worldgen tables actually registers ore blocks.
 *
 * <p>GT6 equivalent: {@code WorldgenOresLarge}/{@code WorldgenOresSmall} called
 * {@code OreDictManager.triggerVisibility("ore<material>")} for their materials; in this port
 * ore blocks only register for materials flagged {@link MaterialProperty#GENERATE_ORE}, so the
 * flag is applied to all worldgen-referenced materials before item and block registration.</p>
 */
public final class GTWorldgenMaterials {
    private GTWorldgenMaterials() {}

    /** Must run after the material registry is loaded and before raw item and ore block registration. */
    public static void flagOreMaterials() {
        Set<GTMaterial> materials = new LinkedHashSet<>();
        for (GTOreVeins.OreVein vein : GTOreVeins.OVERWORLD_VEINS) {
            materials.add(vein.top());
            materials.add(vein.bottom());
            materials.add(vein.between());
            materials.add(vein.spread());
        }
        for (GTOreVeins.OreVein vein : GTOreVeins.END_VEINS) {
            materials.add(vein.top());
            materials.add(vein.bottom());
            materials.add(vein.between());
            materials.add(vein.spread());
        }
        for (GTOreVeins.SmallOre ore : GTOreVeins.OVERWORLD_SMALL_ORES) {
            materials.add(ore.material());
        }
        for (GTOreVeins.SmallOre ore : GTOreVeins.NETHER_SMALL_ORES) {
            materials.add(ore.material());
        }
        for (GTOreVeins.SmallOre ore : GTOreVeins.END_SMALL_ORES) {
            materials.add(ore.material());
        }
        for (GTBedrockOres.BedrockOre ore : GTBedrockOres.OVERWORLD) {
            materials.add(ore.material());
        }
        for (GTStoneLayersGen.LayerDef layer : GTStoneLayersGen.LAYERS) {
            for (GTStoneLayersGen.OreDef ore : layer.ores()) {
                materials.add(com.gregtech.gregtech.api.material.GTMaterialRegistry.get(ore.material()));
            }
        }
        for (GTStoneLayersGen.ContactDef contact : GTStoneLayersGen.CONTACTS) {
            for (GTStoneLayersGen.OreDef ore : contact.ores()) {
                materials.add(com.gregtech.gregtech.api.material.GTMaterialRegistry.get(ore.material()));
            }
        }

        for (GTMaterial material : materials) {
            if (material == null) continue;
            GTMaterial resolved = material.resolve();
            if (resolved == null || !resolved.isValid()) continue;
            resolved.put(MaterialProperty.GENERATE_ORE, MaterialProperty.GENERATE_ORE_PROCESSING);
        }
    }
}
