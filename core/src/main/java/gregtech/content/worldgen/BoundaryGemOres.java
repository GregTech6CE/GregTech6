package gregtech.content.worldgen;

import gregtech.api.material.GTMaterial;
import gregtech.content.material.Materials;
import gregtech.content.material.generated.CompoundMaterials;
import java.util.List;

/** Retained gemstone materials for the GT6 random layer-boundary small-ore roll. */
public final class BoundaryGemOres {
    public static final int CHANCE_DIVISOR = 100;
    private static final long CHANCE_SALT = 0x82FE3B7104D3695AL;
    private static final long MATERIAL_SALT = 0xF91680D2E57A3B4CL;
    public static final List<GTMaterial> MATERIALS = List.of(
            Materials.Amber,
            Materials.Amethyst,
            Materials.Diamond,
            Materials.Emerald,
            Materials.Ruby,
            Materials.Sapphire,
            Materials.BlueSapphire,
            Materials.GreenSapphire,
            Materials.YellowSapphire,
            Materials.OrangeSapphire,
            Materials.PurpleSapphire,
            Materials.Almandine,
            Materials.Andradite,
            Materials.Pyrope,
            Materials.Spessartine,
            Materials.Uvarovite,
            Materials.Topaz,
            Materials.BlueTopaz,
            Materials.Peridot,
            Materials.Spinel,
            Materials.BalasRuby,
            Materials.Tanzanite,
            Materials.Zircon,
            CompoundMaterials.Opal);

    private BoundaryGemOres() {}

    public static GTMaterial choose(long positionSeed, int divisor) {
        if (WorldgenHash.roll(WorldgenHash.mix(positionSeed ^ CHANCE_SALT), divisor) != 0) return null;
        int index = WorldgenHash.roll(WorldgenHash.mix(positionSeed ^ MATERIAL_SALT), MATERIALS.size());
        return MATERIALS.get(index);
    }
}
