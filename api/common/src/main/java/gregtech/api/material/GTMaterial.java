/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.mod.ModData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Core material definition, analogous to GregTech {@code OreDictMaterial}.
 * <p>
 * Host-side mutable runtime model. Addons declare materials through
 * {@link gregtech.api.addon.material.MaterialDeclaration} and read them through
 * {@link gregtech.api.addon.material.GregTechMaterials.Material}.
 * <p>
 * Internal: not part of the addon contract. The setters are for host initialization and are not
 * thread-safe; addons must not configure shared materials. A frozen instance (the sentinels in
 * {@link MaterialSentinels}) rejects every mutator with {@link IllegalStateException}.
 * <p>
 * Material-to-material links are stored as declared and resolved by the getters, so a
 * {@link #setRegistration(GTMaterial)} applied after the link is still visible. Registration
 * cycles are rejected when they are created.
 */
@ApiStatus.Internal
public final class GTMaterial {
    public static final int INVALID_ID = -1;
    public static final int NULL_ID = 0;
    private static final int MAX_ALPHA = 255;

    private final int id;
    private final String name;
    private String localName;
    private String displayName;
    private boolean displayNameExplicit;
    private @Nullable ModData sourceMod;
    private final int color;
    private int alpha = MAX_ALPHA;
    private AtomicProperties atomicProperties = AtomicProperties.GT6_DEFAULT;
    private @Nullable MaterialTextureSet textureSet;
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
    private boolean frozen;

    public GTMaterial(int id, String name, String localName, int color) {
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
        checkMutable();
        atomicProperties = java.util.Objects.requireNonNull(value);
        return this;
    }

    public GTMaterial setAlpha(int value) {
        checkMutable();
        if (value < 0 || value > MAX_ALPHA) throw new IllegalArgumentException("Alpha must be 0.." + MAX_ALPHA);
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

    /** Null when no texture set was declared. */
    public @Nullable MaterialTextureSet getTextureSet() {
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

    /** Resolved on read; null when no burning target was set. */
    public @Nullable GTMaterial getTargetBurningMaterial() {
        return targetBurningMaterial == null ? null : targetBurningMaterial.resolve();
    }

    public long getTargetBurningAmount() {
        return targetBurningAmount;
    }

    /** GT6 {@code mByProducts}; ordered ore-processing byproducts, resolved on read. */
    public List<GTMaterial> getByProducts() {
        if (byProducts.isEmpty()) return List.of();
        List<GTMaterial> resolved = new ArrayList<>(byProducts.size());
        for (GTMaterial material : byProducts) {
            resolved.add(material.resolve());
        }
        return List.copyOf(resolved);
    }

    /** Ordinary material recycling, distinct from ore crushing (GT6 mTargetPulver); resolved on read. */
    public GTMaterial getTargetPulverMaterial() { return targetPulverMaterial.resolve(); }
    public long getTargetPulverAmount() { return targetPulverAmount; }
    /**
     * Null material means this; a negative amount is stored as zero. The material is stored as
     * declared and resolved by the getter, so a later registration of it stays visible.
     */
    public GTMaterial setPulver(@Nullable GTMaterial material, long amount) {
        checkMutable();
        targetPulverMaterial = material == null ? this : material;
        targetPulverAmount = Math.max(0, amount);
        return this;
    }

    /** Resolved on read; a registration applied after {@link #setCrushing} is still visible. */
    public GTMaterial getTargetCrushingMaterial() {
        return targetCrushingMaterial.resolve();
    }

    public long getTargetCrushingAmount() {
        return targetCrushingAmount;
    }

    /** Resolved on read; a registration applied after {@link #setSmelting} is still visible. */
    public GTMaterial getTargetSmeltingMaterial() {
        return targetSmeltingMaterial.resolve();
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

    /**
     * GT6 {@code setLocalName}. A null name throws {@link NullPointerException}. The local name is
     * the material's alternate name: a value without a space is an identifier or alias and leaves
     * the display name alone, while a literal name (one containing a space) also becomes the
     * display name, unless {@link #setDisplayName(String)} already chose one.
     */
    public GTMaterial setLocalName(String localName) {
        checkMutable();
        java.util.Objects.requireNonNull(localName, "localName");
        this.localName = localName;
        if (!displayNameExplicit && localName.contains(" ")) {
            this.displayName = localName;
        }
        return this;
    }

    /** Null until {@link #setSourceMod(ModData)} names one. */
    public @Nullable ModData getSourceMod() {
        return sourceMod;
    }

    public String getSourceModName() {
        return sourceMod != null ? sourceMod.name : "";
    }

    /** Marks which mod this material originates from (GT6 {@code .put(MD.MC)}). */
    public GTMaterial setSourceMod(ModData sourceMod) {
        checkMutable();
        this.sourceMod = sourceMod;
        return this;
    }

    /** Alias for {@link #setSourceMod(ModData)}  - reads like GT6 {@code .put(MD.MC)}. */
    public GTMaterial mod(ModData sourceMod) {
        checkMutable();
        return setSourceMod(sourceMod);
    }

    /** Chooses the display name explicitly; a later {@link #setLocalName(String)} leaves it alone. */
    public GTMaterial setDisplayName(String displayName) {
        checkMutable();
        this.displayName = displayName;
        this.displayNameExplicit = true;
        return this;
    }

    public GTMaterial setTextureSet(MaterialTextureSet textureSet) {
        checkMutable();
        this.textureSet = textureSet;
        return this;
    }

    /**
     * Temperatures are in Kelvin and density is in g/cm³. Melting point, boiling point and density
     * must be non-negative, and density must be finite; anything else throws
     * {@link IllegalArgumentException}, matching {@code MaterialDeclaration}.
     */
    public GTMaterial setStats(int meltingPoint, int boilingPoint, float density) {
        checkMutable();
        if (meltingPoint < 0 || boilingPoint < 0 || !Float.isFinite(density) || density < 0) {
            throw new IllegalArgumentException("Invalid material stats for " + name + ": melting " + meltingPoint
                    + ", boiling " + boilingPoint + ", density " + density);
        }
        this.meltingPoint = meltingPoint;
        this.boilingPoint = boilingPoint;
        this.density = density;
        return this;
    }

    public GTMaterial setTooltipChemical(String tooltipChemical) {
        checkMutable();
        if (tooltipChemical == null)
            throw new IllegalArgumentException("Chemical tooltip is missing; pass an empty string to clear it");
        this.tooltipChemical = tooltipChemical;
        return this;
    }

    public boolean hasComposition() {
        return !compositionComponents.isEmpty();
    }

    public long getCompositionDivider() {
        return compositionDivider;
    }

    /** Composition with each component's material resolved through {@link #resolve()}. */
    public List<MaterialComponent> getCompositionComponents() {
        if (compositionComponents.isEmpty()) return List.of();
        List<MaterialComponent> resolved = new ArrayList<>(compositionComponents.size());
        for (MaterialComponent component : compositionComponents) {
            GTMaterial material = component.material().resolve();
            resolved.add(material == component.material()
                    ? component
                    : new MaterialComponent(material, component.amount()));
        }
        return List.copyOf(resolved);
    }

    public boolean isValid() {
        return id > 0;
    }

    /**
     * Replaces the composition. Components with a null or invalid material are dropped, so the
     * sentinel a {@code MaterialComponent(null, ...)} stores never reaches this list. A divider
     * of zero or less is computed from the total amount and is at least one.
     */
    public GTMaterial setComposition(long divider, MaterialComponent... components) {
        checkMutable();
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

    /**
     * Tooltip chemical of the resolved registration target, identical to
     * {@link #getTooltipChemical()} when this material is not an alias.
     */
    public String getResolvedTooltipChemical() {
        return resolve().tooltipChemical;
    }

    /** {@code qual(toolTypes, speed, durability, quality)} in GT6. */
    public GTMaterial setToolStats(int toolTypes, float toolSpeed, long toolDurability, int toolQuality) {
        checkMutable();
        this.toolTypes = toolTypes;
        this.toolSpeed = toolSpeed;
        this.toolDurability = toolDurability;
        this.toolQuality = toolQuality;
        return this;
    }

    /**
     * GT6 {@code qual(long)}: change harvest quality without inventing tool types or speed.
     * Like the source delegating qual overload, existing durability is clamped to at least one,
     * and tool types are clamped to 0..3; the quality is clamped to 0..15. Source tool-generation
     * flags are handled by the separate material form policy.
     */
    public GTMaterial setToolQuality(int toolQuality) {
        checkMutable();
        return setToolStats(Math.max(0, Math.min(3, this.toolTypes)), this.toolSpeed,
                Math.max(1L, this.toolDurability), Math.max(0, Math.min(15, toolQuality)));
    }

    /** GT6 {@code setFurnaceBurnTime}; a negative value is stored as zero. */
    public GTMaterial setFurnaceBurnTime(long burnTime) {
        checkMutable();
        this.furnaceBurnTime = Math.max(0L, burnTime);
        return this;
    }

    /** GT6 {@code setBurning}; ash left after burning one item-worth of this material. A negative amount is zero. */
    public GTMaterial setBurning(@Nullable GTMaterial material, long amount) {
        checkMutable();
        this.targetBurningMaterial = material;
        this.targetBurningAmount = Math.max(0L, amount);
        return this;
    }

    /** GT6 {@code ores}; appends ordered ore-processing byproducts. */
    public GTMaterial ores(GTMaterial... materials) {
        checkMutable();
        addOreByProducts(materials);
        return this;
    }

    /** GT6 {@code addOreByProducts}. */
    public void addOreByProducts(GTMaterial... materials) {
        checkMutable();
        if (materials == null) return;
        for (GTMaterial material : materials) {
            if (material != null) {
                byProducts.add(material);
            }
        }
    }

    /**
     * GT6 {@code setCrushing}; result of crushing this. Amount 0 disables; null material means this.
     * A negative amount is stored as zero.
     */
    public GTMaterial setCrushing(@Nullable GTMaterial material, long amount) {
        checkMutable();
        this.targetCrushingMaterial = material == null ? this : material;
        this.targetCrushingAmount = Math.max(0L, amount);
        return this;
    }

    /**
     * GT6 {@code setSmelting}; result of smelting this. Amount 0 disables; null material means this.
     * A negative amount is stored as zero.
     */
    public GTMaterial setSmelting(@Nullable GTMaterial material, long amount) {
        checkMutable();
        this.targetSmeltingMaterial = material == null ? this : material;
        this.targetSmeltingAmount = Math.max(0L, amount);
        if (this.targetSmeltingAmount > 0) {
            put(MaterialProperty.MELTING);
        }
        return this;
    }

    /** Drops the smelting target and its {@link MaterialProperty#MELTING} flag. */
    public GTMaterial clearSmeltingTarget() {
        checkMutable();
        this.targetSmeltingMaterial = this;
        this.targetSmeltingAmount = 0;
        this.properties.remove(MaterialProperty.MELTING);
        return this;
    }

    /** GT6 {@code setOreMultiplier}; crushed-ore output multiplier, clamped to 1..127. */
    public GTMaterial setOreMultiplier(int multiplier) {
        checkMutable();
        this.oreMultiplier = (byte) Math.min(Byte.MAX_VALUE, Math.max(1, multiplier));
        return this;
    }

    /** GT6 {@code setOreProcessingMultiplier}; ore-processing output multiplier, clamped to 1..127. */
    public GTMaterial setOreProcessingMultiplier(int multiplier) {
        checkMutable();
        this.oreProcessingMultiplier = (byte) Math.min(Byte.MAX_VALUE, Math.max(1, multiplier));
        return this;
    }

    public GTMaterial put(MaterialProperty... flags) {
        checkMutable();
        Collections.addAll(properties, flags);
        return this;
    }

    public GTMaterial hide() {
        checkMutable();
        return put(MaterialProperty.HIDDEN);
    }

    public GTMaterial addReRegistrationToThis(GTMaterial... materials) {
        checkMutable();
        for (GTMaterial material : materials) {
            if (material != null) material.checkMutable();
        }
        for (GTMaterial material : materials) {
            if (material != null && reRegistrations.add(material)) {
                material.aliasesToThis.add(this);
            }
        }
        return this;
    }

    /**
     * GT6 {@code setRegistration}: makes this material an alias of {@code target}. The target is
     * stored as declared, so a later re-registration of it is still visible through
     * {@link #resolve()}. A target whose chain already leads back to this material throws
     * {@link IllegalArgumentException}.
     */
    public GTMaterial setRegistration(GTMaterial target) {
        checkMutable();
        if (target == null || target == this) {
            this.targetRegistration = this;
            return put(MaterialProperty.HIDDEN);
        }
        for (GTMaterial current = target; current != null; current = current.targetRegistration) {
            if (current == this) {
                throw new IllegalArgumentException("Material registration cycle at " + name);
            }
            if (current.targetRegistration == current) break;
        }
        this.targetRegistration = target;
        return put(MaterialProperty.HIDDEN);
    }

    public String getTranslationKey() {
        return "material.gt6ce." + name.toLowerCase(Locale.ROOT);
    }

    /** Freezes every mutator; only {@link MaterialSentinels} uses this, after initialization. */
    GTMaterial freeze() {
        frozen = true;
        return this;
    }

    private void checkMutable() {
        if (frozen) throw new IllegalStateException("Material " + name + " is frozen");
    }

    @Override
    public String toString() {
        return "GTMaterial{" + name + ", id=" + id + '}';
    }
}
