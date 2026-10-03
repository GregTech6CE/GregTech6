package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.recipe.MaterialArrowRules;
import com.gregtech.gregtech.content.recipe.PressAmmunitionRecipeRows;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.Set;
import java.util.stream.Collectors;

/** Fixed samples from Gregorius Techneticies' OP, Loader_Items and recipe handlers; no game launch. */
final class AmmunitionSourceSamples {
    private static int checks;
    static int verify() {
        var empty = MaterialItemDefinitions.all().stream().filter(d -> d.material() == Materials.Empty).toList();
        check(empty.size() == 5, "only five original EMPTY ammunition parts are registered");
        check(empty.stream().map(MaterialItemDefinitions.Definition::itemId).collect(Collectors.toSet()).equals(Set.of(
                "arrow_gt_wood_empty", "arrow_gt_plastic_empty", "bullet_gt_small_empty",
                "bullet_gt_medium_empty", "bullet_gt_large_empty")), "EMPTY registry identities are stable");
        check(!MaterialPrefix.ingot.isValidFor(Materials.Empty), "no empty ingots");
        check(!MaterialPrefix.arrowGtWood.isValidFor(MaterialSentinels.Invalid), "invalid sentinel stays hidden");
        check(MaterialIconDefinitions.resolveTextureSet(Materials.Empty) == MaterialTextureSet.NONE, "EMPTY uses source NONE icons");

        var shaft = MaterialChemistry.prefixMaterialWeights(Materials.Empty, MaterialPrefix.arrowGtWood);
        check(shaft.size() == 1 && shaft.get(0).amount() == 324_324_000, "headless shaft contains half a wood unit");
        var charged = MaterialChemistry.prefixMaterialWeights(Materials.Empty, MaterialPrefix.bulletGtSmall);
        check(charged.size() == 2, "charged casing has no fictitious EMPTY head");
        check(charged.get(0).material() == Materials.Brass && charged.get(0).amount() == 72_072_000, "small casing contains one ninth brass");
        check(charged.get(1).material() == Materials.Gunpowder && charged.get(1).amount() == 72_072_000, "small casing contains one ninth gunpowder");
        var arrow = MaterialChemistry.prefixMaterialWeights(Materials.Iron, MaterialPrefix.arrowGtPlastic);
        check(arrow.size() == 2 && arrow.get(0).amount() == 72_072_000 && arrow.get(1).amount() == 324_324_000,
                "OP actual amount U9 and plastic stick U2 take precedence over the stale quarter-unit comment");

        var press = PressAmmunitionRecipeRows.PRESS_ROWS;
        check(press.size() == 8, "two arrow and six bullet press rows");
        check(press.get(0).input() == MaterialPrefix.toolHeadArrow && press.get(0).component() == MaterialPrefix.arrowGtWood,
                "arrow head consumes an EMPTY wood shaft");
        check(press.get(1).component() == MaterialPrefix.arrowGtPlastic, "plastic arrow consumes an EMPTY plastic shaft");
        check(press.get(2).inCount() == 1 && press.get(2).component() == MaterialPrefix.bulletGtSmall, "small bullet consumes a charged casing");
        check(press.get(4).inCount() == 2 && press.get(4).ticks() == 32, "medium bullet needs two rounds and 32 ticks");
        check(press.get(6).inCount() == 3 && press.get(6).ticks() == 64, "large bullet needs three rounds and 64 ticks");
        check(PressAmmunitionRecipeRows.UNBOX_ROWS.size() == 5, "arrows and all bullet sizes recover their parts");
        check(!MaterialArrowRules.isArrow(MaterialPrefix.arrowGtWood, Materials.Empty), "shafts cannot be shot");
        check(MaterialArrowRules.isArrow(MaterialPrefix.arrowGtWood, Materials.Iron), "material wood arrow is ammunition");
        check(!MaterialArrowRules.isArrow(MaterialPrefix.bulletGtSmall, Materials.Iron), "bullets cannot be shot by a bow");
        check(MaterialArrowRules.speedMultiplier(MaterialPrefix.arrowGtWood) == 1.0F
                && MaterialArrowRules.speedMultiplier(MaterialPrefix.arrowGtPlastic) == 1.5F, "source arrow speeds");
        check(MaterialArrowRules.LIFETIME_TICKS == 3000, "source lifetime is 3000 ticks");
        var replacement = com.gregtech.gregtech.content.recipe.VanillaCraftingReplacements.rows().stream()
                .filter(row -> row.id().equals("minecraft:arrow")).findFirst().orElseThrow();
        check(replacement.pattern() == null && replacement.count() == 4
                && replacement.keys().values().stream().filter("item:gregtech:arrow_gt_wood_empty"::equals).count() == 4
                && replacement.keys().values().contains("item:minecraft:flint"), "vanilla arrow shortcut is replaced by four shafts and one flint");
        return checks;
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
