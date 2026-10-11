package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.machine.MachineConstructionMaterials;
import com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData;
import com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData.Kind;
import com.gregtech.gregtech.data.SourceBlockProperties;
import java.util.*;

/** Fixed original Loader rows / LH arithmetic, independent of the live rating table. */
final class GeneratorTooltipContracts {
    private static int assertions;
    static int verify() {
        assertions=0;
        long[] gasInput={6144,12288,24576,196608},output={4096,8192,16384,131072};
        int[] walls={18022,18026,18023,18025};
        check(OriginalGeneratorTooltipData.ALL.size()==12&&OriginalGeneratorTooltipData.aliases().size()==12,"Twelve original converters / distinct legacy aliases");
        for(int i=0;i<4;i++)for(var kind:Kind.values()) {
            int id=(kind==Kind.STEAM?17211:kind==Kind.DYNAMO?17221:17231)+i;
            var p=OriginalGeneratorTooltipData.ALL.stream().filter(x->x.originalId()==id).findFirst().orElseThrow();
            long in=kind==Kind.DYNAMO?output[i]:gasInput[i]*(kind==Kind.STEAM?2:1);
            long out=kind==Kind.DYNAMO?output[i]*3/4:output[i];
            check(p.input()==in&&p.output()==out&&p.wallId()==walls[i],"Fixed original converter ratings "+id);
            check(p.inputMinimum()==in/2&&p.inputMaximum()==in*2&&p.outputMinimum()==out/2&&p.outputMaximum()==out*2,"Original EnergyStats range "+id);
            check(p.showsInput()==(kind!=Kind.GAS)&&p.efficiency()==(kind==Kind.DYNAMO?7500:6666),"Source gas hides fuel-HU input; original truncated efficiency "+id);
            check(p.structureKeys().size()==(kind==Kind.DYNAMO?3:4),"Source specialized structure count "+id);
            check(OriginalGeneratorTooltipData.find(p.legacyPath()).equals(p)&&OriginalGeneratorTooltipData.find(p.sourcePath()).equals(p),"Two actual identities resolve one source "+id);
            var a=MachineConstructionMaterials.block(p.legacyPath()).orElseThrow();var b=MachineConstructionMaterials.block(p.sourcePath()).orElseThrow();
            check(a.equals(b)&&a.recoverable(),"Legacy conversion preserves precise positive source REV "+id);
            check(SourceBlockProperties.block(p.legacyPath()).orElseThrow().equals(SourceBlockProperties.block(p.sourcePath()).orElseThrow())&&SourceBlockProperties.block(p.legacyPath()).orElseThrow().sourceId()==id,"Harvest level/source identity preserved for alias "+id);
        }
        composition("large_turbine_main",Map.of("Magnalium",72L,"StainlessSteel",36L));
        composition("large_steam_turbine_vibramantium",Map.of("Vibramantium",72L,"Adamantium",36L));
        composition("large_dynamo_main",Map.of("StainlessSteel",40L));
        composition("large_dynamo_adamantium",Map.of("Adamantium",40L));
        check(OriginalGeneratorTooltipData.find("large_dynamo_main").outputUnitKey().equals("gt.td.short.energy.electricity"),"Actual source EU language key");
        return assertions;
    }
    private static void composition(String path,Map<String,Long> units) {
        var actual=new HashMap<String,Long>();
        for(var part:MachineConstructionMaterials.block(path).orElseThrow().components())actual.put(part.material().resolve().getName(),part.amount());
        var expected=new HashMap<String,Long>();units.forEach((m,n)->expected.put(GTMaterialRegistry.get(m).resolve().getName(),n*GTValues.U));
        check(actual.equals(expected),"Original known controller-only CR.REV quantities "+path);
    }
    private static void check(boolean ok,String message) {assertions++;if(!ok)throw new AssertionError(message);}
}
