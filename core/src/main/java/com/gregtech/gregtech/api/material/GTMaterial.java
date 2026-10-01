package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.api.mod.ModData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Core material definition, analogous to GregTech {@code OreDictMaterial}.
 */
public final class GTMaterial {
    public static final int INVALID_ID = -1;
    public static final int NULL_ID = 0;

    private final int id;
    private final String name;
    private String localName;
    private String displayName;
    private ModData sourceMod;
    private final int color;
    private int alpha = 255;
    private AtomicProperties atomicProperties = AtomicProperties.GT6_DEFAULT;
    private MaterialTextureSet textureSet;
    private int meltingPoint = 300;
    private int boilingPoint = 1000;
    private float density = 1.0F;
    private String tooltipChemical = "";
    private long compositionDivider;
    private final List<MaterialComponent> compositionComponents = new ArrayList<>();
    private int toolTypes = 0;
    private int toolQuality = 0;
    private float toolSpeed = 1.0F;
    private long toolDurability = 0;
    /** GT6 {@code mFurnaceBurnTime}; HU = value * CS.EU_PER_FURNACE_TICK. */
    private long furnaceBurnTime = 0;
    private GTMaterial targetBurningMaterial;
    private long targetBurningAmount = 0;
    /** GT6 {@code mByProducts}; ordered list of ore-processing byproduct materials. */
    private final List<GTMaterial> byProducts = new ArrayList<>();
    /** GT6 {@code mTargetCrushing}; default is (this, U). */
    private GTMaterial targetPulverMaterial;
    private long targetPulverAmount = GTValues.U;
    private GTMaterial targetCrushingMaterial;
    private long targetCrushingAmount;
    /** GT6 {@code mTargetSmelting}; default is (this, U). */
    private GTMaterial targetSmeltingMaterial;
    private long targetSmeltingAmount;
    /** GT6 {@code mOreMultiplier}; crushed-ore output multiplier. */
    private byte oreMultiplier = 1;
    /** GT6 {@code mOreProcessingMultiplier}. */
    private byte oreProcessingMultiplier = 1;
    private final Set<MaterialProperty> properties = EnumSet.noneOf(MaterialProperty.class);
    private final Set<GTMaterial> reRegistrations = new HashSet<>();
    private final Set<GTMaterial> aliasesToThis = new HashSet<>();
    private GTMaterial targetRegistration;

    GTMaterial(int id, String name, String localName, int color) {
        this.id = id;
        this.name = sanitize(name);
        this.localName = localName == null ? this.name : localName;
        this.displayName = this.localName;
        this.color = color;
        this.targetRegistration = this;
        this.targetPulverMaterial = this;
        this.targetCrushingMaterial = this;
        this.targetCrushingAmount = GTValues.U;
        this.targetSmeltingMaterial = this;
        this.targetSmeltingAmount = GTValues.U;
    }

    public static String sanitize(String name) {
        return name.replace(" ", "").replace("-", "").replace("'", "");
    }

    public int getId() {
        return id;
    }

    public AtomicProperties getAtomicProperties() { return atomicProperties; }
    public long getProtons() { return atomicProperties.protons(); }
    public long getElectrons() { return atomicProperties.electrons(); }
    public long getNeutrons() { return atomicProperties.neutrons(); }
    public long getMass() { return atomicProperties.mass(); }
    public int getAlpha() { return alpha; }

    public GTMaterial setAtomicProperties(AtomicProperties value) {
        atomicProperties = java.util.Objects.requireNonNull(value);
        return this;
    }

