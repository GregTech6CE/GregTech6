package gregtech.content.transport.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.api.machine.PipeSpec;
import gregtech.api.machine.TankSpec;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
/**
 * Shared fluid pipe ratings and ordinary tank definitions, preserving registration order.
 * Pipe ratings originate in GT6; tanks retain the saltnya catalog. Native hosts own
 * inventory, distribution and hazards. A matching rating does not imply matching behavior.
 */
public final class FluidTransportDefinitions {
    private FluidTransportDefinitions() {}
    private record FluidPipeMat(String idSuffix, GTMaterial material, long baseCapacity,
                                boolean gasProof, boolean acidProof, boolean plasmaProof,
                                boolean magicProof, long maxTemperature, int flammability) {}

    private static final FluidPipeMat[] FLUID_PIPE_MATS = {
            new FluidPipeMat("wood",                     gregtech.content.material.generated.WoodMaterials.Wood,                       50,         false, false, false, false, 340, 150),
            new FluidPipeMat("treated_wood",             gregtech.content.material.generated.WoodMaterials.WoodTreated,                75,         false, false, false, false, 340, 150),
            new FluidPipeMat("plastic",                  gregtech.content.material.generated.CompoundMaterials.Plastic,                100,        true,  false, false, false, 370, 0),
            new FluidPipeMat("rubber",                   gregtech.content.material.generated.CompoundMaterials.Rubber,                 100,        true,  false, false, false, 350, 0),
            new FluidPipeMat("copper",                   gregtech.content.material.generated.ElementMaterials.Copper,                  100,        true,  false, false, false, 0,   0),
            new FluidPipeMat("aluminium",                gregtech.content.material.generated.ElementMaterials.Aluminium,               100,        true,  false, false, false, 0,   0),
            new FluidPipeMat("tin_alloy",                gregtech.content.material.generated.CompoundMaterials.TinAlloy,               125,        true,  false, false, false, 0,   0),
            new FluidPipeMat("bronze",                   gregtech.content.material.generated.CompoundMaterials.Bronze,                 150,        true,  false, false, false, 0,   0),
            new FluidPipeMat("invar",                    gregtech.content.material.generated.CompoundMaterials.Invar,                  200,        true,  false, false, false, 0,   0),
            new FluidPipeMat("steel",                    gregtech.content.material.generated.CompoundMaterials.Steel,                  200,        true,  false, false, false, 0,   0),
            new FluidPipeMat("galvanized_steel",         gregtech.content.material.generated.CompoundMaterials.SteelGalvanized,        250,        true,  false, false, false, 0,   0),
            new FluidPipeMat("hsla",                     gregtech.content.material.generated.CompoundMaterials.HSLASteel,              250,        true,  false, false, false, 0,   0),
            new FluidPipeMat("gold",                     gregtech.content.material.generated.ElementMaterials.Gold,                    100,        true,  true,  false, false, 0,   0),
            new FluidPipeMat("chrome",                   gregtech.content.material.generated.ElementMaterials.Chromium,                200,        true,  true,  false, false, 0,   0),
            new FluidPipeMat("stainless_steel",          gregtech.content.material.generated.CompoundMaterials.StainlessSteel,         250,        true,  true,  false, false, 0,   0),
            new FluidPipeMat("vanadium_steel",           gregtech.content.material.generated.CompoundMaterials.VanadiumSteel,          400,        true,  true,  false, false, 0,   0),
            new FluidPipeMat("desh",                     gregtech.content.material.generated.CompoundMaterials.Desh,                   200,        true,  false, false, true,  0,   0),
            new FluidPipeMat("tungsten_alloy",           gregtech.content.material.generated.CompoundMaterials.HSLATungstenAlloy,      300,        true,  false, false, true,  0,   0),
            new FluidPipeMat("tungsten_steel",           gregtech.content.material.generated.CompoundMaterials.Tungstensteel,          400,        true,  false, false, true,  0,   0),
            new FluidPipeMat("tungsten_carbide",         gregtech.content.material.generated.CompoundMaterials.TungstenCarbide,        450,        true,  false, false, true,  0,   0),
            new FluidPipeMat("desh_alloy",               gregtech.content.material.generated.CompoundMaterials.WorkersAlloy,           350,        true,  false, false, true,  0,   0),
            new FluidPipeMat("palladium",                gregtech.content.material.generated.ElementMaterials.Palladium,               400,        true,  false, false, true,  0,   0),
            new FluidPipeMat("carbon",                   gregtech.content.material.generated.ElementMaterials.Carbon,                  1000,       true,  false, false, false, 0,   0),
            new FluidPipeMat("tantalum_hafnium_carbide", gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, 300,        true,  false, false, true,  0,   0),
            new FluidPipeMat("titanium",                 gregtech.content.material.generated.ElementMaterials.Titanium,                300,        true,  false, false, false, 0,   0),
            new FluidPipeMat("tungsten",                 gregtech.content.material.generated.ElementMaterials.Tungsten,                350,        true,  true,  false, true,  0,   0),
            new FluidPipeMat("efrine",                   gregtech.content.material.generated.CompoundMaterials.Efrine,                 250,        true,  false, true,  true,  0,   0),
            new FluidPipeMat("netherite",                gregtech.content.material.generated.CompoundMaterials.Netherite,              300,        true,  true,  true,  true,  0,   0),
            new FluidPipeMat("iridium",                  gregtech.content.material.generated.ElementMaterials.Iridium,                 500,        true,  true,  false, true,  0,   0),
            new FluidPipeMat("ironwood",                 gregtech.content.material.generated.CompoundMaterials.Ironwood,               200,        true,  false, false, true,  0,   0),
            new FluidPipeMat("thaumium",                 gregtech.content.material.generated.CompoundMaterials.Thaumium,               250,        true,  true,  false, true,  0,   0),
            new FluidPipeMat("manasteel",                gregtech.content.material.generated.CompoundMaterials.Manasteel,              250,        true,  true,  false, true,  0,   0),
            new FluidPipeMat("void_metal",               gregtech.content.material.generated.CompoundMaterials.VoidMetal,              500,        true,  true,  false, true,  0,   0),
            new FluidPipeMat("terrasteel",               gregtech.content.material.generated.CompoundMaterials.Terrasteel,             500,        true,  true,  false, true,  0,   0),
            new FluidPipeMat("gaia_spirit",              gregtech.content.material.generated.CompoundMaterials.GaiaSpirit,             1000,       true,  true,  true,  true,  0,   0),
            new FluidPipeMat("bedrock_hsla",             gregtech.content.material.generated.CompoundMaterials.BedrockHSLAAlloy,       1000,       true,  false, false, true,  0,   0),
            new FluidPipeMat("adamantium",               gregtech.content.material.generated.ElementMaterials.Adamantium,              10000,      true,  true,  true,  true,  0,   0),
            new FluidPipeMat("draconium",                gregtech.content.material.generated.CompoundMaterials.Draconium,              2500,       true,  true,  true,  true,  0,   0),
            new FluidPipeMat("awakened_draconium",       gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened,      10000,      true,  true,  true,  true,  0,   0),
            new FluidPipeMat("infinity",                 gregtech.content.material.generated.CompoundMaterials.Infinity,               1000000000, true,  true,  true,  true,  0,   0),
    };

