package gregtech.content.energy;

/** Forge registration over the shared family catalog; NeoForge consumes it in GTWires.register. */
public final class WireDefinitions {
    private WireDefinitions() {}

    public static void register() {
        var registrar = new gregtech.platform.forge.ForgeContentRegistrar();
        for (var spec : WireCatalog.specifications())
            gregtech.content.registry.GTWires.register(
                    registrar,
                    spec.id(),
                    spec.material(),
                    spec.size(),
                    spec.voltage(),
                    spec.amperage(),
                    spec.lossPerBlock(),
                    spec.insulated(),
                    spec.contactDamage());
    }
}
