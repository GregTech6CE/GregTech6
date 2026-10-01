package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;
/** Original PressAmmunitionRecipes parameters; native stack/registry construction stays in the platforms. */
public final class PressAmmunitionRecipeRows {
    private PressAmmunitionRecipeRows(){}
    public record PressRow(MaterialPrefix input, int inCount, String moldId, MaterialPrefix output, long ticks) {}

    public record UnboxRow(MaterialPrefix input, MaterialPrefix output, int outCount, String moldId) {}

    public static final List<PressRow> PRESS_ROWS = List.of(
            new PressRow(MaterialPrefix.round, 1, "press_bullet_casing_shape_small",
                    MaterialPrefix.bulletGtSmall, 16),
            new PressRow(MaterialPrefix.bolt, 1, "press_bullet_casing_shape_small",
                    MaterialPrefix.bulletGtSmall, 16),
            new PressRow(MaterialPrefix.round, 2, "press_bullet_casing_shape_medium",
                    MaterialPrefix.bulletGtMedium, 32),
            new PressRow(MaterialPrefix.bolt, 2, "press_bullet_casing_shape_medium",
                    MaterialPrefix.bulletGtMedium, 32),
            new PressRow(MaterialPrefix.round, 3, "press_bullet_casing_shape_large",
                    MaterialPrefix.bulletGtLarge, 64),
            new PressRow(MaterialPrefix.bolt, 3, "press_bullet_casing_shape_large",
                    MaterialPrefix.bulletGtLarge, 64));

    public static final List<UnboxRow> UNBOX_ROWS = List.of(
            new UnboxRow(MaterialPrefix.bulletGtSmall, MaterialPrefix.dustTiny, 1, "press_bullet_casing_shape_small"),
            new UnboxRow(MaterialPrefix.bulletGtMedium, MaterialPrefix.dustTiny, 2, "press_bullet_casing_shape_medium"),
            new UnboxRow(MaterialPrefix.bulletGtLarge, MaterialPrefix.dustTiny, 3, "press_bullet_casing_shape_large"));
}
