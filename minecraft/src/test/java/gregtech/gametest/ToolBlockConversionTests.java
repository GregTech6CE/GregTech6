package gregtech.gametest;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.ToolBlockConversionEvents;
import gregtech.content.item.GTToolItem;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.tool.GTToolType;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Only GT6's openable-crowbar blocks unpack through the Unboxinator when mined with those tools. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ToolBlockConversionTests {
    private static final String BATCH = "tool_block_conversion";

    private ToolBlockConversionTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void universalSpadeLeavesOrdinaryBlocksAlone(GameTestHelper helper) {
        Block sand = BuiltInRegistries.BLOCK
                .getOptional(ResourceLocation.tryParse("gt6ce:sand_magnetite"))
                .orElse(null);
        helper.assertTrue(sand != null, "black sand is registered");
        ItemStack spade = universalSpade();
        helper.assertFalse(spade.isEmpty(), "the universal spade item is registered");

        List<ItemStack> drops = List.of(new ItemStack(sand));
        var result = ToolBlockConversionEvents.convert(helper.getLevel(), sand.defaultBlockState(), drops, spade);
        helper.assertFalse(result.converted(), "black sand is not in GT6's openable-crowbar set");
        helper.assertTrue(result.drops().size() == 1 && result.drops().get(0).getItem() == sand.asItem(),
                "the block keeps its own drop");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void universalSpadeUnboxesCrowbarBlocks(GameTestHelper helper) {
        var iron = GTMaterialRegistry.get("Iron");
        ItemStack block = GTBlocks.getStack(BlockMaterialPrefix.blockIngot, iron);
        helper.assertFalse(block.isEmpty(), "blockIngot iron is registered");
        Block carrier = ((BlockItem) block.getItem()).getBlock();
        ItemStack spade = universalSpade();
        helper.assertFalse(spade.isEmpty(), "the universal spade item is registered");

        var result = ToolBlockConversionEvents.convert(
                helper.getLevel(), carrier.defaultBlockState(), List.of(block), spade);
        helper.assertTrue(result.converted(), "blockIngot is in GT6's openable-crowbar set");
        ItemStack ingots = GTItems.getStack(MaterialPrefix.ingot, iron, 9);
        helper.assertTrue(result.drops().stream()
                        .anyMatch(stack -> stack.getItem() == ingots.getItem() && stack.getCount() == 9),
                "one blockIngot unpacks into nine ingots");
        helper.succeed();
    }

    private static ItemStack universalSpade() {
        return GTToolItem.create(
                GTToolType.UNIVERSAL_SPADE, GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood"));
    }
}
