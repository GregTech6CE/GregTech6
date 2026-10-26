package gregtech.engine.machine.crucible;

import gregtech.api.material.*;
import gregtech.engine.material.*;
import gregtech.content.data.generated.GT6Materials.*;
import java.util.*;

/** GT6 MT.addAlloyingRecipe ratios; output yield is independent of consumed flux. */
public final class CrucibleReactions {
    public record Part(GTMaterial material, long ratio) {}
    public record Reaction(GTMaterial output, long yield, List<Part> parts) {}
    private static final List<Reaction> RECIPES = new ArrayList<>();
    static {
        // MT.java: uumAloy single-material purification and explicit air-blown steel recipes.
        recipe(Elements.WroughtIron, 1, new Part(Elements.Fe, 1));
        recipe(Compounds.AnnealedCopper, 1, new Part(Elements.Cu, 1));
        recipe(Compounds.Steel, 1, new Part(Elements.WroughtIron, 1), new Part(Compounds.Air, 1));
        recipe(Compounds.MeteoricSteel, 1, new Part(Compounds.MeteoricIron, 1), new Part(Compounds.Air, 1));
        recipe(Elements.Fe, 2, new Part(Ores.Fe2O3, 5), new Part(Elements.C, 1), new Part(Ores.CaCO3, 1));
        recipe(Elements.Fe, 6, new Part(Ores.Magnetite, 14), new Part(Elements.C, 3));
        recipe(Elements.Fe, 6, new Part(Ores.BasalticMineralSand, 14), new Part(Elements.C, 3));
        recipe(Elements.Fe, 6, new Part(Ores.GraniticMineralSand, 14), new Part(Elements.C, 3));
        recipe(Elements.Fe, 6, new Part(Ores.Ferrovanadium, 28), new Part(Elements.C, 3));
        recipe(Elements.Si, 1, new Part(Ores.SiO2, 3), new Part(Elements.C, 1));
        recipe(Elements.Fe, 2, new Part(Compounds.ShadowIron, 1), new Part(Compounds.Ignatius, 1));
        recipe(Elements.Fe, 2, new Part(Compounds.DeepIron, 1), new Part(Compounds.Prometheum, 1));
        recipe(Compounds.BlackSteel, 2, new Part(Compounds.DeepIron, 1), new Part(Compounds.Infuscolium, 1));
        recipe(Compounds.Ultimet, 36, new Part(Elements.Co, 20), new Part(Compounds.Nichrome, 5), new Part(Elements.Cr, 7), new Part(Elements.Mo, 4));
        recipe(Compounds.StainlessSteel, 36, new Part(Elements.WroughtIron, 24), new Part(Compounds.Nichrome, 5), new Part(Elements.Cr, 3), new Part(Elements.Mn, 4));
        recipe(Compounds.TungstenSteel, 2, new Part(Compounds.MeteoricSteel, 1), new Part(Elements.W, 1));
        recipe(Compounds.TungstenSteel, 2, new Part(Compounds.MeteoricSteel, 1), new Part(Compounds.TungstenSintered, 1));
        recipe(Compounds.TungstenSteel, 2, new Part(Compounds.Steel, 1), new Part(Compounds.TungstenSintered, 1));
        recipe(Compounds.VanadiumSteel, 5, new Part(Compounds.MeteoricSteel, 4), new Part(Elements.V, 1));
        recipe(Compounds.ElectricalSteel, 1, new Part(Compounds.MeteoricSteel, 1), new Part(Elements.Si, 1));
        recipe(Compounds.ObsidianSteel, 1, new Part(Compounds.MeteoricSteel, 1), new Part(Compounds.Lava, 9));
        recipe(Compounds.ObsidianSteel, 1, new Part(Compounds.Steel, 1), new Part(Compounds.Lava, 9));
        recipe(Compounds.EndSteel, 1, new Part(Compounds.ObsidianSteel, 1), new Part(Stones.Endstone, 1), new Part(Compounds.Lava, 9));
        recipe(Compounds.Alumite, 5, new Part(Elements.Al, 5), new Part(Elements.WroughtIron, 2), new Part(Compounds.Lava, 18));
        recipe(Compounds.Hepatizon, 24, new Part(Compounds.Bronze, 8), new Part(Elements.Sn, 1), new Part(Compounds.RoseGold, 15));
        recipe(Compounds.RedAlloy, 1, new Part(Compounds.Mingrade, 2), new Part(Compounds.Redstone, 3));
        recipe(Compounds.RedAlloy, 1, new Part(Compounds.AnnealedCopper, 1), new Part(Compounds.Redstone, 4));
        recipe(Compounds.RoseGold, 5, new Part(Compounds.AnnealedCopper, 1), new Part(Elements.Au, 4));
        recipe(Compounds.SterlingSilver, 5, new Part(Compounds.AnnealedCopper, 1), new Part(Elements.Ag, 4));
        recipe(Compounds.AluminiumBrass, 4, new Part(Compounds.AnnealedCopper, 1), new Part(Elements.Al, 3));
        recipe(Compounds.Brass, 4, new Part(Compounds.AnnealedCopper, 3), new Part(Elements.Zn, 1));
        recipe(Compounds.Bronze, 4, new Part(Compounds.AnnealedCopper, 3), new Part(Elements.Sn, 1));
        recipe(Compounds.ArsenicCopper, 4, new Part(Compounds.AnnealedCopper, 3), new Part(Elements.As, 1));
        recipe(Compounds.ArsenicBronze, 5, new Part(Compounds.ArsenicCopper, 4), new Part(Elements.Sn, 1));
        recipe(Compounds.BlackBronze, 5, new Part(Compounds.AnnealedCopper, 3), new Part(Compounds.Electrum, 2));
        recipe(Compounds.BlackBronze, 20, new Part(Elements.Cu, 11), new Part(Compounds.RoseGold, 5), new Part(Elements.Ag, 4));
        recipe(Compounds.BlackBronze, 20, new Part(Compounds.AnnealedCopper, 11), new Part(Compounds.RoseGold, 5), new Part(Elements.Ag, 4));
        recipe(Compounds.BlackBronze, 20, new Part(Elements.Cu, 11), new Part(Compounds.SterlingSilver, 5), new Part(Elements.Au, 4));
        recipe(Compounds.BlackBronze, 20, new Part(Compounds.AnnealedCopper, 11), new Part(Compounds.SterlingSilver, 5), new Part(Elements.Au, 4));
        recipe(Compounds.Signalum, 8, new Part(Compounds.AnnealedCopper, 1), new Part(Elements.Ag, 2), new Part(Compounds.RedAlloy, 5));
        recipe(Compounds.Signalum, 16, new Part(Elements.Cu, 1), new Part(Compounds.SterlingSilver, 5), new Part(Compounds.RedAlloy, 10));
        recipe(Compounds.Signalum, 16, new Part(Compounds.AnnealedCopper, 1), new Part(Compounds.SterlingSilver, 5), new Part(Compounds.RedAlloy, 10));
        recipe(Compounds.Constantan, 2, new Part(Compounds.AnnealedCopper, 1), new Part(Elements.Ni, 1));
        recipe(Compounds.YttriumBariumCuprate, 6, new Part(Compounds.AnnealedCopper, 3), new Part(Elements.Ba, 2), new Part(Elements.Y, 1));
        recipe(Compounds.YttriumBariumCuprate, 6, new Part(Elements.Cu, 3), new Part(Elements.Ba, 2), new Part(Elements.Y, 1));
        recipe(Compounds.Li2Fe2O4, 8, new Part(Ores.Fe2O3, 5), new Part(Compounds.Li2O, 3));
    }
    private static void recipe(GTMaterial out, long yield, Part... parts) {
        RECIPES.add(new Reaction(out.resolve(), yield, List.of(parts)));
    }
    public static List<Reaction> recipes() { return Collections.unmodifiableList(RECIPES); }

