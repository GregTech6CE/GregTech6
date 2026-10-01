package com.gregtech.gregtech.content.fluid;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.RegisteredFluids;

/** Static and material-derived fluid definitions, prepared before Forge registration. */
public final class FluidDefinitions {
    private FluidDefinitions() {}
    public static void prepare() {
        RegisteredFluids.bootstrap();
        // GT6 reactor coolant isotopologues have liquid phases; retain old gas IDs for saves.
        for(var material:java.util.List.of(
                com.gregtech.gregtech.content.material.generated.CompoundMaterials.SemiheavyWater,
                com.gregtech.gregtech.content.material.generated.CompoundMaterials.HeavyWater,
                com.gregtech.gregtech.content.material.generated.CompoundMaterials.TritiatedWater))
            material.put(com.gregtech.gregtech.api.material.MaterialProperty.LIQUID);


        // GT6 fusion explicitly uses these molten phases; the element import omitted their MOLTEN flag.
        for(var material:java.util.List.of(
                com.gregtech.gregtech.content.material.generated.ElementMaterials.Carbon,
                com.gregtech.gregtech.content.material.generated.ElementMaterials.Carbon13,
                com.gregtech.gregtech.content.material.generated.ElementMaterials.Adamantium))
            material.put(com.gregtech.gregtech.api.material.MaterialProperty.MOLTEN);

        // chemistry acids/salts the transpiled recipes use as fluids; the
        // generated material data lost their state flags, so restore them
        // before the molten/gas/liquid fluid generation below
        for (var m : new com.gregtech.gregtech.api.material.GTMaterial[]{
                com.gregtech.gregtech.content.material.Materials.TitaniumTetrachloride, com.gregtech.gregtech.content.material.Materials.NitricAcid,
                com.gregtech.gregtech.content.material.Materials.SulfuricAcid, com.gregtech.gregtech.content.material.Materials.DisulfuricAcid,
                com.gregtech.gregtech.content.material.Materials.HexafluorosilicicAcid, com.gregtech.gregtech.content.material.Materials.Cryolite,
                com.gregtech.gregtech.content.material.Materials.LithiumChloride, com.gregtech.gregtech.content.material.Materials.LithiumChlorate,
                com.gregtech.gregtech.content.material.Materials.AluminiumFluoride}) {
            if (m != null && m.resolve().isValid()) {
                m.resolve().put(com.gregtech.gregtech.api.material.MaterialProperty.LIQUID);
            }
        }

        // GT6's lqudacid* factories (MT.java: Aqua Regia 9827, Chloroauric/Chloroplatinic Acid,
        // Stannic Chloride, the whole vitriol family) give those materials a LIQUID state; the
        // import dropped it, so the acids the chemistry tables consume had no fluid at all.
        for (String name : new String[]{"AquaRegia", "ChloroauricAcid", "ChloroplatinicAcid",
                "StannicChloride", "BlueVitriol", "CyanVitriol", "GrayVitriol", "GreenVitriol",
                "PinkVitriol", "RedVitriol", "WhiteVitriol", "MartianVitriol", "VitriolOfClay"}) {
            var material = GTMaterialRegistry.get(name);
            if (material != null && material.resolve().isValid()) {
                material.resolve().put(com.gregtech.gregtech.api.material.MaterialProperty.LIQUID);
            }
        }
        // MT.sulfur() is created with MELTING + MOLTEN (MT.java:160), so molten sulfur is a real
        // GT6 fluid; the import kept the dust only.
        var sulfur = GTMaterialRegistry.get("Sulfur");
        if (sulfur != null && sulfur.resolve().isValid()) {
            sulfur.resolve().put(com.gregtech.gregtech.api.material.MaterialProperty.MOLTEN);
        }
        // GT6's *dcmp* compound factories (MT.java:1081 Al2O3, :1094 Fe2O3, :1104 CaCl2,
        // :1139 Na2CO3, :1180 UF4 and their siblings) declare MELTING/MOLTEN, so their molten phases are
        // real GT6 fluids that the chemistry tables consume (`m:Al2O3:2000` …); the import kept the dust
        // only, which left 11 chemistry recipes with an unresolvable fluid.
        for (String name : new String[]{"Alumina", "Hematite", "CalciumChloride", "Fluorite",
                "MagnesiumCarbonate", "MagnesiumChloride", "ManganeseChloride", "SodiumCarbonate",
                "UraniumTetrafluoride", "Uranium235Tetrafluoride", "Uranium238Tetrafluoride"}) {
            var material = GTMaterialRegistry.get(name);
            if (material != null && material.resolve().isValid()) {
                material.resolve().put(com.gregtech.gregtech.api.material.MaterialProperty.MOLTEN);
            }
        }
        if (com.gregtech.gregtech.content.material.Materials.HydrochloricAcid != null && com.gregtech.gregtech.content.material.Materials.HydrochloricAcid.resolve().isValid()) {
            com.gregtech.gregtech.content.material.Materials.HydrochloricAcid.resolve().put(com.gregtech.gregtech.api.material.MaterialProperty.GAS);
        }

        // ---- GT6 Loader_Fluids: generated molten fluids (auto-register molten.{material}) ----
        // (after the state flags above, so the restored MOLTEN/LIQUID materials get their fluid)
        RegisteredFluids.registerGeneratedMoltenFluids(GTMaterialRegistry.allMaterials());

        // ---- generated gas./liquid. fluids for gaseous/liquid materials (chemistry chains) ----
        RegisteredFluids.registerGeneratedGasLiquidFluids(GTMaterialRegistry.allMaterials());

        // ---- GT6 Loader_Fluids: FL.make() material-fluid binding ----
        bindMaterialFluids();

    }

