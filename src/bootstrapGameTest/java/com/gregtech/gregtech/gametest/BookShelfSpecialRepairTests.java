package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.BookShelfBlock;
import com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity;
import com.gregtech.gregtech.content.book.GTBookList;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Original GT6 shelf's display items, secret redstone switches and manual extraction. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BookShelfSpecialRepairTests {
    // POS must remain inside the structure. A 1x1 test_empty template let neighboring
    // concurrent tests clear this shelf while its 300-tick proximity check was pending.
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static BlockHitResult topLeft(BlockPos pos) {
        return new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.125, .75, .125),
                Direction.NORTH, pos, false);
    }

    @GameTest(template = "coin_pile_space")
    public static void originalShelfItemsAndSecretRedstone(GameTestHelper helper) {
        helper.setBlock(POS, GTDecorBlocks.BOOKSHELF.get().defaultBlockState()
                .setValue(BookShelfBlock.FACING, Direction.NORTH));
        var pos = helper.absolutePos(POS);
        var level = helper.getLevel();
        var block = GTDecorBlocks.BOOKSHELF.get();
        var shelf = (BookShelfBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockPlayer();
        var hit = topLeft(pos);

        helper.assertTrue(block.getFlammability(level.getBlockState(pos), level, pos, Direction.NORTH) == 150
                        && block.getFireSpreadSpeed(level.getBlockState(pos), level, pos, Direction.NORTH) == 150,
                "wooden GT6 shelf keeps its original fire properties");

        for (var item : new net.minecraft.world.item.Item[] {Items.OAK_BUTTON, Items.STONE_BUTTON,
                Items.LEVER, Items.REDSTONE_TORCH, Items.COBBLESTONE}) {
            helper.assertTrue(GTBookList.canPlace(new ItemStack(item)), "GT6 registers " + item + " on the shelf");
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_BUTTON));
        helper.assertTrue(block.use(level.getBlockState(pos), level, pos, player,
                InteractionHand.MAIN_HAND, hit).consumesAction(), "insert stone button");
        helper.assertTrue(shelf.inventory().getStackInSlot(0).is(Items.STONE_BUTTON), "button occupies clicked slot");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(shelf.inventory().getStackInSlot(0).is(Items.STONE_BUTTON), "button click does not take item");
        helper.assertTrue(level.getBlockState(pos).getSignal(level, pos, Direction.NORTH) == 15,
                "button makes shelf emit weak redstone on all sides");
        for (int i = 0; i < 119; i++) shelf.tickRedstone();
        helper.assertTrue(shelf.redstoneSignal() == 15, "button remains active through tick 119");
        shelf.tickRedstone();
        helper.assertTrue(shelf.redstoneSignal() == 0, "button switches off at tick 120");

        // Like GT6 canExtractItem2, pipes cannot steal the secret controls.
        helper.assertTrue(shelf.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(handler -> handler.extractItem(0, 1, false).isEmpty()).orElse(false),
                "automation cannot extract button");
        player.setShiftKeyDown(true);
        block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(shelf.inventory().getStackInSlot(0).isEmpty(), "sneaking removes button");
        player.setShiftKeyDown(false);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEVER));
        block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(shelf.redstoneSignal() == 15, "lever turns shelf on");
        block.use(level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(shelf.redstoneSignal() == 0, "lever turns shelf off");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void pincersTakeSpecialItemWithoutSneaking(GameTestHelper helper) {
        helper.setBlock(POS, GTDecorBlocks.BOOKSHELF.get());
        var pos = helper.absolutePos(POS);
        var level = helper.getLevel();
        var block = GTDecorBlocks.BOOKSHELF.get();
        var shelf = (BookShelfBlockEntity) level.getBlockEntity(pos);
        shelf.inventory().setStackInSlot(0, new ItemStack(Items.REDSTONE_TORCH));
        var player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                GTToolItem.create(GTToolType.PINCERS, Materials.Steel, GTMaterialRegistry.get("Wood")));
        helper.assertTrue(block.use(level.getBlockState(pos), level, pos, player,
                InteractionHand.MAIN_HAND, topLeft(pos)).consumesAction(), "pincers click shelf slot");
        helper.assertTrue(shelf.inventory().getStackInSlot(0).isEmpty(),
                "pincers remove redstone torch without sneaking");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void wrenchRotatesShelfButKeepsBooks(GameTestHelper helper) {
        helper.setBlock(POS, GTDecorBlocks.BOOKSHELF.get().defaultBlockState()
                .setValue(BookShelfBlock.FACING, Direction.NORTH));
        var pos = helper.absolutePos(POS);
        var level = helper.getLevel();
        var shelf = (BookShelfBlockEntity) level.getBlockEntity(pos);
        shelf.inventory().setStackInSlot(0, new ItemStack(Items.BOOK));
        var player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                GTToolItem.create(GTToolType.WRENCH, Materials.Steel, GTMaterialRegistry.get("Wood")));
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(1, .5, .5), Direction.EAST, pos, false);
        helper.assertTrue(GTDecorBlocks.BOOKSHELF.get().use(level.getBlockState(pos), level, pos,
                player, InteractionHand.MAIN_HAND, hit).consumesAction(), "wrench rotates GT6 shelf");
        helper.assertTrue(level.getBlockState(pos).getValue(BookShelfBlock.FACING) == Direction.EAST,
                "front face moved to east");
        helper.assertTrue(shelf.inventory().getStackInSlot(0).is(Items.BOOK), "rotation keeps books");
        var shape = level.getBlockState(pos).getShape(level, pos);
        helper.assertTrue(shape.min(Direction.Axis.X) == .125 && shape.max(Direction.Axis.X) == .875,
                "rotated shelf keeps original inset selection box");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space", timeoutTicks = 400)
    public static void approachingDungeonShelfRevealsItsBooks(GameTestHelper helper) {
        helper.setBlock(POS, GTDecorBlocks.BOOKSHELF.get());
        var pos = helper.absolutePos(POS);
        var shelf = (BookShelfBlockEntity) helper.getLevel().getBlockEntity(pos);
        shelf.setDungeonLoot(ResourceLocation.parse("gregtech_repair:shelf_paper"), null, 99L);
        // GameTestHelper.makeMockServerPlayerInLevel creates a Connection without a Netty channel;
        // Forge's login pipeline dereferences it. Add a plain mock Player to the level instead:
        // the production shelf checks actual nearby Player entities, not PlayerList logins.
        var player = helper.makeMockPlayer();
        player.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + .5, 0, 0);
        helper.assertTrue(helper.getLevel().addFreshEntity(player), "mock player entered the server level");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                new AABB(pos).inflate(32)).contains(player), "shelf's actual proximity query sees the player");
        helper.runAfterDelay(305, () -> {
            try {
                int filled = 0;
                for (int slot = 0; slot < 14; slot++) {
                    if (!shelf.inventory().getStackInSlot(slot).isEmpty()) filled++;
                }
                helper.assertTrue(filled > 0, "nearby player causes pending shelf books to render within 300 ticks");
            } finally {
                player.discard();
            }
            helper.succeed();
        });
    }
}
