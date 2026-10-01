package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The machine index is common data and can be checked without starting the JEI client. */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class JeiMachineIndexTests {
    @GameTest(template = "test_empty")
    public static void allEnergyVariantsShareRecipeMap(GameTestHelper helper) {
        var index = RecipeMachines.collect();
        int checked = 0;
        for (var entry : MachineRegistry.basicMachines()) {
            if (!entry.isPresent() || entry.get().basicSpec().machineName().equals("fusionreactor")) continue;
            var map = MachineRecipeMaps.byMachineName(entry.get().basicSpec().machineName());
            if (map == null) continue;
            helper.assertTrue(index.containsKey(map) && index.get(map).stream().anyMatch(stack -> stack.is(entry.get().asItem())),
                    "machine catalyst remains in its common recipe map: " + entry.getId());
            checked++;
        }
        helper.assertTrue(checked > 0, "registered machine catalysts tested");
        for (var stacks : index.values()) {
            helper.assertTrue(stacks.stream().map(stack -> stack.getItem()).distinct().count() == stacks.size(), "no duplicate catalysts");
        }
        helper.succeed();
    }
}
