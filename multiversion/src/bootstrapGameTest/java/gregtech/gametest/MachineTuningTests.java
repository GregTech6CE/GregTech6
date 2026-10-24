package gregtech.gametest;

import gregtech.api.addon.MachineTuning;
import gregtech.api.addon.MachineTuningRules;
import gregtech.content.machine.BasicMachineCatalog;
import gregtech.content.machine.BasicMachineDefinitions;
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
}
