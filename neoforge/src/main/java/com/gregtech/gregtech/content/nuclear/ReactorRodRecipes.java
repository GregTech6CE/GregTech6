package com.gregtech.gregtech.content.nuclear;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;
/** Original rod canning and centrifuging quantities, including isotope fraction dust. */
public final class ReactorRodRecipes {
    private static int count;
    public static int count(){return count;}
    public static int register(){
        if(count!=0)throw new IllegalStateException("Reactor recipes registered twice");
        RadiationMedicineRecipes.register();
        for(var d:ReactorRodCatalog.ALL){
            if(d.kind()==ReactorRodCatalog.Kind.NUCLEAR||d.kind()==ReactorRodCatalog.Kind.ABSORBER||d.kind()==ReactorRodCatalog.Kind.MODERATOR||d.kind()==ReactorRodCatalog.Kind.REFLECTOR)
                add(MachineRecipeMaps.Canner,new Recipe(new ItemStack[]{mat(MaterialPrefix.stick,d.material(),1),GTFuelRods.stack(9201)},new ItemStack[]{GTFuelRods.stack(d.originalId())},null,null,null,null,16,16,0));
            if(d.kind()==ReactorRodCatalog.Kind.BREEDER)
                add(MachineRecipeMaps.Canner,new Recipe(new ItemStack[]{mat(MaterialPrefix.bolt,d.material(),4),GTFuelRods.stack(9201)},new ItemStack[]{GTFuelRods.stack(d.originalId())},null,null,null,null,16,16,0));
        }
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9310)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Thorium",1),mat(MaterialPrefix.dustDiv72,"Uranium",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9319)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Cyanite",1),mat(MaterialPrefix.dustDiv72,"Blutonium",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9320)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Uranium",1),mat(MaterialPrefix.dustDiv72,"Uranium235",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9321)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Uranium235",1),mat(MaterialPrefix.dustDiv72,"Plutonium",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9322)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Uranium233",1),mat(MaterialPrefix.dustDiv72,"Plutonium243",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9329)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Yellorium",1),mat(MaterialPrefix.dustDiv72,"Cyanite",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9330)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Plutonium",1),mat(MaterialPrefix.dustDiv72,"Plutonium241",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9331)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Plutonium241",1),mat(MaterialPrefix.dustDiv72,"Plutonium243",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9332)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Plutonium243",1),mat(MaterialPrefix.dustDiv72,"Americium",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9333)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Plutonium239",1),mat(MaterialPrefix.dustDiv72,"Americium241",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9339)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Blutonium",1),mat(MaterialPrefix.dustDiv72,"Ludicrite",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9340)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Americium",1),mat(MaterialPrefix.dustDiv72,"Americium241",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9341)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Americium241",1),mat(MaterialPrefix.dustDiv72,"NaquadahEnriched",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9349)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Ludicrite",1),mat(MaterialPrefix.dustDiv72,"Yellorium",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9350)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Cobalt60",1),mat(MaterialPrefix.dustDiv72,"Thorium",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9360)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"NaquadahEnriched",1),mat(MaterialPrefix.dustDiv72,"Naquadria",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9361)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Naquadria",1),mat(MaterialPrefix.dustDiv72,"Cobalt60",6)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9411)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Uranium233",4),mat(MaterialPrefix.dustDiv72,"Thorium",4)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9421)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"Plutonium239",4),mat(MaterialPrefix.dustDiv72,"Uranium",4)},null,null,null,null,256,64,0));
        add(MachineRecipeMaps.Centrifuge,new Recipe(new ItemStack[]{GTFuelRods.stack(9441)},new ItemStack[]{mat(MaterialPrefix.scrapGt,"Zirconium",9),mat(MaterialPrefix.dustTiny,"NaquadahEnriched",4),mat(MaterialPrefix.dustDiv72,"Naquadah",4)},null,null,null,null,256,512,0));
        var tritium=GTFluids.still("Tritium");
        if(tritium==null)throw new IllegalStateException("Missing tritium");
        add(MachineRecipeMaps.Canner,new Recipe(new ItemStack[]{GTFuelRods.stack(9431)},new ItemStack[]{GTFuelRods.stack(9201)},null,null,null,new FluidStack[]{new FluidStack(tritium.get(),500)},16,16,0));
        var zirconium=GTMaterialRegistry.get("Zirconium");
        var shape=GTTechnological.get("extruder_shape_cell");
        long ticks=Math.max(64,(long)((zirconium.getMeltingPoint()-293)*(1+zirconium.getDensity())/6144));
        for(var prefix:new MaterialPrefix[]{MaterialPrefix.ingot,MaterialPrefix.dust})
            add(MachineRecipeMaps.Extruder,new Recipe(new ItemStack[]{mat(prefix,"Zirconium",1),new ItemStack(shape)},new ItemStack[]{GTFuelRods.stack(9201)},null,null,null,null,ticks,96,0));
        return count;
    }
    private static ItemStack mat(MaterialPrefix prefix,String material,int amount){var stack=GTItems.getStack(prefix,GTMaterialRegistry.get(material),amount);if(stack.isEmpty())throw new IllegalStateException("Missing reactor recipe form: "+prefix.getName()+"/"+material);return stack;}
    private static void add(RecipeMap map,Recipe recipe){if(map.addRecipe(recipe)==null)throw new IllegalStateException("Rejected reactor recipe: "+map.mNameInternal);count++;}
}
