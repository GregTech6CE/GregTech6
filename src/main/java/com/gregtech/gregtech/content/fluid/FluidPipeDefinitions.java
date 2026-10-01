package com.gregtech.gregtech.content.fluid;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.registry.*;

/** Built-in GT6 definitions; registration IDs and ordering are preserved. */
public final class FluidPipeDefinitions {
    private FluidPipeDefinitions() {}

    // ==================== Fluid Pipes ====================

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

    public static void register() {
        for (FluidPipeMat mat : FLUID_PIPE_MATS) {
            for (PipeSpec.PipeSize size : PipeSpec.PipeSize.values()) {
                String id = "pipe_" + size.name().toLowerCase() + "_" + mat.idSuffix;
                var block = GTFluidPipes.register(id, mat.material, size,
                        mat.baseCapacity, mat.gasProof, mat.acidProof, mat.plasmaProof);
                if (mat.idSuffix.equals("steel") && size == PipeSpec.PipeSize.MEDIUM) {
                    GTFluidPipes.PIPE_MEDIUM_STEEL = block;
                }
            }
        }
    }

}