    private record WoodDef(String id, GTMaterial material, long capacity) {}
    private static final WoodDef[] WOODS = {
            new WoodDef("wood_barrel_treated",     gregtech.content.material.generated.WoodMaterials.WoodTreated,  16000),
            new WoodDef("wood_barrel_skyroot",     gregtech.content.material.generated.WoodMaterials.Skyroot,      16000),
            new WoodDef("wood_barrel_livingwood",  gregtech.content.material.generated.WoodMaterials.Livingwood,   16000),
            new WoodDef("wood_barrel_dreamwood",   gregtech.content.material.generated.WoodMaterials.Dreamwood,    64000),
            new WoodDef("wood_barrel_shimmerwood", gregtech.content.material.generated.WoodMaterials.Shimmerwood,  64000),
            new WoodDef("wood_barrel_ironwood",    gregtech.content.material.generated.CompoundMaterials.Ironwood, 32000),
            new WoodDef("wood_barrel_greatwood",   gregtech.content.material.generated.WoodMaterials.Greatwood,    16000),
            new WoodDef("wood_barrel_silverwood",  gregtech.content.material.generated.WoodMaterials.Silverwood,   64000),
    };

    private record DrumDef(String id, GTMaterial material, long capacity,
                           boolean gasProof, boolean acidProof, boolean plasmaProof) {}
    private static final DrumDef[] DRUMS = {
            new DrumDef("drum_bronze",                   gregtech.content.material.generated.CompoundMaterials.Bronze,                 64000,        true, false, false),
            new DrumDef("drum_invar",                    gregtech.content.material.generated.CompoundMaterials.Invar,                  64000,        true, false, false),
            new DrumDef("drum_steel",                    gregtech.content.material.generated.CompoundMaterials.Steel,                  64000,        true, false, false),
            new DrumDef("drum_stainless_steel",          gregtech.content.material.generated.CompoundMaterials.StainlessSteel,         64000,        true, true,  false),
            new DrumDef("drum_desh",                     gregtech.content.material.generated.CompoundMaterials.Desh,                   64000,        true, true,  false),
            new DrumDef("drum_syrmorite",                gregtech.content.material.generated.CompoundMaterials.Syrmorite,              64000,        true, true,  false),
            new DrumDef("drum_efrine",                   gregtech.content.material.generated.CompoundMaterials.Efrine,                 64000,        true, true,  true),
            new DrumDef("drum_thaumium",                 gregtech.content.material.generated.CompoundMaterials.Thaumium,               64000,        true, true,  false),
            new DrumDef("drum_manasteel",                gregtech.content.material.generated.CompoundMaterials.Manasteel,              64000,        true, true,  false),
            new DrumDef("drum_tungsten_alloy",           gregtech.content.material.generated.CompoundMaterials.HSLATungstenAlloy,      128000,       true, false, true),
            new DrumDef("drum_titanium",                 gregtech.content.material.generated.ElementMaterials.Titanium,                128000,       true, true,  true),
            new DrumDef("drum_netherite",                gregtech.content.material.generated.CompoundMaterials.Netherite,              128000,       true, true,  true),
            new DrumDef("drum_tungsten_steel",           gregtech.content.material.generated.CompoundMaterials.Tungstensteel,          256000,       true, true,  true),
            new DrumDef("drum_tungsten",                 gregtech.content.material.generated.ElementMaterials.Tungsten,                256000,       true, true,  true),
            new DrumDef("drum_void_metal",               gregtech.content.material.generated.CompoundMaterials.VoidMetal,              256000,       true, true,  true),
            new DrumDef("drum_tantalum_hafnium_carbide", gregtech.content.material.generated.CompoundMaterials.TantalumHafniumCarbide, 512000,       true, true,  true),
            new DrumDef("drum_gaia_spirit",              gregtech.content.material.generated.CompoundMaterials.GaiaSpirit,             1024000,      true, true,  true),
            new DrumDef("drum_adamantium",               gregtech.content.material.generated.ElementMaterials.Adamantium,              4096000,      true, true,  true),
            new DrumDef("drum_draconium",                gregtech.content.material.generated.CompoundMaterials.Draconium,              4096000,      true, true,  true),
            new DrumDef("drum_awakened_draconium",       gregtech.content.material.generated.CompoundMaterials.DraconiumAwakened,      8192000,      true, true,  true),
            new DrumDef("drum_infinity",                 gregtech.content.material.generated.CompoundMaterials.Infinity,               10000000000L, true, true,  true),
    };


