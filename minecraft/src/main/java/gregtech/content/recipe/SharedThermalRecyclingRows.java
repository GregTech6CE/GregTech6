package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.BlockMaterialWeights;
import gregtech.content.block.MaterialBlockItem;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneMaterialWeights;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.SourceRecyclingMath;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
//?}
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static gregtech.content.recipe.SourceRecyclingFacts.*;

/**
 * The general source {@code RecyclingProcessing} listener for every admitted recyclable item
 * composition. The bounded audited producer in {@link SharedThermalInputRows} keeps its 81
 * single-component prefixes; this producer owns the rest:
 *
 * <ul>
 *   <li>recyclable prefixes outside that audited group (pipes, wires, plants, ammunition,
 *       composite tool heads, machinery, storage blocks);</li>
 *   <li>multi-component compositions, where the listener converts every material stack, keeps the
 *       single one that has a liquid (Aerotheum: gas) phase, and proves the item mass for timing;</li>
 *   <li>null-prefix item data (block items, machine casings, transports, unified vanilla items).</li>
 * </ul>
 *
 * <p>Source: {@code OreDictManager:650-669} (recyclable admission), {@code Loader_OreProcessing:240-291}
 * (accumulate converted target stacks, choose one phase, weight-based duration, Melter eligibility).
 * Ore and dirty ore-processing prefixes are rejected before accumulation, as in the source.</p>
 */
final class SharedThermalRecyclingRows {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SOURCE_UNIT = gregtech.content.recipe.OriginalMaterialFluidUnits.SOURCE_UNIT;
    private static final Map<RecipeMap, Set<Recipe>> OWNED = new IdentityHashMap<>();
    private static final List<Deferred> DEFERRED = new ArrayList<>();
    private static boolean initialized;

    record Deferred(String source, String reason) {}

    /** Source {@code OP} prefixes carrying RECYCLABLE (pinned commit 3703e403). */
    private static final Set<String> RECYCLABLE = Set.of(
            "crushed", "crushedTiny", "crushedPurified", "crushedPurifiedTiny", "crushedCentrifuged",
            "crushedCentrifugedTiny", "rockGt", "rawOreChunk", "clump", "cluster", "pebbles", "rubble",
            "chunk", "crystalline", "reduced", "cleanGravel", "dirtyGravel", "dust", "dustSmall",
            "dustTiny", "dustDiv72", "ingotQuintuple", "ingotQuadruple", "ingotTriple", "ingotDouble",
            "ingot", "billet", "chunkGt", "nugget", "gem", "gemChipped", "gemFlawed", "gemFlawless",
            "gemExquisite", "gemLegendary", "bouleGt", "crystalPure", "crystal", "lens", "scrapGt",
            "plateSteamcraft", "plateDense", "plateQuintuple", "plateQuadruple", "plateTriple",
            "plateDouble", "plate", "plateGem", "plateTiny", "plateGemTiny", "plateCurved", "compressed",
            "sheetGt", "foil", "stick", "stickLong", "bolt", "screw", "round", "ring", "chain", "spring",
            "springSmall", "wireFine", "minecartWheels", "gearGt", "gearGtSmall", "railGt", "casingSmall",
            "casingMachine", "casingMachineDouble", "casingMachineQuadruple", "casingMachineDense", "rotor",
            "chemtube", "cell", "toolHeadSaw", "toolHeadFile", "toolHeadChisel", "toolHeadBuzzSaw",
            "toolHeadChainsaw", "toolHeadWrench", "toolHeadDrill", "toolHeadSword", "toolHeadPickaxe",
            "toolHeadShovel", "toolHeadSpade", "toolHeadAxe", "toolHeadHoe", "toolHeadSense",
            "toolHeadPlow", "toolHeadHammer", "toolHeadScrewdriver", "toolHeadBuilderwand",
            "toolHeadConstructionPickaxe", "toolHeadPickaxeGem", "toolHeadAxeDouble",
            "toolHeadUniversalSpade", "toolHeadArrow", "toolHeadRawSaw", "toolHeadRawChisel",
            "toolHeadRawSword", "toolHeadRawPickaxe", "toolHeadRawShovel", "toolHeadRawSpade",
            "toolHeadRawUniversalSpade", "toolHeadRawAxe", "toolHeadRawAxeDouble", "toolHeadRawHoe",
            "toolHeadRawSense", "toolHeadRawPlow", "toolHeadRawArrow", "toolSword", "toolPickaxe",
            "toolShovel", "toolAxe", "toolHoe", "toolShears", "bulletGtSmall", "bulletGtMedium",
            "bulletGtLarge", "arrowGtWood", "arrowGtPlastic", "armorHelmet", "armorChestplate",
            "armorLeggings", "armorBoots", "frameGt", "capcellcon", "pipeTiny", "pipeSmall",
            "pipeMedium", "pipeLarge", "pipeHuge", "pipeQuadruple", "pipeNonuple", "pipeRestrictiveTiny",
            "pipeRestrictiveSmall", "pipeRestrictiveMedium", "pipeRestrictiveLarge",
            "pipeRestrictiveHuge", "wireGt16", "wireGt15", "wireGt14", "wireGt13", "wireGt12", "wireGt11",
            "wireGt10", "wireGt09", "wireGt08", "wireGt07", "wireGt06", "wireGt05", "wireGt04", "wireGt03",
            "wireGt02", "wireGt01", "cableGt12", "cableGt08", "cableGt04", "cableGt02", "cableGt01",
            "blockGem", "blockDust", "blockIngot", "blockPlate", "blockPlateGem", "blockSolid",
            "oreberry", "plantGtBerry", "plantGtTwig", "plantGtFiber", "plantGtWart", "plantGtBlossom");

