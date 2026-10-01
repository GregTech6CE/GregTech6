package com.gregtech.gregtech.content.fluid;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.TankSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.registry.*;
import net.minecraftforge.registries.RegistryObject;

/** Built-in GT6 definitions; registration IDs and ordering are preserved. */
public final class TankDefinitions {
    private TankDefinitions() {}

    // ==================== Tanks ====================

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

    public static void register() {
        RegistryObject<TankBlock> firstWood = null;
        for (WoodDef w : WOODS) {
            var block = GTTanks.register(w.id, w.material, TankSpec.TankType.WOOD_BARREL,
                    w.capacity, false, false, false, false, true, 2.0F, 3.0F);
            if (firstWood == null) firstWood = block;
        }
        GTTanks.WOOD_BARREL = firstWood;

        GTTanks.register("plastic_canister", com.gregtech.gregtech.content.material.generated.CompoundMaterials.Plastic, TankSpec.TankType.PLASTIC_CANISTER,
                32000, true, false, false, false, false, 3.0F, 5.0F);

        RegistryObject<TankBlock> firstBronze = null;
        RegistryObject<TankBlock> firstSteel = null;
        for (DrumDef d : DRUMS) {
            float hardness = d.material.getToolDurability() > 0 ? 5.0F + d.material.getToolQuality() * 1.5F : 5.0F;
            float blastRes = d.material.getToolDurability() > 0 ? 7.0F + d.material.getToolQuality() * 1.5F : 6.0F;
            var block = GTTanks.register(d.id, d.material, TankSpec.TankType.METAL_DRUM,
                    d.capacity, d.gasProof, d.acidProof, d.plasmaProof, false, false, hardness, blastRes);
            if (d.id.equals("drum_bronze")) firstBronze = block;
            if (d.id.equals("drum_steel")) firstSteel = block;
        }
        GTTanks.DRUM_BRONZE = firstBronze;
        GTTanks.DRUM_STEEL = firstSteel;
        GTTanks.LOGISTICS_TANK = GTTanks.registerLogisticsTank();
    }

}
