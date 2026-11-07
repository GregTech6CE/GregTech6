package gregtech.content.material.generated;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.data.GregTechConstants;
import static gregtech.engine.material.MaterialFactories.*;

/** Generated from GT6 material definitions. Regenerate with tools/transpile_gt6_materials.py. */
public final class StoneMaterials {
    private StoneMaterials() {}

    public static final GTMaterial Stone          = stone(8500, "Stone",          "Stone",          0xCDCDCD).setTextureSet(MaterialTextureSet.STONE).setStats(1100, 2200, 0.777486F).setToolStats(1, 2F, 16, 1);
    public static final GTMaterial Concrete       = stone(8501, "Concrete",       "Concrete",       0x646464).setTextureSet(MaterialTextureSet.BRICK).setStats(500, 1000, 0.777486F).setToolStats(1, 2.5F, 32, 0);
    public static final GTMaterial Netherrack     = stone(8502, "Netherrack",     "Netherrack",     0xC80000).setTextureSet(MaterialTextureSet.STONE).setStats(1500, 3000, 0.777486F).setToolStats(1, 2F, 8, 0);
    public static final GTMaterial NetherBrick    = stone(8503, "Nether Brick",   "BrickNether",    0x640000).setTextureSet(MaterialTextureSet.BRICK).setLocalName("BrickNether").setStats(1800, 3000, 0.777486F).setToolStats(1, 2F, 24, 1);
    public static final GTMaterial Endstone       = stone(8504, "Endstone",       "Endstone",       0xD9DE9E).setTextureSet(MaterialTextureSet.STONE).setStats(1200, 2400, 0.777486F).setToolStats(1, 3F, 16, 1);
    public static final GTMaterial Prismarine     = stone(9219, "Prismarine",     "Prismarine",     0x6EB2A5).setTextureSet(MaterialTextureSet.PRISMARINE).setStats(1000, 3000, 1.0F).setToolStats(1, 4F, 48, 1);
    public static final GTMaterial PrismarineDark = stone(9220, "PrismarineDark", "PrismarineDark", 0x587D6C).setTextureSet(MaterialTextureSet.PRISMARINE).setStats(1000, 3000, 1.0F).setToolStats(1, 4F, 48, 1);
    public static final GTMaterial Bluestone      = stone(9185, "Bluestone",      "Bluestone",      0x3434FC).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F);
    public static final GTMaterial MarsSand       = stone(8516, "Mars Sand",      "Mars Sand",      0xCF4242).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 3F, 16, 1);
    public static final GTMaterial SkyStone       = stone(8528, "Sky Stone",      "Sky Stone",      0x515C60).setTextureSet(MaterialTextureSet.STONE).setStats(2200, 4400, 2.35097994F).setTooltipChemical("Peridot₂RareEarthFeObsidian₅").setToolStats(1, 5F, 64, 2);
    public static final GTMaterial Livingrock     = stone(8521, "Livingrock",     "Livingrock",     0xC3CDC3).setTextureSet(MaterialTextureSet.STONE).setStats(1800, 3600, 1.0F).setToolStats(1, 5F, 128, 2);
    public static final GTMaterial Shale          = stone(9190, "Shale",          "Shale",          0x8E8EA8).setTextureSet(MaterialTextureSet.STONE).setStats(1802, 3305, 0.905085186F).setTooltipChemical("CaCO3₂MilkyQuartzClay").setToolStats(1, 2F, 16, 0);
    public static final GTMaterial Komatiite      = stone(9177, "Komatiite",      "Komatiite",      0xBEBE69).setTextureSet(MaterialTextureSet.STONE).setStats(1673, 3346, 0.987468767F).setTooltipChemical("PeridotMgCO3₂Flint₆DarkAsh₃").setToolStats(1, 3F, 32, 2);
    public static final GTMaterial Pumice         = stone(9000, "Pumice",         "Pumice",         0xDCD87F).setTextureSet(MaterialTextureSet.DULL).setStats(1673, 3346, 1.31424018F).setTooltipChemical("Peridot₃MgCO3₂Flint₄DarkAsh₂").setToolStats(1, 3F, 32, 2);
    public static final GTMaterial Basalt         = stone(8505, "Basalt",         "Basalt",         0x3C3232).setTextureSet(MaterialTextureSet.STONE).setStats(1673, 3346, 0.942978413F).setTooltipChemical("PeridotCaCO3₃Flint₈DarkAsh₄").setToolStats(1, 3F, 32, 2);
    public static final GTMaterial Marble         = stone(8506, "Marble",         "Marble",         0xC8C8C8).setTextureSet(MaterialTextureSet.STONE).setStats(1525, 2795, 0.884225225F).setTooltipChemical("MgCaCO3₇").setToolStats(1, 2.5F, 16, 1);
    public static final GTMaterial Limestone      = stone(9189, "Limestone",      "Limestone",      0xE6C882).setTextureSet(MaterialTextureSet.STONE).setStats(1612, 3000, 0.7622574F).setTooltipChemical("CaCO3").setToolStats(1, 2.5F, 16, 1);
    public static final GTMaterial Greenschist    = stone(9171, "Greenschist",    "Greenschist",    0x69BE69).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2F, 24, 1);
    public static final GTMaterial Blueschist     = stone(9184, "Blueschist",     "Blueschist",     0x6969BE).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2F, 24, 1);
    public static final GTMaterial Kimberlite     = stone(9218, "Kimberlite",     "Kimberlite",     0x64460A).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2F, 24, 2);
    public static final GTMaterial Quartzite      = stone(9180, "Quartzite",      "Quartzite",      0xE6CDCD).setTextureSet(MaterialTextureSet.QUARTZ).setStats(1000, 3000, 1.0F).setToolStats(1, 1.7F, 32, 1);
    public static final GTMaterial Andesite       = stone(9188, "Andesite",       "Andesite",       0xBFBFBF).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2.5F, 16, 1);
    public static final GTMaterial Diorite        = stone(8511, "Diorite",        "Diorite",        0xF0F0F0).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2.5F, 16, 1);
    public static final GTMaterial Chert          = stone(9186, "Chert",          "Chert",          0x690A0A).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2F, 16, 0);
    public static final GTMaterial Dacite         = stone(9187, "Dacite",         "Dacite",         0x838383).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2F, 16, 1);
    public static final GTMaterial Slate          = stone(9222, "Slate",          "Slate",          0x94979C).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2F, 16, 0);
    public static final GTMaterial Deepslate      = stone(9248, "Deepslate",      "Deepslate",      0x393B3D).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 2F, 32, 1);
    public static final GTMaterial TitanRock      = stone(9261, "TitanRock",      "TitanRock",      0xBDBDBD).setTextureSet(MaterialTextureSet.STONE).setStats(1000, 3000, 1.0F).setToolStats(1, 5F, 128, 1);
}