    /** The listener rejects prefixes carrying ORE or ORE_PROCESSING_DIRTY before conversion. */
    private static final Set<String> ORE_FORMS = Set.of(
            "chunk", "cluster", "crushed", "crushedTiny", "dirtyGravel", "pebbles", "rawOreChunk",
            "rubble", "clump", "crystalline", "reduced", "cleanGravel");

    private SharedThermalRecyclingRows() {}

    static void initialize() {
        if (initialized) throw new IllegalStateException("Thermal recycling rows initialized twice");
        initialized = true;
    }

    static Set<Recipe> owned() {
        var result = Collections.newSetFromMap(new IdentityHashMap<Recipe, Boolean>());
        OWNED.values().forEach(result::addAll);
        return Collections.unmodifiableSet(result);
    }

    static List<Deferred> deferred() {
        return List.copyOf(DEFERRED);
    }

    static void clear() {
        OWNED.forEach((map, rows) -> rows.forEach(map::unlink));
        OWNED.clear();
        DEFERRED.clear();
    }

    static void rebuild() {
        if (!initialized) return;
        clear();
        var snapshot = new SourceThermalNativeAdapter.Snapshot();
        int rows = 0;
        for (var candidate : candidates()) {
            var outcome = plan(candidate, snapshot);
            if (outcome.action() == Action.DEFERRED) {
                DEFERRED.add(new Deferred(candidate.name(), outcome.reason().name()));
                continue;
            }
            for (var row : outcome.rows()) {
                Recipe accepted = row.admit();
                if (accepted != null) {
                    OWNED.computeIfAbsent(row.map(), ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                            .add(accepted);
                    rows++;
                }
            }
        }
        var reasons = new java.util.TreeMap<String, Integer>();
        for (Deferred deferred : DEFERRED) reasons.merge(deferred.reason(), 1, Integer::sum);
        LOGGER.info(
                "[gregtech] Thermal recycling input rows: {} accepted, {} deferred {}, {} owned",
                rows, DEFERRED.size(), reasons, owned().size());
        for (int i = 0; i < Math.min(8, DEFERRED.size()); i++)
            LOGGER.info("[gregtech] Thermal deferred {}: {}", DEFERRED.get(i).source(), DEFERRED.get(i).reason());
    }

    private enum Action { ROWS, NO_ROW, DEFERRED }

    private enum Reason {
        SOURCE_NOT_RECYCLABLE,
        SOURCE_ORE_FORM,
        UNKNOWN_SOURCE_IDENTITY,
        UNKNOWN_SOURCE_FACT,
        MATERIAL_OVERRIDE,
        TARGET_IDENTITY_OVERRIDE,
        FLUID_BINDING_OVERRIDE,
        REQUIRED_NATIVE_FLUID_ABSENT,
        INVALID_RUNTIME_STATS,
        UNVERIFIED_INPUT_BEHAVIOR
    }

    private record Outcome(Action action, Reason reason, List<Row> rows) {
        static Outcome ok(List<Row> rows) { return new Outcome(Action.ROWS, null, List.copyOf(rows)); }
        static Outcome none() { return new Outcome(Action.NO_ROW, null, List.of()); }
        static Outcome deferred(Reason reason) { return new Outcome(Action.DEFERRED, reason, List.of()); }
    }

    private record Row(RecipeMap map, ItemStack input, FluidStack output, long ticks, ThermalRecyclingProof proof) {
        Recipe admit() {
            var recipe = RecipeMap.make(
                    true,
                    new ItemStack[] {input.copy()},
                    new ItemStack[0],
                    null,
                    null,
                    new FluidStack[0],
                    new FluidStack[] {output.copy()},
                    ticks,
                    SourceThermalRecyclingRules.POWER,
                    0);
            return recipe == null ? null : map.addRecipe(recipe.withMaterialRecovery().withItemInputGuard(0, proof::matches));
        }
    }

    private record Candidate(
            ItemStack stack,
            MaterialPrefix prefix,
            BlockMaterialPrefix blockPrefix,
            List<MaterialComponent> components,
            StoneType stoneType,
            StoneVariant stoneVariant,
            boolean stoneSlab) {
        String name() { return stack.getHoverName().getString(); }
    }

    private static List<Candidate> candidates() {
        var out = new ArrayList<Candidate>();
        var materialItems = new ArrayList<>(ItemMaterialRegistry.entries().entrySet());
        materialItems.sort(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()));
        for (var entry : materialItems) {
            Item item = entry.getKey();
            ItemComposition data = entry.getValue();
            if (data == null || !data.recoverable() || data.components().isEmpty()) continue;
            MaterialPrefix prefix = data.prefix();
            if (prefix != null && ownedByAuditedProducer(prefix, data)) continue;
            if (!allows(prefix)) continue;
            if (data.material() != null && containerForm(prefix) && !"Empty".equals(data.material().getName()))
                continue;
            // Every component counts: the source scores duration by the whole item's mass, and a
            // crafting-derived block keeps the parts its recipe puts in it.
            out.add(new Candidate(new ItemStack(item), prefix, null, data.components(), null, null, false));
        }
        for (BlockMaterialPrefix prefix : BlockPrefixRegistry.all()) {
            if (!allowsBlock(prefix.getName())) continue;
            for (GTMaterial raw : GTMaterialRegistry.sortedMaterials()) {
                GTMaterial material = raw.resolve();
                if (!material.isValid() || material.has(MaterialProperty.ANTIMATTER)) continue;
                ItemStack stack = GTBlocks.getStack(prefix, material);
                if (stack.isEmpty()) continue;
                var components = BlockMaterialWeights.contained(material, prefix);
                if (components.isEmpty()) continue;
                out.add(new Candidate(stack, null, prefix, components, null, null, false));
            }
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            StoneType type;
            StoneVariant variant;
            boolean slab;
            if (block instanceof GTStoneBlock stone) {
                type = stone.stoneType();
                variant = stone.variant();
                slab = false;
            } else if (block instanceof GTStoneSlabBlock stone) {
                type = stone.stoneType();
                variant = stone.variant();
                slab = true;
            } else continue;
            GTMaterial material = type.material();
            if (material == null || !material.isValid()) continue;
            var components = StoneMaterialWeights.contained(material, variant, slab);
            if (components.isEmpty()) continue;
            out.add(new Candidate(new ItemStack(block), null, null, components, type, variant, slab));
        }
        return out;
    }

