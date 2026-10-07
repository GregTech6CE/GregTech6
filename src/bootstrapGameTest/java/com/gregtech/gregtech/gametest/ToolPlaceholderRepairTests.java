package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.AdvancedButtonBlock;
import com.gregtech.gregtech.block.tool.CoinMoldBlock;
import com.gregtech.gregtech.block.tool.ScaffoldBlock;
import com.gregtech.gregtech.blockentity.tool.AdvancedButtonBlockEntity;
import com.gregtech.gregtech.blockentity.tool.CoinMoldBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Original behavior beyond the three utility blocks' static F10 models. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ToolPlaceholderRepairTests {
    @GameTest(template = "test_empty")
    public static void advancedButtonShapeTouchesSupportOnEveryFace(GameTestHelper helper) {
        var block = (AdvancedButtonBlock) ForgeRegistries.BLOCKS.getValue(
                com.gregtech.gregtech.GregTech.id("advanced_button"));
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (Direction facing : Direction.values()) {
            var state = block.defaultBlockState().setValue(AdvancedButtonBlock.FACING, facing);
            var shape = state.getShape(helper.getLevel(), pos);
            var axis = facing.getAxis();
            double expectedMin = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : 14 / 16.0;
            helper.assertTrue(Math.abs(shape.min(axis) - expectedMin) < 0.00001
                            && Math.abs(shape.max(axis) - expectedMin - 2 / 16.0) < 0.00001,
                    "button must touch its supporting face for " + facing);
            helper.assertTrue(state.getCollisionShape(helper.getLevel(), pos).isEmpty(),
                    "GT6 button has a selection box but no physical collision");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void scaffoldSupportDesignAndCollapse(GameTestHelper helper) {
        var level = helper.getLevel();
        Block registered = ForgeRegistries.BLOCKS.getValue(com.gregtech.gregtech.GregTech.id("scaffold"));
        helper.assertTrue(registered instanceof ScaffoldBlock, "scaffold has structural GT6 behavior");
        ScaffoldBlock scaffold = (ScaffoldBlock) registered;
        BlockPos lower = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos upper = lower.above();
        helper.assertTrue(!scaffold.getCollisionShape(scaffold.defaultBlockState(), level, lower,
                        net.minecraft.world.phys.shapes.CollisionContext.empty()).isEmpty(),
                "Forge can bake the scaffold collision shape without an entity");
        var walker = helper.makeMockPlayer();
        var standing = scaffold.defaultBlockState().getCollisionShape(level, lower,
                net.minecraft.world.phys.shapes.CollisionContext.of(walker));
        walker.setShiftKeyDown(true);
        var sneaking = scaffold.defaultBlockState().getCollisionShape(level, lower,
                net.minecraft.world.phys.shapes.CollisionContext.of(walker));
        helper.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(standing, sneaking,
                        net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST),
                "hatch collision changes for a sneaking player instead of reusing Forge's cached shape");
        level.setBlock(lower.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(lower, scaffold.defaultBlockState(), 3);
        level.setBlock(upper, scaffold.defaultBlockState(), 3);
        scaffold.tick(level.getBlockState(lower), level, lower, level.random);
        scaffold.tick(level.getBlockState(upper), level, upper, level.random);
        helper.assertTrue(level.getBlockState(lower).getValue(ScaffoldBlock.DESIGN) == 2,
                "lower connected segment has GT6's open frame");
        helper.assertTrue(level.getBlockState(upper).getValue(ScaffoldBlock.DESIGN) == 1,
                "upper connected segment has GT6's hatch");
        level.removeBlock(lower, false);
        scaffold.tick(level.getBlockState(upper), level, upper, level.random);
        helper.assertTrue(level.getBlockState(upper).isAir(), "unsupported scaffold falls off");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void advancedButtonPulseSwitchAndLamp(GameTestHelper helper) {
        var level = helper.getLevel();
        Block registered = ForgeRegistries.BLOCKS.getValue(com.gregtech.gregtech.GregTech.id("advanced_button"));
        helper.assertTrue(registered instanceof AdvancedButtonBlock, "advanced button is functional");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, registered.defaultBlockState().setValue(AdvancedButtonBlock.FACING, Direction.NORTH), 3);
        AdvancedButtonBlockEntity button = (AdvancedButtonBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(button != null && button.press(), "default button can be pressed");
        helper.assertTrue(level.getBlockState(pos).getValue(AdvancedButtonBlock.ACTIVE)
                && registered.getSignal(level.getBlockState(pos), level, pos, Direction.SOUTH) == 15,
                "pressed button emits strength 15");
        for (int i = 0; i < 21; i++) button.serverTick();
        helper.assertTrue(!level.getBlockState(pos).getValue(AdvancedButtonBlock.ACTIVE),
                "default GT6 pulse ends after 20 ticks plus expiration check");
        button.cycleMode(false); // button -> switch
        button.press();
        for (int i = 0; i < 25; i++) button.serverTick();
        helper.assertTrue(button.isActive(), "switch remains active without a pulse timer");
        button.adjustStrength(-1);
        helper.assertTrue(registered.getSignal(level.getBlockState(pos), level, pos, Direction.SOUTH) == 14,
                "cutter changes output strength");
        button.cycleMode(true);  // switch -> 20-tick button
        button.cycleMode(true);  // button -> lamp
        helper.assertTrue(button.isLampMode()
                && registered.getSignal(level.getBlockState(pos), level, pos, Direction.SOUTH) == 0,
                "lamp mode does not emit power");
        level.setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        button.updateLampInput();
        helper.assertTrue(button.isActive()
                && registered.getSignal(level.getBlockState(pos), level, pos, Direction.SOUTH) == 0,
                "lamp mirrors incoming redstone without feeding it back");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void coinMoldPlateToCoinAndAutomation(GameTestHelper helper) {
        var level = helper.getLevel();
        Block registered = ForgeRegistries.BLOCKS.getValue(com.gregtech.gregtech.GregTech.id("coin_mold"));
        helper.assertTrue(registered instanceof CoinMoldBlock, "coin mold has one-slot GT6 behavior");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, registered.defaultBlockState(), 3);
        CoinMoldBlockEntity mold = (CoinMoldBlockEntity) level.getBlockEntity(pos);
        ItemStack plate = GTItems.getStack(MaterialPrefix.plateTiny, Materials.Copper, 1);
        ItemStack coin = GTItems.getStack(MaterialPrefix.coin, Materials.Copper, 1);
        helper.assertTrue(mold != null && !plate.isEmpty() && !coin.isEmpty(),
                "tiny copper plate and copper coin exist");
        helper.assertTrue(mold.insertPlate(plate, false) && plate.isEmpty(), "one plate enters mold");
        var handler = mold.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        helper.assertTrue(handler != null && handler.extractItem(0, 1, false).isEmpty(),
                "automation cannot extract unfinished plate");
        helper.assertTrue(mold.strike() && ItemStack.isSameItem(mold.contents(), coin)
                        && mold.contents().getTag() != null
                        && mold.contents().getTag().contains("gt.coin.shape.0.0"),
                "hard hammer converts the plate into a copper coin bearing its die pattern");
        ItemStack extracted = handler.extractItem(0, 1, false);
        helper.assertTrue(ItemStack.isSameItem(extracted, coin)
                        && extracted.getTag() != null
                        && extracted.getTag().contains("gt.coin.shape.1.15"),
                "automation may extract the finished coin without losing its die pattern");
        helper.assertTrue(mold.contents().isEmpty(), "extraction empties the single mold slot");
        ItemStack secondPlate = GTItems.getStack(MaterialPrefix.plateTiny, Materials.Copper, 1);
        helper.assertTrue(mold.insertPlate(secondPlate, false), "mold accepts another plate");
        level.removeBlock(pos, false);
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                        .anyMatch(entity -> ItemStack.isSameItemSameTags(entity.getItem(),
                                GTItems.getStack(MaterialPrefix.plateTiny, Materials.Copper, 1))),
                "breaking the mold releases its unfinished plate");
        helper.succeed();
    }
}
