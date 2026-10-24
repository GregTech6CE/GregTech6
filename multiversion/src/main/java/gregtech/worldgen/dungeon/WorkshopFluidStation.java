package gregtech.worldgen.dungeon;

//? if >=1.21.1 {

/*import gregtech.registry.*;
*///?} else {
import gregtech.GregTech;
//?}
import gregtech.block.tool.FluidAttachmentBlock;
import gregtech.registry.GTFluids;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** GT6 workshop 32716 drum, two 32730 taps and 32725 ceiling funnel. */
public final class WorkshopFluidStation {
    private WorkshopFluidStation() {}
    public static void place(GTDungeonData data,int x,int y,int z) {
//? if >=1.21.1 {
        /*var drum=java.util.Objects.requireNonNull(BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","drum_stainless_steel")));
*///?} else {
        var drum=java.util.Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(GregTech.id("drum_stainless_steel")));
//?}
        data.tank(x,y,z,drum,GTFluids.stack("Water",64000));
        attachment(data,x-1,y,z,"tap_stainless_steel",Direction.EAST);
        attachment(data,x,y,z-1,"tap_stainless_steel",Direction.SOUTH);
        attachment(data,x,y+1,z,"fluid_funnel_stainless_steel",Direction.DOWN);
    }
    private static void attachment(GTDungeonData data,int x,int y,int z,String id,Direction direction) {
//? if >=1.21.1 {
        /*var block=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",id));
*///?} else {
        var block=ForgeRegistries.BLOCKS.getValue(GregTech.id(id));
//?}
        if(!(block instanceof FluidAttachmentBlock)) throw new IllegalStateException("Missing workshop attachment "+id);
        data.set(x,y,z,block.defaultBlockState().setValue(FluidAttachmentBlock.FACING,direction));
    }
}
