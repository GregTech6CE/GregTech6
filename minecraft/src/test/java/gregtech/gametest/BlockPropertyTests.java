package gregtech.gametest;

import gregtech.content.block.BlackSandDefinitions;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTDungeonBlocks;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.BlockMaterialPrefix;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Block-property regressions that only need a loaded registry. */
//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gt6ce")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gt6ce")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class BlockPropertyTests {
    @GameTest(template = "test_empty", batch = "block_properties", timeoutTicks = 600)
    public static void netherPortalHasCollision(GameTestHelper helper) {
        var state = GTDungeonBlocks.PORTAL_NETHER.get().defaultBlockState();
        helper.assertFalse(
                state.getCollisionShape(helper.getLevel(), BlockPos.ZERO).isEmpty(),
                "nether mini portal has no collision");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "block_properties", timeoutTicks = 600)
    public static void vanillaBlocksOnlyReplaceIngotBlocks(GameTestHelper helper) {
        var ingot = GTBlocks.getStack(BlockMaterialPrefix.blockIngot, Materials.Iron);
        helper.assertFalse(ingot.isEmpty(), "missing blockIngot/Iron");
        helper.assertTrue(
                MaterialUnification.canonical(ingot).is(Items.IRON_BLOCK),
                "blockIngot/Iron should canonicalize to the vanilla block");
        for (var prefix : List.of(BlockMaterialPrefix.blockSolid, BlockMaterialPrefix.blockPlate)) {
            var stack = GTBlocks.getStack(prefix, Materials.Iron);
            helper.assertFalse(stack.isEmpty(), "missing " + prefix.getName() + "/Iron");
            helper.assertTrue(
                    MaterialUnification.canonical(stack).is(stack.getItem()),
                    prefix.getName() + "/Iron must stay a GT block");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "block_properties", timeoutTicks = 600)
    public static void blackSandNeedsNoToolForDrops(GameTestHelper helper) {
        for (String id : BlackSandDefinitions.IDS) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
            helper.assertTrue(block != Blocks.AIR, "missing block " + id);
            var state = block.defaultBlockState();
            helper.assertFalse(state.requiresCorrectToolForDrops(), id + " still requires a correct tool");
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_SHOVEL), id + " is missing mineable/shovel");
        }
        helper.succeed();
    }
}
