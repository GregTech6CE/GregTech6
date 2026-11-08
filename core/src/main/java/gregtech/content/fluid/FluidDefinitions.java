package gregtech.content.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.FluidCatalog;
import gregtech.engine.material.GTMaterialRegistry;

/** Static and material-derived fluid definitions, prepared before Forge registration. */
public final class FluidDefinitions {
    private FluidDefinitions() {}

    public static void prepare() {
        FluidCatalog.bootstrap();
        registerGoldenBottleFluids();
        // Phase corrections live in the canonical material definitions, before READY freezes them.
        FluidCatalog.registerGeneratedMoltenFluids(GTMaterialRegistry.allMaterials());

        // ---- generated gas./liquid. fluids for gaseous/liquid materials (chemistry chains) ----
        FluidCatalog.registerGeneratedGasLiquidFluids(GTMaterialRegistry.allMaterials());
        // Material factories depend on initialized stats, and must finalize before native holders.
        FluidCatalog.finalizeMaterialFluidProperties();

        // ---- GT6 Loader_Fluids: FL.make() material-fluid binding ----
        bindMaterialFluids();
    }

    /** GT6 {@code Loader_Fluids}: unconditional FL.create declarations. */
    private static void registerGoldenBottleFluids() {
        long base = FluidCatalog.FluidFlags.LIQUID
                | FluidCatalog.FluidFlags.SIMPLE
                | FluidCatalog.FluidFlags.FOOD
                | FluidCatalog.FluidFlags.ENCHANTED_EFFECT;
        for (String name : new String[] {
            "goldencarrotjuice",
            "potion.goldenapplejuice",
            "potion.goldencider",
            "potion.idunsapplejuice",
            "potion.notchesbrew"
        }) {
            long flags = base
                    | (name.endsWith("cider") || name.endsWith("brew")
                            ? FluidCatalog.FluidFlags.ALCOHOLIC | FluidCatalog.FluidFlags.CIDER
                            : FluidCatalog.FluidFlags.JUICE
                                    | (name.equals("goldencarrotjuice") ? 0 : FluidCatalog.FluidFlags.FRUIT_JUICE));
            if (FluidCatalog.get(name) == null)
                FluidCatalog.registerDefinition(
                        name,
                        new FluidCatalog.FluidEntry(
                                name,
                                FluidCatalog.FluidTextureMode.DEDICATED,
                                null,
                                0,
                                false,
                                true,
                                false,
                                1000,
                                1000,
                                15,
                                300,
                                flags));
        }
    }

    /**
     * GT6 {@code FL.make()} pattern — links fluid registry entries to their GTMaterial.
     * Equivalent to {@code MT.Iron.liquid(FL.make("iron.molten", 144))} in the original.
     */
    private static void bindMaterialFluids() {
        // Mod-compat molten metals from GT6's Loader_Fluids

        bindMolten("aluminumbrass.molten", "AluminiumBrass");
        bindMolten("bronze.molten", "Bronze");
        bindMolten("steel.molten", "Steel");
        bindMolten("electrum.molten", "Electrum");
        bindMolten("invar.molten", "Invar");
        bindMolten("alumite.molten", "Alumite");
        bindMolten("lumium.molten", "Lumium");
        bindMolten("signalum.molten", "Signalum");
        bindMolten("enderium.molten", "Enderium");

        bindMolten("molten.aluminum", "Al");
        bindMolten("aluminium.molten", "Al");
        bindMolten("aluminum.molten", "Al");
        bindMolten("titanium.molten", "Ti");
        bindMolten("magnesium.molten", "Mg");
        bindMolten("zinc.molten", "Zn");
        bindMolten("osmium.molten", "Ge"); // GT6 used Ge for Osmium molten
        bindMolten("iron.molten", "Fe");
        bindMolten("gold.molten", "Au");
        bindMolten("silver.molten", "Ag");
        bindMolten("lead.molten", "Pb");
        bindMolten("copper.molten", "Cu");
        bindMolten("tin.molten", "Sn");
        bindMolten("nickel.molten", "Ni");
        bindMolten("platinum.molten", "Pt");
        bindMolten("cobalt.molten", "Co");

        // Special material bindings
        bindMolten("molten_tritanium", "TritaniumAlloy");

        // Non-auto-generated molten materials
        bindMolten("plastic", "Plastic");
        bindMolten("glass", "Glass");
        bindMolten("molten.enderpearl", "EnderPearl");
        bindMolten("molten.redstone", "Redstone");
        bindMolten("blaze", "Blaze");
        bindMolten("concrete", "Concrete");
        bindMolten("molten.latex", "Latex");
        bindMolten("latex", "Latex");

        // Additional material bindings from static entries
        bindMolten("molten.brass", "Brass");
        bindMolten("molten.zinc", "Zinc");
        bindMolten("molten.calcite", "Calcite");

        // createMolten materials
        bindMolten("molten.chocolate", "Chocolate");
        bindMolten("molten.cheese", "Cheese");
        bindMolten("molten.sugar", "Sugar");
        bindMolten("molten.rubber", "Rubber");
        bindMolten("molten.wax", "Wax");
        bindMolten("molten.waxbee", "WaxBee");
        bindMolten("molten.waxparaffin", "WaxParaffin");
        bindMolten("molten.waxplant", "WaxPlant");
    }

    /** Links a molten fluid registry name to a GTMaterial by material name. */
    private static void bindMolten(String registryName, String materialName) {
        FluidCatalog.FluidEntry entry = FluidCatalog.get("GenMolten_" + materialName);
        if (entry == null) {
            // Try finding via registry name
            for (var e : FluidCatalog.all().entrySet()) {
                if (e.getValue().registryName().equalsIgnoreCase(registryName)) {
                    entry = e.getValue();
                    break;
                }
            }
        }
        if (entry == null) {

            return;
        }
        GTMaterial material = GTMaterialRegistry.get(materialName).resolve();
        if (!material.isValid()) {

            return;
        }
        // GT6 material.liquid(tFluid) stores the material's liquid form.
        // In the port, this association is tracked implicitly via materialKey in FluidEntry.

    }
}
