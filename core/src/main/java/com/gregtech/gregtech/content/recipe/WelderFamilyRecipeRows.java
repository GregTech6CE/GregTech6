package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import java.util.List;
/** Original WelderFamilyRecipes parameters; native stack/registry construction stays in the platforms. */
public final class WelderFamilyRecipeRows {
    private WelderFamilyRecipeRows(){}
    public record Row(MaterialPrefix input, int inCount, MaterialPrefix second, int secondCount,
                       int circuit, MaterialPrefix output,
                       com.gregtech.gregtech.api.prefix.BlockMaterialPrefix blockOutput) {
        static Row of(MaterialPrefix input, int inCount, int circuit, MaterialPrefix output) {
            return new Row(input, inCount, null, 0, circuit, output, null);
        }
        static Row of(MaterialPrefix input, int inCount, MaterialPrefix second, int secondCount, int circuit,
                      MaterialPrefix output) {
            return new Row(input, inCount, second, secondCount, circuit, output, null);
        }
        static Row block(MaterialPrefix input, int inCount, int circuit,
                         com.gregtech.gregtech.api.prefix.BlockMaterialPrefix output) {
            return new Row(input, inCount, null, 0, circuit, null, output);
        }
        long inUnits() {
            return input.getMaterialWeight() * inCount
                    + (second == null ? 0 : second.getMaterialWeight() * secondCount);
        }
        long outUnits() {
            return output != null ? output.getMaterialWeight() : blockOutput.getMaterialWeight();
        }
    }

    public static final List<Row> ROWS = List.of(
            Row.of(MaterialPrefix.ingot, 2, 2, MaterialPrefix.ingotDouble),
            Row.of(MaterialPrefix.ingot, 3, 3, MaterialPrefix.ingotTriple),
            Row.of(MaterialPrefix.ingot, 4, 4, MaterialPrefix.ingotQuadruple),
            Row.of(MaterialPrefix.ingot, 5, 5, MaterialPrefix.ingotQuintuple),
            Row.block(MaterialPrefix.ingot, 9, 9, BlockMaterialPrefix.blockSolid),
            Row.of(MaterialPrefix.bolt, 4, 4, MaterialPrefix.stick),
            Row.of(MaterialPrefix.bolt, 8, 8, MaterialPrefix.stickLong),
            Row.of(MaterialPrefix.stick, 2, 2, MaterialPrefix.stickLong),
            Row.of(MaterialPrefix.plateCurved, 4, MaterialPrefix.ring, 1, 0, MaterialPrefix.rotor));
}