    /**
     * GT6 {@code FL.make()} pattern — links fluid registry entries to their GTMaterial.
     * Equivalent to {@code MT.Iron.liquid(FL.make("iron.molten", 144))} in the original.
     */
    private static void bindMaterialFluids() {
        // Mod-compat molten metals (original GT6 Loader_Fluids lines 161-189)
        bindMolten("ardite.molten",       "Ardite");
        bindMolten("manyullyn.molten",    "Manyullyn");
        bindMolten("aluminumbrass.molten","AluminiumBrass");
        bindMolten("bronze.molten",       "Bronze");
        bindMolten("steel.molten",        "Steel");
        bindMolten("electrum.molten",     "Electrum");
        bindMolten("invar.molten",        "Invar");
        bindMolten("alumite.molten",      "Alumite");
        bindMolten("lumium.molten",       "Lumium");
        bindMolten("signalum.molten",     "Signalum");
        bindMolten("enderium.molten",     "Enderium");
        bindMolten("mithril.molten",      "Mithril");
        bindMolten("pigiron.molten",      "PigIron");
        bindMolten("molten.aluminum",     "Al");
        bindMolten("aluminium.molten",    "Al");
        bindMolten("aluminum.molten",     "Al");
        bindMolten("titanium.molten",     "Ti");
        bindMolten("magnesium.molten",    "Mg");
        bindMolten("zinc.molten",         "Zn");
        bindMolten("osmium.molten",       "Ge");  // GT6 used Ge for Osmium molten
        bindMolten("iron.molten",         "Fe");
        bindMolten("gold.molten",         "Au");
        bindMolten("silver.molten",       "Ag");
        bindMolten("lead.molten",         "Pb");
        bindMolten("copper.molten",       "Cu");
        bindMolten("tin.molten",          "Sn");
        bindMolten("nickel.molten",       "Ni");
        bindMolten("platinum.molten",     "Pt");
        bindMolten("cobalt.molten",       "Co");

        // Special material bindings (original lines 135-158)
        bindMolten("molten_tritanium",    "TritaniumAlloy");

        // Non-auto-generated molten materials (lines 191-199)
        bindMolten("plastic",             "Plastic");
        bindMolten("glass",               "Glass");
        bindMolten("molten.enderpearl",   "EnderPearl");
        bindMolten("molten.redstone",     "Redstone");
        bindMolten("blaze",               "Blaze");
        bindMolten("concrete",            "Concrete");
        bindMolten("molten.latex",        "Latex");
        bindMolten("latex",               "Latex");
        bindMolten("molten.hsla",         "HSLA");

        // Additional material bindings from static entries
        bindMolten("molten.brass",        "Brass");
        bindMolten("molten.zinc",         "Zinc");
        bindMolten("molten.calcite",      "Calcite");

        // createMolten materials (lines 201-213)
        bindMolten("molten.chocolate",    "Chocolate");
        bindMolten("molten.cheese",       "Cheese");
        bindMolten("molten.sugar",        "Sugar");
        bindMolten("molten.rubber",       "Rubber");
        bindMolten("molten.wax",          "Wax");
        bindMolten("molten.waxbee",       "WaxBee");
        bindMolten("molten.waxparaffin",  "WaxParaffin");
        bindMolten("molten.waxplant",     "WaxPlant");
        bindMolten("molten.waxrefractory","WaxRefractory");
        bindMolten("molten.waxmagic",     "WaxMagic");
        bindMolten("molten.waxamnesic",   "WaxAmnesic");
        bindMolten("molten.waxsoulful",   "WaxSoulful");
        bindMolten("molten.al2o3",        "Al2O3");
    }

    /** Links a molten fluid registry name to a GTMaterial by material name. */
    private static void bindMolten(String registryName, String materialName) {
        RegisteredFluids.FluidEntry entry = RegisteredFluids.get("GenMolten_" + materialName);
        if (entry == null) {
            // Try finding via registry name
            for (var e : RegisteredFluids.all().entrySet()) {
                if (e.getValue().registryName().equalsIgnoreCase(registryName)) {
                    entry = e.getValue();
                    break;
                }
            }
        }
        if (entry == null) {
            GregTech.LOGGER.debug("FL.make: no fluid entry for '{}' (material '{}') — skipping bind",
                    registryName, materialName);
            return;
        }
        GTMaterial material = GTMaterialRegistry.get(materialName).resolve();
        if (!material.isValid()) {
            GregTech.LOGGER.debug("FL.make: material '{}' not found — skipping bind for '{}'", materialName, registryName);
            return;
        }
        // GT6 material.liquid(tFluid) stores the material's liquid form.
        // In the port, this association is tracked implicitly via materialKey in FluidEntry.
        GregTech.LOGGER.debug("FL.make: bound '{}' -> material '{}'", registryName, materialName);
    }
}
