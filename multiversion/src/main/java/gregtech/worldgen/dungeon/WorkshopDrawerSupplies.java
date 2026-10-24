package gregtech.worldgen.dungeon;

//? if >=1.21.1 {
/*import gregtech.registry.*;
*///?} else {
//?}
import gregtech.api.material.*;
import gregtech.data.MaterialPrefix;
import gregtech.content.material.Materials;
import gregtech.registry.*;
import gregtech.block.inventory.DrawerQuadBlock;
import gregtech.blockentity.inventory.DrawerQuadBlockEntity;
import net.minecraft.core.*;

/** GT6 DungeonChunkRoomWorkshop:63-97. Rolls counts before slots; duplicate slots are overwritten. */
public final class WorkshopDrawerSupplies {
    private WorkshopDrawerSupplies() {}
    public static void place(GTDungeonData data, int x, int y, int z) {
//? if >=1.21.1 {
        /*data.set(x,y,z,DungeonBindings.block("drawer_quad").defaultBlockState().setValue(DrawerQuadBlock.FACING,Direction.EAST));
*///?} else {
        data.set(x,y,z,GTStorage.DRAWER_QUAD.get().defaultBlockState().setValue(DrawerQuadBlock.FACING,Direction.EAST));
//?}
        if (!(data.level.getBlockEntity(new BlockPos(data.x+x,data.y+y,data.z+z)) instanceof DrawerQuadBlockEntity drawer))
            throw new IllegalStateException("Workshop drawer failed to place");
        GTMaterial[] materials = {Materials.StainlessSteel,Materials.Bronze,Materials.Invar,Materials.Brass};
        MaterialPrefix[] forms = {MaterialPrefix.stick,MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.plateCurved,
                MaterialPrefix.screw,MaterialPrefix.ring,MaterialPrefix.gearGt,MaterialPrefix.gearGtSmall};
        int[] base = {32,32,32,16,16,8,1,8}, range = {33,33,33,49,49,25,4,25};
        for (int page=0;page<4;page++) for (int form=0;form<forms.length;form++) {
            int count=base[form]+data.next(range[form]);
            int slot=page*36+data.next(36);
            var stack=GTItems.getStack(forms[form],materials[page],count);
            if (stack.isEmpty()) throw new IllegalStateException("Missing workshop part "+forms[form]+" / "+materials[page].getName());
            drawer.items().setStackInSlot(slot,stack);
        }
    }
}