    /** Built-in suffixes, in registration order. Addon rows are not included. */
    public static List<String> builtinFluidSuffixes() {
        var suffixes = new ArrayList<String>();
        for (var family : FLUID_PIPE_MATS) suffixes.add(family.idSuffix());
        return List.copyOf(suffixes);
    }

    public static List<PipeSpec> pipes() {
        var result = new ArrayList<PipeSpec>();
        for (var family : FLUID_PIPE_MATS)
            for (var size : PipeSpec.PipeSize.values())
                result.add(PipeSpec.of("pipe_" + size.name().toLowerCase(Locale.ROOT) + "_" + family.idSuffix,
                        family.material, size, family.baseCapacity, family.gasProof, family.acidProof,
                        family.plasmaProof, family.magicProof,
                        family.maxTemperature > 0 ? family.maxTemperature
                                : (long) (family.material.getMeltingPoint() * 1.25D),
                        family.flammability));
        result.addAll(gregtech.api.addon.AddonPipes.addonFluidSpecs());
        return List.copyOf(result);
    }
    public static List<TankSpec> tanks() {
        var result = new ArrayList<TankSpec>();
        for (var cheap : CheapWoodBarrelCatalog.ENTRIES) result.add(cheap.spec());
        for (var wood : WOODS)
            result.add(TankSpec.of(wood.id, wood.material, TankSpec.TankType.WOOD_BARREL,
                    wood.capacity, false, false, false, false, true, 2F, 3F));
        result.add(TankSpec.of("plastic_canister",
                gregtech.content.material.generated.CompoundMaterials.Plastic,
                TankSpec.TankType.PLASTIC_CANISTER, 32000, true, false, false, false, false, 3F, 5F));
        for (var drum : DRUMS) {
            float hardness = drum.material.getToolDurability() > 0
                    ? 5F + drum.material.getToolQuality() * 1.5F : 5F;
            float resistance = drum.material.getToolDurability() > 0
                    ? 7F + drum.material.getToolQuality() * 1.5F : 6F;
            result.add(TankSpec.of(drum.id, drum.material, TankSpec.TankType.METAL_DRUM, drum.capacity,
                    drum.gasProof, drum.acidProof, drum.plasmaProof, false, false, hardness, resistance));
        }
        return List.copyOf(result);
    }
}