    public GTMaterial setAlpha(int value) {
        if (value < 0 || value > 255) throw new IllegalArgumentException("Alpha must be 0..255");
        alpha = value;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getLocalName() {
        return localName;
    }

    public String getDisplayNameFallback() {
        return displayName != null ? displayName : localName;
    }

    public int getColor() {
        return color;
    }

    public MaterialTextureSet getTextureSet() {
        return textureSet;
    }

    public int getMeltingPoint() {
        return meltingPoint;
    }

    public int getBoilingPoint() {
        return boilingPoint;
    }

    public float getDensity() {
        return density;
    }

    public String getTooltipChemical() {
        return tooltipChemical;
    }

    public int getToolTypes() {
        return toolTypes;
    }

    public int getToolQuality() {
        return toolQuality;
    }

    public float getToolSpeed() {
        return toolSpeed;
    }

    public long getToolDurability() {
        return toolDurability;
    }

    /** GT6 {@code mFurnaceBurnTime}. */
    public long getFurnaceBurnTime() {
        return furnaceBurnTime;
    }

    public GTMaterial getTargetBurningMaterial() {
        return targetBurningMaterial;
    }

    public long getTargetBurningAmount() {
        return targetBurningAmount;
    }

    /** GT6 {@code mByProducts}; ordered ore-processing byproducts. */
    public List<GTMaterial> getByProducts() {
        return Collections.unmodifiableList(byProducts);
    }

    /** Ordinary material recycling, distinct from ore crushing (GT6 mTargetPulver). */
    public GTMaterial getTargetPulverMaterial() { return targetPulverMaterial; }
    public long getTargetPulverAmount() { return targetPulverAmount; }
    public GTMaterial setPulver(GTMaterial material, long amount) {
        targetPulverMaterial = material == null ? this : material.resolve();
        targetPulverAmount = Math.max(0, amount);
        return this;
    }

    public GTMaterial getTargetCrushingMaterial() {
        return targetCrushingMaterial;
    }

    public long getTargetCrushingAmount() {
        return targetCrushingAmount;
    }

    public GTMaterial getTargetSmeltingMaterial() {
        return targetSmeltingMaterial;
    }

    public long getTargetSmeltingAmount() {
        return targetSmeltingAmount;
    }

    /** GT6 {@code mOreMultiplier}. */
    public byte getOreMultiplier() {
        return oreMultiplier;
    }

    /** GT6 {@code mOreProcessingMultiplier}. */
    public byte getOreProcessingMultiplier() {
        return oreProcessingMultiplier;
    }

    public boolean hasToolStats() {
        return toolTypes > 0;
    }

    public Set<MaterialProperty> getProperties() {
        return Collections.unmodifiableSet(properties);
    }

    public Set<GTMaterial> getReRegistrations() {
        return Collections.unmodifiableSet(reRegistrations);
    }

    public Set<GTMaterial> getAliasesToThis() {
        return Collections.unmodifiableSet(aliasesToThis);
    }

    public GTMaterial resolve() {
        GTMaterial current = this;
        while (current.targetRegistration != null && current.targetRegistration != current) {
            current = current.targetRegistration;
        }
        return current;
    }

    public boolean has(MaterialProperty property) {
        return properties.contains(property);
    }

    public boolean hasAny(MaterialProperty... flags) {
        for (MaterialProperty flag : flags) {
            if (properties.contains(flag)) return true;
        }
        return false;
    }

    public GTMaterial setLocalName(String localName) {
        this.localName = localName;
        if (localName.contains(" ")) {
            this.displayName = localName;
        }
        return this;
    }

    public ModData getSourceMod() {
        return sourceMod;
    }

    public String getSourceModName() {
        return sourceMod != null ? sourceMod.name : "";
    }

    /** Marks which mod this material originates from (GT6 {@code .put(MD.MC)}). */
    public GTMaterial setSourceMod(ModData sourceMod) {
        this.sourceMod = sourceMod;
        return this;
    }

    /** Alias for {@link #setSourceMod(ModData)}  - reads like GT6 {@code .put(MD.MC)}. */
    public GTMaterial mod(ModData sourceMod) {
        return setSourceMod(sourceMod);
    }

    public GTMaterial setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public GTMaterial setTextureSet(MaterialTextureSet textureSet) {
        this.textureSet = textureSet;
        return this;
    }

    public GTMaterial setStats(int meltingPoint, int boilingPoint, float density) {
        this.meltingPoint = meltingPoint;
        this.boilingPoint = boilingPoint;
        this.density = density;
        return this;
    }

    public GTMaterial setTooltipChemical(String tooltipChemical) {
        this.tooltipChemical = tooltipChemical == null ? "" : tooltipChemical;
        return this;
    }

    public boolean hasComposition() {
        return !compositionComponents.isEmpty();
    }

    public long getCompositionDivider() {
        return compositionDivider;
    }

    public List<MaterialComponent> getCompositionComponents() {
        return Collections.unmodifiableList(compositionComponents);
    }

    public boolean isValid() {
        return id > 0;
    }

    public GTMaterial setComposition(long divider, MaterialComponent... components) {
        compositionComponents.clear();
        if (components != null) {
            for (MaterialComponent component : components) {
                if (component != null && component.material().isValid()) {
                    compositionComponents.add(component);
                }
            }
        }
        if (divider <= 0 && !compositionComponents.isEmpty()) {
            long total = 0;
            for (MaterialComponent component : compositionComponents) {
                total += component.amount();
            }
            divider = Math.max(1L, total / GTValues.U);
        }
        compositionDivider = divider;
        return this;
    }

    public boolean isElementLike() {
        return has(MaterialProperty.ELEMENT) && !hasComposition();
    }

    /** Resolved tooltip chemical; uses composition tree when present. */
    public String getResolvedTooltipChemical() {
        return hasComposition() ? MaterialChemistry.buildFormula(this) : tooltipChemical;
    }

    /** {@code qual(toolTypes, speed, durability, quality)} in GT6. */
    public GTMaterial setToolStats(int toolTypes, float toolSpeed, long toolDurability, int toolQuality) {
        this.toolTypes = toolTypes;
        this.toolSpeed = toolSpeed;
        this.toolDurability = toolDurability;
        this.toolQuality = toolQuality;
        return this;
    }

    /** GT6 {@code setFurnaceBurnTime}. */
    public GTMaterial setFurnaceBurnTime(long burnTime) {
        this.furnaceBurnTime = Math.max(0L, burnTime);
        return this;
    }

    /** GT6 {@code setBurning}; ash left after burning one item-worth of this material. */
    public GTMaterial setBurning(GTMaterial material, long amount) {
        this.targetBurningMaterial = material == null ? null : material.resolve();
        this.targetBurningAmount = Math.max(0L, amount);
        return this;
    }

    /** GT6 {@code ores}; appends ordered ore-processing byproducts. */
    public GTMaterial ores(GTMaterial... materials) {
        addOreByProducts(materials);
        return this;
    }

    /** GT6 {@code addOreByProducts}. */
    public void addOreByProducts(GTMaterial... materials) {
        if (materials == null) return;
        for (GTMaterial material : materials) {
            if (material != null) {
                byProducts.add(material.resolve());
            }
        }
    }

    /** GT6 {@code setCrushing}; result of crushing this. Amount 0 disables; null material means this. */
    public GTMaterial setCrushing(GTMaterial material, long amount) {
        this.targetCrushingMaterial = material == null ? this : material.resolve();
        this.targetCrushingAmount = Math.max(0L, amount);
        return this;
    }

    /** GT6 {@code setSmelting}; result of smelting this. Amount 0 disables; null material means this. */
    public GTMaterial setSmelting(GTMaterial material, long amount) {
        this.targetSmeltingMaterial = material == null ? this : material.resolve();
        this.targetSmeltingAmount = Math.max(0L, amount);
        if (this.targetSmeltingAmount > 0) {
            put(MaterialProperty.MELTING);
        }
        return this;
    }

    /** GT6 {@code setOreMultiplier}; crushed-ore output multiplier, clamped to at least 1. */
    public GTMaterial setOreMultiplier(int multiplier) {
        this.oreMultiplier = (byte) Math.max(1, multiplier);
        return this;
    }

    public GTMaterial put(MaterialProperty... flags) {
        Collections.addAll(properties, flags);
        return this;
    }

    public GTMaterial hide() {
        return put(MaterialProperty.HIDDEN);
    }

    public GTMaterial addReRegistrationToThis(GTMaterial... materials) {
        for (GTMaterial material : materials) {
            if (material != null && reRegistrations.add(material)) {
                material.aliasesToThis.add(this);
            }
        }
        return this;
    }

    public GTMaterial setRegistration(GTMaterial target) {
        this.targetRegistration = target == null ? this : target.resolve();
        return put(MaterialProperty.HIDDEN);
    }

    public String getTranslationKey() {
        return "material.gregtech." + name.toLowerCase();
    }

    @Override
    public String toString() {
        return "GTMaterial{" + name + ", id=" + id + '}';
    }
}
