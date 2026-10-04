/* Gregorius Techneticies / GregTech-6 Team source rules, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;

/** OP:570-573 / OreDictListenerItem_Washing: one item and one water level per server tick. */
public final class MaterialWashingRules {
    private MaterialWashingRules() {}
    public record Row(MaterialPrefix input, MaterialPrefix output, MaterialPrefix byproduct, int chance) {
        public boolean givesByproduct(int roll) { return byproduct != null && roll > 0 && roll < chance; }
        // Washing cleans the original material; shredding instead follows mTargetPulver.
        public GTMaterial outputMaterial(GTMaterial inputMaterial) { return inputMaterial; }
    }
    public static final List<Row> ROWS = List.of(
            new Row(MaterialPrefix.crushed, MaterialPrefix.crushedPurified, MaterialPrefix.crushedPurifiedTiny, 2),
            new Row(MaterialPrefix.dustImpure, MaterialPrefix.dust, null, 1),
            new Row(MaterialPrefix.dustPure, MaterialPrefix.dust, null, 1),
            new Row(MaterialPrefix.dustRefined, MaterialPrefix.dust, null, 1));
    public static Row row(MaterialPrefix prefix) {
        for (var row : ROWS) if (row.input() == prefix) return row;
        return null;
    }
    public record Cell(int x, int y, int z) {}
    public static Cell cell(double x, double y, double z) {
        return new Cell((int) Math.floor(x), (int) Math.floor(y - .25), (int) Math.floor(z));
    }
    public record Step(int remainingCount, int remainingWater) {}
    public static Step step(int count, int water, boolean outputAvailable) {
        return count > 0 && water > 0 && outputAvailable ? new Step(count - 1, water - 1) : null;
    }
    /** UT.Code.select uses the input material when its byproduct list is empty. */
    public static GTMaterial byproductMaterial(GTMaterial material, int choice) {
        var candidates = material.getByProducts();
        return candidates.isEmpty() ? material : candidates.get(choice);
    }
}
