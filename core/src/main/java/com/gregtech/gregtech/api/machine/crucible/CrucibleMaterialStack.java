package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialMass;

import java.util.ArrayList;
import java.util.List;

/** One material amount inside a crucible (GT6 {@code OreDictMaterialStack}). */
public final class CrucibleMaterialStack {
    public GTMaterial material;
    public long amount;

    public CrucibleMaterialStack(GTMaterial material, long amount) {
        this.material = material == null ? Materials.Invalid : material.resolve();
        this.amount = amount;
    }

    public static CrucibleMaterialStack of(GTMaterial material, long amount) {
        return new CrucibleMaterialStack(material, amount);
    }

    public CrucibleMaterialStack copy() {
        return new CrucibleMaterialStack(material, amount);
    }

    public double weightKg() {
        return MaterialMass.kilograms(material, amount);
    }

    public void addToList(List<CrucibleMaterialStack> list) {
        material = material == null ? Materials.Invalid : material.resolve();
        if (material == Materials.Invalid || amount <= 0) {
            return;
        }
        int id = material.getId();
        for (CrucibleMaterialStack entry : list) {
            GTMaterial entryMaterial = entry.material.resolve();
            if (id > 0 && entryMaterial.getId() == id) {
                entry.material = entryMaterial;
                entry.amount += amount;
                return;
            }
            if (id <= 0 && entryMaterial.getName().equals(material.getName())) {
                entry.material = entryMaterial;
                entry.amount += amount;
                return;
            }
        }
        list.add(copy());
    }

    /** Merge stacks that share the same material id (GT6 single-list semantics). */
    public static void consolidate(List<CrucibleMaterialStack> list) {
        if (list.isEmpty()) {
            return;
        }
        List<CrucibleMaterialStack> merged = new ArrayList<>();
        for (CrucibleMaterialStack stack : list) {
            if (stack == null || stack.material == Materials.Invalid || stack.amount <= 0) {
                continue;
            }
            stack.addToList(merged);
        }
        list.clear();
        list.addAll(merged);
    }

    public static long total(List<CrucibleMaterialStack> stacks) {
        long total = 0;
        for (CrucibleMaterialStack stack : stacks) {
            if (stack != null) {
                total += stack.amount;
            }
        }
        return total;
    }

    public static double weight(List<CrucibleMaterialStack> stacks) {
        double total = 0;
        for (CrucibleMaterialStack stack : stacks) {
            if (stack != null) {
                total += stack.weightKg();
            }
        }
        return total;
    }

}
