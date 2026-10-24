/* Gregorius Techneticies / GregTech-6 Team; LGPL-3.0-or-later.
 * Loader_MultiTileEntities 11021–11025, 11111–11115. */
package gregtech.content.energy;

import gregtech.internal.energy.EnergyNodeSpec;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.content.material.Materials;
import gregtech.data.GregTechTags;
import java.util.*;

public final class FluxRotaryCatalog {
    private FluxRotaryCatalog() {}
    public record Tier(String tier, GTMaterial material, long voltage) {}
    public static final List<Tier> TIERS = List.of(
            new Tier("lv", Materials.Lead, 32), new Tier("mv", Materials.Invar, 128),
            new Tier("hv", Materials.Electrum, 512), new Tier("ev", Materials.EnderiumBase, 2048),
            new Tier("iv", Materials.Enderium, 8192));
    public static List<EnergyNodeSpec> specifications() {
        var values = new ArrayList<EnergyNodeSpec>();
        for (var tier : TIERS) {
            values.add(EnergyNodeSpec.builder("flux_motor_" + tier.tier(), tier.material())
                    .kind(EnergyNodeSpec.Kind.CONVERTER).texture("motors/rotation_flux")
                    .input(GregTechTags.Energy.RF, tier.voltage() * 4).output(GregTechTags.Energy.RU, tier.voltage() / 2)
                    .capacity(tier.voltage() * 8).name("Flux Motor (" + tier.tier().toUpperCase(Locale.ROOT) + ")").build());
            values.add(EnergyNodeSpec.builder("flux_dynamo_" + tier.tier(), tier.material())
                    .kind(EnergyNodeSpec.Kind.CONVERTER).texture("dynamos/flux_rotation")
                    .input(GregTechTags.Energy.RU, tier.voltage()).output(GregTechTags.Energy.RF, tier.voltage() * 11 / 4)
                    .capacity(tier.voltage() * 2).name("Flux Dynamo (" + tier.tier().toUpperCase(Locale.ROOT) + ")").build());
        }
        return List.copyOf(values);
    }
}
