package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.content.worldgen.GTBedrockOres.BedrockOre;
import gregtech.content.worldgen.OreGenerationSummary.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** The bedrock deposits available under this world's geology settings. */
public final class GTBedrockOreDimensions {
    private static final Generation EMPTY = new Generation(0, List.of());
    private static final Map<String, Resolved> CACHE = new ConcurrentHashMap<>();

    private GTBedrockOreDimensions() {}

    record Generation(int density, List<BedrockOre> ores) {}

    private record Settings(
            OreSourceCatalog catalog,
            OreSourceCatalog.Geology geology,
            Dimension ordinary,
            int density,
            List<? extends String> disabledOres,
            List<? extends String> disabledDepths,
            List<? extends String> overrides) {}

    private record Resolved(Settings settings, Generation generation) {}

    static Generation generation(String dimension, Dimension ordinary) {
        if (!GregTechConfig.Worldgen.BEDROCK_ORES.enabled()) return EMPTY;
        int density = GregTechConfig.Number.BEDROCK_DENSITY.value();
        if (density == 0) return EMPTY;
        var catalog = OreSourceCatalog.current();
        if (!catalog.bedrockDimensions().contains(dimension)) return EMPTY;
        var scope = OreVeinDeclaration.OVERWORLD.equals(dimension) && ordinary != null && ordinary.hasSources()
                ? ordinary
                : null;
        var geology = scope == null
                ? OreSourceCatalog.Geology.DISABLED
                : VanillaOreGeneration.veinsDisabled()
                        ? OreSourceCatalog.Geology.WITHOUT_VANILLA_ORES
                        : OreSourceCatalog.Geology.WITH_VANILLA_ORES;
        var settings = new Settings(
                catalog,
                geology,
                scope,
                density,
                GregTechConfig.disabledBedrockOres(),
                GregTechConfig.disabledBedrockDepths(),
                GregTechConfig.bedrockChanceOverrides());
        var previous = CACHE.get(dimension);
        if (previous != null && previous.settings().equals(settings)) return previous.generation();
        var generated = resolve(dimension, settings);
        CACHE.put(dimension, new Resolved(settings, generated));
        return generated;
    }

    private static Generation resolve(String dimension, Settings settings) {
        Set<String> disabledOres = keys(settings.disabledOres());
        if (disabledOres.contains("*")) return EMPTY;
        Set<String> disabledDepths = keys(settings.disabledDepths());
        Map<String, Integer> overrides = divisors(settings.overrides());
        var ordinary = settings.ordinary();
        var disabledBlocks =
                ordinary != null && ordinary.strata() ? StoneLayerOrePlacement.disabledOutputs() : Set.<String>of();
        var available = ordinary == null
                ? settings.catalog().bedrockOres(dimension, settings.geology())
                : settings.catalog()
                        .bedrockOres(
                                dimension,
                                settings.geology(),
                                ordinary.heights(),
                                source ->
                                        ordinary.supports(source.kind()) && !disabledBlocks.contains(source.blockId()));
        var ores = new ArrayList<BedrockOre>();
        for (var ore : available) {
            String material = key(ore.material().getName());
            if (disabledOres.contains(ore.name())
                    || disabledOres.contains(material)
                    || disabledDepths.contains(key(ore.depth().name()))) continue;
            int divisor = overrides.getOrDefault(ore.name(), overrides.getOrDefault(material, ore.chance()));
            ores.add(divisor == ore.chance() ? ore : ore.withChance(divisor));
        }
        return ores.isEmpty() ? EMPTY : new Generation(settings.density(), List.copyOf(ores));
    }

    private static Set<String> keys(List<? extends String> entries) {
        return entries.stream().map(GTBedrockOreDimensions::key).collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, Integer> divisors(List<? extends String> entries) {
        var overrides = new HashMap<String, Integer>();
        for (String entry : entries) {
            int equals = entry.indexOf('=');
            if (equals <= 0) continue;
            try {
                int divisor = Integer.parseInt(entry.substring(equals + 1).strip());
                if (divisor > 0) overrides.put(key(entry.substring(0, equals)), divisor);
            } catch (NumberFormatException ignored) {
                // Keep the catalog divisor for malformed entries.
            }
        }
        return overrides;
    }

    private static String key(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
