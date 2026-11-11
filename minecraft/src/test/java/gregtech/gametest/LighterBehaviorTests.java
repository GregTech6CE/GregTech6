package gregtech.gametest;

import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.item.behavior.BehaviorLighter;
import gregtech.content.item.behavior.ItemBehaviors;
import gregtech.content.registry.GTMultiItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * GT6 {@code Behavior_Lighter} on the port's machines: a GT machine that declines the igniter
 * keeps the click and its fuel, accepting one spends a unit, and the tooltip carries the counter.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class LighterBehaviorTests {
    private static final String BATCH = "lighter_behavior";

    private LighterBehaviorTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void decliningMachineKeepsTheClickAndTheFuel(GameTestHelper helper) {
        BasicMachineBlock machine = basicMachine("centrifuge");
        helper.assertTrue(machine != null, "a centrifuge machine is registered");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, machine.defaultBlockState());

        ItemStack lighter = stack("lighter_full");
        var outcome = ItemBehaviors.useOn(
                helper.getLevel(), pos, Direction.UP, null, lighter, 0.5F, 0.5F, 0.5F);

        helper.assertFalse(outcome.acted(), "a machine that does not need ignition leaves the click to itself");
        helper.assertTrue(helper.getLevel().getBlockState(pos.above()).isAir(),
                "no fire block is placed on the machine face");
        helper.assertTrue(remaining(outcome.stack(), "lighter_full") == 100L,
                "the lighter keeps all 100 uses");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void ignitingMachineSpendsOneUse(GameTestHelper helper) {
        BasicMachineBlock machine = basicMachine("burnmixer");
        helper.assertTrue(machine != null, "a burner mixer machine is registered");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, machine.defaultBlockState());

        ItemStack lighter = stack("lighter_full");
        var outcome = ItemBehaviors.useOn(
                helper.getLevel(), pos, Direction.UP, null, lighter, 0.5F, 0.5F, 0.5F);

        helper.assertTrue(outcome.acted(), "an ignition machine handles the click");
        helper.assertTrue(remaining(outcome.stack(), "lighter_full") == 99L,
                "one fuel unit is spent");
        helper.assertTrue(helper.getLevel().getBlockState(pos.above()).isAir(),
                "the accepted click does not also light a fire");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void tooltipPrintsTheRemainingUsesAndChance(GameTestHelper helper) {
        BehaviorLighter.LighterSpec spec = ItemBehaviors.lighterSpec("match_box_full");
        helper.assertTrue(spec != null, "the match box family is registered");
        ItemStack full = stack("match_box_full");
        ItemStack used = spec.can().prepared(full);
        used = spec.can().spent(used, 4L);

        List<Component> lines = new ArrayList<>();
        ItemBehaviors.tooltip(used, lines);

        helper.assertTrue(lines.size() == 3, "hint, remaining uses and chance");
        helper.assertTrue(lines.get(1).getString().endsWith(" 60"), "the counter shows 60 uses left");
        helper.assertTrue(lines.get(2).getString().endsWith("90.0%"), "the 90% roll is printed");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void usedAndBrokenLighterVariantsAreHiddenFromCreative(GameTestHelper helper) {
        for (String hidden : List.of("match_box", "lighter", "shiny_lighter", "plastic_lighter",
                "plastic_lighter_broken")) {
            helper.assertTrue(GTMultiItems.creativeHidden(id(hidden)), hidden + " is hidden");
        }
        for (String visible : List.of("match", "match_box_full", "lighter_empty", "lighter_full",
                "shiny_lighter_empty", "shiny_lighter_full", "plastic_lighter_empty",
                "plastic_lighter_full")) {
            helper.assertFalse(GTMultiItems.creativeHidden(id(visible)), visible + " stays in the creative tab");
        }
        helper.succeed();
    }

    private static BasicMachineBlock basicMachine(String machineName) {
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            if (BuiltInRegistries.BLOCK.get(id) instanceof BasicMachineBlock block
                    && block.basicSpec() != null
                    && machineName.equals(block.basicSpec().machineName())
                    && block.basicSpec().tier() == 1) {
                return block;
            }
        }
        return null;
    }

    private static long remaining(ItemStack stack, String family) {
        BehaviorLighter.LighterSpec spec = ItemBehaviors.lighterSpec(family);
        return spec == null ? -1L : spec.can().remaining(stack);
    }

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id(path)));
    }

    private static ResourceLocation id(String path) {
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
*///?} else {
        return new ResourceLocation("gt6ce", path);
//?}
    }
}
