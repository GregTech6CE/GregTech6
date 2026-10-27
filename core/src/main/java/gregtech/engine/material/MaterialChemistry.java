package gregtech.engine.material;

import gregtech.api.material.GTValues;
import gregtech.api.material.*;

import gregtech.content.material.Materials;
import gregtech.content.data.MaterialPrefix;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds GT6-style tooltip chemical formulas from material composition trees.
 */
public final class MaterialChemistry {
    private static final String SUBSCRIPTS = "\u2080\u2081\u2082\u2083\u2084\u2085\u2086\u2087\u2088\u2089";

    private MaterialChemistry() {}

    public static void rebuildAll() {
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.hasComposition()) {
                material.setTooltipChemical(buildFormula(material));
            }
        }
    }

    public static String buildFormula(GTMaterial material) {
        if (!material.hasComposition()) {
            return material.getTooltipChemical();
        }
        List<MaterialComponent> components = material.getCompositionComponents();
        if (components.isEmpty()) {
            return material.getTooltipChemical();
        }
        if (components.size() == 1 && components.get(0).amount() == GTValues.U) {
            return chemicalPart(components.get(0).material());
        }

        StringBuilder out = new StringBuilder();
        long divider = Math.max(1L, material.getCompositionDivider());
        for (MaterialComponent component : components) {
            String part = chemicalPart(component.material());
            if (part.isEmpty()) {
                continue;
            }
            out.append(part);
            appendSubscript(out, component.amount(), divider, component.material());
        }
        return out.toString();
    }

    /** F3+H rows: primary material only (matches GT6 {@code getAllMaterialWeights}). */
    public static List<WeightedMaterial> materialWeights(GTMaterial material, long prefixWeight) {
        if (!material.isValid()) {
            return List.of();
        }
        return List.of(new WeightedMaterial(material, prefixWeight));
    }

    /** F3+H rows including prefix-specific components (e.g. steel in wrench heads). */
    public static List<WeightedMaterial> prefixMaterialWeights(GTMaterial material, MaterialPrefix prefix) {
        if (prefix == null || (!material.isValid()
                && !("Empty".equals(material.getName()) && prefix.hasEmptyAmmunitionForm()))) {
            return List.of();
        }
        if (prefix == MaterialPrefix.toolHeadWrench) {
            return headComponents(material, GTValues.U * 4, Materials.Steel, GTValues.U4 + GTValues.U9 * 2);
        }
        if (prefix == MaterialPrefix.toolHeadChainsaw) {
            return headComponents(material, GTValues.U * 2, Materials.Steel, GTValues.U * 9 / 2);
        }
        if (prefix == MaterialPrefix.toolHeadDrill) {
            return headComponents(material, GTValues.U * 4, Materials.Steel, GTValues.U * 4);
        }
        if (prefix == MaterialPrefix.toolHeadPickaxeGem) {
            // OP716: ANY.Iron targets MT.Fe, not the WroughtIron group member.
            return headComponents(material, GTValues.U, Materials.Iron, GTValues.U9 * 26);
        }
        if (prefix == MaterialPrefix.glasstube) {
            List<WeightedMaterial> out = new ArrayList<>(2);
            out.add(new WeightedMaterial(material, GTValues.U9));
            out.add(new WeightedMaterial(Materials.Glass, GTValues.U3));
            return List.copyOf(out);
        }
        if (prefix == MaterialPrefix.plantGtTwig) {
            // OP357/730: U/9 of the named material plus a generic ANY.Wood stick (U/2).
            GTMaterial primary = material.resolve();
            GTMaterial wood = gregtech.content.material.generated.WoodMaterials.Wood.resolve();
            if (primary == wood) return List.of(new WeightedMaterial(wood, GTValues.U9 + GTValues.U2));
            return List.of(new WeightedMaterial(primary, GTValues.U9), new WeightedMaterial(wood, GTValues.U2));
        }
        if (prefix == MaterialPrefix.arrowGtWood) {
            List<WeightedMaterial> out = new ArrayList<>(2);
            if (material.isValid()) out.add(new WeightedMaterial(material, GTValues.U9));
            out.add(new WeightedMaterial(gregtech.content.material.generated.WoodMaterials.Wood, GTValues.U2));
            return List.copyOf(out);
        }
        if (prefix == MaterialPrefix.arrowGtPlastic) {
            List<WeightedMaterial> out = new ArrayList<>(2);
            if (material.isValid()) out.add(new WeightedMaterial(material, GTValues.U9));
            out.add(new WeightedMaterial(Materials.Plastic, GTValues.U2));
            return List.copyOf(out);
        }
        if (prefix == MaterialPrefix.bulletGtSmall) {
            List<WeightedMaterial> out = new ArrayList<>(3);
            if (material.isValid()) out.add(new WeightedMaterial(material, GTValues.U9));
            out.add(new WeightedMaterial(Materials.Brass, GTValues.U9));
            out.add(new WeightedMaterial(Materials.Gunpowder, GTValues.U9));
            return List.copyOf(out);
        }
        if (prefix == MaterialPrefix.bulletGtMedium) {
            List<WeightedMaterial> out = new ArrayList<>(3);
            if (material.isValid()) out.add(new WeightedMaterial(material, GTValues.U9 * 2));
            out.add(new WeightedMaterial(Materials.Brass, GTValues.U9 * 2));
            out.add(new WeightedMaterial(Materials.Gunpowder, GTValues.U9 * 2));
            return List.copyOf(out);
        }
        if (prefix == MaterialPrefix.bulletGtLarge) {
            List<WeightedMaterial> out = new ArrayList<>(3);
            if (material.isValid()) out.add(new WeightedMaterial(material, GTValues.U3));
            out.add(new WeightedMaterial(Materials.Brass, GTValues.U3));
            out.add(new WeightedMaterial(Materials.Gunpowder, GTValues.U3));
            return List.copyOf(out);
        }
        return materialWeights(material, prefix.getMaterialWeight());
    }

    private static String chemicalPart(GTMaterial material) {
        if (!material.isValid()) {
            return "";
        }
        if (material.hasComposition()) {
            String nested = buildFormula(material);
            if (material.isElementLike()) {
                return nested;
            }
            return "(" + nested + ")";
        }
        String chemical = material.getTooltipChemical();
        if (chemical != null && !chemical.isEmpty()) {
            return chemical;
        }
        return material.getLocalName();
    }

    private static void appendSubscript(StringBuilder out, long amount, long divider, GTMaterial componentMaterial) {
        long units = amount / GTValues.U;
        if (units <= 1 && amount == GTValues.U) {
            return;
        }
        long componentDivider = componentMaterial.getCompositionDivider();
        if (componentMaterial.hasComposition() && divider > 0 && componentDivider > 0 && units % componentDivider == 0) {
            long normalized = units / componentDivider;
            if (normalized > 1) {
                out.append(subscript(normalized));
            }
            return;
        }
        if (units > 1) {
            out.append(subscript(units));
        }
    }

    public static String subscript(long value) {
        if (value < 0) {
            return "";
        }
        if (value < 10) {
            return String.valueOf(SUBSCRIPTS.charAt((int) value));
        }
        StringBuilder sb = new StringBuilder();
        String digits = Long.toString(value);
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(SUBSCRIPTS.charAt(c - '0'));
            }
        }
        return sb.toString();
    }

    /** Keep shared display and recoverable registration totals identical for composite heads. */
    private static List<WeightedMaterial> headComponents(GTMaterial primary, long primaryUnits,
                                                         GTMaterial secondary, long secondaryUnits) {
        primary = primary.resolve();
        secondary = secondary.resolve();
        if (primary == secondary)
            return List.of(new WeightedMaterial(primary, Math.addExact(primaryUnits, secondaryUnits)));
        return List.of(new WeightedMaterial(primary, primaryUnits), new WeightedMaterial(secondary, secondaryUnits));
    }

}
