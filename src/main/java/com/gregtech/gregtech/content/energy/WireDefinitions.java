package com.gregtech.gregtech.content.energy;
/** Original native registration over the single shared family catalog. */
public final class WireDefinitions {
    private WireDefinitions(){}
    public static void register(){for(var spec:WireCatalog.specifications())com.gregtech.gregtech.registry.GTWires.register(spec.id(),spec.material(),spec.size(),spec.voltage(),spec.amperage(),spec.lossPerBlock(),spec.insulated(),spec.contactDamage());}
}
