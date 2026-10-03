package com.gregtech.gregtech.content.storage;

import com.gregtech.gregtech.block.wood.WoodSpecies;
import java.util.ArrayList;
import java.util.List;

/** Loader_MultiTileEntities:184: one wooden bottle crate per available plank identity.
 * Treated planks retain the existing bottle_crate registry id (original texture index 62).
 * Modern vanilla woods use the same plank-driven rule as the original wood dictionary.
 */
public final class BottleCrateVariants {
    private BottleCrateVariants() {}
    public record Wood(String suffix, String plank, String texture) {
        public String id() { return "bottle_crate_" + suffix; }
    }
    public static final List<Wood> WOODS = woods();

    private static List<Wood> woods() {
        List<Wood> result = new ArrayList<>();
        for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "bamboo", "crimson", "warped"))
            result.add(new Wood(wood, "minecraft:" + wood + "_planks", "minecraft:block/" + wood + "_planks"));
        for (WoodSpecies species : WoodSpecies.values()) {
            String texture = switch (species.id()) {
                case "blue_mahoe" -> "bluemahoe";
                case "white_mahoe" -> "wood";
                default -> species.id();
            };
            result.add(new Wood(species.id(), "gregtech:planks_" + species.id(),
                    switch (species.id()) {
                        case "pine" -> "minecraft:block/spruce_planks";
                        case "ebony" -> "minecraft:block/dark_oak_planks";
                        case "white_mahoe" -> "minecraft:block/birch_planks";
                        default -> "gregtech:block/iconsets/planks_" + texture;
                    }));
        }
        for (String wood : List.of("compressed", "dry", "rotten", "mossy", "frozen", "wood"))
            result.add(new Wood(wood, "gregtech:planks_" + wood, "gregtech:block/iconsets/planks_" + wood));
        return List.copyOf(result);
    }
}
