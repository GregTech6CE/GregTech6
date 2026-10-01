package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.registry.GTWires;

/** GT6 Loader_MultiTileEntities electric wire families; losses are distinct from amperage. */
public final class WireDefinitions {
    private WireDefinitions() {}
    private static final int[] CABLE_SIZES = {1, 2, 4, 8, 12};
    private record WireDef(String idSuffix, GTMaterial material, long voltage, long amperage,
                           long wireLoss, long cableLoss, boolean contactDamage, boolean hasCable) {}
    private static final WireDef[] WIRE_DEFS = {
            new WireDef("tin", Materials.Tin, 32L, 1, 2, 1, true, true), // MT.Sn
            new WireDef("lead", Materials.Lead, 64L, 1, 2, 1, true, true), // MT.Pb
            new WireDef("constantan", Materials.Constantan, 128L, 4, 4, 3, true, true), // MT.Constantan
            new WireDef("copper", Materials.Copper, 256L, 1, 2, 1, true, true), // MT.Cu
            new WireDef("annealed_copper", Materials.AnnealedCopper, 384L, 1, 2, 1, true, true), // MT.AnnealedCopper
            new WireDef("efrine", Materials.Efrine, 256L, 1, 2, 1, true, true), // MT.Efrine
            new WireDef("kanthal", Materials.Kanthal, 512L, 4, 4, 3, true, true), // MT.Kanthal
            new WireDef("silver", Materials.Silver, 1536L, 1, 2, 1, true, true), // MT.Ag
            new WireDef("gold", Materials.Gold, 512L, 3, 2, 1, true, true), // MT.Au
            new WireDef("electrum", Materials.Electrum, 1024L, 2, 2, 1, true, true), // MT.Electrum
            new WireDef("blue_alloy", Materials.BlueAlloy, 1536L, 2, 2, 1, true, true), // MT.BlueAlloy
            new WireDef("electrotine_alloy", Materials.ElectrotineAlloy, 1024L, 3, 2, 1, true, true), // MT.ElectrotineAlloy
            new WireDef("nichrome", Materials.Nichrome, 2048L, 4, 4, 3, true, true), // MT.Nichrome
            new WireDef("steel", Materials.Steel, 2048L, 2, 3, 2, true, true), // MT.Steel
            new WireDef("hsla", Materials.HSLASteel, 2048L, 3, 3, 2, true, true), // MT.HSLA
            new WireDef("aluminium", Materials.Aluminium, 2048L, 1, 2, 1, true, true), // MT.Al
            new WireDef("tungsten_steel", Materials.Tungstensteel, 4096L, 4, 3, 2, true, true), // MT.TungstenSteel
            new WireDef("tungsten", Materials.Tungsten, 6144L, 8, 3, 2, true, true), // MT.W
            new WireDef("netherite", Materials.Netherite, 2048L, 1, 2, 1, true, true), // MT.Netherite
            new WireDef("osmium", Materials.OsmiumElemental, 16384L, 4, 4, 3, true, true), // MT.Os
            new WireDef("platinum", Materials.Platinum, 24576L, 2, 2, 1, true, true), // MT.Pt
            new WireDef("osmiridium", Materials.Osmiridium, 24576L, 4, 2, 1, true, true), // MT.Osmiridium
            new WireDef("silicon_carbide", Materials.Carborundum, 8192L, 4, 4, 3, true, true), // MT.SiC
            new WireDef("iridium", Materials.Iridium, 24576L, 4, 4, 2, true, true), // MT.Ir
            new WireDef("naquadah", Materials.Naquadah, 98304L, 4, 2, 1, true, true), // MT.Nq
            new WireDef("niobium_titanium", Materials.NiobiumTitanium, 32768L, 4, 4, 3, true, true), // MT.NiobiumTitanium
            new WireDef("vanadium_gallium", Materials.VanadiumGallium, 65536L, 4, 4, 3, true, true), // MT.VanadiumGallium
            new WireDef("yttrium_barium_cuprate", Materials.YttriumBariumCuprate, 98304L, 4, 4, 3, true, true), // MT.YttriumBariumCuprate
            new WireDef("graphene", Materials.Graphene, 65536L, 1, 2, 2, false, false), // MT.Graphene
            new WireDef("superconductor", Materials.Superconductor, 8589934592L, 4, 1, 1, false, false), // MT.Superconductor
    };
    public static void register() {
        for (WireDef def : WIRE_DEFS) {
            for (int size = 1; size <= 16; size++)
                GTWires.register(def.idSuffix(), def.material(), size, def.voltage(), def.amperage()*size,
                        def.wireLoss(), false, def.contactDamage());
            if (def.hasCable()) for (int size : CABLE_SIZES)
                GTWires.register(def.idSuffix(), def.material(), size, def.voltage(), def.amperage()*size,
                        def.cableLoss(), true, false);
        }
    }
}
