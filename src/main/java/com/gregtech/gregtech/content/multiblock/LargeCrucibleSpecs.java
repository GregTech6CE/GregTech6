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
                    acidProof, 100L * GTValues.U);
        }

        public Block wall() {
            return wallId < 0 ? GTMultiblocks.LARGE_CRUCIBLE_WALL.get() : LargeMachineParts.block(wallId);
        }
    }

    private static final List<Variant> VARIANTS = List.of(
            new Variant(17309, "large_steel_crucible", Materials.Steel, 18009, 6, false),
            new Variant(17302, "large_stainless_steel_crucible", Materials.StainlessSteel, 18002, 6, true),
            new Variant(17307, "large_invar_crucible", Materials.Invar, 18007, 6, false),
            new Variant(17306, "large_titanium_crucible", Materials.Titanium, 18006, 9, false),
            new Variant(17303, "large_tungstensteel_crucible", Materials.Tungstensteel, 18003, 12.5f, false),
            new Variant(17304, "large_tungsten_crucible", Materials.Tungsten, 18004, 10, true),
            new Variant(17312, "large_tantalum_hafnium_carbide_crucible", Materials.TantalumHafniumCarbide, 18012, 12.5f, false),
            new Variant(17305, "large_adamantium_crucible", Materials.Adamantium, 18005, 100, true));

    /** The older port-only main stays usable in existing test worlds. */
    public static final Variant LEGACY = new Variant(-1, "large_crucible_main", Materials.StainlessSteel, -1, 5, true);

    private LargeCrucibleSpecs() {}

    public static List<Variant> all() { return VARIANTS; }

    public static Variant byOriginalId(int id) {
        return VARIANTS.stream().filter(variant -> variant.originalId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GT6 large crucible " + id));
    }
}
