package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;
/** Original AutoclaveRecipes parameters; native stack/registry construction stays in the platforms. */
public final class AutoclaveRecipeRows {
    private AutoclaveRecipeRows(){}
    public record Row(MaterialPrefix input, int inCount, int steam, int distW, long ticks, int circuit,
                       MaterialPrefix output, int outCount) {}

    public static final List<Row> ROWS = List.of(
            new Row(MaterialPrefix.dustSmall, 1, 25600, 120, 800, 0, MaterialPrefix.gemChipped, 1),
            new Row(MaterialPrefix.dustSmall, 2, 51200, 240, 1600, 1, MaterialPrefix.gemFlawed, 1),
            new Row(MaterialPrefix.dustSmall, 4, 102400, 480, 3200, 2, MaterialPrefix.gem, 1),
            new Row(MaterialPrefix.dustSmall, 8, 204800, 960, 6400, 3, MaterialPrefix.gemFlawless, 1),
            new Row(MaterialPrefix.dustSmall, 16, 409600, 1920, 12800, 4, MaterialPrefix.gemExquisite, 1),
            new Row(MaterialPrefix.dustSmall, 32, 819200, 3840, 25600, 5, MaterialPrefix.gemLegendary, 1),
            new Row(MaterialPrefix.dust, 1, 102400, 480, 3200, 0, MaterialPrefix.gemChipped, 4),
            new Row(MaterialPrefix.dust, 1, 102400, 480, 3200, 1, MaterialPrefix.gemFlawed, 2),
            new Row(MaterialPrefix.dust, 1, 102400, 480, 3200, 2, MaterialPrefix.gem, 1),
            new Row(MaterialPrefix.dust, 2, 204800, 960, 6400, 3, MaterialPrefix.gemFlawless, 1),
            new Row(MaterialPrefix.dust, 4, 409600, 1920, 12800, 4, MaterialPrefix.gemExquisite, 1),
            new Row(MaterialPrefix.dust, 8, 819200, 3840, 25600, 5, MaterialPrefix.gemLegendary, 1));
}
