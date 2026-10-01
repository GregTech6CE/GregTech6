package com.gregtech.gregtech.content.transport.fluid;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.machine.TankSpec;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
/** Original saltnya tank and pipe catalog, in original registration order. Logistics tank is a separate host. */
public final class FluidTransportDefinitions {
    private FluidTransportDefinitions() {}
    private record FluidPipeMat(String idSuffix, GTMaterial material, long baseCapacity,
                                boolean gasProof, boolean acidProof, boolean plasmaProof) {}

    private static final FluidPipeMat[] FLUID_PIPE_MATS = {
            new FluidPipeMat("wood",                  com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood,                50,   false, false, false),
            new FluidPipeMat("treated_wood",          com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated,         75,   false, false, false),
            new FluidPipeMat("plastic",               com.gregtech.gregtech.content.material.generated.CompoundMaterials.Plastic,        100,   true,  false, false),
            new FluidPipeMat("rubber",                com.gregtech.gregtech.content.material.generated.CompoundMaterials.Rubber,         100,   true,  false, false),
            new FluidPipeMat("copper",                com.gregtech.gregtech.content.material.generated.ElementMaterials.Copper,              100,   true,  false, false),
            new FluidPipeMat("aluminium",             com.gregtech.gregtech.content.material.generated.ElementMaterials.Aluminium,              100,   true,  false, false),
            new FluidPipeMat("tin_alloy",             com.gregtech.gregtech.content.material.generated.CompoundMaterials.TinAlloy,       125,   true,  false, false),
            new FluidPipeMat("bronze",                com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze,         120,   true,  false, false),
            new FluidPipeMat("invar",                 com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar,          200,   true,  false, false),
            new FluidPipeMat("steel",                 com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel,          200,   true,  false, false),
            new FluidPipeMat("galvanized_steel",      com.gregtech.gregtech.content.material.generated.CompoundMaterials.SteelGalvanized, 250,  true,  false, false),
            new FluidPipeMat("hsla",                  com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLASteel,           250,   true,  false, false),
            new FluidPipeMat("gold",                  com.gregtech.gregtech.content.material.generated.ElementMaterials.Gold,              100,   true,  true,  false),
            new FluidPipeMat("chrome",                com.gregtech.gregtech.content.material.generated.ElementMaterials.Chromium,              200,   true,  true,  false),
            new FluidPipeMat("stainless_steel",       com.gregtech.gregtech.content.material.generated.CompoundMaterials.StainlessSteel, 250,   true,  true,  false),
            new FluidPipeMat("vanadium_steel",        com.gregtech.gregtech.content.material.generated.CompoundMaterials.VanadiumSteel,  400,   true,  true,  false),
            new FluidPipeMat("desh",                  com.gregtech.gregtech.content.material.generated.CompoundMaterials.Desh,           200,   true,  false, true),
            new FluidPipeMat("tungsten_alloy",        com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLATungstenAlloy,  300,   true,  false, true),
            new FluidPipeMat("tungsten_steel",        com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel,  400,   true,  false, true),
            new FluidPipeMat("tungsten_carbide",      com.gregtech.gregtech.content.material.generated.CompoundMaterials.TungstenCarbide, 450,  true,  false, true),
            new FluidPipeMat("desh_alloy",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.WorkersAlloy,      350,   true,  false, true),
            new FluidPipeMat("palladium",             com.gregtech.gregtech.content.material.generated.ElementMaterials.Palladium,              400,   true,  false, true),
            new FluidPipeMat("carbon",                com.gregtech.gregtech.content.material.generated.ElementMaterials.Carbon,              1000,   true,  false, false),
            new FluidPipeMat("tantalum_hafnium_carbide", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide,    300,   true,  false, true),
            new FluidPipeMat("titanium",              com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium,              400,   true,  true,  true),
            new FluidPipeMat("tungsten",              com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten,               600,   true,  true,  true),
            new FluidPipeMat("efrine",                com.gregtech.gregtech.content.material.generated.CompoundMaterials.Efrine,         250,   true,  true,  true),
            new FluidPipeMat("netherite",             com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite,      300,   true,  true,  true),
            new FluidPipeMat("iridium",               com.gregtech.gregtech.content.material.generated.ElementMaterials.Iridium,              500,   true,  true,  true),
            new FluidPipeMat("ironwood",              com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ironwood,       200,   true,  false, true),
            new FluidPipeMat("thaumium",              com.gregtech.gregtech.content.material.generated.CompoundMaterials.Thaumium,       250,   true,  true,  true),
            new FluidPipeMat("manasteel",             com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manasteel,      250,   true,  true,  true),
            new FluidPipeMat("void_metal",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.VoidMetal,      500,   true,  true,  true),
            new FluidPipeMat("terrasteel",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.Terrasteel,     500,   true,  true,  true),
            new FluidPipeMat("gaia_spirit",           com.gregtech.gregtech.content.material.generated.CompoundMaterials.GaiaSpirit,    1000,   true,  true,  true),
            new FluidPipeMat("bedrock_hsla",          com.gregtech.gregtech.content.material.generated.CompoundMaterials.BedrockHSLAAlloy, 1000, true, false, true),
            new FluidPipeMat("adamantium",            com.gregtech.gregtech.content.material.generated.ElementMaterials.Adamantium,             10000,   true,  true,  true),
            new FluidPipeMat("draconium",             com.gregtech.gregtech.content.material.generated.CompoundMaterials.Draconium,      2500,  true,  true,  true),
            new FluidPipeMat("awakened_draconium",    com.gregtech.gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened, 10000, true, true, true),
            new FluidPipeMat("infinity",              com.gregtech.gregtech.content.material.generated.CompoundMaterials.Infinity,       1_000_000_000, true, true, true),
    };

