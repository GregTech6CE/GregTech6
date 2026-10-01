package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;
/** Original CrusherFamilyRecipes parameters; native stack/registry construction stays in the platforms. */
public final class CrusherFamilyRecipeRows {
    private CrusherFamilyRecipeRows(){}
    public record Row(MaterialPrefix input, MaterialPrefix output, int outCount, long multiplier) {}

    public static final List<Row> ROWS = List.of(
            new Row(MaterialPrefix.gemLegendary, MaterialPrefix.gemExquisite, 2, 256),
            new Row(MaterialPrefix.gemExquisite, MaterialPrefix.gemFlawless, 2, 256),
            new Row(MaterialPrefix.gemFlawless, MaterialPrefix.gem, 2, 256),
            new Row(MaterialPrefix.gem, MaterialPrefix.gemFlawed, 2, 256),
            new Row(MaterialPrefix.gemFlawed, MaterialPrefix.gemChipped, 2, 256),
            new Row(MaterialPrefix.bouleGt, MaterialPrefix.gem, 4, 256),
            new Row(MaterialPrefix.gemChipped, null, 0, 256),
            new Row(MaterialPrefix.rockGt, null, 0, 16));
}
