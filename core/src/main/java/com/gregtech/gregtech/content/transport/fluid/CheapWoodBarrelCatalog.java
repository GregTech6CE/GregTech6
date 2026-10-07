/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from Loader_MultiTileEntities, source IDs 32733/32752/32753/32754. */
package com.gregtech.gregtech.content.transport.fluid;

import com.gregtech.gregtech.api.machine.TankSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.generated.GT6Materials;
import java.util.List;
import java.util.Optional;

/** Four source identities; the existing generic ID retains the lead-rod barrel. */
public final class CheapWoodBarrelCatalog {
    private CheapWoodBarrelCatalog() {}
    public record Entry(String id, int sourceId, GTMaterial rod) {
        public TankSpec spec() {
            return TankSpec.of(id, GT6Materials.Woods.Wood, TankSpec.TankType.WOOD_BARREL,
                    8000, false, false, false, false, true, 1F, 5F).withMaxTemperature(340);
        }
    }
    public static final List<Entry> ENTRIES = List.of(
            new Entry("wood_barrel", 32733, GT6Materials.Elements.Pb),
            new Entry("wood_barrel_bismuth", 32752, GT6Materials.Elements.Bi),
            new Entry("wood_barrel_bronze", 32753, GT6Materials.Compounds.Bronze),
            new Entry("wood_barrel_brass", 32754, GT6Materials.Compounds.Brass));
    public static Optional<Entry> entry(String id) {
        return ENTRIES.stream().filter(e -> e.id().equals(id)).findFirst();
    }
}
