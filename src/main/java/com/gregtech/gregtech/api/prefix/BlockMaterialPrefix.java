package com.gregtech.gregtech.api.prefix;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Predicate;

/** Storage / casing / crate block prefix (GT6 {@code OP} block form). Instances are created from {@link com.gregtech.gregtech.data.MaterialPrefixes}. */
public final class BlockMaterialPrefix {
    public static BlockMaterialPrefix blockRaw;
    public static BlockMaterialPrefix blockGem;
    public static BlockMaterialPrefix blockDust;
    public static BlockMaterialPrefix blockIngot;
    public static BlockMaterialPrefix blockPlate;
    public static BlockMaterialPrefix blockPlateGem;
    public static BlockMaterialPrefix blockSolid;
    public static BlockMaterialPrefix casingMachine;
    public static BlockMaterialPrefix casingMachineDouble;
    public static BlockMaterialPrefix casingMachineQuadruple;
    public static BlockMaterialPrefix casingMachineDense;
    public static BlockMaterialPrefix crateGtRaw;
    public static BlockMaterialPrefix crateGtGem;
    public static BlockMaterialPrefix crateGtDust;
    public static BlockMaterialPrefix crateGtIngot;
    public static BlockMaterialPrefix crateGtPlate;
    public static BlockMaterialPrefix crateGtPlateGem;
    public static BlockMaterialPrefix crateGt64Raw;
    public static BlockMaterialPrefix crateGt64Gem;
    public static BlockMaterialPrefix crateGt64Dust;
    public static BlockMaterialPrefix crateGt64Ingot;
    public static BlockMaterialPrefix crateGt64Plate;
    public static BlockMaterialPrefix crateGt64PlateGem;
    public static BlockMaterialPrefix ore;
    public static BlockMaterialPrefix oreSmall;

    private final String name;
    private final String displayName;
    private final String namePrefix;
    private final String nameSuffix;
    private final Predicate<GTMaterial> validator;
    private final float baseHardness;
    private final float baseResistance;
    private final int harvestLevelOffset;
    private final MapColor mapColor;
    private final SoundType soundType;
    private final boolean falling;
    private final long materialWeight;
    private final String registryName;
    private final String textureFileName;

    public static BlockMaterialPrefix define(String name, String displayName, String namePrefix, String nameSuffix,
                                             Predicate<GTMaterial> validator, float baseHardness, float baseResistance,
                                             int harvestLevelOffset, MapColor mapColor, SoundType soundType,
                                             boolean falling, long materialWeight) {
        return define(name, displayName, namePrefix, nameSuffix, validator, baseHardness, baseResistance,
                harvestLevelOffset, mapColor, soundType, falling, materialWeight, null);
    }

    public static BlockMaterialPrefix define(String name, String displayName, String namePrefix, String nameSuffix,
                                             Predicate<GTMaterial> validator, float baseHardness, float baseResistance,
                                             int harvestLevelOffset, MapColor mapColor, SoundType soundType,
                                             boolean falling, long materialWeight, @javax.annotation.Nullable String textureFileName) {
        return new BlockMaterialPrefix(name, displayName, namePrefix, nameSuffix, validator, baseHardness,
                baseResistance, harvestLevelOffset, mapColor, soundType, falling, materialWeight, textureFileName);
    }

    BlockMaterialPrefix(String name, String displayName, String namePrefix, String nameSuffix,
                        Predicate<GTMaterial> validator, float baseHardness, float baseResistance,
                        int harvestLevelOffset, MapColor mapColor, SoundType soundType, boolean falling,
                        long materialWeight, @javax.annotation.Nullable String textureFileNameOverride) {
        this.name = name;
        this.displayName = displayName;
        this.namePrefix = namePrefix;
        this.nameSuffix = nameSuffix;
        this.validator = validator;
        this.baseHardness = baseHardness;
        this.baseResistance = baseResistance;
        this.harvestLevelOffset = harvestLevelOffset;
        this.mapColor = mapColor;
        this.soundType = soundType;
        this.falling = falling;
        this.materialWeight = materialWeight;
        this.registryName = MaterialPrefix.camelToSnake(name);
        this.textureFileName = textureFileNameOverride != null ? textureFileNameOverride.toLowerCase() : name.toLowerCase();
        BlockPrefixRegistry.register(this);
    }

    public static void bootstrap() {
        com.gregtech.gregtech.data.MaterialPrefixes.bootstrap();
        if (blockRaw == null || blockIngot == null || crateGtRaw == null) {
            throw new IllegalStateException("BlockMaterialPrefix constants failed to initialize");
        }
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getNamePrefix() { return namePrefix; }
    public String getNameSuffix() { return nameSuffix; }
    public String getRegistryName() { return registryName; }
    public String getTextureFileName() { return textureFileName; }
    public float baseHardness() { return baseHardness; }
    public float baseResistance() { return baseResistance; }
    public int harvestLevelOffset() { return harvestLevelOffset; }
    public MapColor mapColor() { return mapColor; }
    public SoundType soundType() { return soundType; }
    public boolean falling() { return falling; }

    public boolean isValidFor(GTMaterial material) {
        return material != null && material.isValid() && !material.has(com.gregtech.gregtech.api.material.MaterialProperty.HIDDEN)
                && validator.test(material);
    }

    public String getBlockId(GTMaterial material) {
        return registryName + "_" + material.getName().toLowerCase();
    }

    public long getMaterialWeight() {
        return materialWeight;
    }

    /** GT6 crate blocks use a wooden hull ({@code iconsets/crate}) on the bottom face. */
    public boolean isCrate() {
        return name.startsWith("crateGt");
    }

    /** GT6 keeps the sixteen-piece crates out of the creative inventory. */
    public boolean isPartialCrate() {
        return isCrate() && !name.startsWith("crateGt64");
    }

    public float computeHardness(GTMaterial material) {
        int level = harvestLevel(material);
        return Math.max(0.1F, baseHardness * (1.0F + level));
    }

    public float computeResistance(GTMaterial material) {
        int level = harvestLevel(material);
        return baseResistance * (1.0F + level);
    }

    public int harvestLevel(GTMaterial material) {
        return Math.max(0, harvestLevelOffset + material.getToolQuality());
    }
}
