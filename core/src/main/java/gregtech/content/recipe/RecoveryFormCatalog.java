package gregtech.content.recipe;

import gregtech.content.data.generated.RecyclablePrefixes;
import java.util.Set;

/**
 * The single prefix membership table used by recovery producers.
 *
 * <p>{@link RecyclablePrefixes} is the generated Shredder-handler subset. The broader source
 * RECYCLABLE set also feeds thermal conversion in another recipe family, so both roles live here
 * instead of as parallel private tables in their producers.</p>
 */
public final class RecoveryFormCatalog {
    private RecoveryFormCatalog() {}

    /** Source {@code OP} prefixes carrying RECYCLABLE at the pinned source commit. */
    private static final Set<String> SOURCE_RECYCLABLE = Set.of(
            "crushed", "crushedTiny", "crushedPurified", "crushedPurifiedTiny", "crushedCentrifuged",
            "crushedCentrifugedTiny", "rockGt", "rawOreChunk", "clump", "cluster", "pebbles", "rubble",
            "chunk", "crystalline", "reduced", "cleanGravel", "dirtyGravel", "dust", "dustSmall",
            "dustTiny", "dustDiv72", "ingotQuintuple", "ingotQuadruple", "ingotTriple", "ingotDouble",
            "ingot", "billet", "chunkGt", "nugget", "gem", "gemChipped", "gemFlawed", "gemFlawless",
            "gemExquisite", "gemLegendary", "bouleGt", "crystalPure", "crystal", "lens", "scrapGt",
            "plateSteamcraft", "plateDense", "plateQuintuple", "plateQuadruple", "plateTriple",
            "plateDouble", "plate", "plateGem", "plateTiny", "plateGemTiny", "plateCurved", "compressed",
            "sheetGt", "foil", "stick", "stickLong", "bolt", "screw", "round", "ring", "chain", "spring",
            "springSmall", "wireFine", "minecartWheels", "gearGt", "gearGtSmall", "railGt", "casingSmall",
            "casingMachine", "casingMachineDouble", "casingMachineQuadruple", "casingMachineDense", "rotor",
            "cell", "toolHeadSaw", "toolHeadFile", "toolHeadChisel", "toolHeadBuzzSaw",
            "toolHeadChainsaw", "toolHeadWrench", "toolHeadDrill", "toolHeadSword", "toolHeadPickaxe",
            "toolHeadShovel", "toolHeadSpade", "toolHeadAxe", "toolHeadHoe", "toolHeadSense",
            "toolHeadPlow", "toolHeadHammer", "toolHeadScrewdriver", "toolHeadBuilderwand",
            "toolHeadConstructionPickaxe", "toolHeadPickaxeGem", "toolHeadAxeDouble",
            "toolHeadUniversalSpade", "toolHeadArrow", "toolHeadRawSaw", "toolHeadRawChisel",
            "toolHeadRawSword", "toolHeadRawPickaxe", "toolHeadRawShovel", "toolHeadRawSpade",
            "toolHeadRawUniversalSpade", "toolHeadRawAxe", "toolHeadRawAxeDouble", "toolHeadRawHoe",
            "toolHeadRawSense", "toolHeadRawPlow", "toolHeadRawArrow", "toolSword", "toolPickaxe",
            "toolShovel", "toolAxe", "toolHoe", "toolShears", "bulletGtSmall", "bulletGtMedium",
            "bulletGtLarge", "arrowGtWood", "arrowGtPlastic", "armorHelmet", "armorChestplate",
            "armorLeggings", "armorBoots", "frameGt", "capcellcon", "pipeTiny", "pipeSmall",
            "pipeMedium", "pipeLarge", "pipeHuge", "pipeQuadruple", "pipeNonuple", "pipeRestrictiveTiny",
            "pipeRestrictiveSmall", "pipeRestrictiveMedium", "pipeRestrictiveLarge",
            "pipeRestrictiveHuge", "wireGt16", "wireGt15", "wireGt14", "wireGt13", "wireGt12", "wireGt11",
            "wireGt10", "wireGt09", "wireGt08", "wireGt07", "wireGt06", "wireGt05", "wireGt04", "wireGt03",
            "wireGt02", "wireGt01", "cableGt12", "cableGt08", "cableGt04", "cableGt02", "cableGt01",
            "blockGem", "blockDust", "blockIngot", "blockPlate", "blockPlateGem", "blockSolid",
            "oreberry");

    /**
     * The source listener rejects these dirty ore-processing forms before thermal conversion.
     * They retain their distinct Shredder and ore-processing producers.
     */
    private static final Set<String> THERMAL_ORE_EXCLUSIONS = Set.of(
            "chunk", "cluster", "crushed", "crushedTiny", "dirtyGravel", "pebbles", "rawOreChunk", "rubble");

    /** The generated prefix-handler subset eligible for generic Shredder recycling. */
    public static boolean shredder(String prefixName) {
        return RecyclablePrefixes.NAMES.contains(prefixName);
    }

    /** Read-only Shredder eligibility set for prefix-availability diagnostics. */
    public static Set<String> shredderNames() {
        return RecyclablePrefixes.NAMES;
    }

    /** The source recyclable set after the thermal listener's dirty-ore exclusion. */
    public static boolean thermal(String prefixName) {
        return SOURCE_RECYCLABLE.contains(prefixName) && !THERMAL_ORE_EXCLUSIONS.contains(prefixName);
    }

    /** Filled vessels are not recycled by either source listener. */
    public static boolean container(String prefixName) {
        return prefixName.equals("chemtube") || prefixName.equals("cell") || prefixName.equals("capcellcon");
    }
}
