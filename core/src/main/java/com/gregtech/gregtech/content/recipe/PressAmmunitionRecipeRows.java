package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;

/** GT6 Loader_Recipes_Handlers:252-258,452-456. EMPTY forms are consumed shafts/charged casings. */
public final class PressAmmunitionRecipeRows {
    private PressAmmunitionRecipeRows() {}
    public record PressRow(MaterialPrefix input, int inCount, MaterialPrefix component,
                           MaterialPrefix output, long ticks) {}
    public record UnboxRow(MaterialPrefix input, MaterialPrefix output, int outCount,
                           MaterialPrefix component) {}
    public static final List<PressRow> PRESS_ROWS = List.of(
            new PressRow(MaterialPrefix.toolHeadArrow,1,MaterialPrefix.arrowGtWood,MaterialPrefix.arrowGtWood,16),
            new PressRow(MaterialPrefix.toolHeadArrow,1,MaterialPrefix.arrowGtPlastic,MaterialPrefix.arrowGtPlastic,16),
            new PressRow(MaterialPrefix.round,1,MaterialPrefix.bulletGtSmall,MaterialPrefix.bulletGtSmall,16),
            new PressRow(MaterialPrefix.bolt,1,MaterialPrefix.bulletGtSmall,MaterialPrefix.bulletGtSmall,16),
            new PressRow(MaterialPrefix.round,2,MaterialPrefix.bulletGtMedium,MaterialPrefix.bulletGtMedium,32),
            new PressRow(MaterialPrefix.bolt,2,MaterialPrefix.bulletGtMedium,MaterialPrefix.bulletGtMedium,32),
            new PressRow(MaterialPrefix.round,3,MaterialPrefix.bulletGtLarge,MaterialPrefix.bulletGtLarge,64),
            new PressRow(MaterialPrefix.bolt,3,MaterialPrefix.bulletGtLarge,MaterialPrefix.bulletGtLarge,64));
    public static final List<UnboxRow> UNBOX_ROWS = List.of(
            new UnboxRow(MaterialPrefix.arrowGtWood,MaterialPrefix.toolHeadArrow,1,MaterialPrefix.arrowGtWood),
            new UnboxRow(MaterialPrefix.arrowGtPlastic,MaterialPrefix.toolHeadArrow,1,MaterialPrefix.arrowGtPlastic),
            new UnboxRow(MaterialPrefix.bulletGtSmall,MaterialPrefix.dustTiny,1,MaterialPrefix.bulletGtSmall),
            new UnboxRow(MaterialPrefix.bulletGtMedium,MaterialPrefix.dustTiny,2,MaterialPrefix.bulletGtMedium),
            new UnboxRow(MaterialPrefix.bulletGtLarge,MaterialPrefix.dustTiny,3,MaterialPrefix.bulletGtLarge));
}
