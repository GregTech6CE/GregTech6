package gregtech.content.recipe;

import gregtech.api.material.GTValues;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * GT6's generifier rule: every material whose {@code mTargetGenerifying} differs from itself becomes
 * one {@code RecipeMapHandlerMaterial} on {@code RM.Generifier}, and that handler expands over every
 * prefix the condition {@code And(SIMPLIFIABLE, ingotHot.NOT)} accepts. Each row converts one item of
 * a form in the source material into one item of the same form in the target material; the duration
 * is the form's material weight in ticks, the EU cost is zero, and nothing converts back.
 *
 * <p>The material targets are {@code MT.java}'s {@code setGenerifying} calls plus the
 * {@code ANY.*.mToThis} loops that follow them, keyed by GT6 material id so the table does not depend
 * on display names. {@code ANY} groups that point at a plain material other than their own name
 * (ashes, gems, woods, wax, rubber, plastics, phosphorus, clay, silicon) are already folded into the
 * targets, because the loops overwrite the explicit rows they contain.</p>
 */
public final class GenerifyingRules {
    private GenerifyingRules() {}

    /** {@code MT.java} {@code setGenerifying} rows and {@code ANY.*.mToThis} loops, as source/target ids. */
    private static final int[] TARGET_IDS = {
        8196, 8218, 8197, 8218, 8201, 8200, 8222, 8221, 8224, 8221, 8236, 8235, 8238, 8235, 8239, 8235, 8267, 8221,
        8276, 8215, 8278, 8300, 8289, 8221, 8290, 8221, 8296, 8221, 8297, 8221, 8302, 8304, 8305, 8304, 8307, 8306,
        8314, 8304, 8315, 8304, 8323, 8301, 8324, 8301, 8328, 8304, 8346, 8445, 8347, 8445, 8348, 8347, 8375, 8333,
        8376, 8332, 8377, 8300, 8378, 8334, 8379, 8301, 8380, 260, 8383, 8304, 8384, 8301, 8385, 8301, 8386, 8301, 8387,
        8301, 8393, 8445, 8395, 8445, 8396, 8445, 8397, 8445, 8398, 8445, 8399, 8445, 8418, 8221, 8422, 8310, 8423,
        8300, 8424, 8300, 8425, 8309, 8426, 8309, 8427, 8309, 8428, 8309, 8429, 8309, 8431, 8430, 8432, 8430, 8433,
        8430, 8434, 8430, 8435, 8430, 8436, 9215, 8437, 9215, 8438, 9215, 8439, 9215, 8440, 9215, 8441, 9215, 8442,
        9215, 8443, 9215, 8444, 9215, 8455, 8215, 8458, 8208, 8459, 8208, 8460, 8208, 8464, 8300, 8465, 8300, 8467,
        8300, 8468, 8300, 8501, 8500, 8503, 8502, 8505, 8500, 8506, 8500, 8507, 8500, 8508, 8500, 8511, 8500, 8516,
        8500, 8518, 8500, 8521, 8500, 8528, 8500, 8599, 8500, 8640, 290, 8643, 260, 8645, 260, 8646, 8630, 8647, 600,
        8652, 740, 8674, 8630, 8675, 8630, 8679, 260, 8697, 8634, 8698, 8673, 8720, 260, 8803, 260, 9000, 8500, 9153,
        8215, 9154, 8215, 9167, 8215, 9170, 8500, 9171, 8500, 9176, 8500, 9177, 8500, 9179, 8500, 9180, 8500, 9184,
        8500, 9185, 8500, 9186, 8500, 9187, 8500, 9188, 8500, 9189, 8500, 9190, 8500, 9218, 8500, 9222, 8500, 9223,
        8500, 9248, 8500, 9261, 8500, 9300, 8221, 9301, 8221, 9302, 8221, 9303, 8221, 9304, 8221, 9305, 8221, 9306,
        8221, 9307, 8221, 9308, 8221, 9309, 8221, 9310, 8221, 9311, 8221, 9312, 8221, 9313, 8221, 9314, 8221, 9315,
        8221, 9316, 8221, 9317, 8221, 9318, 8221, 9319, 8221, 9344, 8221, 9372, 8221, 9409, 8221, 9853, 9190,
    };

