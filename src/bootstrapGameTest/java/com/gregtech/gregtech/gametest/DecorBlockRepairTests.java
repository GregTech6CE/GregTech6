package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.ColoredGlassBlock;
import com.gregtech.gregtech.block.misc.ConcreteBlock;
import com.gregtech.gregtech.block.misc.GregLanternBlock;
import com.gregtech.gregtech.block.misc.RoadStripeRailBlock;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Source-backed replacements for GT6 decorative placeholders. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DecorBlockRepairTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void concreteAndGlowGlassCarryAllSixteenColors(GameTestHelper h) {
        var level = h.getLevel();
        for (var color : DyeColor.values()) {
            ItemStack concrete = ConcreteBlock.coloredItem(GTDecorBlocks.CONCRETE.get(), color);
            ItemStack reinforced = ConcreteBlock.coloredItem(GTDecorBlocks.CONCRETE_REINFORCED.get(), color);
            ItemStack glow = ColoredGlassBlock.coloredItem(GTDecorBlocks.GLASS_GLOW.get(), color);
            h.assertTrue(ConcreteBlock.itemColor(concrete) == color
                            && ConcreteBlock.itemColor(reinforced) == color
                            && ColoredGlassBlock.itemColor(glow) == color,
                    "all GT6 dye metadata variants survive as BlockStateTag: " + color);
        }
        var red = GTDecorBlocks.CONCRETE.get().defaultBlockState()
                .setValue(ConcreteBlock.COLOR, DyeColor.RED);
        level.setBlock(h.absolutePos(POS), red, 3);
        var drops = Block.getDrops(red, level, h.absolutePos(POS), null);
        h.assertTrue(drops.size() == 1 && ConcreteBlock.itemColor(drops.get(0)) == DyeColor.RED,
                "mined concrete retains the original color");
        h.assertTrue(ConcreteBlock.tint(DyeColor.RED) == 0xFF0000
                        && ConcreteBlock.tint(DyeColor.LIGHT_GRAY) == 0xC0C0C0,
                "GT6's exact paint colors are used by the client tint handler");
        h.assertTrue(red.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        && red.is(BlockTags.NEEDS_STONE_TOOL)
                        && GTDecorBlocks.CONCRETE_REINFORCED.get().defaultBlockState()
                                .is(BlockTags.NEEDS_DIAMOND_TOOL),
                "survival mining honors GT6's concrete harvest levels 1 and 3");
        var cyanGlow = GTDecorBlocks.GLASS_GLOW.get().defaultBlockState()
                .setValue(ColoredGlassBlock.COLOR, DyeColor.CYAN);
        h.assertTrue(cyanGlow.getLightEmission(level, h.absolutePos(POS)) == 15
                        && cyanGlow.getLightBlock(level, h.absolutePos(POS)) == 0,
                "GT6 colored glow glass emits 15 light without blocking light");
        level.setBlock(h.absolutePos(POS), cyanGlow, 3);
        var glassDrops = Block.getDrops(cyanGlow, level, h.absolutePos(POS), null);
        h.assertTrue(glassDrops.size() == 2 && glassDrops.get(0).getCount() == 64
                        && glassDrops.get(1).getCount() == 16
                        && MaterialItem.isMaterialItem(glassDrops.get(0), MaterialPrefix.scrapGt, Materials.Glass),
                "GT6 full glass drops 80 glass scraps: " + glassDrops);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void roadStripeIsAFastThinRail(GameTestHelper h) {
        var rail = GTDecorBlocks.RAILROAD.get();
        var level = h.getLevel();
        for (int dz = 0; dz < 3; dz++) {
            var p = h.absolutePos(POS.offset(0, 0, dz));
            level.setBlockAndUpdate(p.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(p, rail.defaultBlockState().setValue(RoadStripeRailBlock.SHAPE,
                    RailShape.NORTH_SOUTH));
        }
        var p = h.absolutePos(POS.offset(0, 0, 1));
        var state = level.getBlockState(p);
        h.assertTrue(state.is(BlockTags.RAILS), "road stripe belongs to the minecart rail tag");
        h.assertTrue(state.getShape(level, p).bounds().maxY == 2.0 / 16,
                "road stripe has the vanilla 2px rail shape, not a full cube");
        var cart = EntityType.MINECART.create(level);
        h.assertTrue(cart != null && Math.abs(rail.getRailMaxSpeed(state, level, p, cart) - 0.5F) < 1e-6,
                "GT6 road stripe allows 0.5 block/t on a continuous straight");
        h.assertTrue(state.cycle(RoadStripeRailBlock.REFLECTOR).getValue(RoadStripeRailBlock.REFLECTOR),
                "crowbar/chisel/shears/knife have a separate reflector state");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void gregLanternFacesHorizontallyAndShines(GameTestHelper h) {
        var lantern = GTDecorBlocks.GREG_LANTERN.get();
        var state = lantern.defaultBlockState();
        h.assertTrue(state.getValue(GregLanternBlock.FACING) == Direction.NORTH,
                "Greg-o-lantern starts with a horizontal face");
        h.assertTrue(state.getLightEmission(h.getLevel(), h.absolutePos(POS)) == 15,
                "GT6 lantern emits 15 light");
        h.assertTrue(lantern.getLightBlock(state, h.getLevel(), h.absolutePos(POS)) == 0,
                "GT6 lantern does not block light");
        h.assertTrue(state.setValue(GregLanternBlock.FACING, Direction.WEST)
                .getValue(GregLanternBlock.FACING) == Direction.WEST,
                "front texture can face every horizontal direction");
        h.succeed();
    }
}
