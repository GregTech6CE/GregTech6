package gregtech.content.worldgen;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.stone.StoneType;
import gregtech.content.material.Materials;
import gregtech.content.material.generated.OreMaterials;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import java.util.List;

/** Mineral tables and placement ranges for the special deposits inherited from GT6. */
public final class SpecialOreDeposits {
    private SpecialOreDeposits() {}

    public static final int COLTAN_RADIUS = 480;
    public static final int COLTAN_FULL_ORE_RADIUS = 64;
    public static final int COLTAN_AMOUNT = 32;
    public static final int COLTAN_FIRST_LEGACY_Y = 20;
    public static final int COLTAN_END_LEGACY_Y = 40;
    public static final HeightRange COLTAN_HEIGHTS = new HeightRange(
            MineralWorldgenRules.overworldHeight(COLTAN_FIRST_LEGACY_Y, MineralWorldgenRules.LEGACY_ORE_FLOOR),
            MineralWorldgenRules.overworldHeight(COLTAN_END_LEGACY_Y, MineralWorldgenRules.LEGACY_ORE_FLOOR) - 1);
    public static final List<GTMaterial> COLTAN_ROLLS = List.of(
            OreMaterials.Columbite,
            OreMaterials.Tantalite,
            OreMaterials.Coltan,
            OreMaterials.Coltan,
            OreMaterials.Coltan);

    public static final int OCEAN_CENTER_MIN = 30;
    public static final int OCEAN_CENTER_MAX = 38;
    public static final int OCEAN_ORE_DIVISOR = 8;
    public static final int OCEAN_PYLON_REACH = MineralWorldgenRules.PYLON_TIERS.stream()
            .mapToInt(MineralWorldgenRules.Tier::toL)
            .max()
            .orElseThrow();
    public static final HeightRange OCEAN_ORE_HEIGHTS =
            new HeightRange(OCEAN_CENTER_MIN, OCEAN_CENTER_MAX + OCEAN_PYLON_REACH);

    public record Pylon(StoneType stone, GTMaterial material, int firstNoise, int lastNoise) {}

    public static final List<Pylon> OCEAN_PYLONS = List.of(
            new Pylon(StoneType.PRISMARINE_DARK, OreMaterials.Garnierite, 12, 13),
            new Pylon(StoneType.PRISMARINE_LIGHT, Materials.Pyrolusite, 14, 15));

    public static final int DUNGEON_RAW_ORE_OFFSET = 5;
    public static final List<GTMaterial> DUNGEON_ROLLS = List.of(
            Materials.Redstone,
            Materials.Redstone,
            Materials.Sulfur,
            Materials.Hematite,
            Materials.Pyrolusite,
            Materials.Apatite,
            OreMaterials.Molybdenite,
            OreMaterials.Bauxite,
            OreMaterials.Sphalerite,
            OreMaterials.Tetrahedrite,
            OreMaterials.Cassiterite,
            OreMaterials.Garnierite,
            OreMaterials.Galena);
}
