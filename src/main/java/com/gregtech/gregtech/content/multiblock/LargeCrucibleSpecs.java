package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** GT6 Loader_MultiTileEntities 1270–1277 and MultiTileEntityCrucible#mWalls. */
public final class LargeCrucibleSpecs {
    public record Variant(int originalId, String path, GTMaterial material, int wallId,
                          float hardness, boolean acidProof) {
        public CrucibleSpec crucible() {
            // GT6 weighs the hull as 100 material units for heat calculations.
            return CrucibleSpec.of(path, material, originalId, hardness, hardness,
                    acidProof, OriginalLargeCrucibleParameters.HULL_UNITS);
        }

        public Block wall() {
            return wallId < 0 ? GTMultiblocks.LARGE_CRUCIBLE_WALL.get() : LargeMachineParts.block(wallId);
        }
    }

    private static final List<Variant> VARIANTS = OriginalLargeCrucibleParameters.DEFINITIONS.stream().map(d -> new Variant(d.originalId(), d.path(), com.gregtech.gregtech.api.material.GTMaterialRegistry.get(d.material()), d.wallId(), d.hardness(), d.acidProof())).toList();

    /** The older port-only main stays usable in existing test worlds. */
    public static final Variant LEGACY = new Variant(-1, "large_crucible_main", Materials.StainlessSteel, -1, 5, true);

    private LargeCrucibleSpecs() {}

    public static List<Variant> all() { return VARIANTS; }

    public static Variant byOriginalId(int id) {
        return VARIANTS.stream().filter(variant -> variant.originalId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GT6 large crucible " + id));
    }
}
