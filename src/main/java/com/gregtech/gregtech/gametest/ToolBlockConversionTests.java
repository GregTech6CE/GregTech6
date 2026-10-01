package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.RockOreBlock;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.ToolBlockConversionEvents;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Guards the GT6 hard-hammer / chisel world conversion ({@code convertBlockDrops}).
 * <p>
 * Regression these tests exist for: the port registered the {@code Hammer} and {@code Chisel}
 * recipe maps — including the ~1000 vanilla block processing routes — but no gameplay code read
 * them, so hitting a block with a hammer did nothing.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ToolBlockConversionTests {

    private static ItemStack hammer() {
        return GTToolHelper.write(GTToolItems.empty(GTToolType.HARD_HAMMER),
                GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood"));
    }

    /** GT6's hard hammer uses the whole-block recipe before considering normal ore drops. */
    @GameTest(template = "test_empty")
    public static void hammerConvertsOreSeamBlock(GameTestHelper helper) {
        Block ore = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", "block_ore_bauxite"));
        helper.assertTrue(ore instanceof RockOreBlock, "bauxite seam block is a GT6 rock ore");
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, ore.defaultBlockState());

        var result = ToolBlockConversionEvents.convert(level, level.getBlockState(pos),
                java.util.List.of(new ItemStack(ore)), hammer());

        helper.assertTrue(result.conversions() == 1, "hammering one seam block performs one conversion");
        var material = ((RockOreBlock) ore).material();
        ItemStack crushed = GTItems.getStack(MaterialPrefix.crushed,
                material.getTargetCrushingMaterial().resolve());
        helper.assertTrue(!crushed.isEmpty(), "GT6 bauxite has a crushed-ore form");
        var recipe = MachineRecipeMaps.Hammer.findRecipe(List.of(new ItemStack(ore)), List.of(), false, 1, 1);
        helper.assertTrue(recipe != null && recipe.mOutputs.length == 1,
                "the whole-block Hammer recipe exists for bauxite");
        helper.assertTrue(recipe.mOutputs[0].is(crushed.getItem())
                        && result.drops().size() == 1
                        && ItemStack.isSameItemSameTags(result.drops().get(0), recipe.mOutputs[0])
                        && result.drops().get(0).getCount() == recipe.mOutputs[0].getCount(),
                "GT6 hard hammer yields the whole-block crushed-ore recipe, not normal mining's two raw ores: "
                        + result.drops());
        helper.succeed();
    }

    /** Blocks the map does not know keep their normal drops. */
    @GameTest(template = "test_empty")
    public static void unknownBlockKeepsItsDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());

        var vanilla = java.util.List.of(new ItemStack(Items.DIRT, 1));
        var result = ToolBlockConversionEvents.convert(level, level.getBlockState(pos), vanilla, hammer());

        helper.assertTrue(result.conversions() == 0, "dirt has no hammer conversion");
        helper.assertTrue(result.drops().size() == 1 && result.drops().get(0).is(Items.DIRT),
                "dirt keeps its normal drop: " + result.drops());
        helper.succeed();
    }

    /** A tool that is not a hammer/chisel never converts anything. */
    @GameTest(template = "test_empty")
    public static void otherToolsDoNotConvert(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Block ore = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech", "block_ore_bauxite"));
        level.setBlockAndUpdate(pos, ore.defaultBlockState());

        var wrench = GTToolHelper.write(GTToolItems.empty(GTToolType.WRENCH),
                GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood"));
        var result = ToolBlockConversionEvents.convert(level, level.getBlockState(pos),
                java.util.List.of(new ItemStack(ore)), wrench);
        helper.assertTrue(result.conversions() == 0, "a wrench does not hammer blocks");
        helper.succeed();
    }

    /** The conversion must stay reachable: the map has to be populated. */
    @GameTest(template = "test_empty")
    public static void hammerAndChiselMapsArePopulated(GameTestHelper helper) {
        int hammer = com.gregtech.gregtech.data.MachineRecipeMaps.Hammer.mRecipeList.size();
        int chisel = com.gregtech.gregtech.data.MachineRecipeMaps.Chisel.mRecipeList.size();
        helper.assertTrue(hammer > 500, "hammer recipes registered: " + hammer);
        helper.assertTrue(chisel > 0, "chisel recipes registered: " + chisel);
        helper.succeed();
    }

    /** GT6 crowbar / universal spade unpack a block's drops through the Unboxinator map. */
    @GameTest(template = "test_empty")
    public static void crowbarUnpacksBoxedDrops(GameTestHelper helper) {
        var recipe = com.gregtech.gregtech.data.MachineRecipeMaps.Unboxinator.mRecipeList.stream()
                .filter(r -> !r.mFakeRecipe && r.mInputs.length > 0 && !r.mInputs[0].isEmpty())
                .findFirst().orElse(null);
        helper.assertTrue(recipe != null, "unboxinator has a real recipe to convert");

        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        // The block itself must not convert, so the per-drop path is exercised.
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());

        var crowbar = GTToolHelper.write(GTToolItems.empty(GTToolType.CROWBAR),
                GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood"));
        var boxed = recipe.mInputs[0].copyWithCount(1);
        var result = ToolBlockConversionEvents.convert(level, level.getBlockState(pos),
                java.util.List.of(boxed), crowbar);

        helper.assertTrue(result.conversions() == 1, "one boxed drop converts, got " + result.conversions());
        helper.assertTrue(result.drops().stream().noneMatch(stack -> ItemStack.isSameItemSameTags(stack, boxed)),
                "the boxed item is replaced by its contents: " + result.drops());
        helper.assertTrue(!result.drops().isEmpty(), "unboxing yields items");
        helper.succeed();
    }
}