    /** One addon row. Built-in rows are not tagged. */
    public record AddonRow(String addonId, Reaction reaction) {
        public AddonRow {
            Objects.requireNonNull(addonId, "addonId");
            Objects.requireNonNull(reaction, "reaction");
        }
    }

    /** A removal. An empty input set drops every row of {@code output}. */
    public record AddonRemoval(String addonId, GTMaterial output, Set<GTMaterial> inputs) {
        public AddonRemoval {
            Objects.requireNonNull(addonId, "addonId");
            Objects.requireNonNull(output, "output");
            Objects.requireNonNull(inputs, "inputs");
            output = output.resolve();
            var resolved = new HashSet<GTMaterial>();
            for (GTMaterial input : inputs) resolved.add(input.resolve());
            inputs = Set.copyOf(resolved);
        }
    }

    private static List<Reaction> all;
    private static boolean sealed;

    /**
     * Freezes the table: built-in explicit rows, built-in composition rows, addon explicit rows,
     * addon composition rows, then removals. Call this once, after materials have linked and before
     * {@link #allRecipes()}.
     */
    public static synchronized void seal(List<AddonRow> explicit, List<AddonRow> derived, List<AddonRemoval> removals) {
        if (sealed || all != null) throw new IllegalStateException(all != null
                ? "Crucible reactions were read before addon linking finished"
                : "Crucible reactions are already sealed");
        Objects.requireNonNull(explicit, "explicit");
        Objects.requireNonNull(derived, "derived");
        Objects.requireNonNull(removals, "removals");
        List<Reaction> result = new ArrayList<>(RECIPES);
        appendBuiltinCompositions(result);
        var addonIds = new IdentityHashMap<Reaction, String>();
        for (AddonRow row : explicit) addonIds.put(row.reaction(), row.addonId());
        for (AddonRow row : derived) addonIds.put(row.reaction(), row.addonId());
        for (AddonRow row : explicit) result.add(row.reaction());
        for (AddonRow row : derived) result.add(row.reaction());
        applyRemovals(result, removals);
        rejectDuplicates(result, addonIds);
        warnOverlappingInputs(result, addonIds);
        all = List.copyOf(result);
        sealed = true;
    }