    private record WoodDef(String id, GTMaterial material, long capacity) {}
    private static final WoodDef[] WOODS = {
            new WoodDef("wood_barrel",                  com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood,          16000),
            new WoodDef("wood_barrel_treated",          com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated,   16000),
            new WoodDef("wood_barrel_skyroot",          com.gregtech.gregtech.content.material.generated.WoodMaterials.Skyroot,       16000),
            new WoodDef("wood_barrel_livingwood",       com.gregtech.gregtech.content.material.generated.WoodMaterials.Livingwood,    16000),
            new WoodDef("wood_barrel_dreamwood",        com.gregtech.gregtech.content.material.generated.WoodMaterials.Dreamwood,     64000),
            new WoodDef("wood_barrel_shimmerwood",      com.gregtech.gregtech.content.material.generated.WoodMaterials.Shimmerwood,   64000),
            new WoodDef("wood_barrel_ironwood",         com.gregtech.gregtech.content.material.generated.CompoundMaterials.Ironwood,  32000),
            new WoodDef("wood_barrel_greatwood",        com.gregtech.gregtech.content.material.generated.WoodMaterials.Greatwood,     16000),
            new WoodDef("wood_barrel_silverwood",       com.gregtech.gregtech.content.material.generated.WoodMaterials.Silverwood,    64000),
    };

