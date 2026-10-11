package com.gregtech.gregtech.content.energy;
import com.gregtech.gregtech.api.energy.*;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.data.GregTechTags;
/** Exact original custom gearboxes and rotational transformers, independent of platform registration. */
public final class GearboxCatalog {private GearboxCatalog(){}
    public record Tier(String suffix, GTMaterial material, int voltageTier, long maxSpeed) {}
    private static final Tier[] TIERS = {
            new Tier("wood", WoodMaterials.WoodTreated, 0, 16),
            new Tier("bronze", Materials.Bronze, 1, 64),
            new Tier("brass", Materials.Brass, 1, 64),
            new Tier("arsenic_copper", Materials.ArsenicCopper, 1, 64),
            new Tier("arsenic_bronze", Materials.ArsenicBronze, 1, 64),
            new Tier("steel", Materials.Steel, 2, 256),
            new Tier("titanium", Materials.Titanium, 3, 1024),
            new Tier("tungstensteel", Materials.Tungstensteel, 4, 4096),
            new Tier("iridium", Materials.Iridium, 5, 16384),
            new Tier("iritanium", Materials.TitaniumIridium, 6, 65536),
            new Tier("trinitanium", Materials.Trinitanium, 7, 262144),
            new Tier("trinaquadalloy", Materials.Trinaquadalloy, 8, 1048576),
            new Tier("adamantium", Materials.Adamantium, 9, 4194304),
    };
    private static final long[] V = {8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152};

 public static Tier[] tiers(){return TIERS.clone();}
 public static GearboxSpec gearbox(Tier tier){return new GearboxSpec("gearbox_"+tier.suffix(),tier.material(),tier.maxSpeed(),tier.maxSpeed()/4);}
 public static EnergyNodeSpec transformer(Tier tier){int t=tier.voltageTier();long input=V[t],output=t==0?2:V[t-1];return new EnergyNodeSpec("rotation_transformer_"+tier.suffix(),tier.material(),EnergyNodeSpec.Kind.CONVERTER,"transformers/rotation_transformer",GregTechTags.Energy.RU,GregTechTags.Energy.RU,input,output,input*2,tier.material().getLocalName()+" Transformer Gearbox",0);}
}
