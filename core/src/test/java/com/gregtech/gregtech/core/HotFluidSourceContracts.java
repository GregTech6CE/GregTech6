package com.gregtech.gregtech.core;

import com.gregtech.gregtech.content.fluid.FluidDefinitions;
import com.gregtech.gregtech.content.recipe.OriginalFuelRecipeRows;
import com.gregtech.gregtech.data.FluidCatalog;

/** Pinned Loader_Fuels:191-213 and CS:216-234, including per-litre rather than per-bucket energy. */
final class HotFluidSourceContracts {
    private static int assertions;
    static int verify() {
        assertions=0;
        FluidDefinitions.prepare();
        check(FluidCatalog.Water.isVanillaWater() && FluidCatalog.Lava.isVanillaLava(), "Only source Water/Lava reuse vanilla identity");
        check(FluidCatalog.all().values().stream().filter(FluidCatalog.FluidEntry::usesVanillaFluid).count()==2,
                "Sprite reuse does not suppress distinct registrations");
        for(String key:new String[]{"Lava_Pahoehoe","Lava_Volcanic","Lava_Pure"}) {
            var entry=FluidCatalog.get(key);
            check(!entry.usesVanillaFluid() && entry.textureMode()==FluidCatalog.FluidTextureMode.VANILLA_LAVA,
                    "Distinct fluid despite shared lava texture: "+key);
        }
        var cold=FluidCatalog.get("Lava_Pahoehoe");
        check(cold.temperature()==1200 && cold.luminosity()==10 && cold.density()==50000 && cold.viscosity()==250000,
                "Original Loader_Fluids:99 pahoehoe physical properties");
        var rows=OriginalFuelRecipeRows.ROWS.stream().filter(row->row.kind().equals("hot")).toList();
        String[] inputs={"Blaze","Lava","Lava_Volcanic","Hot_Water","Water_Hot","Water_Boiling","Water_Geothermal",
                "Coolant_IC2_Hot","Hot_Molten_Sodium","Hot_Molten_Tin","Hot_Heavy_Water","Hot_Semi_Heavy_Water",
                "Hot_Tritiated_Water","Hot_Carbon_Dioxide","Hot_Helium","Hot_Molten_LiCl"};
        String[] outputs={null,"Lava_Pahoehoe","Lava_Pahoehoe","water","water","water","MnWtr",
                "Coolant_IC2","GenMolten_Sodium","GenMolten_Tin","GenLiquid_HeavyWater","GenLiquid_SemiheavyWater",
                "GenLiquid_TritiatedWater","CarbonDioxide","Helium","GenLiquid_LithiumChloride"};
        long[] energy={96,80,1280,2,2,2,64,20,30,40,50,40,60,20,30,15};
        check(rows.size()==16,"All sixteen source hot-fluid rows");
        for(int i=0;i<inputs.length;i++) {
            var row=rows.get(i);
            check(row.input().equals(inputs[i]) && java.util.Objects.equals(row.output(),outputs[i]) && row.amount()==1
                    && row.outputAmount()==(i==0?0:1) && row.output2()==null && row.eut()*row.duration()==energy[i],
                    "Pinned source fluid/amount/energy "+inputs[i]);
            check(FluidCatalog.get(row.input())!=null && (row.output()==null || row.output().equals("water") || FluidCatalog.get(row.output())!=null),
                    "Source fluids exist before native registration: "+inputs[i]);
        }
        return assertions;
    }
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
}
