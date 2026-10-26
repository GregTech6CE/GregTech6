package gregtech.content.worldgen.dungeon;

//? if >=1.21.1 {
/*import gregtech.content.registry.*;
*///?} else {
//?}
import gregtech.content.block.misc.*;
import gregtech.content.material.Materials;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.*;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;

/** Original workshop 5011: eight steel part rolls, followed by an empty/full Invar lighter. */
public final class WorkshopCraftingSupplies {
    private WorkshopCraftingSupplies(){}
    public static void place(GTDungeonData data,int x,int y,int z){
//? if >=1.21.1 {
        /*data.set(x,y,z,DungeonBindings.craftingTable(Materials.StainlessSteel).defaultBlockState().setValue(AdvancedCraftingTableBlock.FACING,Direction.EAST));
*///?} else {
        data.set(x,y,z,GTMiscBlocks.advancedCraftingTable(Materials.StainlessSteel).defaultBlockState().setValue(AdvancedCraftingTableBlock.FACING,Direction.EAST));
//?}
        if(!(data.level.getBlockEntity(new BlockPos(data.x+x,data.y+y,data.z+z)) instanceof AdvancedCraftingTableBlockEntity table))throw new IllegalStateException("Missing workshop crafting table");
        MaterialPrefix[] forms={MaterialPrefix.stick,MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.plateCurved,MaterialPrefix.screw,MaterialPrefix.ring,MaterialPrefix.gearGt,MaterialPrefix.gearGtSmall};
        int[] base={32,32,32,16,16,8,1,8},range={33,33,33,49,49,25,4,25};
        for(int i=0;i<8;i++){int count=base[i]+data.next(range[i]);int slot=data.next(16);ItemStack stack=GTItems.getStack(forms[i],Materials.Steel,count);
            if(stack.isEmpty())throw new IllegalStateException("Missing workshop part "+forms[i]);table.items().setStackInSlot(slot,stack);}
        String lighter=data.next1in2()?"lighter_full":"lighter_empty";
//? if >=1.21.1 {
        /*ItemStack stack=new ItemStack(java.util.Objects.requireNonNull(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",lighter))));
*///?} else {
        ItemStack stack=new ItemStack(java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(gregtech.GregTech.id(lighter))));
//?}
        table.items().setStackInSlot(35+data.next(36),stack);
    }
}