    private static boolean ownedByAuditedProducer(MaterialPrefix prefix, ItemComposition data) {
        if (SourceThermalInputRules.form(prefix) != null) return true;
        if (!GTMaterialRegistry.isCoreMaterial(data.material().resolve())) return false;
        GTMaterial material = data.material().resolve();
        if (material.getId() == 9114 && material.getName().equals("Cinnabar"))
            return prefix == MaterialPrefix.dust || prefix == MaterialPrefix.dustSmall
                    || prefix == MaterialPrefix.dustTiny || prefix == MaterialPrefix.dustDiv72
                    || prefix == MaterialPrefix.crushedPurified || prefix == MaterialPrefix.crushedPurifiedTiny
                    || prefix == MaterialPrefix.crushedCentrifuged || prefix == MaterialPrefix.crushedCentrifugedTiny;
        boolean metal = material.getId() == 220 && material.getName().equals("Titanium")
                || material.getId() == 260 && material.getName().equals("Iron");
        return metal && (prefix == MaterialPrefix.dustSmall || prefix == MaterialPrefix.dustTiny
                || prefix == MaterialPrefix.dustDiv72);
    }

    private static boolean allows(MaterialPrefix prefix) {
        if (prefix == null) return true;
        String source = PrefixRegistry.sourceName(prefix.getName());
        return RECYCLABLE.contains(source) && !ORE_FORMS.contains(source);
    }

