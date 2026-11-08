/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.FuelRule;
import gregtech.api.addon.material.MaterialComposition;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.addon.material.MaterialTarget;
import gregtech.api.addon.material.OreProcessingRule;
import gregtech.api.mod.ModData;
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
 * Internal compatibility model. The canonical authoring model is
 * {@code MaterialDefinition}; new addon reads use immutable catalog views. The setters are for host
 * initialization and are not thread-safe. Every material is frozen before the registry becomes
 * READY, and a frozen instance rejects every mutator with {@link IllegalStateException}.
 * <p>
 * Material-to-material links are stored as declared and resolved by the getters, so a
 * {@link #setRegistration(GTMaterial)} applied after the link is still visible. Registration
 * cycles are rejected when they are created.
 */
@ApiStatus.Internal
@Deprecated(since = "1.7.0", forRemoval = false)
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
    }

    public static String sanitize(String name) {
        // Most lookups already pass a registry name; avoid three allocations for the common case.
        if (name.indexOf(' ') < 0 && name.indexOf('-') < 0 && name.indexOf('\'') < 0) return name;
        return name.replace(" ", "").replace("-", "").replace("'", "");
    }

    public int getId() {
        return id;
    }

    public AtomicProperties getAtomicProperties() {
        return atomicProperties;
    }

    public long getProtons() {
        return atomicProperties.protons();
    }

    public long getElectrons() {
        return atomicProperties.electrons();
    }

    public long getNeutrons() {
        return atomicProperties.neutrons();
    }

    public long getMass() {
        return atomicProperties.mass();
    }

    public int getAlpha() {
        return alpha;
    }

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

    @Deprecated(since = "1.5.0", forRemoval = false)
    public int getToolTypes() {
        return MaterialToolHelper.legacyTypes(this);
    }

    @Deprecated(since = "1.5.0", forRemoval = false)
    public int getToolQuality() {
        return MaterialToolHelper.legacyQuality(this);
    }

    @Deprecated(since = "1.5.0", forRemoval = false)
    public float getToolSpeed() {
        return MaterialToolHelper.legacySpeed(this);
    }

    @Deprecated(since = "1.5.0", forRemoval = false)
    public long getToolDurability() {
        return MaterialToolHelper.legacyDurability(this);
    }

    /** GT6 {@code mFurnaceBurnTime}. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public long getFurnaceBurnTime() {
        return currentTarget(MaterialProcessKeys.FUEL).map(FuelRule::burnTime).orElse(0L);
    }

    /** Resolved on read; null when no burning target was set. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public @Nullable GTMaterial getTargetBurningMaterial() {
        return currentTarget(MaterialProcessKeys.FUEL)
                .flatMap(FuelRule::residue)
                .flatMap(target -> MaterialInfoTool.findMaterial(target.material()))
                .map(GTMaterial::resolve)
                .orElse(null);
    }

    @Deprecated(since = "1.5.0", forRemoval = false)
    public long getTargetBurningAmount() {
        return currentTarget(MaterialProcessKeys.FUEL)
                .flatMap(FuelRule::residue)
                .map(MaterialTarget::amount)
                .orElse(0L);
    }

    /** GT6 {@code mByProducts}; ordered ore-processing byproducts, resolved on read. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public List<GTMaterial> getByProducts() {
        return currentTarget(MaterialProcessKeys.ORE_PROCESSING)
                .map(OreProcessingRule::byproducts)
                .orElse(List.of())
                .stream()
                .flatMap(key -> MaterialInfoTool.findMaterial(key).stream())
                .map(GTMaterial::resolve)
                .toList();
    }

    /** Ordinary material recycling, distinct from ore crushing (GT6 mTargetPulver); resolved on read. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial getTargetPulverMaterial() {
        return targetOrSelf(MaterialProcessKeys.PULVERIZING);
    }

    @Deprecated(since = "1.5.0", forRemoval = false)
    public long getTargetPulverAmount() {
        return targetAmount(MaterialProcessKeys.PULVERIZING);
    }
    /**
     * Null material means this; a negative amount is stored as zero. The material is stored as
     * declared and resolved by the getter, so a later registration of it stays visible.
     */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setPulver(@Nullable GTMaterial material, long amount) {
        checkMutable();
        return updateTarget(MaterialProcessKeys.PULVERIZING, material, amount);
    }

    /** Resolved on read; a registration applied after {@link #setCrushing} is still visible. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial getTargetCrushingMaterial() {
        return targetOrSelf(MaterialProcessKeys.CRUSHING);
    }

    @Deprecated(since = "1.5.0", forRemoval = false)
    public long getTargetCrushingAmount() {
        return targetAmount(MaterialProcessKeys.CRUSHING);
    }

    /** Resolved on read; a registration applied after {@link #setSmelting} is still visible. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial getTargetSmeltingMaterial() {
        return targetOrSelf(MaterialProcessKeys.SMELTING);
    }

    @Deprecated(since = "1.5.0", forRemoval = false)
    public long getTargetSmeltingAmount() {
        return targetAmount(MaterialProcessKeys.SMELTING);
    }

    /** GT6 {@code mOreMultiplier}. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public byte getOreMultiplier() {
        return (byte) (int) currentTarget(MaterialProcessKeys.ORE_PROCESSING)
                .map(OreProcessingRule::oreMultiplier)
                .orElse(1);
    }

    /** GT6 {@code mOreProcessingMultiplier}. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public byte getOreProcessingMultiplier() {
        return (byte) (int) currentTarget(MaterialProcessKeys.ORE_PROCESSING)
                .map(OreProcessingRule::processingMultiplier)
                .orElse(1);
    }

    public boolean hasToolStats() {
        return tool().types() > 0;
    }

    /** @deprecated Read tool data through {@link MaterialToolHelper#profile(MaterialKey)}. */
    @Deprecated(since = "1.7.0", forRemoval = false)
    public MaterialToolProfile tool() {
        return MaterialToolHelper.profile(this);
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
        return infoKey().flatMap(MaterialInfoTool::currentComposition).isPresent();
    }

    public long getCompositionDivider() {
        return infoKey()
                .flatMap(MaterialInfoTool::currentComposition)
                .map(MaterialComposition::divider)
                .orElse(0L);
    }

    /** Composition with each component's material resolved through {@link #resolve()}. */
    public List<MaterialComponent> getCompositionComponents() {
        return infoKey()
                .flatMap(MaterialInfoTool::currentComposition)
                .map(MaterialComposition::parts)
                .orElse(List.of())
                .stream()
                .flatMap(part -> MaterialInfoTool.findMaterial(new MaterialKey(part.material()))
                        .stream()
                        .map(material -> new MaterialComponent(material.resolve(), part.amount())))
                .toList();
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
        var parts = new java.util.ArrayList<MaterialComposition.Part>();
        if (components != null) {
            for (MaterialComponent component : components) {
                if (component != null && component.material().isValid() && component.amount() > 0) {
                    parts.add(new MaterialComposition.Part(
                            component.material().resolve().getName(), component.amount()));
                }
            }
        }
        if (parts.isEmpty()) {
            MaterialInfoTool.updateComposition(materialKey(), java.util.Optional.empty());
            return this;
        }
        if (divider <= 0) {
            long total = 0;
            for (var component : parts) total = Math.addExact(total, component.amount());
            divider = Math.max(1L, total / GTValues.U);
        }
        boolean crucible = MaterialInfoTool.currentComposition(materialKey())
                .map(MaterialComposition::crucible)
                .orElse(false);
        MaterialInfoTool.updateComposition(
                materialKey(),
                java.util.Optional.of(
                        new MaterialComposition(materialKey().name(), divider, parts, crucible)));
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
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setToolStats(int toolTypes, float toolSpeed, long toolDurability, int toolQuality) {
        checkMutable();
        MaterialInfoTool.updateTool(
                materialKey(), new MaterialToolProfile(toolTypes, toolSpeed, toolDurability, toolQuality));
        return this;
    }

    /**
     * GT6 {@code qual(long)}: change harvest quality without inventing tool types or speed.
     * Like the source delegating qual overload, existing durability is clamped to at least one,
     * and tool types are clamped to 0..3; the quality is clamped to 0..15. Source tool-generation
     * flags are handled by the separate material form policy.
     */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setToolQuality(int toolQuality) {
        checkMutable();
        MaterialToolProfile profile = tool();
        return setToolStats(
                Math.max(0, Math.min(3, profile.types())),
                profile.speed(),
                Math.max(1L, profile.durability()),
                Math.max(0, Math.min(15, toolQuality)));
    }

    /** GT6 {@code setFurnaceBurnTime}; a negative value is stored as zero. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setFurnaceBurnTime(long burnTime) {
        checkMutable();
        FuelRule current = currentTarget(MaterialProcessKeys.FUEL).orElse(new FuelRule(0L, java.util.Optional.empty()));
        return updateFuel(new FuelRule(Math.max(0L, burnTime), current.residue()));
    }

    /** GT6 {@code setBurning}; ash left after burning one item-worth of this material. A negative amount is zero. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setBurning(@Nullable GTMaterial material, long amount) {
        checkMutable();
        FuelRule current = currentTarget(MaterialProcessKeys.FUEL).orElse(new FuelRule(0L, java.util.Optional.empty()));
        long residueAmount = Math.max(0L, amount);
        var residue = material != null && residueAmount > 0
                ? java.util.Optional.of(new MaterialTarget(
                        new MaterialKey(material.resolve().getName()), residueAmount))
                : java.util.Optional.<MaterialTarget>empty();
        return updateFuel(new FuelRule(current.burnTime(), residue));
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
        OreProcessingRule current = currentTarget(MaterialProcessKeys.ORE_PROCESSING)
                .orElse(new OreProcessingRule(List.of(), 1, 1));
        var byproducts = new java.util.ArrayList<>(current.byproducts());
        for (GTMaterial material : materials) {
            if (material != null)
                byproducts.add(new MaterialKey(material.resolve().getName()));
        }
        updateProcess(
                MaterialProcessKeys.ORE_PROCESSING,
                new OreProcessingRule(byproducts, current.oreMultiplier(), current.processingMultiplier()));
    }

    /**
     * GT6 {@code setCrushing}; result of crushing this. Amount 0 disables; null material means this.
     * A negative amount is stored as zero.
     */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setCrushing(@Nullable GTMaterial material, long amount) {
        checkMutable();
        return updateTarget(MaterialProcessKeys.CRUSHING, material, amount);
    }

    /**
     * GT6 {@code setSmelting}; result of smelting this. Amount 0 disables; null material means this.
     * A negative amount is stored as zero.
     */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setSmelting(@Nullable GTMaterial material, long amount) {
        checkMutable();
        long targetAmount = Math.max(0L, amount);
        updateTarget(MaterialProcessKeys.SMELTING, material, targetAmount);
        if (targetAmount > 0) put(MaterialProperty.MELTING);
        return this;
    }

    /** Drops the smelting target and its {@link MaterialProperty#MELTING} flag. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial clearSmeltingTarget() {
        checkMutable();
        updateProcess(MaterialProcessKeys.SMELTING, null);
        this.properties.remove(MaterialProperty.MELTING);
        return this;
    }

    /** GT6 {@code setOreMultiplier}; crushed-ore output multiplier, clamped to 1..127. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setOreMultiplier(int multiplier) {
        checkMutable();
        OreProcessingRule current = currentTarget(MaterialProcessKeys.ORE_PROCESSING)
                .orElse(new OreProcessingRule(List.of(), 1, 1));
        return updateProcess(
                MaterialProcessKeys.ORE_PROCESSING,
                new OreProcessingRule(
                        current.byproducts(),
                        Math.min(Byte.MAX_VALUE, Math.max(1, multiplier)),
                        current.processingMultiplier()));
    }

    /** GT6 {@code setOreProcessingMultiplier}; ore-processing output multiplier, clamped to 1..127. */
    @Deprecated(since = "1.5.0", forRemoval = false)
    public GTMaterial setOreProcessingMultiplier(int multiplier) {
        checkMutable();
        OreProcessingRule current = currentTarget(MaterialProcessKeys.ORE_PROCESSING)
                .orElse(new OreProcessingRule(List.of(), 1, 1));
        return updateProcess(
                MaterialProcessKeys.ORE_PROCESSING,
                new OreProcessingRule(
                        current.byproducts(),
                        current.oreMultiplier(),
                        Math.min(Byte.MAX_VALUE, Math.max(1, multiplier))));
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

    private MaterialKey materialKey() {
        return new MaterialKey(resolve().getName());
    }

    private java.util.Optional<MaterialKey> infoKey() {
        try {
            return java.util.Optional.of(materialKey());
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }

    private <T> java.util.Optional<T> currentTarget(
            gregtech.api.addon.material.MaterialProcessKey<T> process) {
        return infoKey()
                .flatMap(key -> MaterialInfoTool.currentProcess(key, process))
                .map(MaterialProcessRule::value);
    }

    private GTMaterial targetOrSelf(gregtech.api.addon.material.MaterialProcessKey<MaterialTarget> process) {
        return currentTarget(process)
                .flatMap(target -> MaterialInfoTool.findMaterial(target.material()))
                .map(GTMaterial::resolve)
                .orElse(this);
    }

    private long targetAmount(gregtech.api.addon.material.MaterialProcessKey<MaterialTarget> process) {
        return currentTarget(process).map(MaterialTarget::amount).orElse(GTValues.U);
    }

    private GTMaterial updateTarget(
            gregtech.api.addon.material.MaterialProcessKey<MaterialTarget> process,
            @Nullable GTMaterial target,
            long amount) {
        GTMaterial resolved = target == null ? this : target.resolve();
        updateProcess(
                process,
                new MaterialTarget(new MaterialKey(resolved.getName()), Math.max(0L, amount)));
        return this;
    }

    private <T> GTMaterial updateProcess(
            gregtech.api.addon.material.MaterialProcessKey<T> process, @Nullable T value) {
        MaterialInfoTool.updateProcess(
                materialKey(),
                process,
                value == null
                        ? java.util.Optional.empty()
                        : java.util.Optional.of(MaterialProcessRule.anyForm(value)));
        return this;
    }

    private GTMaterial updateFuel(FuelRule rule) {
        if (rule.burnTime() <= 0 && rule.residue().isEmpty()) {
            return updateProcess(MaterialProcessKeys.FUEL, null);
        }
        return updateProcess(MaterialProcessKeys.FUEL, rule);
    }

    public String getTranslationKey() {
        return "material.gt6ce." + name.toLowerCase(Locale.ROOT);
    }

    /** Freezes every mutator before the material registry becomes READY. */
    @ApiStatus.Internal
    public GTMaterial freeze() {
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
