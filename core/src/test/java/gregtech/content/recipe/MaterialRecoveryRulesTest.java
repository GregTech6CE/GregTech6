package gregtech.content.recipe;

import static gregtech.api.material.GTValues.*;
import static org.junit.jupiter.api.Assertions.*;

import gregtech.api.material.MaterialComponent;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.material.Materials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemCompositionScaling;
import gregtech.engine.material.ItemCompositionSource;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MaterialRecoveryRulesTest {
    @BeforeAll
    static void init() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void damagedIronSwordRetainsIronWhenWoodIsBelowTheFinestDust() {
        var sword = new ItemComposition(
                null,
                List.of(
                        MaterialComponent.of(Materials.Iron, 17 * U9),
                        MaterialComponent.of(GTMaterialRegistry.get("Wood"), U2)),
                Materials.Iron,
                List.of(new ItemCompositionSource(
                        ItemCompositionSource.Kind.VANILLA_JSON, "iron_sword", "test", "", "")));
        for (int remaining = 1; remaining <= 3; remaining++) {
            var worn = ItemCompositionScaling.scale(sword, remaining, 128);
            for (boolean staticPrefix : List.of(false, true)) {
                var plan = MaterialRecoveryRules.planShredderDamaged(worn, 1, staticPrefix);
                assertTrue(plan.accepted());
                assertEquals(1, plan.outputs().size());
                var iron = plan.outputs().get(0);
                assertEquals(Materials.Iron.resolve(), iron.material());
                assertEquals(
                        List.of(new MaterialRecoveryRules.DustPile(MaterialPrefix.dustDiv72, remaining)),
                        iron.dust().piles());
                long woodUnits = ItemCompositionScaling.scale(U2, remaining, 128);
                assertTrue(plan.skippedComponents()
                        .contains(new MaterialRecoveryRules.SkippedComponent(
                                "Wood", woodUnits, MaterialRecoveryRules.ShredderRejection.UNREPRESENTABLE_AMOUNT)));
            }
            assertFalse(MaterialRecoveryRules.planShredder(worn, 12).accepted());
            assertFalse(MaterialRecoveryRules.planShredderStatic(worn, 12).accepted());
        }
    }

    @Test
    void damagedInputWithNoRepresentableDustIsRejected() {
        var wood = new ItemComposition(null, GTMaterialRegistry.get("Wood"), U72 - 1);
        var plan = MaterialRecoveryRules.planShredderDamaged(wood, 1, false);
        assertEquals(MaterialRecoveryRules.ShredderRejection.UNREPRESENTABLE_AMOUNT, plan.rejection());
        assertTrue(plan.outputs().isEmpty());
    }

    @Test
    void damagedInputCannotDiscardARepresentableRemainderWhenSlotsAreFull() {
        var iron = new ItemComposition(null, Materials.Iron, 64 * U + U72);
        var limited = MaterialRecoveryRules.planShredderDamaged(iron, 1, false);
        assertEquals(MaterialRecoveryRules.ShredderRejection.OUTPUT_LIMIT, limited.rejection());
        assertTrue(limited.outputs().isEmpty());
        var fits = MaterialRecoveryRules.planShredderDamaged(iron, 2, false);
        assertEquals(2, fits.stackCount());
        assertEquals(64 * U + U72, fits.outputs().get(0).dust().representedUnits());
    }
}