    /** Fluid/tube container forms: the source listener rejects filled containers, keeping only the empty item. */
    private static boolean containerForm(MaterialPrefix prefix) {
        if (prefix == null) return false;
        String source = PrefixRegistry.sourceName(prefix.getName());
        return source.equals("chemtube") || source.equals("cell") || source.equals("capcellcon");
    }

    private static boolean allowsBlock(String name) {
        String source = PrefixRegistry.sourceName(name);
        return RECYCLABLE.contains(source) && !ORE_FORMS.contains(source);
    }

    private static Outcome plan(Candidate candidate, SourceThermalNativeAdapter.Snapshot snapshot) {
        var targetUnits = new java.util.LinkedHashMap<GTMaterial, Long>();
        var factsByTarget = new java.util.LinkedHashMap<GTMaterial, SourceRecyclingFacts.Facts>();
        double weight = 0;
        try {
            for (MaterialComponent component : candidate.components()) {
                GTMaterial material = component.material().resolve();
                if (!valid(material)) return Outcome.deferred(Reason.UNKNOWN_SOURCE_IDENTITY);
                var facts = find(material.getId(), material.getName());
                if (facts == null) return Outcome.deferred(Reason.UNKNOWN_SOURCE_FACT);
                if (snapshot.materialOverride(material, facts) != null)
                    return Outcome.deferred(Reason.MATERIAL_OVERRIDE);
                weight += SourceRecyclingMath.materialWeight(material.getDensity(), component.amount(), SOURCE_UNIT);
                if (facts.melting() == Decision.NO
                        || facts.blacklistedSmelter() == Decision.YES
                        || facts.targetUnitsPerU() == 0) continue;
                if (facts.melting() != Decision.YES
                        || facts.blacklistedSmelter() != Decision.NO
                        || facts.targetUnitsPerU() < 0) return Outcome.deferred(Reason.UNKNOWN_SOURCE_FACT);
                GTMaterial target = GTMaterialRegistry.get(facts.target().name()).resolve();
                var resolvedTargetFacts = find(facts.target().id(), facts.target().name());
                if (!valid(target) || target.getId() != facts.target().id()
                        || !target.getName().equals(facts.target().name()) || resolvedTargetFacts == null)
                    return Outcome.deferred(Reason.TARGET_IDENTITY_OVERRIDE);
                if (snapshot.materialOverride(target, resolvedTargetFacts) != null)
                    return Outcome.deferred(Reason.MATERIAL_OVERRIDE);
                long units = SourceRecyclingMath.targetUnits(
                        component.amount(), SOURCE_UNIT, facts.targetUnitsPerU());
                if (units == 0) continue;
                // Source addToList merges equal target materials before the single-phase choice.
                targetUnits.merge(target, units, Math::addExact);
                factsByTarget.putIfAbsent(target, resolvedTargetFacts);
            }
        } catch (IllegalArgumentException | ArithmeticException invalid) {
            return Outcome.deferred(Reason.INVALID_RUNTIME_STATS);
        }
        var phases = new ArrayList<PhaseCandidate>();
        for (var entry : targetUnits.entrySet()) {
            var facts = factsByTarget.get(entry.getKey());
            ResolvedPhase phase = resolvePhase(entry.getKey(), facts, snapshot);
            if (phase == null) continue; // source skips a target without a native phase
            phases.add(new PhaseCandidate(entry.getKey(), entry.getValue(), phase, facts));
        }
        // The source keeps only a single phase-capable target; a second one clears the fluid.
        if (phases.size() != 1) return Outcome.none();
        var chosen = phases.get(0);
        int millibuckets;
        try {
            millibuckets = gregtech.content.recipe.OriginalMaterialFluidUnits.millibuckets(
                    chosen.targetUnits(), chosen.phase().binding().referenceMaterialUnits(),
                    chosen.phase().binding().millibuckets(), false);
        } catch (ArithmeticException invalid) {
            return Outcome.deferred(Reason.INVALID_RUNTIME_STATS);
        }
        if (millibuckets <= 0) return Outcome.none();
        var chosenFacts = chosen.targetFacts();
        boolean calcite = chosenFacts.target().id() == 9107 && chosenFacts.target().name().equals("Calcite");
        if (!calcite && chosenFacts.furnace() == Decision.UNKNOWN)
            return Outcome.deferred(Reason.UNKNOWN_SOURCE_FACT);
        boolean melter = chosenFacts.furnace() == Decision.YES || calcite;
        long ticks;
        try {
            ticks = SourceRecyclingMath.weightedTicks(
                    weight,
                    chosen.target().getMeltingPoint(),
                    chosen.phase().temperature(),
                    SourceThermalRecyclingRules.ENVIRONMENT_TEMPERATURE,
                    millibuckets,
                    chosen.phase().lava());
        } catch (IllegalArgumentException | ArithmeticException invalid) {
            return Outcome.deferred(Reason.INVALID_RUNTIME_STATS);
        }
        var proof = ThermalRecyclingProof.capture(candidate);
        if (proof == null) return Outcome.deferred(Reason.UNVERIFIED_INPUT_BEHAVIOR);
        FluidStack output = chosen.phase().fluid().copy();
        output.setAmount(millibuckets);
        var rows = new ArrayList<Row>(2);
        if (melter)
            rows.add(new Row(
                    gregtech.content.data.MachineRecipeMaps.Melter, candidate.stack(), output.copy(), ticks, proof));
        rows.add(new Row(
                gregtech.content.data.MachineRecipeMaps.Smelter, candidate.stack(), output.copy(), ticks, proof));
        return Outcome.ok(rows);
    }