    public static synchronized List<Reaction> allRecipes() {
        if (all != null) return all;
        throw new IllegalStateException("Crucible reactions were read before addon linking finished");
    }

    private static void appendBuiltinCompositions(List<Reaction> result) {
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.resolve() != material || !OriginalCrucibleCompositions.hasRecipe(material.getId()) || !material.hasComposition()) continue;
            List<Part> parts = material.getCompositionComponents().stream()
                    .map(p -> new Part(p.material(), Math.max(1, p.amount() / GTValues.U))).toList();
            if (parts.stream().anyMatch(p -> p.material() == material)) continue;
            result.add(new Reaction(material, material.getCompositionDivider(), parts));
        }
    }

    private static void applyRemovals(List<Reaction> result, List<AddonRemoval> removals) {
        for (AddonRemoval removal : removals) {
            int matched = 0;
            for (int i = result.size() - 1; i >= 0; i--) {
                if (!matches(result.get(i), removal)) continue;
                Reaction row = result.remove(i);
                matched++;
                GTMaterialRegistry.warn("Addon '" + removal.addonId() + "' removed crucible reaction " + describe(row));
            }
            if (matched == 0) GTMaterialRegistry.warn("Addon '" + removal.addonId()
                    + "' crucible removal " + removalLabel(removal) + " matched nothing");
        }
    }

    private static boolean matches(Reaction row, AddonRemoval removal) {
        if (row.output().resolve() != removal.output()) return false;
        if (removal.inputs().isEmpty()) return true;
        var parts = new HashSet<GTMaterial>();
        for (Part part : row.parts()) parts.add(part.material().resolve());
        return parts.equals(removal.inputs());
    }

    private static void rejectDuplicates(List<Reaction> result, IdentityHashMap<Reaction, String> addonIds) {
        for (int i = 0; i < result.size(); i++) {
            Reaction row = result.get(i);
            String addonId = addonIds.get(row);
            if (addonId == null) continue;
            for (int j = 0; j < result.size(); j++) {
                if (i == j || !sameRecipe(row, result.get(j))) continue;
                String otherId = addonIds.get(result.get(j));
                String where = otherId == null ? "a built-in row" : "addon '" + otherId + "'";
                throw new IllegalStateException("Addon '" + addonId + "' crucible reaction duplicates "
                        + where + ": " + describe(row));
            }
        }
    }

    private static void warnOverlappingInputs(List<Reaction> result, IdentityHashMap<Reaction, String> addonIds) {
        for (int i = 0; i < result.size(); i++) {
            Reaction row = result.get(i);
            String addonId = addonIds.get(row);
            if (addonId == null) continue;
            Map<GTMaterial, Long> ratios = ratioMap(row);
            for (int j = 0; j < result.size(); j++) {
                if (i == j || sameRecipe(row, result.get(j)) || !ratios.equals(ratioMap(result.get(j)))) continue;
                if (addonIds.get(result.get(j)) != null && j < i) continue;
                GTMaterialRegistry.warn("Addon '" + addonId + "' crucible reaction " + describe(row)
                        + " uses the same inputs as " + describe(result.get(j)) + " and can take that reaction over");
            }
        }
    }

    private static boolean sameRecipe(Reaction left, Reaction right) {
        return left.output().resolve() == right.output().resolve() && left.yield() == right.yield()
                && ratioMap(left).equals(ratioMap(right));
    }

    private static Map<GTMaterial, Long> ratioMap(Reaction reaction) {
        var ratios = new HashMap<GTMaterial, Long>();
        for (Part part : reaction.parts()) ratios.merge(part.material().resolve(), part.ratio(), Math::addExact);
        return ratios;
    }

    private static String describe(Reaction reaction) {
        var text = new StringBuilder(reaction.output().resolve().getName())
                .append(" yield ").append(reaction.yield()).append(" from ");
        for (int i = 0; i < reaction.parts().size(); i++) {
            if (i > 0) text.append(", ");
            Part part = reaction.parts().get(i);
            text.append(part.material().resolve().getName()).append('*').append(part.ratio());
        }
        return text.toString();
    }

    private static String removalLabel(AddonRemoval removal) {
        if (removal.inputs().isEmpty()) return removal.output().getName() + " (any inputs)";
        var names = new ArrayList<String>();
        for (GTMaterial input : removal.inputs()) names.add(input.getName());
        names.sort(String::compareTo);
        return removal.output().getName() + " from " + String.join(", ", names);
    }
    /** One winning reaction per tick, with at most one solid ingredient, as in GT6. */
    public static boolean react(List<CrucibleMaterialStack> content, long temperature) {
        if (content.isEmpty()) return false;
        Reaction best = null; long bestCount = 0, bestOutput = 0;
        for (Reaction r : allRecipes()) {
            if (temperature < r.output().getMeltingPoint()) continue;
            long count = Long.MAX_VALUE; int solid = 0;
            for (Part p : r.parts()) {
                if (temperature < p.material().getMeltingPoint()) solid++;
                long available = 0;
                for (var stack : content) if (stack.material == p.material().resolve()) available += stack.amount;
                count = Math.min(count, available / p.ratio());
            }
            if (solid > 1 || count <= 0 || count == Long.MAX_VALUE) continue;
            long output = Math.multiplyExact(count, r.yield());
            if (output > bestOutput) { best = r; bestCount = count; bestOutput = output; }
        }
        if (best == null) return false;
        for (Part p : best.parts()) {
            long remaining = p.ratio() * bestCount;
            for (CrucibleMaterialStack s : content) if (s.material == p.material().resolve()) {
                long take = Math.min(s.amount, remaining); s.amount -= take; remaining -= take;
            }
        }
        content.removeIf(s -> s.amount <= 0);
        CrucibleMaterialStack.of(best.output(), bestOutput).addToList(content);
        return true;
    }
}
