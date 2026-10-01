package com.gregtech.gregtech.content.energy;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.api.energy.AxleSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import java.util.List;
import java.util.ArrayList;
/** Exact original 13 material families / 52 axle specifications. */
public final class AxleCatalog {private AxleCatalog(){}
    private record Tier(String name, GTMaterial mat, long speed, long[] powers) {}

    public static List<AxleSpec> specifications() {
        List<AxleSpec> specs=new ArrayList<>();
        // GT6 Loader_MultiTileEntities: 52 axles, four diameters per material.
        // NBT_PIPESIZE is the maximum RU speed; NBT_PIPEBANDWIDTH is the maximum power.
        // Names are stable registry IDs; the source's legacy numeric IDs are noted below.
        Tier[] tiers = {
                new Tier("wood", WoodMaterials.WoodTreated, 16, new long[]{1, 2, 4, 8}),                           // 24800-24803
                new Tier("bronze", Materials.Bronze, 64, new long[]{2, 4, 8, 16}),                                  // 24810-24813
                new Tier("brass", Materials.Brass, 64, new long[]{2, 4, 8, 16}),                                    // 24770-24773
                new Tier("arsenic_copper", Materials.ArsenicCopper, 64, new long[]{2, 4, 8, 16}),                  // 24780-24783
                new Tier("arsenic_bronze", Materials.ArsenicBronze, 64, new long[]{3, 6, 12, 24}),                // 24790-24793
                new Tier("steel", Materials.Steel, 256, new long[]{4, 8, 16, 32}),                                // 24820-24823
                new Tier("titanium", Materials.Titanium, 1024, new long[]{8, 16, 32, 64}),                         // 24830-24833
                new Tier("tungstensteel", Materials.Tungstensteel, 4096, new long[]{16, 32, 64, 128}),            // 24840-24843
                new Tier("iridium", Materials.Iridium, 16384, new long[]{32, 64, 128, 256}),                      // 24850-24853
                new Tier("iritanium", Materials.TitaniumIridium, 65536, new long[]{64, 128, 256, 512}),           // 24860-24863
                new Tier("trinitanium", Materials.Trinitanium, 262144, new long[]{128, 256, 512, 1024}),          // 24870-24873
                new Tier("trinaquadalloy", Materials.Trinaquadalloy, 1048576, new long[]{256, 512, 1024, 2048}), // 24880-24883
                new Tier("adamantium", Materials.Adamantium, 4194304, new long[]{512, 1024, 2048, 4096}),        // 24890-24893
        };

        for (Tier t : tiers) {
            for (int s = 1; s <= 4; s++) {
                long power = t.powers()[s - 1];
                long loss = 0; // GT6 axles do not lose speed per block
                String id = String.format("axle_%s_%d", t.name(), s);
                specs.add(new AxleSpec(id, t.mat(), s, t.speed(), power, loss));
            }
        }
        return List.copyOf(specs);
    }

}
