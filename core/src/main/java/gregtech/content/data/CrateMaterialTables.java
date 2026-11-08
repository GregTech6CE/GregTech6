package gregtech.content.data;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.material.SupplementalMaterials;

/**
 * GT6's crate material tables ({@code DungeonChunkRoomStorage}) as port materials, in GT6's
 * own order <em>and multiplicity</em> (a material listed three times is three times as likely).
 *
 * <p>The tables are shared by the dungeon storage room's generation (loose piles and raw ore blocks)
 * and by {@code MaterialCrateLoot} when a crate opens, so they live in {@code content.data} rather
 * than either consumer's package.</p>
 *
 * <p>The port keeps GT6's symbol facade ({@link GT6Materials}), so every entry is the port's material
 * for that GT6 symbol. {@code MT.WOODS.*} is held by {@code Woods}, the ore list by {@code Ores} and
 * the dusts/gems by {@code Compounds}: GT6's dust table mixes ore materials (salt, clay, bone, rare
 * earth) with compounds, exactly as its symbol names say.</p>
 *
 * <p>Porcelain comes from {@link SupplementalMaterials} (id 8273), the port's material for GT6's
 * {@code MT.Porcelain}. Kept out: the nine GT6 dust materials the port does not register
 * ({@code Vanilla}, {@code PepperBlack}, {@code Curry}, {@code Barley}, {@code Rice}, {@code Oat},
 * {@code OatAbyssal}, {@code Corn}, {@code WaxRefractory}) and the two other-mod tables
 * ({@code sHexoriums} of Hexxit and {@code sInfused} of Thaumcraft), which GT6 itself only uses when
 * those mods are loaded.</p>
 */
public final class CrateMaterialTables {
    private CrateMaterialTables() {}

    public static final GTMaterial[] CRATE_DUSTS = {
        GT6Materials.Ores.NaCl,
        GT6Materials.Ores.NaCl,
        GT6Materials.Ores.KCl,
        GT6Materials.Ores.KCl,
        GT6Materials.Compounds.Gunpowder,
        GT6Materials.Compounds.Gunpowder,
        GT6Materials.Compounds.Gunpowder,
        GT6Materials.Ores.Bone,
        GT6Materials.Ores.Bone,
        GT6Materials.Compounds.Asphalt,
        GT6Materials.Compounds.Asphalt,
        GT6Materials.Ores.Clay,
        GT6Materials.Compounds.ClayBrown,
        GT6Materials.Compounds.ClayRed,
        GT6Materials.Compounds.Bentonite,
        GT6Materials.Compounds.Palygorskite,
        GT6Materials.Compounds.Kaolinite,
        SupplementalMaterials.porcelain(),
        GT6Materials.Ores.RareEarth,
        GT6Materials.Compounds.Sugar,
        GT6Materials.Compounds.Cocoa,
        GT6Materials.Compounds.Coffee,
        GT6Materials.Compounds.Wheat,
        GT6Materials.Compounds.Rye,
        GT6Materials.Compounds.Potato,
        GT6Materials.Compounds.WaxBee,
        GT6Materials.Compounds.Blaze,
        GT6Materials.Compounds.Breeze,
        GT6Materials.Compounds.Blizz,
        GT6Materials.Compounds.Blitz,
        GT6Materials.Compounds.Basalz
    };

    /** GT6's {@code sWoods}: the crate material list of the wood quadrant. */
    public static final GTMaterial[] CRATE_WOODS = {
        GT6Materials.Woods.Oak, GT6Materials.Woods.Birch, GT6Materials.Woods.Spruce,
        GT6Materials.Woods.Jungle, GT6Materials.Woods.Acacia, GT6Materials.Woods.DarkOak,
        GT6Materials.Woods.Crimson, GT6Materials.Woods.Warped, GT6Materials.Woods.Compressed,
        GT6Materials.Woods.WoodRubber, GT6Materials.Woods.Maple, GT6Materials.Woods.Willow,
        GT6Materials.Woods.BlueMahoe, GT6Materials.Woods.Hazel, GT6Materials.Compounds.Cinnamon,
        GT6Materials.Woods.Coconut, GT6Materials.Woods.Rainbowood, GT6Materials.Woods.BlueSpruce,
        GT6Materials.Woods.Livingwood, GT6Materials.Woods.Greatwood, GT6Materials.Woods.Silverwood,
        GT6Materials.Woods.WoodTreated
    };