    /**
     * GT6 {@code OP.java} prefixes carrying {@code TD.Prefix.SIMPLIFIABLE}, under the port's prefix
     * name ({@code OP.casingSmall} is {@code itemCasing} and {@code OP.boule} is {@code bouleGt}).
     * {@code OP.ingotHot} carries no SIMPLIFIABLE tag, which is how hot ingots stay out of the rule.
     */
    private static final Set<String> SIMPLIFIABLE = Set.of(
            "arrowGtPlastic",
            "arrowGtWood",
            "billet",
            "blockDust",
            "blockGem",
            "blockIngot",
            "blockPlate",
            "blockPlateGem",
            "blockSolid",
            "bolt",
            "bouleGt",
            "bulletGtLarge",
            "bulletGtMedium",
            "bulletGtSmall",
            "cableGt01",
            "cableGt02",
            "cableGt04",
            "cableGt08",
            "cableGt12",
            "capcellcon",
            "casingMachine",
            "casingMachineDense",
            "casingMachineDouble",
            "casingMachineQuadruple",
            "itemCasing",
            "chain",
            "chunkGt",
            "compressed",
            "dust",
            "dustDiv72",
            "dustSmall",
            "dustTiny",
            "foil",
            "frameGt",
            "gearGt",
            "gearGtSmall",
            "gem",
            "gemChipped",
            "gemExquisite",
            "gemFlawed",
            "gemFlawless",
            "gemLegendary",
            "ingot",
            "ingotDouble",
            "ingotQuadruple",
            "ingotQuintuple",
            "ingotTriple",
            "lens",
            "minecartWheels",
            "nugget",
            "pipeHuge",
            "pipeLarge",
            "pipeMedium",
            "pipeNonuple",
            "pipeQuadruple",
            "pipeRestrictiveHuge",
            "pipeRestrictiveLarge",
            "pipeRestrictiveMedium",
            "pipeRestrictiveSmall",
            "pipeRestrictiveTiny",
            "pipeSmall",
            "pipeTiny",
            "plate",
            "plateCurved",
            "plateDense",
            "plateDouble",
            "plateGem",
            "plateGemTiny",
            "plateQuadruple",
            "plateQuintuple",
            "plateSteamcraft",
            "plateTiny",
            "plateTriple",
            "railGt",
            "ring",
            "rotor",
            "round",
            "scrapGt",
            "screw",
            "sheetGt",
            "spring",
            "springSmall",
            "stick",
            "stickLong",
            "wireFine",
            "wireGt01",
            "wireGt02",
            "wireGt03",
            "wireGt04",
            "wireGt05",
            "wireGt06",
            "wireGt07",
            "wireGt08",
            "wireGt09",
            "wireGt10",
            "wireGt11",
            "wireGt12",
            "wireGt13",
            "wireGt14",
            "wireGt15",
            "wireGt16");

    private static final Map<Integer, Integer> TARGETS;

    static {
        Map<Integer, Integer> targets = new HashMap<>(TARGET_IDS.length / 2);
        for (int i = 0; i < TARGET_IDS.length; i += 2) targets.put(TARGET_IDS[i], TARGET_IDS[i + 1]);
        TARGETS = Map.copyOf(targets);
    }

    /** The target material id, or {@code materialId} itself when the source has no conversion. */
    public static int target(int materialId) {
        Integer target = TARGETS.get(materialId);
        return target == null ? materialId : target;
    }

    /** Whether the source applies the material conversion to this form at all. */
    public static boolean simplifiable(String prefixName) {
        return SIMPLIFIABLE.contains(prefixName);
    }

    /** {@code RecipeMapHandlerMaterial.getCosts}: the form's material weight in ticks, at least one. */
    public static long duration(long formWeight) {
        return Math.max(1, (formWeight + GTValues.U - 1) / GTValues.U);
    }
}
