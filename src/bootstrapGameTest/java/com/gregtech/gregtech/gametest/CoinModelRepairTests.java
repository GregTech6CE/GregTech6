package com.gregtech.gregtech.gametest;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.block.misc.CoinPileBlock;
import com.gregtech.gregtech.blockentity.misc.CoinPileBlockEntity;
import com.gregtech.gregtech.blockentity.tool.CoinMoldBlockEntity;
import com.gregtech.gregtech.content.tool.CoinGeometry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.CoinItem;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Checks the model chain and GT6's block-relative world UV coordinates of minted coins. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class CoinModelRepairTests {
    private CoinModelRepairTests() {}

    @GameTest(template = "test_empty")
    public static void everyRegisteredCoinHasMintModelAndTextures(GameTestHelper helper) throws Exception {
        JsonObject mint = json("assets/gregtech/models/item/coin_minted.json");
        helper.assertTrue(mint.get("parent").getAsString().equals("minecraft:block/block"),
                "minted coin inherits the vanilla 3D block item transforms");
        helper.assertTrue(mint.getAsJsonArray("elements").size() > 100,
                "minted coin has the original pixel relief rather than a flat missing model");
        for (String texture : List.of("coin", "coin_side")) {
            helper.assertTrue(exists("assets/gregtech/textures/block/iconsets/" + texture + ".png"),
                    "GT6 coin texture " + texture + " is bundled");
        }
        helper.assertTrue(json("assets/gregtech/models/item/coin_pile.json").get("parent").getAsString()
                .equals("gregtech:item/coin_minted"), "the bare pile item previews its mint geometry");

        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
            String modelPath = "assets/gregtech/models/item/material/" + set.folder() + "/coin.json";
            helper.assertTrue(json(modelPath).get("parent").getAsString().equals("gregtech:item/coin_minted"),
                    modelPath + " inherits minted geometry");
        }

        List<String> problems = new ArrayList<>();
        int coins = 0;
        for (var entry : GTItems.allEntries()) {
            if (!(entry.get() instanceof MaterialItem item) || item.getPrefix() != MaterialPrefix.coin) continue;
            coins++;
            if (item.getTintColor() != (0xFF000000 | (item.getMaterial().getColor() & 0xFFFFFF)))
                problems.add(entry.getId() + " lost its material tint");
        }
        helper.assertTrue(coins > 500, "all GT6 coin-form materials were registered");
        helper.assertTrue(problems.isEmpty(), "coin model/tint failures: " + problems.stream().limit(5).toList());
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void worldCoinPixelsKeepOriginalBlockRelativeUvs(GameTestHelper helper) {
        helper.assertTrue(CoinGeometry.surfaceU(Direction.UP, 0, 0) == 0
                        && CoinGeometry.surfaceU(Direction.UP, .25F, 0) == .25F
                        && CoinGeometry.surfaceU(Direction.UP, .75F, 0) == .75F
                        && CoinGeometry.surfaceV(Direction.UP, 0, 0) == 0
                        && CoinGeometry.surfaceV(Direction.UP, 0, .25F) == .25F
                        && CoinGeometry.surfaceV(Direction.DOWN, 0, .25F) == .75F,
                "world coin pixels sample the texture at their whole-block X/Z position");
        helper.assertTrue(CoinGeometry.surfaceU(Direction.NORTH, 0, 0) == 0
                        && CoinGeometry.surfaceU(Direction.NORTH, .25F, 0) == .25F
                        && CoinGeometry.surfaceU(Direction.WEST, 0, .25F) == .25F
                        && CoinGeometry.surfaceU(Direction.SOUTH, 0, 0) == 1
                        && CoinGeometry.surfaceU(Direction.SOUTH, .25F, 0) == .75F,
                "world coin sides preserve the original RenderBlocks texture orientation");
        helper.assertTrue(CoinGeometry.surfaceV(Direction.NORTH, 0, 0) == 1
                        && CoinGeometry.surfaceV(Direction.NORTH, 1, 0) == 0,
                "the side sprite spans the sixteen-coin stack height");
        helper.assertTrue(CoinGeometry.exposedSide(1, 6, Direction.EAST)
                        && !CoinGeometry.exposedSide(2, 6, Direction.WEST)
                        && CoinGeometry.exposedSide(0, 7, Direction.WEST),
                "mint relief only draws a side where the adjacent pixel is lower or outside the coin");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void inventoryCoinUsesStackDieAndKeepsBakedDefault(GameTestHelper helper) throws Exception {
        ItemStack plain = GTItems.getStack(MaterialPrefix.coin, GTMaterialRegistry.get("Copper"));
        helper.assertTrue(!CoinGeometry.hasCustomPattern(plain)
                        && CoinGeometry.pattern(plain) == CoinGeometry.defaultPattern(),
                "an ordinary coin keeps the shared baked default model");

        var baked = json("assets/gregtech/models/item/coin_minted.json").getAsJsonArray("elements");
        var defaultBoxes = CoinGeometry.defaultPattern().inventoryPixelBoxes();
        helper.assertTrue(defaultBoxes.size() == baked.size(),
                "dynamic coin geometry has the same number of default mint pixels as the baked item");
        for (int index = 0; index < defaultBoxes.size(); index++) {
            var box = defaultBoxes.get(index);
            var element = baked.get(index).getAsJsonObject();
            var from = element.getAsJsonArray("from");
            var to = element.getAsJsonArray("to");
            helper.assertTrue(box.minX * 16 == from.get(0).getAsDouble()
                            && box.minY * 16 == from.get(1).getAsDouble()
                            && box.minZ * 16 == from.get(2).getAsDouble()
                            && box.maxX * 16 == to.get(0).getAsDouble()
                            && box.maxY * 16 == to.get(1).getAsDouble()
                            && box.maxZ * 16 == to.get(2).getAsDouble(),
                    "dynamic inventory pixel " + index + " matches the ordinary coin model");
        }

        ItemStack stamped = plain.copy();
        stamped.getOrCreateTag().putString("note", "not a die");
        helper.assertTrue(!CoinGeometry.hasCustomPattern(stamped),
                "unrelated NBT does not force the custom renderer");
        stamped.getOrCreateTag().putShort("gt.coin.shape.0.0", (short) 0b110);
        stamped.getOrCreateTag().putShort("gt.coin.shape.1.0", (short) 0b100);
        helper.assertTrue(CoinGeometry.hasCustomPattern(stamped)
                        && CoinGeometry.pattern(stamped).pixelDepth(0, 1) == 1
                        && CoinGeometry.pattern(stamped).pixelDepth(0, 2) == 3
                        && CoinGeometry.pattern(stamped).inventoryPixelBoxes().size() == 255,
                "coin NBT chooses a different inventory relief, including cut-away pixels");
        helper.succeed();
    }

    @GameTest(template = "coin_pile_space")
    public static void moldStruckCoinKeepsCustomDieInWorldSaveAndClientPacket(GameTestHelper helper) {
        var level = helper.getLevel();
        var moldBlock = ForgeRegistries.BLOCKS.getValue(com.gregtech.gregtech.GregTech.id("coin_mold"));
        helper.assertTrue(moldBlock != null, "GT6 coinage mold is registered");
        BlockPos moldPos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(moldPos, moldBlock.defaultBlockState(), 3);
        CoinMoldBlockEntity mold = (CoinMoldBlockEntity) level.getBlockEntity(moldPos);
        helper.assertTrue(mold != null, "coinage mold has pattern storage");

        CompoundTag die = new CompoundTag();
        die.putBoolean("gt.coin.unique", true);
        die.putShort("gt.coin.shape.0.0", (short) 0b110);
        die.putShort("gt.coin.shape.1.0", (short) 0b100);
        mold.loadItemConfig(die);
        ItemStack plate = GTItems.getStack(MaterialPrefix.plateTiny, GTMaterialRegistry.get("Copper"));
        helper.assertTrue(mold.insertPlate(plate, true) && mold.strike(),
                "a copper tiny plate can be struck using a custom die");
        ItemStack minted = mold.takeCoin();
        CompoundTag mintTag = minted.getTag();
        helper.assertTrue(mintTag != null && mintTag.getBoolean("gt.coin.unique")
                        && mintTag.getShort("gt.coin.shape.0.0") == 0b110
                        && mintTag.getShort("gt.coin.shape.1.0") == 0b100
                        && mintTag.contains("gt.coin.shape.0.15")
                        && mintTag.contains("gt.coin.shape.1.15"),
                "GT6 carries both full 16-row bit planes, including zero rows, on the minted item");
        helper.assertTrue(CoinGeometry.pattern(minted).pixelDepth(0, 1) == 1
                        && CoinGeometry.pattern(minted).pixelDepth(0, 2) == 3
                        && CoinGeometry.pattern(minted).pixelDepth(0, 3) == 0
                        && CoinGeometry.pattern(minted).pixelBoxes(1).size() == 255,
                "the custom die changes the raised pixel and cuts away the two-bit pixel");

        BlockPos pilePos = helper.absolutePos(new BlockPos(3, 1, 1));
        level.setBlock(pilePos, GTDecorBlocks.COIN_PILE.get().defaultBlockState(), 3);
        CoinPileBlockEntity pile = (CoinPileBlockEntity) level.getBlockEntity(pilePos);
        helper.assertTrue(pile != null && pile.add(0, minted) == 1
                        && pile.coinPattern().pixelDepth(0, 2) == 3,
                "the placed pile renders the die of its stored coin rather than the default mint");
        ItemStack ordinary = GTItems.getStack(MaterialPrefix.coin, GTMaterialRegistry.get("Copper"));
        helper.assertTrue(pile.add(1, ordinary) == 0,
                "a default coin cannot mix into a custom-die pile of the same metal");
        helper.assertTrue(ItemStack.isSameItemSameTags(pile.contents().get(0), minted),
                "breaking the pile returns coins with the custom die intact");

        CompoundTag update = pile.getUpdateTag();
        CoinPileBlockEntity client = new CoinPileBlockEntity(pilePos, level.getBlockState(pilePos));
        client.load(update);
        helper.assertTrue(client.faceCount(0) == 1
                        && ItemStack.isSameItemSameTags(client.coinItem(), minted)
                        && client.coinPattern().pixelDepth(0, 2) == 3,
                "the save and client block-entity packet preserve the die and face count");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void aCoinPlacesItsOwnMetalOnTheClickedCell(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos target = floor.above();
        helper.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        var player = helper.makeMockSurvivalPlayer();
        ItemStack held = GTItems.getStack(MaterialPrefix.coin, GTMaterialRegistry.get("Silver"), 3);
        helper.assertTrue(held.getItem() instanceof CoinItem, "silver coins use the placeable coin item");
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(floor).add(.1, 1, .9),
                Direction.UP, floor, false);
        helper.assertTrue(held.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit))
                .consumesAction(), "a coin may be placed directly onto a solid floor");
        helper.assertTrue(helper.getLevel().getBlockState(target).is(GTDecorBlocks.COIN_PILE.get()),
                "the coin creates a pile, not an unrelated decoration block");
        CoinPileBlockEntity pile = (CoinPileBlockEntity) helper.getLevel().getBlockEntity(target);
        int face = CoinPileBlockEntity.faceAt(.1, .9);
        helper.assertTrue(pile != null && pile.faceCount(face) == 1 && pile.total() == 1,
                "GT6 onPlaced seeds only the clicked 4x4 coin cell");
        helper.assertTrue(pile.coinItem().getItem() == held.getItem() && held.getCount() == 2,
                "the placed coin preserves silver and spends exactly one item");

        BlockPos blockedFloor = helper.absolutePos(new BlockPos(3, 1, 1));
        helper.getLevel().setBlock(blockedFloor, Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(blockedFloor.above(), Blocks.STONE.defaultBlockState(), 3);
        BlockHitResult blockedHit = new BlockHitResult(Vec3.atLowerCornerOf(blockedFloor).add(.5, 1, .5),
                Direction.UP, blockedFloor, false);
        helper.assertTrue(!held.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, blockedHit))
                .consumesAction() && held.getCount() == 2,
                "a blocked placement leaves the coin stack untouched");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fullOrUnmatchedPileClicksNeverDuplicateCoins(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var level = helper.getLevel();
        CoinPileBlock block = (CoinPileBlock) GTDecorBlocks.COIN_PILE.get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        CoinPileBlockEntity pile = (CoinPileBlockEntity) level.getBlockEntity(pos);
        ItemStack silver = GTItems.getStack(MaterialPrefix.coin, GTMaterialRegistry.get("Silver"));
        ItemStack gold = GTItems.getStack(MaterialPrefix.coin, GTMaterialRegistry.get("Gold"));
        helper.assertTrue(pile != null && !silver.isEmpty() && !gold.isEmpty(),
                "the silver and gold coin types and pile entity are registered");
        for (int count = 0; count < CoinPileBlockEntity.FACE_STACK_SIZE; count++)
            helper.assertTrue(pile.add(0, silver) == 1, "the test fills one face to GT6's sixteen-coin cap");

        var player = helper.makeMockSurvivalPlayer();
        ItemStack twoSilver = silver.copyWithCount(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, twoSilver);
        BlockHitResult top = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.1, 1, .1),
                Direction.UP, pos, false);
        BlockHitResult openTop = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.6, 1, .6),
                Direction.UP, pos, false);
        BlockHitResult side = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0, .5, .1),
                Direction.WEST, pos, false);
        helper.assertTrue(block.use(block.defaultBlockState(), level, pos, player,
                        InteractionHand.MAIN_HAND, top).consumesAction()
                        && pile.faceCount(0) == 16 && twoSilver.getCount() == 2,
                "GT6 consumes a full-face click without spending or adding a coin");
        helper.assertTrue(block.use(block.defaultBlockState(), level, pos, player,
                        InteractionHand.MAIN_HAND, side).consumesAction()
                        && pile.total() == 16 && twoSilver.getCount() == 2,
                "GT6 consumes an ignored side click without placing an adjacent coin pile");
        player.setItemInHand(InteractionHand.MAIN_HAND, gold.copyWithCount(2));
        helper.assertTrue(block.use(block.defaultBlockState(), level, pos, player,
                        InteractionHand.MAIN_HAND, openTop).consumesAction()
                        && pile.total() == 16 && player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2,
                "a different coin metal is refused without being spent");

        for (int slot = 0; slot < player.getInventory().items.size(); slot++)
            player.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        block.use(block.defaultBlockState(), level, pos, player, InteractionHand.OFF_HAND, top);
        helper.assertTrue(pile.total() == 16,
                "an empty offhand cannot remove a coin when the survival inventory is full");

        var creative = helper.makeMockPlayer();
        creative.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        block.use(block.defaultBlockState(), level, pos, creative, InteractionHand.MAIN_HAND, top);
        helper.assertTrue(pile.total() == 15
                        && creative.getInventory().items.stream().noneMatch(stack -> stack.is(silver.getItem())),
                "GT6 creative pickup removes a coin without duplicating it into inventory");
        helper.succeed();
    }

    private static InputStream resource(String path) {
        return CoinModelRepairTests.class.getClassLoader().getResourceAsStream(path);
    }

    private static boolean exists(String path) {
        return CoinModelRepairTests.class.getClassLoader().getResource(path) != null;
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream stream = resource(path)) {
            if (stream == null) throw new IllegalStateException("missing model " + path);
            return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
