package gregtech.engine.material;

import gregtech.api.material.*;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Original Loader_Blocks material order, validators and namespace collision policy. */
public final class MaterialBlockDefinitions {
    private static final Set<String> ADDON_ORE_HELD_BACK =
            Set.of("ore", "oreSmall", "blockRaw", "crateGtRaw", "crateGt64Raw");

    public record Definition(String blockId, String baseBlockId, BlockMaterialPrefix prefix, GTMaterial material) {}

    private MaterialBlockDefinitions() {}

    public static List<Definition> all() {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        var ids = new HashSet<String>();
        var result = new ArrayList<Definition>();
        var addonOres = new ArrayList<GTMaterial>();
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.has(MaterialProperty.HIDDEN) || material.resolve() != material) continue;
            boolean addonOre = AddonMaterials.hasOreForm(material) && !GTMaterialRegistry.isCoreMaterial(material);
            if (addonOre) addonOres.add(material);
            for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
                if (prefix.isAddon()) continue;
                if (!prefix.isValidFor(material)) continue;
                if (addonOre && ADDON_ORE_HELD_BACK.contains(prefix.getName())) continue;
                add(result, ids, prefix, material);
            }
        }
        // Ore and small ore of an addon material come after every block the directory already emitted,
        // including every core block. Raw storage blocks and raw crates stay unregistered.
        for (GTMaterial material : addonOres) {
            BlockMaterialPrefix ore = BlockMaterialPrefix.ore;
            BlockMaterialPrefix oreSmall = BlockMaterialPrefix.oreSmall;
            if (ore != null && ore.isValidFor(material)) add(result, ids, ore, material);
            if (oreSmall != null && oreSmall.isValidFor(material)) add(result, ids, oreSmall, material);
        }
        // Declared block prefixes follow those, so neither they nor the ores shift a built-in id.
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (!prefix.isAddon()) continue;
            for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
                if (material.has(MaterialProperty.HIDDEN) || material.resolve() != material) continue;
                if (!prefix.isValidFor(material)) continue;
                add(result, ids, prefix, material);
            }
        }
        return List.copyOf(result);
    }

    private static void add(List<Definition> result, Set<String> ids, BlockMaterialPrefix prefix, GTMaterial material) {
        String base = prefix.getBlockId(material);
        String id = base;
        if (!ids.add(id)) {
            id = base + "_" + material.getId();
            if (!ids.add(id))
                throw new IllegalStateException(
                        "Could not allocate unique block id for " + prefix.getName() + " / " + material.getName());
        }
        result.add(new Definition(id, base, prefix, material));
    }
}
