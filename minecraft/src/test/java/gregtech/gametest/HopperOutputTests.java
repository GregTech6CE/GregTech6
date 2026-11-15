package gregtech.gametest;

import gregtech.content.blockentity.machine.HopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * Hopper output passes against GT6 {@code MultiTileEntityHopper}: {@code ST.move} transfers from
 * one source slot into one target slot per call, and the exact mode stops after one such transfer
 * while the divisible mode repeats until the 64-item tick budget is spent.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class HopperOutputTests {
    private static final String BATCH = "hopper_output";
    private static final int NEARLY_FULL_STACK = 56;

    private HopperOutputTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void exactModeEmitsOneSourceStack(GameTestHelper helper) {
        Container chest = chest(helper);
        HopperBlockEntity hopper = placeHopper(helper, 1, 1, 1, Direction.EAST);
        hopper.setStackInSlot(0, new ItemStack(Items.STONE, 32));
        hopper.setStackInSlot(1, new ItemStack(Items.STONE, 32));
        hopper.toggleExactMode();
        tick(helper, hopper, 4);

        helper.assertTrue(hopperCount(hopper) == 32, "exact mode keeps the second source stack for the next pass");
        helper.assertTrue(containerCount(chest) == 32, "exact mode emits a single source stack");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void divisibleModeEmitsUpToAStackPerPass(GameTestHelper helper) {
        Container chest = chest(helper);
        HopperBlockEntity hopper = placeHopper(helper, 1, 1, 1, Direction.EAST);
        hopper.setStackInSlot(0, new ItemStack(Items.STONE, 32));
        hopper.setStackInSlot(1, new ItemStack(Items.STONE, 32));
        tick(helper, hopper, 4);

        helper.assertTrue(hopperCount(hopper) == 0, "divisible mode drains both source stacks");
        helper.assertTrue(containerCount(chest) == 64, "divisible mode emits up to 64 items per pass");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void exactModeDoesNotSplitAcrossTargetSlots(GameTestHelper helper) {
        Container chest = nearlyFullChest(helper);
        HopperBlockEntity hopper = placeHopper(helper, 1, 1, 1, Direction.EAST);
        hopper.setStackInSlot(0, new ItemStack(Items.STONE, 16));
        hopper.setStackInSlot(1, new ItemStack(Items.STONE, 16));
        for (int i = 0; i < 16; i++) hopper.cycleMode(false);
        hopper.toggleExactMode();
        tick(helper, hopper, 4);

        helper.assertTrue(hopperCount(hopper) == 32, "no single target slot accepts 16, so exact mode moves nothing");
        helper.assertTrue(containerCount(chest) == nearlyFullChestCount(chest), "exact mode leaves the target untouched");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void divisibleModeDoesNotSplitAcrossTargetSlots(GameTestHelper helper) {
        Container chest = nearlyFullChest(helper);
        HopperBlockEntity hopper = placeHopper(helper, 1, 1, 1, Direction.EAST);
        hopper.setStackInSlot(0, new ItemStack(Items.STONE, 16));
        hopper.setStackInSlot(1, new ItemStack(Items.STONE, 16));
        for (int i = 0; i < 16; i++) hopper.cycleMode(false);
        tick(helper, hopper, 4);

        helper.assertTrue(hopperCount(hopper) == 32, "a move of 16 requires one target slot with 16 free");
        helper.assertTrue(containerCount(chest) == nearlyFullChestCount(chest), "divisible mode leaves the target untouched");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void exactModeDropsOneSourceStack(GameTestHelper helper) {
        HopperBlockEntity hopper = placeHopper(helper, 1, 2, 1, Direction.DOWN);
        hopper.setStackInSlot(0, new ItemStack(Items.STONE, 32));
        hopper.setStackInSlot(1, new ItemStack(Items.STONE, 32));
        hopper.toggleExactMode();
        int droppedBefore = droppedCount(helper, hopper);
        tick(helper, hopper, 4);

        helper.assertTrue(hopperCount(hopper) == 32, "exact mode drops a single source stack");
        helper.assertTrue(droppedCount(helper, hopper) - droppedBefore == 32,
                "one item entity carries the single dropped stack");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void divisibleModeDropsUpToAStackPerPass(GameTestHelper helper) {
        HopperBlockEntity hopper = placeHopper(helper, 1, 2, 1, Direction.DOWN);
        hopper.setStackInSlot(0, new ItemStack(Items.STONE, 32));
        hopper.setStackInSlot(1, new ItemStack(Items.STONE, 32));
        int droppedBefore = droppedCount(helper, hopper);
        tick(helper, hopper, 4);

        helper.assertTrue(hopperCount(hopper) == 0, "divisible mode drops both source stacks");
        helper.assertTrue(droppedCount(helper, hopper) - droppedBefore == 64,
                "divisible mode drops up to 64 items per pass");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 100)
    public static void modeTwoDoesNotDropALoneItem(GameTestHelper helper) {
        HopperBlockEntity hopper = placeHopper(helper, 1, 2, 1, Direction.DOWN);
        hopper.setStackInSlot(0, new ItemStack(Items.STONE, 1));
        for (int i = 0; i < 2; i++) hopper.cycleMode(false);
        hopper.toggleExactMode();
        int droppedBefore = droppedCount(helper, hopper);
        tick(helper, hopper, 4);

        helper.assertTrue(hopperCount(hopper) == 1, "a source stack below the mode size is never emitted");
        helper.assertTrue(droppedCount(helper, hopper) == droppedBefore,
                "no item entity is spawned for the lone item");
        helper.succeed();
    }

    private static HopperBlockEntity placeHopper(GameTestHelper helper, int x, int y, int z, Direction facing) {
        BlockPos pos = at(helper, x, y, z);
        var state = BuiltInRegistries.BLOCK
                .get(id("hopper_bronze"))
                .defaultBlockState()
                .setValue(DirectionalBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        return (HopperBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static Container chest(GameTestHelper helper) {
        BlockPos pos = at(helper, 2, 1, 1);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        return (Container) helper.getLevel().getBlockEntity(pos);
    }

    /** Every target slot keeps only 8 free space, below the 16-item move size. */
    private static Container nearlyFullChest(GameTestHelper helper) {
        Container chest = chest(helper);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, NEARLY_FULL_STACK));
        }
        return chest;
    }

    private static int nearlyFullChestCount(Container chest) {
        return chest.getContainerSize() * NEARLY_FULL_STACK;
    }

    /** The hopper decrements its check counter before it first processes. */
    private static void tick(GameTestHelper helper, HopperBlockEntity hopper, int times) {
        for (int i = 0; i < times; i++) {
            HopperBlockEntity.serverTick(helper.getLevel(), hopper.getBlockPos(), hopper.getBlockState(), hopper);
        }
    }

    private static int hopperCount(HopperBlockEntity hopper) {
        int total = 0;
        for (int slot = 0; slot < hopper.getSlots(); slot++) total += hopper.getStackInSlot(slot).getCount();
        return total;
    }

    private static int containerCount(Container container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(Items.STONE)) total += stack.getCount();
        }
        return total;
    }

    private static int droppedCount(GameTestHelper helper, HopperBlockEntity hopper) {
        AABB area = new AABB(hopper.getBlockPos()).inflate(3);
        int total = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) {
            if (entity.getItem().is(Items.STONE)) total += entity.getItem().getCount();
        }
        return total;
    }

    private static ResourceLocation id(String path) {
//? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
*///?} else {
        return new ResourceLocation("gt6ce", path);
//?}
    }

    private static BlockPos at(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }
}