    private record PhaseCandidate(
            GTMaterial target,
            long targetUnits,
            ResolvedPhase phase,
            SourceRecyclingFacts.Facts targetFacts) {}

    private record ResolvedPhase(
            SourceRecyclingFacts.Binding binding, FluidStack fluid, int temperature, boolean lava) {}

    /** Mirrors the audited adapter's phase proof; a target without a phase is skipped by the source. */
    private static ResolvedPhase resolvePhase(
            GTMaterial target, SourceRecyclingFacts.Facts facts, SourceThermalNativeAdapter.Snapshot snapshot) {
        Phase phase = facts.phase();
        if (phase.kind() == PhaseKind.UNKNOWN || phase.kind() == PhaseKind.OPTIONAL_EXTERNAL) return null;
        if (phase.kind() == PhaseKind.NO_DEFAULT_PHASE) return null;
        Binding binding = phase.binding();
        if (binding == null) return null;
        var nativeBinding = SourceThermalNativeAdapter.nativeBinding(target, binding);
        if (nativeBinding == null) return null;
        var entry = snapshot.definition(nativeBinding.field());
        if (entry == null) return null;
        if (!snapshot.owned(target, nativeBinding, entry)) return null;
        if (!snapshot.noExternalPhase(target, facts, nativeBinding.field())) return null;
        FluidStack fluid = snapshot.fluid(nativeBinding.field(), 1);
        if (fluid == null || fluid.isEmpty()) return null;
        if (!snapshot.holder(nativeBinding, entry, fluid)) return null;
        return new ResolvedPhase(binding, fluid, snapshot.temperature(fluid), fluid.getFluid() == Fluids.LAVA);
    }

