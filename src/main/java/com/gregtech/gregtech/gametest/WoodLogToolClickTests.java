package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.wood.TreeHoleBlock;
import com.gregtech.gregtech.block.wood.WoodBeamBlock;
import com.gregtech.gregtech.block.wood.WoodLogBlock;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTWoods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 BlockTreeLogA/B/C tool-click conversion of live tree logs. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class WoodLogToolClickTests {
    private static final BlockPos POS = new BlockPos(49000, 210, 49000);

    private static ItemStack tool(GTToolType type) {
        return GTToolItem.create(type, Materials.Steel, GTMaterialRegistry.get("Wood"));
    }

    private static int count(net.minecraft.world.entity.player.Player player, Item item) {
        int amount = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(item)) amount += stack.getCount();
        return amount;
    }

    private static BlockHitResult hit(Direction side) {
        return new BlockHitResult(Vec3.atCenterOf(POS), side, POS, false);
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void everySpeciesAxeClickYieldsItsOwnBeamAndBark(GameTestHelper helper) {
        var level = helper.getLevel();
        level.getBlockState(POS);
        var player = helper.makeMockSurvivalPlayer();
        Direction.Axis[] axes = Direction.Axis.values();
        for (WoodSpecies species : WoodSpecies.values()) {
            WoodLogBlock log = GTWoods.log(species);
            WoodBeamBlock beam = GTWoods.beam(species);
            helper.assertTrue(log.species() == species && beam.species() == species,
                    species.id() + " is paired with its own log and beam");
            ItemStack expected = species == WoodSpecies.CINNAMON
                    ? new ItemStack(ForgeRegistries.ITEMS.getValue(
                            ResourceLocation.fromNamespaceAndPath("gregtech", "cinnamon_bark")))
                    : GTItems.getStack(MaterialPrefix.dust, Materials.Bark);
            helper.assertTrue(!expected.isEmpty() && log.toolProduct().is(expected.getItem()),
                    species.id() + " has the GT6 bark or cinnamon product");
            player.getInventory().clearContent();
            ItemStack axe = tool(GTToolType.AXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            Direction.Axis axis = axes[species.ordinal() % axes.length];
            var state = log.defaultBlockState().setValue(WoodLogBlock.AXIS, axis);
            level.setBlock(POS, state, 3);
            int before = count(player, expected.getItem());
            InteractionResult result = state.use(level, player, InteractionHand.MAIN_HAND, hit(Direction.UP));
            var after = level.getBlockState(POS);
            helper.assertTrue(result.consumesAction() && after.is(beam)
                            && after.getValue(WoodBeamBlock.AXIS) == axis,
                    species.id() + " strips into the matching beam without changing its axis");
            helper.assertTrue(count(player, expected.getItem()) == before + 1
                            && axe.getDamageValue() == 5,
                    species.id() + " gives one product for GT6's 500-point axe cost");
        }
        level.setBlock(POS, Blocks.AIR.defaultBlockState(), 3);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void sawAndKnifeCostAndHandDrillStillMatchGt6(GameTestHelper helper) {
        var level = helper.getLevel();
        level.getBlockState(POS);
        var player = helper.makeMockSurvivalPlayer();
        for (GTToolType type : new GTToolType[]{GTToolType.SAW, GTToolType.KNIFE}) {
            WoodLogBlock log = GTWoods.log(WoodSpecies.CINNAMON);
            player.getInventory().clearContent();
            ItemStack held = tool(type);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var state = log.defaultBlockState().setValue(WoodLogBlock.AXIS, Direction.Axis.X);
            level.setBlock(POS, state, 3);
            InteractionResult result = state.use(level, player, InteractionHand.MAIN_HAND, hit(Direction.UP));
            helper.assertTrue(result.consumesAction() && level.getBlockState(POS).is(GTWoods.beam(WoodSpecies.CINNAMON))
                            && level.getBlockState(POS).getValue(WoodBeamBlock.AXIS) == Direction.Axis.X
                            && held.getDamageValue() == 10,
                    type + " makes a cinnamon beam at GT6's 1000-point tool cost");
        }
        WoodLogBlock maple = GTWoods.log(WoodSpecies.MAPLE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        level.setBlock(POS, maple.defaultBlockState(), 3);
        helper.assertTrue(maple.defaultBlockState().use(level, player, InteractionHand.MAIN_HAND,
                        hit(Direction.NORTH)) == InteractionResult.PASS
                        && level.getBlockState(POS).is(maple),
                "an ordinary click leaves the live log in place");
        player.setItemInHand(InteractionHand.MAIN_HAND, tool(GTToolType.HAND_DRILL));
        helper.assertTrue(maple.defaultBlockState().use(level, player, InteractionHand.MAIN_HAND,
                        hit(Direction.NORTH)).consumesAction()
                        && level.getBlockState(POS).getBlock() instanceof TreeHoleBlock,
                "maple's existing horizontal hand-drill sap hole still works");
        level.setBlock(POS, Blocks.AIR.defaultBlockState(), 3);
        helper.succeed();
    }
}
