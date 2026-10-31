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
        return enabled(rule.blockId().isEmpty() ? null : ResourceLocation.parse(rule.blockId()));
    }

    static boolean enabled(ResourceLocation id) {
        if (id == null) return true;
        return !VanillaOreGeneration.disabled(id) && BuiltInRegistries.BLOCK.get(id) != Blocks.AIR;
    }

    public static BlockState explicitState(StoneLayerOreRules.Rule rule) {
        return explicitState(rule.blockId().isEmpty() ? null : ResourceLocation.parse(rule.blockId()));
    }

    static BlockState explicitState(ResourceLocation id) {
        return id == null ? null : BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    }

    public static boolean matches(StoneLayerOreRules.Rule rule, Holder<Biome> biome) {
        var groups = rule.biomes();
        if (groups.isEmpty()) return true;
        for (int i = 0; i < groups.size(); i++) if (matches(groups.get(i), biome)) return true;
        return false;
    }

    /** Tag identities are fixed; holder membership is still queried live after datapack reloads. */
    private enum BiomeTagsPair {
        MOUNTAIN("mountain"),
        SNOWY("snowy"),
        DESERT("desert"),
        SAVANNA("savanna"),
        JUNGLE("jungle"),
        OCEAN("ocean"),
        BEACH("beach"),
        TAIGA("taiga"),
        SWAMP("swamp"),
        PLAINS("plains"),
        RIVER("river"),
        LAKE("lake"),
        BADLANDS("badlands"),
        MUSHROOM("mushroom");
        final TagKey<Biome> common, forge;

        BiomeTagsPair(String group) {
            common = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_" + group));
            forge = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("forge", "is_" + group));
        }
    }

    private static boolean tagged(Holder<Biome> biome, BiomeTagsPair tags) {
        return biome.is(tags.common) || biome.is(tags.forge);
    }

    private static boolean matches(StoneLayerOreRules.BiomeGroup group, Holder<Biome> biome) {
        return switch (group) {
            case MOUNTAINS ->
                biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL) || tagged(biome, BiomeTagsPair.MOUNTAIN);
            case FROZEN -> biome.value().getBaseTemperature() < 0.15F || tagged(biome, BiomeTagsPair.SNOWY);
            case DESERT -> biome.is(Biomes.DESERT) || tagged(biome, BiomeTagsPair.DESERT);
            case SAVANNA -> biome.is(BiomeTags.IS_SAVANNA) || tagged(biome, BiomeTagsPair.SAVANNA);
            case JUNGLE -> biome.is(BiomeTags.IS_JUNGLE) || tagged(biome, BiomeTagsPair.JUNGLE);
            case OCEAN_BEACH ->
                biome.is(BiomeTags.IS_OCEAN)
                        || biome.is(BiomeTags.IS_BEACH)
                        || tagged(biome, BiomeTagsPair.OCEAN)
                        || tagged(biome, BiomeTagsPair.BEACH);
            case TAIGA -> biome.is(BiomeTags.IS_TAIGA) || tagged(biome, BiomeTagsPair.TAIGA);
            case SWAMP ->
                biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP) || tagged(biome, BiomeTagsPair.SWAMP);
            case PLAINS ->
                biome.is(Biomes.PLAINS)
                        || biome.is(Biomes.SUNFLOWER_PLAINS)
                        || biome.is(Biomes.SNOWY_PLAINS)
                        || tagged(biome, BiomeTagsPair.PLAINS);
            case RIVER_LAKE ->
                biome.is(BiomeTags.IS_RIVER) || tagged(biome, BiomeTagsPair.RIVER) || tagged(biome, BiomeTagsPair.LAKE);
            case MESA -> biome.is(BiomeTags.IS_BADLANDS) || tagged(biome, BiomeTagsPair.BADLANDS);
            case SHROOM -> biome.is(Biomes.MUSHROOM_FIELDS) || tagged(biome, BiomeTagsPair.MUSHROOM);
        };
    }
}
