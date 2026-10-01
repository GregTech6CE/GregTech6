package com.gregtech.gregtech.worldgen;
import com.gregtech.gregtech.block.wood.WoodSpecies;
/** Original GT6 decay boxes and tree-hole refill gates, used by both platforms. */
public final class TreeSpeciesRules {
 private TreeSpeciesRules(){}
 public static boolean allowsSand(WoodSpecies species){return species==WoodSpecies.COCONUT;}
    public static int rangeSide(WoodSpecies species) {
        return switch (species) {
            case RUBBER -> 2;                                    // AB meta 0
            case WILLOW, COCONUT -> 4;                           // AB metas 2 and 6
            case BLUE_SPRUCE, PINE -> 6;                         // CD meta 0 (the conifer's wide skirt)
            case MAPLE, BLUE_MAHOE, HAZEL, CINNAMON, RAINBOWOOD -> 3;
            // The wood-dictionary species borrow their shape's ranges.
            case EBONY -> 3;
            case WHITE_MAHOE -> 3;
        };
    }
    public static int rangeYNeg(WoodSpecies species) {
        return switch (species) {
            case BLUE_MAHOE -> 4;                                // AB meta 3
            case CINNAMON, RAINBOWOOD -> 3;                      // AB metas 5 and 7
            case COCONUT -> 1;                                   // AB meta 6
            case WHITE_MAHOE -> 4;                               // borrows the blue mahoe shape
            default -> 2;                                        // rubber, maple, willow, hazel, spruce, pine, ebony
        };
    }
    public static int threshold(WoodSpecies species) {
        return species == WoodSpecies.RUBBER ? 60 : 250;
    }
    public static int divisor(WoodSpecies species) {
        return switch (species) {
            case RUBBER -> 260;
            case MAPLE -> 560;
            case RAINBOWOOD -> 420;
            default -> 1; // no GT6 hole exists for the other species
        };
    }
}
