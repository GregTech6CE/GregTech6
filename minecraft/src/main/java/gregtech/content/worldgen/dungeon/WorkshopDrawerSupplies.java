package gregtech.content.worldgen.dungeon;

import gregtech.api.material.*;
import gregtech.content.block.inventory.DrawerQuadBlock;
import gregtech.content.blockentity.inventory.DrawerQuadBlockEntity;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.Materials;
import gregtech.content.registry.*;
import gregtech.engine.material.*;
import net.minecraft.core.*;

/** GT6 DungeonChunkRoomWorkshop. Rolls counts before slots; duplicate slots are overwritten. */
public final class WorkshopDrawerSupplies {
    private WorkshopDrawerSupplies() {}

    public static void place(GTDungeonData data, int x, int y, int z) {
        data.set(
                x,
                y,
                z,
                GTStorageContainers.DRAWER_QUAD
                        .get()
                        .defaultBlockState()
                        .setValue(DrawerQuadBlock.FACING, Direction.EAST));
        if (!(data.level.getBlockEntity(new BlockPos(data.x + x, data.y + y, data.z + z))
                instanceof DrawerQuadBlockEntity drawer))
            throw new IllegalStateException("Workshop drawer failed to place");
        GTMaterial[] materials = {Materials.StainlessSteel, Materials.Bronze, Materials.Invar, Materials.Brass};
        MaterialPrefix[] forms = {
            MaterialPrefix.stick,
            MaterialPrefix.ingot,
            MaterialPrefix.plate,
            MaterialPrefix.plateCurved,
            MaterialPrefix.screw,
            MaterialPrefix.ring,
            MaterialPrefix.gearGt,
            MaterialPrefix.gearGtSmall
        };
        int[] base = {32, 32, 32, 16, 16, 8, 1, 8}, range = {33, 33, 33, 49, 49, 25, 4, 25};
        for (int page = 0; page < 4; page++)
            for (int form = 0; form < forms.length; form++) {
                int count = base[form] + data.next(range[form]);
                int slot = page * 36 + data.next(36);
                var stack = GTItems.getStack(forms[form], materials[page], count);
                if (stack.isEmpty())
                    throw new IllegalStateException(
                            "Missing workshop part " + forms[form] + " / " + materials[page].getName());
                drawer.items().setStackInSlot(slot, stack);
            }
    }
}
