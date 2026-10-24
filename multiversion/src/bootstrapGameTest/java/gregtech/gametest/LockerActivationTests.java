package gregtech.gametest;

import gregtech.block.inventory.LockerBlock;
import gregtech.blockentity.inventory.LockerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class LockerActivationTests {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Item[] IRON = {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
    private static final Item[] DIAMOND = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};

    private static LockerBlockEntity locker(GameTestHelper h) {
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "locker"));
        h.assertTrue(block instanceof LockerBlock, "registered ordinary locker exists");
        h.setBlock(new BlockPos(2, 2, 2), block.defaultBlockState().setValue(LockerBlock.FACING, Direction.NORTH));
        return (LockerBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 2)));
    }

    private static InteractionResult click(LockerBlockEntity locker, Player player, Direction face, InteractionHand hand) {
        var pos = locker.getBlockPos();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, pos, false);
//? if >=1.21.1 {
        /*var block = (LockerBlock) locker.getBlockState().getBlock();
        var held = player.getItemInHand(hand);
        if (held.isEmpty()) return block.useWithoutItem(locker.getBlockState(), locker.getLevel(), pos, player, hit);
        return switch (block.useItemOn(held, locker.getBlockState(), locker.getLevel(), pos, player, hand, hit)) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME, CONSUME_PARTIAL -> InteractionResult.CONSUME;
            default -> InteractionResult.PASS;
        };
*///?} else {
        return locker.getBlockState().use(locker.getLevel(), player, hand, hit);
//?}
    }


    private static Player player(GameTestHelper h) {
//? if >=1.21.1 {
        /*return h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
*///?} else {
        return h.makeMockPlayer();
//?}
    }

    private static CompoundTag saveLocker(LockerBlockEntity locker) {
//? if >=1.21.1 {
        /*return locker.saveWithoutMetadata(locker.getLevel().registryAccess());
*///?} else {
        return locker.saveWithoutMetadata();
//?}
    }

    private static net.minecraft.nbt.Tag saveStack(Player player, ItemStack stack) {
//? if >=1.21.1 {
        /*return stack.saveOptional(player.level().registryAccess());
*///?} else {
        return stack.save(new CompoundTag());
//?}
    }

    private static ItemStack readStack(Player player, CompoundTag tag) {
//? if >=1.21.1 {
        /*return ItemStack.parseOptional(player.level().registryAccess(), tag);
*///?} else {
        return ItemStack.of(tag);
//?}
    }

    private static ListTag saveInventory(Player player) {
        var saved = new ListTag();
        // Includes main inventory, equipment and off-hand, retaining empty slot positions.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            saved.add(saveStack(player, player.getInventory().getItem(i)));
        return saved;
    }

    private static void wear(Player player, Item[] items) {
        for (int i = 0; i < SLOTS.length; i++) player.setItemSlot(SLOTS[i], new ItemStack(items[i]));
    }

    private static void assertWorn(GameTestHelper h, Player player, Item[] items) {
        for (int i = 0; i < SLOTS.length; i++) h.assertTrue(player.getItemBySlot(SLOTS[i]).is(items[i]), "worn slot " + SLOTS[i]);
    }

    @GameTest(template="test_empty")
    public static void ordinaryLockerOnlyFrontMainHandSwapsArmor(GameTestHelper h) {
        var locker = locker(h);
        var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 7));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 5));
        wear(player, IRON);
        h.assertTrue(click(locker, player, Direction.NORTH, InteractionHand.MAIN_HAND).consumesAction(), "front stores worn armor");
        wear(player, DIAMOND);
        var saved = saveLocker(locker);
        var inventory = saveInventory(player);
        for (var face : Direction.values()) if (face != Direction.NORTH) {
            h.assertTrue(click(locker, player, face, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "non-front passes: " + face);
            h.assertTrue(saved.equals(saveLocker(locker)), "non-front preserves stored armor");
            h.assertTrue(inventory.equals(saveInventory(player)), "non-front preserves complete player inventory");
        }
        h.assertTrue(click(locker, player, Direction.NORTH, InteractionHand.OFF_HAND) == InteractionResult.PASS, "off-hand does not swap back");
        h.assertTrue(inventory.equals(saveInventory(player)), "off-hand preserves inventory");
        h.assertTrue(click(locker, player, Direction.NORTH, InteractionHand.MAIN_HAND).consumesAction(), "front swaps valid armor");
        assertWorn(h, player, IRON);
        h.assertTrue(player.getMainHandItem().getCount() == 7 && player.getOffhandItem().getCount() == 5, "activation consumes neither held stack");
        click(locker, player, Direction.NORTH, InteractionHand.MAIN_HAND);
        assertWorn(h, player, DIAMOND);
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void incompatibleStoredEquipmentSkipsOnlyItsOwnSlot(GameTestHelper h) {
        var locker = locker(h);
        var player = player(h);
        wear(player, DIAMOND);
        // Defensive load boundary: this is deliberately injected invalid saved state,
        // not a claim that ordinary gameplay can create it.
        var saved = new CompoundTag();
        var armor = new ListTag();
        armor.add(saveStack(player, new ItemStack(Items.STONE)));
        for (int i = 1; i < IRON.length; i++) armor.add(saveStack(player, new ItemStack(IRON[i])));
        saved.put("gt.armor", armor);
//? if >=1.21.1 {
        /*locker.loadWithComponents(saved, h.getLevel().registryAccess());
*///?} else {
        locker.load(saved);
//?}
        h.assertTrue(click(locker, player, Direction.NORTH, InteractionHand.MAIN_HAND).consumesAction(), "valid slots still activate");
        h.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "invalid stored stone never enters head slot");
        for (int i = 1; i < SLOTS.length; i++) h.assertTrue(player.getItemBySlot(SLOTS[i]).is(IRON[i]), "valid neighboring slot swaps: " + SLOTS[i]);
        var after = saveLocker(locker).getList("gt.armor", 10);
        h.assertTrue(readStack(player, after.getCompound(0)).is(Items.STONE), "invalid stored item retained without loss");
        for (int i = 1; i < SLOTS.length; i++) h.assertTrue(readStack(player, after.getCompound(i)).is(DIAMOND[i]), "previous armor retained: " + SLOTS[i]);
        h.succeed();
    }
}
