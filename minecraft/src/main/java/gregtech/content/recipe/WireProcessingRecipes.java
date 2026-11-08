package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.data.*;
import gregtech.content.registry.*;
import gregtech.engine.material.*;
import gregtech.engine.recipe.*;
import java.util.*;
import net.minecraft.world.item.ItemStack;

/** GT6 wire handlers and MultiTileEntityWireElectric laminator recipes, resolved against block registries. */
public final class WireProcessingRecipes {
    public record Entry(RecipeMap map, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    // MT's SOFT/FURNACE wire materials. Hard-workable metals use the quality-scaled handler cost.
    private static final Set<String> EASY = Set.of(
            "tin",
            "lead",
            "copper",
            "annealed_copper",
            "silver",
            "gold",
            "aluminium",
            "electrum",
            "blue_alloy",
            "electrotine_alloy");

    private WireProcessingRecipes() {}

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static int register() {
        if (!ENTRIES.isEmpty()) throw new IllegalStateException("Wire recipes registered twice");
        List<GTMaterial> insulations = insulationMaterials();
        if (insulations.isEmpty()) throw new IllegalStateException("No rubber insulation material");
        Map<String, Map<Integer, ElectricWireBlock>> families = new LinkedHashMap<>();
        for (var entry : GTWires.allWires()) {
            var block = entry.get();
            families.computeIfAbsent(block.spec().id(), ignored -> new TreeMap<>())
                    .put(block.spec().size(), block);
        }
        for (var family : families.entrySet()) {
            var sizes = family.getValue();
            var one = sizes.get(1);
            var material = one.spec().material();
            boolean legacyAllowed = material.has(MaterialProperty.SMITHABLE)
                    && !Set.of("graphene", "netherite", "superconductor").contains(family.getKey());
            long legacyTicks = EASY.contains(family.getKey()) ? 16 : 128L * (material.getToolQuality() + 1);
            boolean sourceAdditionalFamily = family.getKey().equals("netherite")
                            && material == gregtech.content.material.Materials.Netherite
                    || family.getKey().equals("naquadah") && material == gregtech.content.material.Materials.Naquadah;
            var decision =
                    SourceRollingWireRules.physicalWire(material, legacyAllowed, legacyTicks, sourceAdditionalFamily);
            if (decision.allowed()) {
                var input = GTItems.getStack(MaterialPrefix.ingot, material.resolve(), 1);
                if (!input.isEmpty())
                    addPhysicalWire(decision.ticks(), material.resolve(), new ItemStack(one, 2), input);
            }
            for (int big = 2; big <= 16; big++) {
                // GT6 registers small=1 first. Keep one deterministic unpacking result instead of competing recipes.
                add(MachineRecipeMaps.Unboxinator, 16, new ItemStack(one, big), new ItemStack(sizes.get(big)));
                for (int small = 1; small < big; small++)
                    if (big % small == 0)
                        add(
                                MachineRecipeMaps.Loom,
                                32L * big * (material.getToolQuality() + 1),
                                new ItemStack(sizes.get(big)),
                                new ItemStack(sizes.get(small), big / small),
                                new ItemStack(GTTechnological.selectorTag(big)));
            }
        }
        for (var entry : GTWires.allCables()) {
            var cable = entry.get();
            int size = cable.spec().size();
            if (size == 16) continue; // Legacy port extension: GT6 only has insulated 1,2,4,8,12.
            int plates = switch (size) {
                case 1, 2 -> 1;
                case 4 -> 2;
                case 8 -> 3;
                case 12 -> 4;
                default -> throw new IllegalStateException("Cable width " + size);
            };
            var wire = families.get(cable.spec().id()).get(size);
            boolean insulated = false;
            for (var rubber : insulations) {
                for (var prefix : new MaterialPrefix[] {MaterialPrefix.plate, MaterialPrefix.foil}) {
                    var insulation = GTItems.getStack(prefix, rubber, plates * (prefix == MaterialPrefix.foil ? 4 : 1));
                    if (insulation.isEmpty()) continue;
                    add(
                            MachineRecipeMaps.Laminator,
                            plates * 16,
                            new ItemStack(cable),
                            insulation,
                            new ItemStack(wire));
                    insulated = true;
                }
            }
            if (!insulated) throw new IllegalStateException("Missing insulation forms for cable width " + size);
        }
        for (var entry : GTSignalWires.all()) {
            var wire = entry.get();
            if (!wire.insulated()) {
                var decision = SourceRollingWireRules.physicalWire(wire.material(), true, 16, false);
                var input =
                        GTItems.getStack(MaterialPrefix.ingot, wire.material().resolve(), 1);
                if (decision.allowed() && !input.isEmpty())
                    addPhysicalWire(decision.ticks(), wire.material().resolve(), new ItemStack(wire, 2), input);
            } else {
                var bare = GTSignalWires.all().stream()
                        .map(r -> r.get())
                        .filter(b -> !b.insulated() && b.material() == wire.material())
                        .findFirst()
                        .orElseThrow();
                for (var rubber : insulations) {
                    for (var prefix : new MaterialPrefix[] {MaterialPrefix.plate, MaterialPrefix.foil}) {
                        var insulation = GTItems.getStack(prefix, rubber, prefix == MaterialPrefix.foil ? 4 : 1);
                        if (insulation.isEmpty()) continue;
                        add(MachineRecipeMaps.Laminator, 16, new ItemStack(wire), insulation, new ItemStack(bare));
                    }
                }
            }
        }
        return ENTRIES.size();
    }
    /**
     * GT6 iterates {@code ANY.Rubber.mToThis} for cable insulation. The port's group is live:
     * addons that register another rubber material get the same plate/foil rows.
     */
    private static List<GTMaterial> insulationMaterials() {
        var out = new java.util.LinkedHashSet<GTMaterial>();
        // Registry access first: it runs the material link that fills the group memberships.
        var rubber = GTMaterialRegistry.get("Rubber");
        if (rubber != null && rubber.isValid()) out.add(rubber.resolve());
        var group = gregtech.content.data.MaterialGroups.Rubber;
        if (group != null && group.isValid()) out.add(group.resolve());
        for (var member : group == null ? java.util.Set.<GTMaterial>of() : group.getReRegistrations())
            if (member != null && member.isValid()) out.add(member.resolve());
        return List.copyOf(out);
    }

    /**
     * One ingot's worth of a wire material becomes two size-1 wires. The rule is published in
     * material units ({@code Loader_Recipes_Handlers}), so every equivalent ingot item of the
     * material is admitted and no representative stack narrows the input set.
     */
    private static void addPhysicalWire(long ticks, GTMaterial material, ItemStack result, ItemStack input) {
        long units = MaterialPrefix.ingot.getMaterialWeight();
        var template =
                new Recipe(new ItemStack[] {input}, new ItemStack[] {result}, null, null, null, null, ticks, 16, 0);
        var rule = new gregtech.engine.recipe.MaterialUnitRecipe(
                template, 0, material, units, gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding.WHOLE_OUTPUTS);
        rule.source(input, units);
        ENTRIES.add(new Entry(MachineRecipeMaps.Wiremill, rule.publish(MachineRecipeMaps.Wiremill)));
    }

    private static void add(RecipeMap map, long ticks, ItemStack result, ItemStack... input) {
        var recipe = new Recipe(input, new ItemStack[] {result}, null, null, null, null, ticks, 16, 0);
        if (map.addRecipe(recipe) == null)
            throw new IllegalStateException("Wire recipe conflict: " + map.mNameInternal + " / " + result);
        ENTRIES.add(new Entry(map, recipe));
    }
}
