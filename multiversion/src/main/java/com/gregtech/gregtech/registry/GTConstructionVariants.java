package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.misc.FixedConstructionBlocks;
import com.gregtech.gregtech.block.misc.FixedConstructionColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** New color identities and the retained legacy palette blocks share this explicit lookup. */
public final class GTConstructionVariants {
    private static final Map<String, RegisteredBlock<? extends Block>> LEGACY = new LinkedHashMap<>();
    private static final Map<String, Map<DyeColor, RegisteredBlock<? extends Block>>> VARIANTS = new LinkedHashMap<>();
    private static final List<RegisteredBlock<? extends Block>> ALL = new ArrayList<>();
    private GTConstructionVariants() {}

    public static void initialize(List<RegisteredBlock<? extends Block>> legacy) {
        if (!VARIANTS.isEmpty()) throw new IllegalStateException("Construction variants initialized twice");
        for (var source : legacy) {
            String family = source.getId().getPath();
            LEGACY.put(family, source);
            var colors = new EnumMap<DyeColor, RegisteredBlock<? extends Block>>(DyeColor.class);
            VARIANTS.put(family, colors);
            for (DyeColor color : DyeColor.values()) {
                var variant = RegisteredBlock.register(family + "_" + color.getName(), () -> create(family, color, properties(source.get())));
                colors.put(color, variant);
                ALL.add(variant);
            }
        }
    }

    private static BlockBehaviour.Properties properties(Block source) {
//? if >=1.21.1 {
        /*return BlockBehaviour.Properties.ofFullCopy(source);
*///?} else {
        return BlockBehaviour.Properties.copy(source);
//?}
    }

    private static Block create(String family, DyeColor color, BlockBehaviour.Properties properties) {
        return switch (family) {
            case "asphalt" -> new FixedConstructionBlocks.Asphalt(color, properties);
            case "concrete", "concrete_reinforced" -> new FixedConstructionBlocks.Concrete(color, family.endsWith("reinforced"), properties);
            case "cfoam" -> new FixedConstructionBlocks.Foam(color, properties);
            case "cfoam_fresh" -> new FixedConstructionBlocks.WetFoam(color, properties);
            case "cfoam_slab" -> new FixedConstructionBlocks.FoamSlab(color, properties);
            case "glass_clear", "glass_glow" -> new FixedConstructionBlocks.Glass(color, family.equals("glass_glow"), properties);
            case "glass_clear_slab", "glass_glow_slab" -> new FixedConstructionBlocks.GlassSlab(color, family.equals("glass_glow_slab"), properties);
            default -> throw new IllegalArgumentException("Unknown construction family " + family);
        };
    }

    public static String family(Block block) {
        if (block instanceof FixedConstructionColor fixed) return fixed.family();
        var key = BuiltInRegistries.BLOCK.getKey(block);
        return key.getNamespace().equals("gt6ce") && LEGACY.containsKey(key.getPath()) ? key.getPath() : null;
    }

    public static boolean legacy(Block block) { return !(block instanceof FixedConstructionColor) && family(block) != null; }

    public static Block coloredBlock(Block block, DyeColor color) {
        String family = family(block);
        return family == null ? null : VARIANTS.get(family).get(color).get();
    }

    public static List<RegisteredBlock<? extends Block>> all() { return List.copyOf(ALL); }

    public static Block[] wetFoams() {
        var result = new ArrayList<Block>();
        result.add(LEGACY.get("cfoam_fresh").get());
        VARIANTS.get("cfoam_fresh").values().forEach(block -> result.add(block.get()));
        return result.toArray(Block[]::new);
    }

    public static DyeColor defaultColor(String family) {
        return switch (family) {
            case "asphalt" -> DyeColor.GRAY;
            case "concrete", "concrete_reinforced", "glass_clear", "glass_glow" -> DyeColor.LIGHT_GRAY;
            default -> DyeColor.WHITE;
        };
    }

    public static String defaultId(String family) { return family + "_" + defaultColor(family).getName(); }

    public static <T extends Block> RegisteredBlock<T> defaultBlock(String family) {
        return RegisteredBlock.reference(defaultId(family));
    }
}
