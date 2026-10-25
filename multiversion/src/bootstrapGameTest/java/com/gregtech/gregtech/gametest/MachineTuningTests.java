package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.addon.MachineTuning;
import com.gregtech.gregtech.api.addon.MachineTuningRules;
import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.content.machine.BasicMachineCatalog;
import com.gregtech.gregtech.content.machine.BasicMachineDefinitions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.List;

/** A tuning patch rebuilds the spec and leaves every undeclared field at its built-in value. */
@GameTestHolder("gregtech_tuning")
@PrefixGameTestTemplate(false)
public final class MachineTuningTests {
    private MachineTuningTests() {}

    @GameTest(template = "test_empty")
    public static void tuningChangesOnlyDeclaredFields(GameTestHelper h) {
        var parameters = BasicMachineCatalog.specifications().stream()
                .filter(row -> row.machineName().equals("crusher") && row.tier() == 1)
                .findFirst().orElseThrow();
        var baseline = BasicMachineDefinitions.from(parameters);
        var key = new MachineTuningRules.Key("crusher", 1);
        var merged = MachineTuningRules.merge(List.of(new MachineTuningRules.Declared("aa",
                new MachineTuning("crusher", 1, 4, 64L, 32L, 128L, 5000, false, true))));
        var tuned = BasicMachineDefinitions.from(parameters, merged.get(key));

        h.assertTrue(tuned.parallelLimit() == 4 && tuned.energyIn() == 64L
                        && tuned.energyInMin() == 32L && tuned.energyInMax() == 128L
                        && tuned.processingEfficiency() == 5000 && !tuned.constantEnergy()
                        && tuned.cheapOverclocking(),
                "declared tuning fields are applied");
        h.assertTrue(tuned.id().equals(baseline.id()) && tuned.material() == baseline.material()
                        && tuned.recipeMap() == baseline.recipeMap() && tuned.faceConfig().equals(baseline.faceConfig())
                        && tuned.tier() == baseline.tier() && tuned.energyType().equals(baseline.energyType()),
                "untouched fields keep their built-in values");
        h.assertTrue(tuned.cheapOverclockingDeclared() && tuned.energyRangeDeclared(),
                "tuned fields become declared");
        var again = BasicMachineDefinitions.from(parameters);
        h.assertTrue(again.parallelLimit() == baseline.parallelLimit()
                        && again.energyIn() == baseline.energyIn()
                        && again.energyInMin() == baseline.energyInMin()
                        && again.energyInMax() == baseline.energyInMax()
                        && again.processingEfficiency() == baseline.processingEfficiency(),
                "an untuned build stays at baseline");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void partialTuningChecksInheritedEnergyRange(GameTestHelper h) {
        var p = BasicMachineCatalog.specifications().stream()
                .filter(row -> row.machineName().equals("crusher") && row.tier() == 1)
                .findFirst().orElseThrow();
        // Each partial declaration is valid alone. The contradiction appears only
        // after from() fills its missing fields from the built-in specification.
        reject(h, p, tuning(p, p.energyInMax() + 1, null, null));
        reject(h, p, tuning(p, p.energyInMin() - 1, null, null));
        reject(h, p, tuning(p, null, p.energyIn() + 1, null));
        reject(h, p, tuning(p, null, null, p.energyIn() - 1));
        reject(h, p, tuning(p, null, p.energyIn() + 1, p.energyInMax() + 1));
        var key = new MachineTuningRules.Key(p.machineName(), p.tier());
        var partialFromTwoAddons = MachineTuningRules.merge(List.of(
                new MachineTuningRules.Declared("first", tuning(p, p.energyInMax() + 1, null, null)),
                new MachineTuningRules.Declared("second", tuning(p, null, p.energyInMin(), null))));
        reject(h, p, partialFromTwoAddons.get(key));
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void tuningEnergyBoundariesAndUntouchedValuesRemainValid(GameTestHelper h) {
        var p = BasicMachineCatalog.specifications().stream()
                .filter(row -> row.machineName().equals("crusher") && row.tier() == 1)
                .findFirst().orElseThrow();
        for (long boundary : new long[]{p.energyInMin(), p.energyInMax()}) {
            var result = BasicMachineDefinitions.from(p, merged(tuning(p, boundary, null, null)));
            h.assertTrue(result.energyIn() == boundary && result.energyInMin() == p.energyInMin()
                            && result.energyInMax() == p.energyInMax(),
                    "inclusive boundary passes with inherited min/max");
        }
        var result = BasicMachineDefinitions.from(p, merged(tuning(p, null, p.energyIn(), p.energyIn())));
        h.assertTrue(result.energyInMin() == result.energyIn() && result.energyInMax() == result.energyIn(),
                "explicit singleton range containing inherited nominal passes");
        var parallelOnly = new MachineTuning(p.machineName(), p.tier(), 4, null, null, null, null, null, null);
        result = BasicMachineDefinitions.from(p, merged(parallelOnly));
        h.assertTrue(result.parallelLimit() == 4 && result.energyIn() == p.energyIn()
                        && result.energyInMin() == p.energyInMin() && result.energyInMax() == p.energyInMax(),
                "non-energy tuning keeps the valid built-in energy fields");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void allBuiltinsAndTuTuningsUseValidEffectiveRanges(GameTestHelper h) {
        int timed = 0;
        for (var p : BasicMachineCatalog.specifications()) {
            var baseline = BasicMachineDefinitions.from(p);
            h.assertTrue(baseline.energyIn() >= baseline.energyInMin()
                            && baseline.energyIn() <= baseline.energyInMax(),
                    "untuned built-in energy sits in range: " + p.id());
            if (!p.energyType().equals("TU")) continue;
            timed++;
            h.assertTrue(baseline.energyInMin() == 1 && baseline.energyInMax() == 16,
                    "TU baseline retains 1..16 packet window");
            reject(h, p, tuning(p, 17L, null, null));
            var tuned = BasicMachineDefinitions.from(p, merged(tuning(p, 16L, null, null)));
            h.assertTrue(tuned.energyIn() == 16 && tuned.energyInMin() == 1 && tuned.energyInMax() == 16,
                    "TU upper boundary remains accepted");
        }
        h.assertTrue(timed > 0, "TU machines were audited");
        h.succeed();
    }

    private static MachineTuning tuning(BasicMachineParameters p, Long energy, Long min, Long max) {
        return new MachineTuning(p.machineName(), p.tier(), null, energy, min, max, null, null, null);
    }

    private static MachineTuningRules.Merged merged(MachineTuning tuning) {
        return MachineTuningRules.merge(List.of(new MachineTuningRules.Declared("tuning_test", tuning)))
                .get(new MachineTuningRules.Key(tuning.machineName(), tuning.tier()));
    }

    private static void reject(GameTestHelper h, BasicMachineParameters p, MachineTuning tuning) {
        reject(h, p, merged(tuning));
    }

    private static void reject(GameTestHelper h, BasicMachineParameters p, MachineTuningRules.Merged patch) {
        try {
            BasicMachineDefinitions.from(p, patch);
        } catch (IllegalStateException failure) {
            h.assertTrue(failure.getMessage().contains(p.machineName() + "/T" + p.tier())
                            && failure.getMessage().contains("outside"),
                    "effective-range failure identifies the machine and cause");
            return;
        }
        throw new AssertionError("Accepted an out-of-range partial tuning: " + patch.tuning());
    }
}
