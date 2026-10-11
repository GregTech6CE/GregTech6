package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.BookShelfBlock;
import com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity;
import com.gregtech.gregtech.content.book.BookShelfVariants;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GT6 Loader_MultiTileEntities: available plank-index shelves plus all 60 metalset shelves. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BookShelfFamilyTests {
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void familyHasOriginalMaterialsAndSharedInventory(GameTestHelper helper) {
        var local = new BlockPos(1, 1, 1);
        var absolute = helper.absolutePos(local);
        helper.assertTrue(BookShelfVariants.all().size() == 75,
                "15 textured GT6 plank variants and all 60 original metalset variants");
        helper.assertTrue(GTDecorBlocks.allBookshelves().size() == BookShelfVariants.all().size(),
                "every bookshelf spec has one registered block and item");
        helper.assertTrue(GTDecorBlocks.bookshelf(7839) == GTDecorBlocks.bookshelf("bookshelf_bluespruce"),
                "GT6 7839 dungeon bookshelf keeps its wood-dictionary identity");
        helper.assertTrue(GTDecorBlocks.bookshelf(7110) == GTDecorBlocks.bookshelf("bookshelf_metal_steel"),
                "GT6 metalset 7110 is the steel bookshelf");

        for (var variant : BookShelfVariants.all()) {
            var entry = GTDecorBlocks.bookshelf(variant.path());
            helper.assertTrue(entry != null && entry.isPresent(), variant.path() + " registered");
            BookShelfBlock block = entry.get();
            helper.assertTrue(block.variant() == variant, variant.path() + " carries its GT6 definition");
            helper.assertTrue(variant.material().isValid(), variant.path() + " material resolved");
            helper.assertTrue(block.getExplosionResistance() == variant.resistance(),
                    variant.path() + " blast resistance matches GT6");
            helper.assertTrue(BlockHarvestPolicy.tool(block) == (variant.metal()
                    ? BlockHarvestPolicy.Tool.WRENCH : BlockHarvestPolicy.Tool.AXE),
                    variant.path() + " uses its GT6 harvest tool");

            var state = block.defaultBlockState().setValue(BookShelfBlock.FACING, Direction.NORTH);
            helper.setBlock(local, state);
            var shelf = helper.getLevel().getBlockEntity(absolute);
            helper.assertTrue(shelf instanceof BookShelfBlockEntity,
                    variant.path() + " shares the 28-slot bookshelf block entity");
            helper.assertTrue(((BookShelfBlockEntity) shelf).inventory().getSlots() == 28,
                    variant.path() + " has 28 displayed slots");
            helper.assertTrue(state.getLightBlock(helper.getLevel(), absolute) == 0,
                    variant.path() + " is non-opaque like GT6's machine/wood wrapper");
            helper.assertTrue(state.getCollisionShape(helper.getLevel(), absolute).bounds().getSize() == 1.0,
                    variant.path() + " has GT6's full collision cube");
            helper.assertTrue(block.getFlammability(state, helper.getLevel(), absolute, Direction.UP)
                            == variant.flammability(), variant.path() + " has GT6 flammability");
        }

        for (String path : new String[] {"bookshelf_bluespruce", "bookshelf_metal_steel"}) {
            var block = GTDecorBlocks.bookshelf(path).get();
            helper.setBlock(local, block.defaultBlockState());
            var shelf = (BookShelfBlockEntity) helper.getLevel().getBlockEntity(absolute);
            shelf.inventory().setStackInSlot(27, new ItemStack(Items.ENCHANTED_BOOK));
            helper.assertTrue(shelf.inventory().getStackInSlot(27).is(Items.ENCHANTED_BOOK),
                    path + " stores a book on its back face");
            helper.assertTrue(block.getEnchantPowerBonus(block.defaultBlockState(), helper.getLevel(), absolute)
                            == 2.0F / 12.0F, path + " preserves book enchantment strength");
        }
        helper.succeed();
    }
}
