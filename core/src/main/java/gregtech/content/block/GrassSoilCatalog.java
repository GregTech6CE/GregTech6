package gregtech.content.block;

/** Six colored grass identities with their side/top texture names and plant-soil flags. */
public final class GrassSoilCatalog {

    private GrassSoilCatalog() {
    }

    public record Spec(String id, String side, String top, boolean plantSoil) {
    }

    public static final java.util.List<Spec> SPECS = java.util.List.of(
            new Spec("grassblock_brown", "grassblock_side_brown", "grassblock_top_brown", true),
            new Spec("grassblock_dark", "grassblock_side_dark", "grassblock_top_dark", true),
            new Spec("grassblock_light", "grassblock_side_light", "grassblock_top_light", true),
            new Spec("grassblock_medium", "grassblock_side_medium", "grassblock_top_medium", true),
            new Spec("grassblock_normal", "grassblock_side_normal", "grassblock_top_normal", true),
            new Spec("grassblock_yellow", "grassblock_side_yellow", "grassblock_top_yellow", true));
}
