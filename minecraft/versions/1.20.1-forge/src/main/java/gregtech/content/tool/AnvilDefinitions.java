package gregtech.content.tool;
/** Platform registration over the shared original variant catalogue. */
public final class AnvilDefinitions {
    private AnvilDefinitions() {}

    public static void registerAll(gregtech.content.registry.ContentRegistrar registrar) {
        for (var v : AnvilVariantCatalog.ALL)
            gregtech.content.registry.GTToolBlocks.anvil(registrar, v.id(), v.material(), v.durability());
    }
}
