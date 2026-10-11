package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.machine.*;
import com.gregtech.gregtech.content.multiblock.*;
import com.gregtech.gregtech.data.*;
import java.util.*;

/** Fixed original registrations, not a replay of the implementation's defaults. */
final class LargeRecipeControllerContracts {
    private static int count;
    private record Row(String name,int id,String energy,int parallel,int efficiency,int structureRows,Map<String,Long> parts) {}
    static int verify() {
        long u=GTValues.U;
        var rows=List.of(
                new Row("largecentrifuge",17100,"RU",16,5000,3,Map.of("TungstenSteel",148*u/9,"Pt",2*u,"Ruby",2*u)),
                new Row("largeelectrolyzer",17103,"EU",16,5000,3,Map.of("StainlessSteel",8*u,"Pt",4*u,"Ruby",2*u)),
                new Row("largecoagulator",17105,"TU",64,10000,3,Map.of("StainlessSteel",49*u,"Pt",u,"Ruby",u)),
                new Row("largeautoclave",17112,"TU",16,10000,3,Map.of("StainlessSteel",81*u,"Pt",u,"Ruby",u)),
                new Row("largebath",17104,"TU",64,10000,3,Map.of("StainlessSteel",31*u,"Pt",u,"Ruby",u,"Cu",33*u,"Rubber",18*u,"Al",175*u/9,"SteelMagnetic",3*u)),
                new Row("largemixer",17102,"RU",256,10000,3,Map.of("StainlessSteel",42*u,"Pt",u,"Ruby",u)),
                new Row("largefermenter",17113,"HU",256,10000,4,Map.of("StainlessSteel",49*u,"Pt",u,"Ruby",u)),
                new Row("largeoven",17106,"EU",64,2500,5,Map.of("Invar",49*u,"Pt",u,"Ruby",u)),
                new Row("largesluice",17107,"RU",64,5000,5,Map.of("Ti",17*u,"Pt",u,"Ruby",u)),
                new Row("largecrusher",17108,"RU",64,5000,4,Map.of("TungstenSteel",19*u,"Pt",u,"Ruby",u)),
                new Row("largeshredder",17109,"RU",64,5000,4,Map.of("TungstenSteel",19*u,"Pt",u,"Ruby",u)),
                new Row("largesqueezer",17114,"RU",64,5000,3,Map.of("Steel",22*u,"Pt",u,"Ruby",u)));
        var catalog=BasicMachineCatalog.specifications();
        for(var row:rows) {
            var spec=catalog.stream().filter(s->s.machineName().equals(row.name)).findFirst().orElseThrow();
            require(spec.energyType().equals(row.energy) && spec.energyIn()==(row.energy.equals("TU")?1:512),"source energy "+row.name);
            require(spec.energyInMin()==(row.energy.equals("TU")||row.energy.equals("HU")?1:512)
                    && spec.energyInMax()==(row.energy.equals("TU")?16:4096),"source accepted range "+row.name);
            require(spec.parallelLimit()==row.parallel && LargeMachineProcessingRules.efficiency(row.name)==row.efficiency,"source parallel/efficiency "+row.name);
            require(OriginalLargeRecipeMachineData.structureKeys(row.name).size()==row.structureRows,"source structure text "+row.name);
            var meta=SourceBlockProperties.basic(row.name,1).orElseThrow();
            require(meta.sourceId()==row.id && meta.tool().equals("wrench")&&!meta.handHarvestable(),"source metadata "+row.name);
            var data=OriginalMachineMaterialData.find(row.name,1).orElseThrow();
            require(data.components().size()==row.parts.size(),"exact known source component count "+row.name);
            for(var expected:row.parts.entrySet()) require(data.components().stream().anyMatch(p->p.material().resolve()==GTMaterialRegistry.get(expected.getKey()).resolve()&&p.amount()==expected.getValue()),"exact REV component "+row.name+"/"+expected.getKey());
            require(spec.faceConfig().itemInputs()==63&&spec.faceConfig().itemOutputs()==63&&spec.faceConfig().fluidInputs()==63&&spec.faceConfig().fluidOutputs()==63&&spec.faceConfig().energyInputs()==63,"original main accepts any face "+row.name);
            require(spec.faceConfig().itemAutoInput()==-1&&spec.faceConfig().fluidAutoInput()==-1,"no source auto input "+row.name);
            require(spec.faceConfig().itemAutoOutput()==(row.name.equals("largefermenter")?5:0)&&spec.faceConfig().fluidAutoOutput()==spec.faceConfig().itemAutoOutput(),"source auto output side "+row.name);
        }
        var f=OriginalLargeRecipeMachineData.output("largefermenter",true);require(f.right()==0&&f.up()==1&&f.back()==5,"fermenter upper back item target");
        f=OriginalLargeRecipeMachineData.output("largefermenter",false);require(f.right()==0&&f.up()==0&&f.back()==5,"fermenter lower back fluid target");
        require(OriginalLargeRecipeMachineData.sources("largecentrifuge").equals(List.of(new OriginalLargeRecipeMachineData.Source(0,-1,1,OriginalLargeRecipeMachineData.Face.UP),new OriginalLargeRecipeMachineData.Source(0,2,1,OriginalLargeRecipeMachineData.Face.DOWN))),"source center top/bottom pair");
        require(OriginalLargeRecipeMachineData.sources("largeelectrolyzer").size()==1,"electrolyzer lower only");
        require(OriginalLargeRecipeMachineData.sources("largecrusher").equals(List.of(new OriginalLargeRecipeMachineData.Source(-3,1,2,OriginalLargeRecipeMachineData.Face.RIGHT),new OriginalLargeRecipeMachineData.Source(3,1,2,OriginalLargeRecipeMachineData.Face.LEFT))),"crusher source lateral pair");
        require(OriginalLargeRecipeMachineData.sources("largesluice").equals(List.of(new OriginalLargeRecipeMachineData.Source(-2,1,5,OriginalLargeRecipeMachineData.Face.RIGHT),new OriginalLargeRecipeMachineData.Source(2,1,5,OriginalLargeRecipeMachineData.Face.LEFT))),"sluice far-side pair");
        for(var name:List.of("largecoagulator","largeautoclave","largebath","largefermenter","largeoven"))require(OriginalLargeRecipeMachineData.sources(name).isEmpty(),"source no fabricated six-neighbor requests "+name);
        require(MultiblockCraftingRecipes.find("largeautoclave_stainless_steel").keys().get('M').equals("item:gregtech:tank_wall_dense"),"source17112 uses dense18022 wall");
        require(MultiblockCraftingRecipes.find("largebath_stainless_steel").keys().get('A').equals("item:gregtech:compact_robot_arm_mv"),"source17104 fixedIL.ROBOT_ARMS[2] component");
        return count;
    }
    private static void require(boolean ok,String message) {count++;if(!ok)throw new AssertionError(message);}
}
