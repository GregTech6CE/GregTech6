package gregtech.content.multiblock;

import gregtech.api.machine.MachineControllerKind;
import java.util.Map;

/** Built-in recipe controller kinds are explicit; a missing layout must never turn one into a single block. */
public final class RecipeControllerFamilies {
    private RecipeControllerFamilies() {}

    public record Family(MachineControllerKind kind, String structure) {}

    private static final Map<String, Family> FAMILIES = Map.ofEntries(
            Map.entry("massfab", new Family(MachineControllerKind.COMPACT_MATTER, null)),
            Map.entry("largemassfab", new Family(MachineControllerKind.MATTER, "largemassfab")),
            Map.entry("distillationtower", new Family(MachineControllerKind.DISTILLATION, "distillationtower")),
            Map.entry("largemixer", recipe("largemixer")),
            Map.entry("largebath", recipe("largebath")),
            Map.entry("largecentrifuge", recipe("largecentrifuge")),
            Map.entry("largecoagulator", recipe("largecoagulator")),
            Map.entry("largeelectrolyzer", recipe("largeelectrolyzer")),
            Map.entry("largefermenter", recipe("largefermenter")),
            Map.entry("largeautoclave", recipe("largeautoclave")),
            Map.entry("largecrusher", recipe("largecrusher")),
            Map.entry("largeshredder", recipe("largeshredder")),
            Map.entry("largesqueezer", recipe("largesqueezer")),
            Map.entry("largeoven", recipe("largeoven")),
            Map.entry("largesluice", recipe("largesluice")));

    private static Family recipe(String structure) {
        return new Family(MachineControllerKind.LARGE_RECIPE, structure);
    }

    public static Family find(String machine) {
        return FAMILIES.get(machine);
    }

    public static MachineControllerKind kind(String machine) {
        var family = find(machine);
        return family == null ? MachineControllerKind.BASIC : family.kind();
    }

    public static String requireStructure(String machine) {
        var family = find(machine);
        if (family == null || family.structure() == null)
            throw new IllegalStateException("No multiblock structure declared for recipe controller " + machine);
        return family.structure();
    }
}
