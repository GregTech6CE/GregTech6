package gregtech.gametest;

import gregtech.content.tool.WoodenBucketCatalog;
import gregtech.content.item.WoodenBucketItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Real registered bucket variants and native cauldron states; no mirrored transfer model. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class WoodenBucketTransactionTests {
    private static ItemStack bucket(WoodenBucketCatalog.Band band, WoodenBucketCatalog.Content content) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce",
                WoodenBucketCatalog.itemId(band, content))));
    }

    @GameTest(template = "test_empty")
    public static void topUpConsumesExactlyOneBucketAndKeepsBand(GameTestHelper h) {
//? if >=1.21.1 {
        /*var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-bucket]"));
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 1, pos.getY(), pos.getZ());
        for (var band : WoodenBucketCatalog.BANDS) for (boolean creative : new boolean[]{false, true}) {
            player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
            for (int waterLevel = 0; waterLevel <= 3; waterLevel++) {
                player.getInventory().clearContent();
                ItemStack full = bucket(band, WoodenBucketCatalog.water());
                h.assertTrue(full.getItem() instanceof WoodenBucketItem, "registered wooden water bucket");
                player.setItemInHand(InteractionHand.MAIN_HAND, full);
                BlockState before = waterLevel == 0 ? Blocks.CAULDRON.defaultBlockState()
                        : Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, waterLevel);
                h.getLevel().setBlockAndUpdate(pos, before);
                var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                full.getItem().onItemUseFirst(full, context);
                h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.WATER_CAULDRON)
                        && h.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 3,
                        "empty and partially filled cauldrons reach full: " + band.id() + "/" + waterLevel);
                var expected = creative || waterLevel == 3 ? WoodenBucketCatalog.water() : WoodenBucketCatalog.empty();
                var held = player.getMainHandItem();
                h.assertTrue(held.getCount() == 1 && held.is(bucket(band, expected).getItem()),
                        "one matching band remains; full target and creative mode do not consume");
                h.assertTrue(player.getInventory().items.stream().filter(s -> !s.isEmpty()).count() == 1,
                        "top-up never inserts duplicate buckets");
            }
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void waterDoesNotReplaceLavaOrPowderSnow(GameTestHelper h) {
//? if >=1.21.1 {
        /*var player = h.makeMockPlayer(GameType.SURVIVAL);
*///?} else {
        var player = h.makeMockSurvivalPlayer();
//?}
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        for (var before : new BlockState[]{Blocks.LAVA_CAULDRON.defaultBlockState(),
                Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1)}) {
            ItemStack full = bucket(WoodenBucketCatalog.BANDS.get(0), WoodenBucketCatalog.water());
            player.setItemInHand(InteractionHand.MAIN_HAND, full);
            h.getLevel().setBlockAndUpdate(pos, before);
            full.getItem().onItemUseFirst(full, new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
            h.assertTrue(h.getLevel().getBlockState(pos).equals(before), "non-water contents remain intact");
            h.assertTrue(player.getMainHandItem().is(full.getItem()), "rejected water retains its bucket");
        }
        h.succeed();
    }
}
