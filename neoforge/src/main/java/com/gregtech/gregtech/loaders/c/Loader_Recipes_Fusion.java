package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTTechnological;
import com.gregtech.gregtech.loaders.IGTLoader;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** GT6 Loader_Recipes_Other lines 949-966: all 18 fusion reactions, including their LU startup cost.
 * Gas units are 1000 mB/U; molten metal units are 144 mB/U. Missing phases never substitute another phase. */
public final class Loader_Recipes_Fusion implements IGTLoader {
    private int added;
    private final Set<String> missing=new TreeSet<>();
    public void run() {
        add(1,730,-8192,95682560L,new String[]{"D:gas:2000"},new String[]{"He_3:gas:500","T:gas:500"},null);
        add(1,1130,-8192,148111360L,new String[]{"T:gas:2000"},new String[]{"He:gas:1000"},null);
        add(1,1290,-8192,169082880L,new String[]{"He_3:gas:2000"},new String[]{"He:gas:1000"},null);
        add(1,1890,0,247726080L,new String[]{"He:gas:2000"},new String[]{"Be_8:liquid:144"},null);
        add(1,3214,0,421265408L,new String[]{"Be_8:liquid:288"},new String[]{"O:gas:1000"},null);
        add(2,546,-8192,1110048768L,new String[]{"H:gas:1000","B_11:liquid:144"},new String[]{"He:gas:3000"},null);
        add(2,315,-8192,41287680L,new String[]{"H:gas:1000","C:liquid:144"},new String[]{"C_13:liquid:144"},null);
        add(2,754,-8192,98828288L,new String[]{"H:gas:1000","C_13:liquid:144"},new String[]{"N:gas:1000"},null);
        add(2,1404,-8192,184025088L,new String[]{"H:gas:2000","N:gas:1000"},new String[]{"He:gas:500","C:liquid:72","O:gas:500"},null);
        add(2,455,-8192,59637760L,new String[]{"H:gas:2000","O:gas:1000"},new String[]{"He:gas:500","F:gas:500","N:gas:500"},null);
        add(2,1760,-8192,230686720L,new String[]{"D:gas:1000","T:gas:1000"},new String[]{"He:gas:1000"},null);
        add(2,1830,-8192,239861760L,new String[]{"D:gas:1000","He_3:gas:1000"},new String[]{"He:gas:1000"},null);
        add(2,2640,-8192,346030080L,new String[]{"T:gas:1000","He_3:gas:1000"},new String[]{"He:gas:750","D:gas:250"},null);
        add(2,3336,-8192,437256192L,new String[]{"D:gas:1000","Li_6:liquid:144"},new String[]{"He:gas:375","He_3:gas:125","Li:liquid:18","Be_7:liquid:18"},null);
        add(2,1690,-8192,221511680L,new String[]{"He_3:gas:1000","Li_6:liquid:144"},new String[]{"He:gas:2000"},null);
        add(2,736,-8192,96468992L,new String[]{"He:gas:1000","Be_8:liquid:144"},new String[]{"C:liquid:144"},null);
        add(2,716,-8192,93847552L,new String[]{"He:gas:1000","C:liquid:144"},new String[]{"O:gas:1000"},null);
        add(2,1956,-8192,12446072832L,new String[]{"Ad:liquid:144","Be_7:liquid:144"},new String[]{"W:liquid:144","He:gas:16000","He_3:gas:24000","T:gas:24000"},"Vb");
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Fusion recipes: {} / 18; unresolved: {}",added,missing);
    }
    private FluidStack fluid(String token) {
        var fields=token.split(":");var material=GTGeneratedChem.material(fields[0]);
        if(material==null){missing.add(token);return FluidStack.EMPTY;}
        var name=material.getName().toLowerCase(Locale.ROOT);
        // GT6 fuels the ring with the material's own gas/liquid phase (MT.D.gas(...)), not with a
        // name-prefixed fluid: the generated gas name is the material name (FL.java:1080) and the
        // molten phase keeps its "molten." prefix (FL.java:1077).
        var stack="gas".equals(fields[1])
                ? GTGeneratedChem.materialFluid(material.getName(),Integer.parseInt(fields[2]))
                : GTGeneratedChem.byRegistryName("molten."+name,Integer.parseInt(fields[2]));
        if(stack==null){missing.add(token);return FluidStack.EMPTY;}
        return stack;
    }
    private void add(int circuit,long ticks,long eut,long startup,String[] inputs,String[] outputs,String dust) {
        var in=Arrays.stream(inputs).map(this::fluid).toArray(FluidStack[]::new);
        var out=Arrays.stream(outputs).map(this::fluid).toArray(FluidStack[]::new);
        if(Arrays.stream(in).anyMatch(FluidStack::isEmpty)||Arrays.stream(out).anyMatch(FluidStack::isEmpty))return;
        ItemStack[] items=new ItemStack[0];
        if(dust!=null) {
            var material=GTGeneratedChem.material(dust);
            var stack=material==null?ItemStack.EMPTY:MaterialStackItemHelper.mat(MaterialPrefix.dust,material,1);
            if(stack.isEmpty()){missing.add(dust);return;}items=new ItemStack[]{stack};
        }
        MachineRecipeMaps.Fusion.addRecipe(new Recipe(new ItemStack[]{new ItemStack(GTTechnological.selectorTag(circuit))},items,null,null,in,out,ticks,eut,startup));
        added++;
    }
}
