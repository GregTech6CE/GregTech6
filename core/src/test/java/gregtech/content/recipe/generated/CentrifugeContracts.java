package gregtech.content.recipe.generated;

/** Dependency-free entry point for the focused core recipe regressions. */
public final class CentrifugeContracts {
    private CentrifugeContracts() {}

    public static void main(String[] args) {
        OptionalCentrifugeContracts.run();
        U235CentrifugeContracts.run();
        BlockDustCentrifugeContracts.run();
        gregtech.content.recipe.BuiltinEventCentrifugeContracts.run();
        MoonStoneCentrifugeContracts.run();
        System.out.println("Centrifuge core contracts passed (no native runtime coverage).");
    }
}
