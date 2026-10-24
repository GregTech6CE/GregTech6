package gregtech.content.energy;

import gregtech.content.material.Materials;
import gregtech.api.material.GTMaterial;
import gregtech.api.machine.PumpSpec;

public final class PumpCatalog {
    private PumpCatalog() {}

    private record Tier(String name, GTMaterial mat, long speed) {}

    private static final Tier[] TIERS = {
            new Tier("bronze",        Materials.Bronze,        32),
            new Tier("steel",         Materials.Steel,         128),
            new Tier("titanium",      Materials.Titanium,      512),
            new Tier("tungstensteel", Materials.Tungstensteel, 2048),
    };

    public static java.util.List<PumpSpec> all() {
        var pumps = new java.util.ArrayList<PumpSpec>();
        for (int tierIndex = 0; tierIndex < TIERS.length; tierIndex++) {
            var tier = TIERS[tierIndex];
            pumps.add(new PumpSpec("rotational_pump_" + tier.name(), tier.mat(), tier.speed(), tierIndex + 1));
        }
        return java.util.List.copyOf(pumps);
    }
}
