package gregtech.content.worldgen.dungeon;

import gregtech.content.block.inventory.BottleCrateBlock;
import gregtech.content.blockentity.inventory.BottleCrateBlockEntity;
import gregtech.content.item.BottleItem;
import gregtech.content.registry.GTStorageContainers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** GT6 DungeonChunkRoomWorkshop–160 and 183–195, including conditional random-call order. */
public final class DungeonBottleSupplies {
    private static final Direction[] FACING = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final String[] CHEMICALS = {
        "mercury_bottle",
        "mercury_bottle",
        "mercury_bottle",
        "glue_bottle",
        "lubricant_bottle",
        "ink_bottle",
        "purple_drink",
        "holy_water",
        "bottled_indigo_dye"
    };

    private DungeonBottleSupplies() {}

    public static void chemicals(GTDungeonData data, int x, int y, int z) {
        place(data, x, y, z, null);
    }

    public static void drinks(GTDungeonData data, int x, int y, int z, String fluidField) {
        String id = DungeonDrinks.drinkPath(fluidField);
        if (id == null) throw new IllegalArgumentException("Unmapped workshop drink: " + fluidField);
        place(data, x, y, z, id);
    }

    private static void place(GTDungeonData data, int x, int y, int z, String drink) {
        var state = GTStorageContainers.BOTTLE_CRATE
                .get()
                .defaultBlockState()
                .setValue(BottleCrateBlock.FACING, FACING[data.next(4)]);
        data.set(x, y, z, state);
        var entity = data.level.getBlockEntity(new BlockPos(data.x + x, data.y + y, data.z + z));
        if (!(entity instanceof BottleCrateBlockEntity crate)) return;
        for (int slot = 0; slot < 9; slot++) {
            boolean filled = drink != null ? data.next1in3() : slot < 6 ? data.next3in4() : data.next1in2();
            String path = drink == null ? CHEMICALS[slot] : drink;
            ItemStack stack = filled
                    ? DungeonDrinks.bottle(
                            path, drink != null || path.equals("purple_drink") || path.equals("holy_water"))
                    : data.next1in2() ? BottleItem.emptyBottle() : ItemStack.EMPTY;
            if (!stack.isEmpty()) stack.setCount(1 + data.next(drink == null ? 8 : 4));
            crate.items().setStackInSlot(slot, stack);
        }
    }
}
