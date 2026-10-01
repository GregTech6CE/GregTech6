package com.gregtech.gregtech.block.stone;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;

/** GT6 rock types — used by {@code Loader_Blocks} for stone block registration. */
public enum StoneType {
    GRANITE_BLACK("granite_black", "gt.stone.granite.black", Materials.GraniteBlack, 6.0F, 3.0F, 3, true),
    GRANITE_RED("granite_red", "gt.stone.granite.red", Materials.GraniteRed, 6.0F, 3.0F, 3, true),
    BASALT("basalt", "gt.stone.basalt", com.gregtech.gregtech.content.material.generated.StoneMaterials.Basalt, 3.0F, 2.0F, 2, false),
    MARBLE("marble", "gt.stone.marble", com.gregtech.gregtech.content.material.generated.StoneMaterials.Marble, 0.75F, 0.50F, 0, false),
    LIMESTONE("limestone", "gt.stone.limestone", com.gregtech.gregtech.content.material.generated.StoneMaterials.Limestone, 0.75F, 0.50F, 0, false),
    GRANITE("granite", "gt.stone.granite", Materials.Granite, 2.0F, 1.0F, 1, false),
    DIORITE("diorite", "gt.stone.diorite", com.gregtech.gregtech.content.material.generated.StoneMaterials.Diorite, 0.75F, 0.50F, 0, false),
    ANDESITE("andesite", "gt.stone.andesite", com.gregtech.gregtech.content.material.generated.StoneMaterials.Andesite, 0.75F, 0.50F, 0, false),
    KOMATIITE("komatiite", "gt.stone.komatiite", com.gregtech.gregtech.content.material.generated.StoneMaterials.Komatiite, 3.0F, 2.0F, 2, false),
    GREENSCHIST("greenschist", "gt.stone.greenschist", com.gregtech.gregtech.content.material.generated.StoneMaterials.Greenschist, 0.75F, 0.50F, 0, false),
    BLUESCHIST("blueschist", "gt.stone.blueschist", com.gregtech.gregtech.content.material.generated.StoneMaterials.Blueschist, 0.75F, 0.50F, 0, false),
    KIMBERLITE("kimberlite", "gt.stone.kimberlite", com.gregtech.gregtech.content.material.generated.StoneMaterials.Kimberlite, 3.0F, 2.0F, 2, false),
    QUARTZITE("quartzite", "gt.stone.quartzite", com.gregtech.gregtech.content.material.generated.StoneMaterials.Quartzite, 0.75F, 0.50F, 0, false),
    PRISMARINE_LIGHT("prismarine_light", "gt.stone.prismarine.light", com.gregtech.gregtech.content.material.generated.StoneMaterials.Prismarine, 0.75F, 0.50F, 0, false),
    PRISMARINE_DARK("prismarine_dark", "gt.stone.prismarine.dark", com.gregtech.gregtech.content.material.generated.StoneMaterials.PrismarineDark, 0.75F, 0.50F, 1, false),
    SLATE("slate", "gt.stone.slate", com.gregtech.gregtech.content.material.generated.StoneMaterials.Slate, 0.75F, 0.50F, 1, false),
    SHALE("shale", "gt.stone.shale", com.gregtech.gregtech.content.material.generated.StoneMaterials.Shale, 0.75F, 0.50F, 0, false),
    // Stones from the GT6 worldgen config without shipped art (textures synthesized
    // from the limestone set tinted with the material color, see tools/generate_stone_textures.py)
    CHALK("chalk", "gt.stone.chalk", Materials.Chalk, 0.75F, 0.50F, 0, false),
    DOLOMITE("dolomite", "gt.stone.dolomite", Materials.Dolomite, 0.75F, 0.50F, 0, false),
    GABBRO("gabbro", "gt.stone.gabbro", com.gregtech.gregtech.content.material.generated.StoneMaterials.Gabbro, 3.0F, 2.0F, 2, false),
    GNEISS("gneiss", "gt.stone.gneiss", com.gregtech.gregtech.content.material.generated.StoneMaterials.Gneiss, 2.0F, 1.0F, 1, false),
    GYPSUM("gypsum", "gt.stone.gypsum", Materials.Gypsum, 0.75F, 0.50F, 0, false),
    OILSHALE("oilshale", "gt.stone.oilshale", Materials.OilShale, 0.75F, 0.50F, 0, false),
    RHYOLITE("rhyolite", "gt.stone.rhyolite", com.gregtech.gregtech.content.material.generated.StoneMaterials.Rhyolite, 2.0F, 1.0F, 1, false),
    SALT("salt", "gt.stone.salt", Materials.Salt, 0.75F, 0.50F, 0, false),
    SYLVITE("sylvite", "gt.stone.sylvite", Materials.Sylvite, 0.75F, 0.50F, 0, false),
    TALC("talc", "gt.stone.talc", Materials.Talc, 0.75F, 0.50F, 0, false);

    private final String id;
    private final String textureFolder;
    private final GTMaterial material;
    private final float resistanceMultiplier;
    private final float hardnessMultiplier;
    private final int harvestLevel;
    private final boolean witherProof;

    StoneType(String id, String textureFolder, GTMaterial material,
              float resistanceMultiplier, float hardnessMultiplier, int harvestLevel, boolean witherProof) {
        this.id = id;
        this.textureFolder = textureFolder;
        this.material = material;
        this.resistanceMultiplier = resistanceMultiplier;
        this.hardnessMultiplier = hardnessMultiplier;
        this.harvestLevel = harvestLevel;
        this.witherProof = witherProof;
    }

    public String registryId() {
        return "stone_" + id;
    }

    public String textureFolder() {
        return textureFolder;
    }

    public GTMaterial material() {
        return material;
    }

    public float resistanceMultiplier() {
        return resistanceMultiplier;
    }

    public float hardnessMultiplier() {
        return hardnessMultiplier;
    }

    public int harvestLevel() {
        return harvestLevel;
    }

    public boolean witherProof() {
        return witherProof;
    }

}
