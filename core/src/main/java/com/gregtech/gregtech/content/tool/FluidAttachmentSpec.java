package com.gregtech.gregtech.content.tool;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import java.util.*;
/** Material/acid flags from GT6 Loader_MultiTileEntities 32079-32750. */
public record FluidAttachmentSpec(String id,String shape,GTMaterial material,boolean acidProof) {
    public static List<FluidAttachmentSpec> all(){
        var result=new ArrayList<FluidAttachmentSpec>();
        for(String kind:List.of("tap","fluid_funnel","cap_nozzle")){
            result.add(new FluidAttachmentSpec(kind+"_"+(kind.equals("cap_nozzle")?"steel":"ceramic"),kind,kind.equals("cap_nozzle")?Materials.Steel:Materials.Ceramic,false));
            result.add(new FluidAttachmentSpec(kind+"_plastic",kind,Materials.Plastic,false));
            result.add(new FluidAttachmentSpec(kind+"_stainless_steel",kind,Materials.StainlessSteel,true));
            result.add(new FluidAttachmentSpec(kind+"_tungsten",kind,Materials.Tungsten,true));
            result.add(new FluidAttachmentSpec(kind+"_tantalum_hafnium_carbide",kind,Materials.TantalumHafniumCarbide,false));
            result.add(new FluidAttachmentSpec(kind+"_adamantium",kind,Materials.Adamantium,true));
        }
        return List.copyOf(result);
    }
}
