package gregtech.content.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Native block and modern biome-tag adapter for the original stratum rules. */
public final class StoneLayerOrePlacement {
    private StoneLayerOrePlacement() {}

    public static boolean enabled(StoneLayerOreRules.Rule rule) {
        if (rule.blockId().isEmpty()) return true;
        var id = ResourceLocation.parse(rule.blockId());
        return !VanillaOreGeneration.disabled(id) && BuiltInRegistries.BLOCK.get(id) != Blocks.AIR;
    }

    public static BlockState explicitState(StoneLayerOreRules.Rule rule) {
        return rule.blockId().isEmpty() ? null
                : BuiltInRegistries.BLOCK.get(ResourceLocation.parse(rule.blockId())).defaultBlockState();
    }

    public static boolean matches(StoneLayerOreRules.Rule rule, Holder<Biome> biome) {
        return rule.biomes().isEmpty() || rule.biomes().stream().anyMatch(group -> matches(group, biome));
    }

    private static boolean tagged(Holder<Biome> biome, String group) {
        return biome.is(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_" + group)))
                || biome.is(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("forge", "is_" + group)));
    }

    private static boolean matches(StoneLayerOreRules.BiomeGroup group, Holder<Biome> biome) {
        return switch (group) {
            case MOUNTAINS -> biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL) || tagged(biome, "mountain");
            case FROZEN -> biome.value().getBaseTemperature() < 0.15F || tagged(biome, "snowy");
            case DESERT -> biome.is(Biomes.DESERT) || tagged(biome, "desert");
            case SAVANNA -> biome.is(BiomeTags.IS_SAVANNA) || tagged(biome, "savanna");
            case JUNGLE -> biome.is(BiomeTags.IS_JUNGLE) || tagged(biome, "jungle");
            case OCEAN_BEACH -> biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) || tagged(biome, "ocean") || tagged(biome, "beach");
            case TAIGA -> biome.is(BiomeTags.IS_TAIGA) || tagged(biome, "taiga");
            case SWAMP -> biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP) || tagged(biome, "swamp");
            case PLAINS -> biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS) || biome.is(Biomes.SNOWY_PLAINS) || tagged(biome, "plains");
            case RIVER_LAKE -> biome.is(BiomeTags.IS_RIVER) || tagged(biome, "river") || tagged(biome, "lake");
            case MESA -> biome.is(BiomeTags.IS_BADLANDS) || tagged(biome, "badlands");
            case SHROOM -> biome.is(Biomes.MUSHROOM_FIELDS) || tagged(biome, "mushroom");
        };
    }
}
