package com.gregtech.gregtech.api.material;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.prefix.BlockPrefixRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
/** Original Loader_Blocks material order, validators and namespace collision policy. */
public final class MaterialBlockDefinitions {
    public record Definition(String blockId, String baseBlockId, BlockMaterialPrefix prefix, GTMaterial material) {}
    private MaterialBlockDefinitions() {}
    public static List<Definition> all() {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        var ids = new HashSet<String>();
        var result = new ArrayList<Definition>();
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.has(MaterialProperty.HIDDEN) || material.resolve() != material) continue;
            for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
                if (!prefix.isValidFor(material)) continue;
                String base = prefix.getBlockId(material);
                String id = base;
                if (!ids.add(id)) {
                    id = base + "_" + material.getId();
                    if (!ids.add(id)) throw new IllegalStateException("Could not allocate unique block id for "
                            + prefix.getName() + " / " + material.getName());
                }
                result.add(new Definition(id, base, prefix, material));
            }
        }
        return List.copyOf(result);
    }
}
