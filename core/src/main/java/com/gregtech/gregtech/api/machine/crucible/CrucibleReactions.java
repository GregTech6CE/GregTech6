package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.generated.GT6Materials.*;
import java.util.*;

/** GT6 MT.addAlloyingRecipe ratios; output yield is independent of consumed flux. */
public final class CrucibleReactions {
    public record Part(GTMaterial material, long ratio) {}
    public record Reaction(GTMaterial output, long yield, List<Part> parts) {}
    private static final List<Reaction> RECIPES = new ArrayList<>();
    static {
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
    private static List<Reaction> all;
    public static synchronized List<Reaction> allRecipes() {
        if (all != null) return all;
        List<Reaction> result = new ArrayList<>(RECIPES);
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.resolve() != material || !material.has(MaterialProperty.ALLOY) || !material.hasComposition()) continue;
            List<Part> parts = material.getCompositionComponents().stream()
                    .map(p -> new Part(p.material(), Math.max(1, p.amount() / GTValues.U))).toList();
            if (parts.stream().anyMatch(p -> p.material() == material)) continue;
            result.add(new Reaction(material, material.getCompositionDivider(), parts));
        }
        return all = List.copyOf(result);
    }
    /** One winning reaction per tick, with at most one solid ingredient, as in GT6. */
    public static boolean react(List<CrucibleMaterialStack> content, long temperature) {
        if (content.size() < 2) return false;
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
            if (solid > 1 || solid == r.parts().size() || count <= 0 || count == Long.MAX_VALUE) continue;
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
