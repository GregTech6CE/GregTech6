package gregtech.content.plant;
/** Every registered GT wood plank has a six-direction half block. */
public final class WoodSlabCatalog {
    private WoodSlabCatalog() {}
    /** LoaderWoodDictionary/PlankEntry identities, including the two retained mahoe spellings. */
    public static String material(String wood) {
        return switch (wood) {
            case "blue_mahoe", "bluemahoe" -> "BlueMahoe";
            case "white_mahoe" -> "Mahoe";
            case "bluespruce" -> "BlueSpruce";
            case "cinnamon" -> "Cinnamonwood";
            case "coconut" -> "Coconutwood";
            case "jungle" -> "Junglewood";
            case "dark_oak" -> "DarkOak";
            case "cherry" -> "Cherrywood";
            case "crimson" -> "Crimsonwood";
            case "warped" -> "Warpedwood";
            case "compressed" -> "WoodCompressed";
            case "dry" -> "WoodDead";
            case "mossy" -> "WoodMossy";
            case "rotten" -> "WoodRotten";
            case "frozen" -> "WoodFrozen";
            case "treated" -> "WoodTreated";
            case "rubber" -> "WoodRubber";
            default -> Character.toUpperCase(wood.charAt(0)) + wood.substring(1);
        };
    }
    public static final java.util.List<String> WOODS = java.util.List.of("blue_mahoe", "bluemahoe", "bluespruce", "cinnamon", "coconut", "compressed", "dry", "ebony", "frozen", "hazel", "maple", "mossy", "pine", "rainbowood", "rotten", "rubber", "treated", "white_mahoe", "willow", "wood");
}
