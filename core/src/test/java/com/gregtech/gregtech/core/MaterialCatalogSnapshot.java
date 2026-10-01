package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Test-only ordered observation of the original object graph, never a runtime catalog. */
public final class MaterialCatalogSnapshot {
    private MaterialCatalogSnapshot() {}

    public static String materialKey(GTMaterial material) {
        return material == null ? "null" : material.getId() + ":" + material.getName();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, GTMaterial> aliases() throws ReflectiveOperationException {
        var names = GTMaterialRegistry.class.getDeclaredField("BY_NAME");
        names.setAccessible(true);
        return new TreeMap<>((Map<String, GTMaterial>) names.get(null));
    }

    public static String sha256() throws Exception {
        List<String> rows = new ArrayList<>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            StringBuilder row = new StringBuilder("M|").append(materialKey(material))
                    .append('|').append(materialKey(material.resolve()))
                    .append('|').append(material.getLocalName()).append('|').append(material.getDisplayNameFallback())
                    .append('|').append(material.getTranslationKey()).append('|').append(material.getColor())
                    .append('|').append(material.getAlpha()).append('|').append(material.getTextureSet())
                    .append('|').append(material.getAtomicProperties()).append('|').append(material.getMeltingPoint())
                    .append('|').append(material.getBoilingPoint()).append('|').append(Float.toHexString(material.getDensity()))
                    .append('|').append(material.getTooltipChemical()).append('|').append(material.getProperties())
                    .append('|').append(material.getSourceMod()).append('|').append(material.getSourceMod() == null ? "null"
                            : material.getSourceMod().name + ":" + material.getSourceMod().prefix + ":" + material.getSourceMod().isLoaded())
                    .append('|').append(material.getCompositionDivider())
                    .append('|').append(material.getToolTypes()).append('|').append(material.getToolQuality())
                    .append('|').append(Float.toHexString(material.getToolSpeed())).append('|').append(material.getToolDurability())
                    .append('|').append(material.getFurnaceBurnTime()).append('|').append(materialKey(material.getTargetBurningMaterial()))
                    .append('|').append(material.getTargetBurningAmount()).append('|').append(materialKey(material.getTargetPulverMaterial()))
                    .append('|').append(material.getTargetPulverAmount()).append('|').append(materialKey(material.getTargetCrushingMaterial()))
                    .append('|').append(material.getTargetCrushingAmount()).append('|').append(materialKey(material.getTargetSmeltingMaterial()))
                    .append('|').append(material.getTargetSmeltingAmount()).append('|').append(material.getOreMultiplier())
                    .append('|').append(material.getOreProcessingMultiplier());
            for (MaterialComponent component : material.getCompositionComponents())
                row.append("|C:").append(materialKey(component.material())).append(':').append(component.amount());
            for (GTMaterial byproduct : material.getByProducts()) row.append("|B:").append(materialKey(byproduct));
            row.append("|R:").append(material.getReRegistrations().stream().map(MaterialCatalogSnapshot::materialKey).sorted().toList());
            row.append("|A:").append(material.getAliasesToThis().stream().map(MaterialCatalogSnapshot::materialKey).sorted().toList());
            rows.add(row.toString());
        }
        for (var name : aliases().entrySet())
            rows.add("N|" + name.getKey() + "|" + materialKey(name.getValue()) + "|" + materialKey(name.getValue().resolve()));
        for (MaterialPrefix prefix : PrefixRegistry.all()) {
            rows.add("P|" + prefix.getName() + '|' + prefix.getRegistryName() + '|' + prefix.getDisplayName()
                    + '|' + prefix.getTextureFileName() + '|' + prefix.isHiddenFromCreative() + '|' + prefix.heatDamage()
                    + '|' + prefix.getMaterialWeight());
            for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
                rows.add("F|" + prefix.getName() + '|' + materialKey(material) + '|' + prefix.isValidFor(material)
                        + '|' + prefix.getItemId(material) + '|' + prefix.getTagPath(material));
            }
        }
        Collections.sort(rows);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(String.join("\n", rows).getBytes(StandardCharsets.UTF_8)));
    }
}