    /** GT6's {@code sGems}: 47 entries covering gems, crystals, coals and lapis. */
    public static final GTMaterial[] CRATE_GEMS = {
        GT6Materials.Compounds.EnderPearl,
        GT6Materials.Compounds.EnderPearl,
        GT6Materials.Compounds.EnderEye,
        GT6Materials.Compounds.Diamond,
        GT6Materials.Compounds.DiamondPink,
        GT6Materials.Compounds.Emerald,
        GT6Materials.Compounds.Aquamarine,
        GT6Materials.Compounds.Ruby,
        GT6Materials.Compounds.GreenSapphire,
        GT6Materials.Compounds.BlueSapphire,
        GT6Materials.Compounds.Amethyst,
        GT6Materials.Compounds.Craponite,
        GT6Materials.Compounds.Amber,
        GT6Materials.Compounds.NetherQuartz,
        GT6Materials.Compounds.NetherQuartz,
        GT6Materials.Compounds.MilkyQuartz,
        GT6Materials.Compounds.MilkyQuartz,
        GT6Materials.Compounds.CertusQuartz,
        GT6Materials.Compounds.ChargedCertusQuartz,
        GT6Materials.Compounds.Lapis,
        GT6Materials.Compounds.Lapis,
        GT6Materials.Compounds.Lapis,
        GT6Materials.Compounds.Redstone,
        GT6Materials.Compounds.Redstone,
        GT6Materials.Compounds.Redstone,
        GT6Materials.Compounds.Glowstone,
        GT6Materials.Compounds.Glowstone,
        GT6Materials.Compounds.Apatite,
        GT6Materials.Compounds.Apatite,
        GT6Materials.Compounds.Apatite,
        GT6Materials.Compounds.Coal,
        GT6Materials.Compounds.Coal,
        GT6Materials.Compounds.Coal,
        GT6Materials.Compounds.Coal,
        GT6Materials.Compounds.Coal,
        GT6Materials.Compounds.Charcoal,
        GT6Materials.Compounds.Charcoal,
        GT6Materials.Compounds.Charcoal,
        GT6Materials.Compounds.Charcoal,
        GT6Materials.Compounds.Charcoal,
        GT6Materials.Compounds.Lignite,
        GT6Materials.Compounds.Lignite,
        GT6Materials.Compounds.Lignite,
        GT6Materials.Compounds.Lignite,
        GT6Materials.Compounds.Lignite
    };

    /** GT6's {@code sOres}: 54 entries, the raw-ore crates and the raw ore blocks. */
    public static final GTMaterial[] CRATE_ORES = {
        GT6Materials.Ores.TiO2,
        GT6Materials.Ores.TiO2,
        GT6Materials.Ores.MnO2,
        GT6Materials.Ores.MnO2,
        GT6Materials.Ores.MnO2,
        GT6Materials.Ores.Fe2O3,
        GT6Materials.Ores.Fe2O3,
        GT6Materials.Ores.Fe2O3,
        GT6Materials.Ores.Fe2O3,
        GT6Materials.Ores.Fe2O3,
        GT6Materials.Ores.Cassiterite,
        GT6Materials.Ores.Cassiterite,
        GT6Materials.Ores.Cassiterite,
        GT6Materials.Ores.Cassiterite,
        GT6Materials.Ores.Cassiterite,
        GT6Materials.Ores.Zeolite,
        GT6Materials.Ores.Pollucite,
        GT6Materials.Ores.Borax,
        GT6Materials.Ores.BrownLimonite,
        GT6Materials.Ores.YellowLimonite,
        GT6Materials.Ores.Garnierite,
        GT6Materials.Ores.Scheelite,
        GT6Materials.Ores.Wolframite,
        GT6Materials.Ores.Ferberite,
        GT6Materials.Ores.Huebnerite,
        GT6Materials.Ores.Tungstate,
        GT6Materials.Ores.Realgar,
        GT6Materials.Ores.Cinnabar,
        GT6Materials.Ores.Molybdenite,
        GT6Materials.Ores.Sphalerite,
        GT6Materials.Ores.Stibnite,
        GT6Materials.Ores.Pentlandite,
        GT6Materials.Ores.Chalcopyrite,
        GT6Materials.Ores.Arsenopyrite,
        GT6Materials.Ores.Cobaltite,
        GT6Materials.Ores.Galena,
        GT6Materials.Ores.Cooperite,
        GT6Materials.Ores.Tetrahedrite,
        GT6Materials.Ores.Kesterite,
        GT6Materials.Ores.Stannite,
        GT6Materials.Ores.Barite,
        GT6Materials.Ores.Celestine,
        GT6Materials.Ores.Ilmenite,
        GT6Materials.Ores.Bauxite,
        GT6Materials.Ores.Chromite,
        GT6Materials.Ores.Powellite,
        GT6Materials.Ores.Wulfenite,
        GT6Materials.Ores.Ferrovanadium,
        GT6Materials.Ores.Bastnasite,
        GT6Materials.Ores.Coltan,
        GT6Materials.Ores.Malachite,
        GT6Materials.Ores.Bromargyrite,
        GT6Materials.Ores.Smithsonite,
        GT6Materials.Ores.Sperrylite
    };

    /** GT6's {@code sMetals}, the same list the loose piles of that room roll from. */
    public static final GTMaterial[] CRATE_METALS = {
        GT6Materials.Elements.Cu,
        GT6Materials.Elements.Cu,
        GT6Materials.Elements.Sn,
        GT6Materials.Compounds.Bronze,
        GT6Materials.Elements.Fe,
        GT6Materials.Elements.Fe,
        GT6Materials.Elements.Fe,
        GT6Materials.Compounds.Steel,
        GT6Materials.Compounds.Steel,
        GT6Materials.Compounds.StainlessSteel,
        GT6Materials.Compounds.StainlessSteel,
        GT6Materials.Compounds.DamascusSteel
    };
}
