package gregtech.api.prefix;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.data.MaterialPrefix;

import java.util.function.Predicate;

/** Storage / casing / crate block prefix (GT6 {@code OP} block form). Instances are created from {@link gregtech.data.MaterialPrefixes}. */
public final class BlockMaterialPrefix {
    public enum MapColor { METAL, STONE, WOOD, SAND }
    public enum SoundType { METAL, STONE, WOOD, SAND, GRAVEL }
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
    private final boolean addon;
    private final String textureNamespace;
    private final String addonTexturePath;
    private final String oreDictionary;

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
                                             boolean falling, long materialWeight, String textureFileName) {
        return new BlockMaterialPrefix(name, displayName, namePrefix, nameSuffix, validator, baseHardness,
                baseResistance, harvestLevelOffset, mapColor, soundType, falling, materialWeight, textureFileName,
                false, "", "", "");
    }

    /** Addon cube. Registered after the built-in prefixes, and appended after built-in blocks. */
    public static BlockMaterialPrefix defineAddon(String name, String displayName, String namePrefix, String nameSuffix,
                                                  long materialWeight, String textureNamespace, String texturePath,
                                                  String oreDictionary, Predicate<GTMaterial> validator) {
        return new BlockMaterialPrefix(name, displayName, namePrefix, nameSuffix, validator, 1.7F, 5.0F, 0,
                MapColor.METAL, SoundType.METAL, false, materialWeight, null, true, textureNamespace, texturePath,
                oreDictionary);
    }

    BlockMaterialPrefix(String name, String displayName, String namePrefix, String nameSuffix,
                        Predicate<GTMaterial> validator, float baseHardness, float baseResistance,
                        int harvestLevelOffset, MapColor mapColor, SoundType soundType, boolean falling,
                        long materialWeight, String textureFileNameOverride, boolean addon,
                        String textureNamespace, String addonTexturePath, String oreDictionary) {
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
        this.addon = addon;
        this.textureNamespace = textureNamespace == null ? "" : textureNamespace;
        this.addonTexturePath = addonTexturePath == null ? "" : addonTexturePath;
        this.oreDictionary = oreDictionary == null ? "" : oreDictionary;
        BlockPrefixRegistry.register(this);
    }

    public static void bootstrap() {
        gregtech.data.MaterialPrefixes.bootstrap();
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
    public boolean isAddon() { return addon; }
    public String textureNamespace() { return textureNamespace; }
    public String addonTexturePath() { return addonTexturePath; }
    /** Tag path under {@code forge:}, or empty for a built-in prefix. */
    public String oreDictionary() { return oreDictionary; }
    /** Prefix name card 04 reports for an addon block. Built-in blocks stay unnamed. */
    public String compositionName() { return addon ? name : ""; }
    public float baseHardness() { return baseHardness; }
    public float baseResistance() { return baseResistance; }
    public int harvestLevelOffset() { return harvestLevelOffset; }
    public MapColor mapColor() { return mapColor; }
    public SoundType soundType() { return soundType; }
    public boolean falling() { return falling; }

    public boolean isValidFor(GTMaterial material) {
        return material != null && material.isValid() && !material.has(gregtech.api.material.MaterialProperty.HIDDEN)
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
