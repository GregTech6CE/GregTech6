package gregtech.content.recipe.generated;

/** Dependency-free entry point for the focused core recipe regressions. */
public final class CentrifugeContracts {
    private CentrifugeContracts() {}

    public static void main(String[] args) {
        gregtech.api.mod.ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        OptionalCentrifugeContracts.run();
        U235CentrifugeContracts.run();
        gregtech.content.recipe.SourceAlloyContracts.run();
        BlockDustCentrifugeContracts.run();
        gregtech.content.recipe.BuiltinEventCentrifugeContracts.run();
        MoonStoneCentrifugeContracts.run();
        System.out.println("Centrifuge core contracts passed (no native runtime coverage).");
    }
}