    private record DrumDef(String id, GTMaterial material, long capacity,
                           boolean gasProof, boolean acidProof, boolean plasmaProof) {}
    private static final DrumDef[] DRUMS = {
            new DrumDef("drum_bronze",           com.gregtech.gregtech.content.material.generated.CompoundMaterials.Bronze,           64000,  true,  false, false),
            new DrumDef("drum_invar",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.Invar,            64000,  true,  false, false),
            new DrumDef("drum_steel",            com.gregtech.gregtech.content.material.generated.CompoundMaterials.Steel,            64000,  true,  false, false),
            new DrumDef("drum_stainless_steel",  com.gregtech.gregtech.content.material.generated.CompoundMaterials.StainlessSteel,   64000,  true,  true,  false),
            new DrumDef("drum_desh",             com.gregtech.gregtech.content.material.generated.CompoundMaterials.Desh,             64000,  true,  true,  false),
            new DrumDef("drum_syrmorite",        com.gregtech.gregtech.content.material.generated.CompoundMaterials.Syrmorite,        64000,  true,  true,  false),
            new DrumDef("drum_efrine",           com.gregtech.gregtech.content.material.generated.CompoundMaterials.Efrine,           64000,  true,  true,  true),
            new DrumDef("drum_thaumium",         com.gregtech.gregtech.content.material.generated.CompoundMaterials.Thaumium,         64000,  true,  true,  false),
            new DrumDef("drum_manasteel",        com.gregtech.gregtech.content.material.generated.CompoundMaterials.Manasteel,        64000,  true,  true,  false),
            new DrumDef("drum_tungsten_alloy",   com.gregtech.gregtech.content.material.generated.CompoundMaterials.HSLATungstenAlloy,   128000, true,  false, true),
            new DrumDef("drum_titanium",         com.gregtech.gregtech.content.material.generated.ElementMaterials.Titanium,               128000, true,  true,  true),
            new DrumDef("drum_netherite",        com.gregtech.gregtech.content.material.generated.CompoundMaterials.Netherite,       128000, true,  true,  true),
            new DrumDef("drum_tungsten_steel",   com.gregtech.gregtech.content.material.generated.CompoundMaterials.Tungstensteel,   256000, true,  true,  true),
            new DrumDef("drum_tungsten",         com.gregtech.gregtech.content.material.generated.ElementMaterials.Tungsten,                256000, true,  true,  true),
            new DrumDef("drum_void_metal",       com.gregtech.gregtech.content.material.generated.CompoundMaterials.VoidMetal,       256000, true,  true,  true),
            new DrumDef("drum_tantalum_hafnium_carbide", com.gregtech.gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, 512000, true, true, true),
            new DrumDef("drum_gaia_spirit",      com.gregtech.gregtech.content.material.generated.CompoundMaterials.GaiaSpirit,      1024000, true, true, true),
            new DrumDef("drum_adamantium",       com.gregtech.gregtech.content.material.generated.ElementMaterials.Adamantium,               4096000, true, true, true),
            new DrumDef("drum_draconium",        com.gregtech.gregtech.content.material.generated.CompoundMaterials.Draconium,       4096000, true, true, true),
            new DrumDef("drum_awakened_draconium", com.gregtech.gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened, 8192000, true, true, true),
            new DrumDef("drum_infinity",         com.gregtech.gregtech.content.material.generated.CompoundMaterials.Infinity,        10000000000L, true, true, true),
    };


    public static List<PipeSpec> pipes() {
        var result = new ArrayList<PipeSpec>();
        for (var mat : FLUID_PIPE_MATS) for (var size : PipeSpec.PipeSize.values())
            result.add(PipeSpec.of("pipe_" + size.name().toLowerCase(Locale.ROOT) + "_" + mat.idSuffix,
                    mat.material, size, mat.baseCapacity, mat.gasProof, mat.acidProof, mat.plasmaProof, false));
        return List.copyOf(result);
    }
    public static List<TankSpec> tanks() {
        var result = new ArrayList<TankSpec>();
        for (var w : WOODS) result.add(TankSpec.of(w.id,w.material,TankSpec.TankType.WOOD_BARREL,
                w.capacity,false,false,false,false,true,2F,3F));
        result.add(TankSpec.of("plastic_canister",com.gregtech.gregtech.content.material.generated.CompoundMaterials.Plastic,
                TankSpec.TankType.PLASTIC_CANISTER,32000,true,false,false,false,false,3F,5F));
        for (var d : DRUMS) {
            float hardness=d.material.getToolDurability()>0?5F+d.material.getToolQuality()*1.5F:5F;
            float resistance=d.material.getToolDurability()>0?7F+d.material.getToolQuality()*1.5F:6F;
            result.add(TankSpec.of(d.id,d.material,TankSpec.TankType.METAL_DRUM,d.capacity,
                    d.gasProof,d.acidProof,d.plasmaProof,false,false,hardness,resistance));
        }
        return List.copyOf(result);
    }
}
