package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechConstants;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Material phase/admission rules extracted from saltnya's real smelting crucible.
 * World removal, sound, inventory and temperature storage stay on the platform.
 */
public final class CrucibleProcess {
    private CrucibleProcess() {}

    public record PhaseResult(int vaporizedStacks, boolean acidDestroyedHull) {}

    /** Run before the thermal step, preserving the original conversion/reaction order. */
    public static PhaseResult process(List<CrucibleMaterialStack> content, long temperature,
                                      long previousTemperature, boolean newContent, boolean acidProof) {
        int vaporized = 0;
        // GT6 alloys before discarding gases, so injected air can react with wrought iron.
        CrucibleReactions.react(content, temperature);
        List<CrucibleMaterialStack> pending = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            CrucibleMaterialStack stack = content.get(i);
            if (stack == null || stack.material == Materials.Invalid
                    || stack.material == Materials.Air || stack.amount <= 0) {
                content.remove(i--);
                continue;
            }
            GTMaterial material = stack.material;
            if (material.getDensity() <= 0.0012F || temperature >= material.getBoilingPoint()
                    || (temperature > GregTechConstants.C + 40 && material.has(MaterialProperty.FLAMMABLE)
                    && !material.hasAny(MaterialProperty.UNBURNABLE, MaterialProperty.MELTING))) {
                content.remove(i--);
                vaporized++;
                continue;
            }
            if (material.has(MaterialProperty.ACID) && !acidProof) {
                content.clear();
                return new PhaseResult(vaporized, true);
            }
            if (temperature >= material.getMeltingPoint()
                    && (material.getTargetSmeltingMaterial().resolve() != material
                    || previousTemperature < material.getMeltingPoint() || newContent)) {
                content.remove(i--);
                CrucibleMaterialStack.of(material.getTargetSmeltingMaterial(),
                        BigInteger.valueOf(stack.amount).multiply(BigInteger.valueOf(material.getTargetSmeltingAmount()))
                                .divide(BigInteger.valueOf(GTValues.U)).longValueExact()).addToList(pending);
            } else if (temperature < material.getMeltingPoint()
                    && (previousTemperature >= material.getMeltingPoint() || newContent)) {
                content.remove(i--);
                CrucibleMaterialStack.of(material, stack.amount).addToList(pending);
            }
        }
        for (CrucibleMaterialStack stack : pending) stack.addToList(content);
        return new PhaseResult(vaporized, false);
    }

    /** Returns the new mixed temperature, or null on rejection without changing content. */
    public static Long admit(List<CrucibleMaterialStack> content, List<CrucibleMaterialStack> incoming,
                             long capacity, double hullMassKg, long temperature, long incomingTemperature) {
        if (incoming.isEmpty()) return null;
        long existing = 0, added = 0;
        try {
            for (CrucibleMaterialStack stack : content) {
                if (stack == null || stack.amount < 0) return null;
                existing = Math.addExact(existing, stack.amount);
            }
            for (CrucibleMaterialStack stack : incoming) {
                if (stack == null || stack.material == null || !stack.material.isValid() || stack.amount <= 0) return null;
                added = Math.addExact(added, stack.amount);
            }
            if (Math.addExact(existing, added) > capacity) return null;
            long mixed = ThermalStep.mixTemperature(temperature, incomingTemperature,
                    hullMassKg + CrucibleMaterialStack.weight(content), CrucibleMaterialStack.weight(incoming));
            for (CrucibleMaterialStack stack : incoming) stack.addToList(content);
            CrucibleMaterialStack.consolidate(content);
            return mixed;
        } catch (IllegalArgumentException | ArithmeticException invalidInput) {
            return null;
        }
    }
}
