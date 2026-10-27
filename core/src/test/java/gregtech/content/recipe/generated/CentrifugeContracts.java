package gregtech.content.recipe.generated;

/** Dependency-free entry point for the focused core recipe regressions. */
public final class CentrifugeContracts {
    private CentrifugeContracts() {}

    public static void main(String[] args) {
        gregtech.api.mod.ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        OptionalCentrifugeContracts.run();
        U235CentrifugeContracts.run();
        gregtech.content.recipe.SourceAlloyContracts.run();
        gregtech.content.fluid.SourceMoltenFluidContracts.run();
        BlockDustCentrifugeContracts.run();
        gregtech.content.recipe.BuiltinEventCentrifugeContracts.run();
        MoonStoneCentrifugeContracts.run();
        gregtech.content.recipe.SourceOreWashingContracts.run();
        gregtech.content.recipe.SourceOreCryotheumContracts.run();
        OreWashingGeneratedContracts.run();
        gregtech.content.recipe.SourceOreMagneticContracts.run();
        OreMagneticGeneratedContracts.run();
        gregtech.content.recipe.SourceOreSiftingContracts.run();
        OreSiftingGeneratedContracts.run();
        gregtech.content.recipe.SourceRecyclingFactsContracts.run();
        gregtech.content.recipe.SourceThermalRecyclingContracts.run();
        gregtech.content.recipe.SourceThermalInputContracts.run();
        gregtech.content.recipe.SourceThermalPrefixContracts.run();
        ThermalIceSnowGeneratedContracts.run();
        gregtech.content.recipe.FurnaceSourcePolicyContracts.run();
        gregtech.content.recipe.SourceOreShreddingContracts.run();
        gregtech.content.recipe.SourceOreCrushingContracts.run();
        gregtech.content.recipe.SourceOilSandContracts.run();
        gregtech.content.recipe.SourcePurifiedAcidContracts.run();
        System.out.println("Centrifuge core contracts passed (no native runtime coverage).");
    }
}