    private static boolean valid(GTMaterial material) {
        return material != null && material.isValid() && material.resolve() == material
                && GTMaterialRegistry.isCoreMaterial(material) && !AddonMaterials.owns(material)
                && !material.has(MaterialProperty.ANTIMATTER)
                && GTMaterialRegistry.get(material.getId()) == material
                && GTMaterialRegistry.get(material.getName()) == material;
    }

    /** Exact composition proof retained on the recipe, so only the admitted item mass is recycled. */
    record ThermalRecyclingProof(
            Item item,
            MaterialPrefix prefix,
            BlockMaterialPrefix blockPrefix,
            GTMaterial blockMaterial,
            List<MaterialComponent> components,
            StoneType stoneType,
            StoneVariant stoneVariant,
            boolean stoneSlab) {
        static ThermalRecyclingProof capture(Candidate candidate) {
            if (candidate.stack().isEmpty() || candidate.components().isEmpty()) return null;
            return new ThermalRecyclingProof(
                    candidate.stack().getItem(),
                    candidate.prefix(),
                    candidate.blockPrefix(),
                    candidate.blockPrefix() == null ? null : candidate.components().get(0).material().resolve(),
                    List.copyOf(candidate.components()),
                    candidate.stoneType(),
                    candidate.stoneVariant(),
                    candidate.stoneSlab());
        }

        boolean matches(ItemStack offered) {
            try {
                if (offered == null || offered.isEmpty() || offered.getItem() != item) return false;
                ItemStack copy = offered.copyWithCount(1);
                if (copy.isDamaged() || copy.isDamageableItem() || copy.getMaxDamage() != 0
                        || ItemMaterialRegistry.hasStoredContents(copy)
                        || copy.getItem() instanceof gregtech.engine.recipe.RecipeCatalystLike
                        || copy.getItem() instanceof gregtech.engine.inventory.ContainerShapeLike
                        || copy.hasCraftingRemainingItem() || !copy.getCraftingRemainingItem().isEmpty()) return false;
                if (stoneType != null) {
                    if (stoneSlab) {
                        if (!(copy.getItem() instanceof gregtech.content.block.stone.GTStoneSlabBlockItem blockItem)
                                || !(blockItem.getBlock() instanceof GTStoneSlabBlock block)
                                || block.stoneType() != stoneType || block.variant() != stoneVariant) return false;
                        return StoneMaterialWeights.contained(block.stoneMaterial(), stoneVariant, true)
                                .equals(components);
                    }
                    if (!(copy.getItem() instanceof gregtech.content.block.MaterialBlockItem blockItem)
                            || !(blockItem.getBlock() instanceof GTStoneBlock block)
                            || block.stoneType() != stoneType || block.variant() != stoneVariant) return false;
                    return StoneMaterialWeights.contained(block.stoneMaterial(), stoneVariant, false)
                            .equals(components);
                }
                if (blockPrefix != null) {
                    if (!(copy.getItem() instanceof MaterialBlockItem blockItem)
                            || !(blockItem.getBlock() instanceof MaterialBlockLike block)
                            || block.prefix() != blockPrefix || block.material().resolve() != blockMaterial)
                        return false;
                    return BlockMaterialWeights.contained(block.material(), block.prefix()).equals(components);
                }
                var data = ItemMaterialRegistry.get(copy).orElse(null);
                if (data == null || !ItemMaterialRegistry.canRecover(copy) || !Objects.equals(prefix, data.prefix()))
                    return false;
                if (!data.components().equals(components)) return false;
                // Reject filled fluid containers exactly like the audited producer.
//? if >=1.21.1 {
                /*var handler = FluidUtil.getFluidHandler(copy).orElse(null);
*///?} else {
                var handler = FluidUtil.getFluidHandler(copy).resolve().orElse(null);
//?}
                if (handler != null) {
                    int tanks = handler.getTanks();
                    if (tanks < 0) return false;
                    for (int tank = 0; tank < tanks; tank++)
                        if (handler.getTankCapacity(tank) != 0) return false;
                }
                return true;
            } catch (RuntimeException | LinkageError invalidBehavior) {
                return false;
            }
        }
    }
}
