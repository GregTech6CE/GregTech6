package gregtech.engine.machine;

import gregtech.api.material.FuelMaterialHelper;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.GregTechConstants;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Material fuel values extracted from saltnya FurnaceFuelValue; item/tag fallback is platform code. */
public final class MaterialFuelRules {
    private MaterialFuelRules() {}

    public record AshPile(GTMaterial material, int tinyDust) {}

    public static long heatUnits(long burnValue, int efficiency) {
        return burnValue * GregTechConstants.EU_PER_FURNACE_TICK * efficiency / 10000L;
    }

    public static int ashTinyCount(GTMaterial material, long itemAmount) {
        return FuelMaterialHelper.rule(material)
                .flatMap(rule -> rule.residue())
                .map(residue -> ashTinyCount(residue.amount(), itemAmount))
                .orElse(0);
    }

    /**
     * Ash from the exact component set used for the burn value. Distinct residues cannot fit the
     * single output slot and are rejected rather than silently taking the first component.
     */
    public static Optional<List<AshPile>> ashResidues(List<MaterialComponent> components) {
        var merged = new ArrayList<AshPile>();
        for (MaterialComponent component : components) {
            GTMaterial material = component.material().resolve();
            var residue = FuelMaterialHelper.rule(material)
                    .flatMap(rule -> rule.residue())
                    .orElse(null);
            if (residue == null) continue;
            GTMaterial ash = GTMaterialRegistry.get(residue.material().name());
            int count = ashTinyCount(residue.amount(), component.amount());
            if (count <= 0) continue;
            int index = indexOf(merged, ash);
            if (index < 0) {
                merged.add(new AshPile(ash, count));
            } else {
                long total = (long) merged.get(index).tinyDust() + count;
                if (total > 64) return Optional.empty();
                merged.set(index, new AshPile(ash, (int) total));
            }
        }
        return merged.size() > 1 ? Optional.empty() : Optional.of(List.copyOf(merged));
    }

    private static int ashTinyCount(long ashAmount, long itemAmount) {
        long scaled = scaleUnits(
                ashAmount, GTValues.U, itemAmount > 0 ? itemAmount : MaterialPrefix.dust.getMaterialWeight());
        return scaled <= 0 ? 0 : (int) Math.min(64L, Math.max(1L, scaled / GregTechConstants.U9));
    }

    private static int indexOf(List<AshPile> piles, GTMaterial material) {
        for (int i = 0; i < piles.size(); i++) if (piles.get(i).material() == material) return i;
        return -1;
    }

    public static long getBurnValue(GTMaterial material, MaterialPrefix prefix, long amount) {
        if (prefix == MaterialPrefix.unit) return 0; // Viewer material placeholders are not physical fuel.
        GTMaterial mat = material.resolve();
        if (!mat.isValid()) {
            return 0;
        }
        long burnTime =
                FuelMaterialHelper.rule(mat).map(rule -> rule.burnTime()).orElse(0L);
        if (mat.getName().equals("Bark")) burnTime = Math.max(100, burnTime);
        if (burnTime <= 0 && prefix == null) {
            return 0;
        }

        long scaled;
        if (prefix == null) {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
        } else if (prefix == MaterialPrefix.oreRaw) {
            scaled = burnTime;
        } else if (isBurnablePrefix(prefix)) {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
            scaled = applyWoodPrefixRules(prefix, mat, scaled);
        } else {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
        }
        return clampFuelValue(scaled);
    }

    private static long applyWoodPrefixRules(MaterialPrefix prefix, GTMaterial material, long burnTime) {
        if (!isWoodFamily(material)) {
            return burnTime;
        }
        if (prefix == MaterialPrefix.stick) {
            return Math.max(GregTechConstants.TICKS_PER_SMELT / 2L, burnTime);
        }
        if (prefix == MaterialPrefix.stickLong) {
            return Math.max(GregTechConstants.TICKS_PER_SMELT, burnTime);
        }
        return burnTime;
    }

    /** Original GT_API_Proxy: blockRaw is 10U fuel; wood bundle minima apply to blocks, not plates. */
    public static long blockBurnValue(GTMaterial material, gregtech.engine.prefix.BlockMaterialPrefix prefix) {
        if (prefix == gregtech.engine.prefix.BlockMaterialPrefix.ore
                || prefix == gregtech.engine.prefix.BlockMaterialPrefix.oreSmall) return 0;
        long time = 0;
        for (var part : gregtech.content.block.BlockMaterialWeights.contained(material, prefix))
            time += scaleUnits(
                    FuelMaterialHelper.rule(part.material())
                            .map(rule -> rule.burnTime())
                            .orElse(0L),
                    GTValues.U,
                    part.amount());
        if (prefix == gregtech.engine.prefix.BlockMaterialPrefix.blockRaw)
            time = FuelMaterialHelper.rule(material.resolve())
                            .map(rule -> rule.burnTime())
                            .orElse(0L)
                    * 10;
        if (isWoodFamily(material)) {
            if (prefix == gregtech.engine.prefix.BlockMaterialPrefix.blockPlate)
                time = Math.max(time, GregTechConstants.TICKS_PER_SMELT * 27L / 2);
        }
        return clampFuelValue(time);
    }

    private static boolean isWoodFamily(GTMaterial material) {
        return material.has(MaterialProperty.WOOD)
                || material.resolve() == gregtech.content.material.generated.WoodMaterials.Wood.resolve();
    }

    private static boolean isBurnablePrefix(MaterialPrefix prefix) {
        return prefix == MaterialPrefix.stick
                || prefix == MaterialPrefix.stickLong
                || prefix == MaterialPrefix.plate
                || prefix == MaterialPrefix.plateDouble
                || prefix == MaterialPrefix.plateTriple
                || prefix == MaterialPrefix.plateQuadruple
                || prefix == MaterialPrefix.plateQuintuple
                || prefix == MaterialPrefix.plateDense
                || prefix == MaterialPrefix.dust
                || prefix == MaterialPrefix.dustSmall
                || prefix == MaterialPrefix.dustTiny
                || prefix == MaterialPrefix.dustDiv72
                || prefix == MaterialPrefix.gem;
    }

    /** GT6 {@code UT.Code.units}. */
    public static long scaleUnits(long value, long unit, long amount) {
        if (value <= 0 || amount <= 0) {
            return 0;
        }
        return value * amount / unit;
    }

    public static long clampFuelValue(long value) {
        return Math.min(32000L, Math.max(0L, value));
    }
}
