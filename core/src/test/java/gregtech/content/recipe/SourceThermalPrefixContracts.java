package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import gregtech.engine.prefix.PrefixRegistry;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static gregtech.content.recipe.SourceThermalInputRules.InputProof;
import static gregtech.content.recipe.SourceThermalInputRules.plan;
import static gregtech.content.recipe.SourceThermalRecyclingRules.*;

/** Finite source-prefix evidence, not a holder census or blanket native ingredable permission. */
public final class SourceThermalPrefixContracts {
    private SourceThermalPrefixContracts() {}
    private static final long U = 648648000L;
    private static final InputProof VERIFIED = InputProof.VERIFIED_SINGLE_COMPONENT;
    private static final Stats NEUTRAL_STATS = new Stats(1, 293);
    private static final Phase MOLTEN = new Phase(PhaseProof.VERIFIED_SINGLE_BINDING, U, 144, 293, false);

    // Independent literal quantities and declaration lines from pinned OP. No expected amount is
    // obtained from the planner or MaterialPrefix.getMaterialWeight(). These are all 38 ordinary
    // and 30 tool-head prefixes with no OP639-738 secondary-material addition.
    private static final String SOURCE_PREFIXES = """
            rockGt 1459458000 141
            nugget 72072000 170
            chunkGt 162162000 169
            billet 432432000 168
            ingot 648648000 167
            ingotDouble 1297296000 165
            ingotTriple 1945944000 164
            ingotQuadruple 2594592000 163
            ingotQuintuple 3243240000 162
            plateGemTiny 72072000 199
            plateGem 648648000 197
            plateTiny 72072000 198
            plate 648648000 196
            plateDouble 1297296000 195
            plateTriple 1945944000 194
            plateQuadruple 2594592000 193
            plateQuintuple 3243240000 192
            plateDense 5837832000 191
            plateCurved 648648000 200
            foil 162162000 204
            scrapGt 72072000 188
            stick 324324000 205
            stickLong 648648000 206
            bolt 81081000 207
            screw 72072000 208
            gearGt 2594592000 216
            gearGtSmall 648648000 217
            ring 162162000 210
            chain 648648000 211
            spring 648648000 212
            springSmall 162162000 213
            rotor 2756754000 224
            lens 486486000 186
            round 72072000 209
            itemCasing 324324000 219
            wireFine 81081000 214
            minecartWheels 648648000 215
            railGt 162162000 218
            toolHeadSword 1225224000 239
            toolHeadRawSword 1297296000 258
            toolHeadPickaxe 1873872000 240
            toolHeadRawPickaxe 1945944000 259
            toolHeadShovel 576576000 241
            toolHeadRawShovel 648648000 260
            toolHeadSpade 576576000 242
            toolHeadRawSpade 648648000 261
            toolHeadAxe 1873872000 243
            toolHeadRawAxe 1945944000 263
            toolHeadAxeDouble 3099096000 252
            toolHeadRawAxeDouble 3243240000 264
            toolHeadHoe 1225224000 244
            toolHeadRawHoe 1297296000 265
            toolHeadHammer 3891888000 247
            toolHeadFile 972972000 233
            toolHeadChisel 900900000 234
            toolHeadRawChisel 972972000 257
            toolHeadSaw 1225224000 232
            toolHeadRawSaw 1297296000 256
            toolHeadScrewdriver 648648000 248
            toolHeadUniversalSpade 504504000 253
            toolHeadRawUniversalSpade 576576000 262
            toolHeadSense 1873872000 245
            toolHeadRawSense 1945944000 266
            toolHeadPlow 2522520000 246
            toolHeadRawPlow 2594592000 267
            toolHeadBuzzSaw 2594592000 235
            toolHeadArrow 72072000 254
            toolHeadRawArrow 81081000 268
            """;

    public static void main(String[] args) {
        run();
        System.out.println("Additional 68-prefix thermal contracts passed (no native coverage).");
    }
    public static void run() {
        completeSourceTable();
        separatelyScopedBoundaries();
        independentGoldens();
    }

    private static void completeSourceTable() {
        var initial = SourceThermalInputRules.initialForms();
        var additional = SourceThermalInputRules.additionalForms();
        var all = SourceThermalInputRules.forms();
        eq(13, initial.size(), "old input-proof scope preserved");
        eq(68, additional.size(), "complete additional single-primary source scope");
        eq(81, all.size(), "combined scope without dust duplicate");
        eq(initial, all.subList(0, 13), "old thirteen retain order");
        eq(additional, all.subList(13, 81), "new scope follows old scope");
        var identities = new HashSet<MaterialPrefix>();
        for (var form : all) eq(true, identities.add(form.prefix()), "no duplicate prefix identity");
        for (var form : initial)
            eq(false, SourceThermalInputRules.isAdditional(form.prefix()), "old proof is not widened");
        var literals = SOURCE_PREFIXES.strip().split("\n");
        eq(68, literals.length, "independent source inventory size");
        int toolHeads = 0;
        for (int i = 0; i < literals.length; i++) {
            var fields = literals[i].trim().split("\s+");
            var form = additional.get(i);
            long units = Long.parseLong(fields[1]);
            eq(fields[0], form.prefix().getName(), "exact canonical prefix and stable order");
            eq(units, form.sourceUnits(), "source amount literal");
            eq(Integer.parseInt(fields[2]), form.sourceLine(), "source OP declaration evidence");
            eq(units, form.prefix().getMaterialWeight(), "current primary quantity agrees; no metadata repair");
            eq(true, form.recyclable(), "source event permission");
            eq(false, form.ore(), "no source ORE");
            eq(false, form.dirty(), "no source DIRTY");
            eq(true, SourceThermalInputRules.isAdditional(form.prefix()), "extra native proof boundary");
            eq(Truth.YES, SourceThermalInputRules.prefixDecision(form.prefix()), "source prefix permission only");
            eq(form, SourceThermalInputRules.form(form.prefix()), "exact descriptor lookup");
            if (fields[0].startsWith("toolHead")) toolHeads++;
            eq(Decision.SUPPORTED_ROW, plan(form.prefix(), VERIFIED, units, positive(U, true),
                    NEUTRAL_STATS, MOLTEN).decision(), "same algorithm for all68 proven inputs");
            eq(Decision.UNSUPPORTED, plan(form.prefix(), InputProof.UNKNOWN, units, positive(U, true),
                    NEUTRAL_STATS, MOLTEN).decision(), "source permission never substitutes for ingredable proof");
            eq(Decision.UNSUPPORTED, plan(form.prefix(), VERIFIED, units + 1, positive(U, true),
                    NEUTRAL_STATS, MOLTEN).decision(), "quantity override is not normalized silently");
            eq(Decision.SUPPORTED_NO_ROW, plan(form.prefix(), InputProof.ABSENT, 0,
                    null, null, null).decision(), "missing actual input independently proves no row");
        }
        eq(30, toolHeads, "all thirty ordinary/raw tool heads included");
        eq("casingSmall", PrefixRegistry.sourceName(MaterialPrefix.itemCasing.getName()),
                "existing explicit alias, not a guessed casing mapping");
        eq(false, SourceThermalInputRules.isAdditional(null), "no unknown-prefix proof");
    }

    private static void separatelyScopedBoundaries() {
        // OP183,356/360/358/359,250/249: recyclable but known source/native main-amount mismatch.
        var amountMismatch = List.of(MaterialPrefix.bouleGt, MaterialPrefix.plantGtBerry,
                MaterialPrefix.plantGtBlossom, MaterialPrefix.plantGtFiber, MaterialPrefix.plantGtWart,
                MaterialPrefix.toolHeadConstructionPickaxe, MaterialPrefix.toolHeadBuilderwand);
        // OP716-719,730-738: source prefix material additions remain multi-component even when
        // the port's current composition omitted them or a future same-material merge is possible.
        var secondary = List.of(MaterialPrefix.toolHeadPickaxeGem, MaterialPrefix.toolHeadDrill,
                MaterialPrefix.toolHeadChainsaw, MaterialPrefix.toolHeadWrench, MaterialPrefix.plantGtTwig,
                MaterialPrefix.arrowGtWood, MaterialPrefix.arrowGtPlastic, MaterialPrefix.bulletGtSmall,
                MaterialPrefix.bulletGtMedium, MaterialPrefix.bulletGtLarge);
        // unit/glasstube have no source same-name mapping; OP473 coin is not the independent coin item.
        var identity = List.of(MaterialPrefix.unit, MaterialPrefix.coin, MaterialPrefix.glasstube);
        eq(7, amountMismatch.size(), "separate amount mismatch group");
        eq(10, secondary.size(), "separate source secondary group");
        eq(3, identity.size(), "separate identity group");
        for (var group : List.of(amountMismatch, secondary, identity)) for (var prefix : group) {
            eq(Truth.UNKNOWN, SourceThermalInputRules.prefixDecision(prefix), "out-of-scope is not source NO");
            eq(null, SourceThermalInputRules.form(prefix), "no admitted single-primary descriptor");
            eq(false, SourceThermalInputRules.isAdditional(prefix), "not accidentally in positive group");
            eq(Decision.UNSUPPORTED, plan(prefix, VERIFIED, prefix.getMaterialWeight(), positive(U, true),
                    NEUTRAL_STATS, MOLTEN).decision(), "native recoverability/single component is insufficient");
        }
        var excluded = List.of(MaterialPrefix.dustImpure, MaterialPrefix.ingotHot,
                MaterialPrefix.crushed, MaterialPrefix.crushedTiny, MaterialPrefix.oreRaw);
        eq(5, excluded.size(), "all definitive source listener exclusions");
        for (var prefix : excluded) {
            eq(Truth.NO, SourceThermalInputRules.prefixDecision(prefix), "source nonrecyclable/ORE/DIRTY");
            eq(Decision.SUPPORTED_NO_ROW, plan(prefix, VERIFIED, prefix.getMaterialWeight(),
                    positive(U, true), NEUTRAL_STATS, MOLTEN).decision(), "native form does not override source NO");
        }
        eq(Truth.UNKNOWN, SourceThermalInputRules.prefixDecision(MaterialPrefix.dust), "old1U owner retained");
        eq(Truth.UNKNOWN, SourceThermalInputRules.prefixDecision(MaterialPrefix.pipeTiny), "outside item scope is not global ban");
        eq(Truth.UNKNOWN, SourceThermalInputRules.prefixDecision(null), "unknown prefix stays unknown");
    }

    private static void independentGoldens() {
        // Tin MT439 self/U with target FURNACE; each literal volume follows OP's one-item mass.
        quantity(MaterialPrefix.ingot, 648648000, positive(U, true), 648648000, 144, true, "Tin ingot");
        quantity(MaterialPrefix.nugget, 72072000, positive(U, true), 72072000, 16, true, "Tin nugget");
        quantity(MaterialPrefix.plate, 648648000, positive(U, true), 648648000, 144, true, "Tin plate");
        quantity(MaterialPrefix.gearGt, 2594592000L, positive(U, true), 2594592000L, 576, true, "Tin gear");
        quantity(MaterialPrefix.rotor, 2756754000L, positive(U, true), 2756754000L, 612, true, "Tin rotor");
        // Iron MT414 NEVER_FURNACE applies to the output target; Smelter remains source-positive.
        quantity(MaterialPrefix.toolHeadSword, 1225224000, positive(U, false), 1225224000, 272, false, "Iron sword head");
        quantity(MaterialPrefix.toolHeadPickaxe, 1873872000, positive(U, false), 1873872000, 416, false, "Iron pickaxe head");
        // Galena MT3717 target LeadU/3, OP141 rock9U/4. Prefix generation conditions are not gates.
        quantity(MaterialPrefix.rockGt, 1459458000, positive(216216000, true), 486486000, 108, true, "Galena rock");
        var runtime = new Stats(16, 1893); // controlled stats, not imported-stat parity claims
        var hot = new Phase(PhaseProof.VERIFIED_SINGLE_BINDING, U, 144, 1893, false);
        var rock = plan(MaterialPrefix.rockGt, VERIFIED, 1459458000, positive(216216000, true), runtime, hot);
        eq(3999L, rock.row().ticks(), "whole original rock mass, not reduced target mass");
        var rotor = plan(MaterialPrefix.rotor, VERIFIED, 2756754000L, positive(U, true), runtime, hot);
        eq(7555L, rotor.row().ticks(), "fractional rotor mass retained for time");
        var old = SourceThermalRecyclingRules.plan(SourceThermalRecyclingRules.InputProof.VERIFIED_SELF_ONE_UNIT,
                positive(U, true), runtime, hot);
        eq(old, plan(MaterialPrefix.ingot, VERIFIED, U, positive(U, true), runtime, hot),
                "old public1U arithmetic unchanged for actual1U input");
    }
    private static Facts positive(long target, boolean furnace) {
        return new Facts(Truth.YES, Truth.NO, target, furnace ? Truth.YES : Truth.NO, Truth.NO);
    }
    private static void quantity(MaterialPrefix prefix, long actual, Facts facts, long target,
                                 int mb, boolean melter, String label) {
        var result = plan(prefix, VERIFIED, actual, facts, NEUTRAL_STATS, MOLTEN);
        eq(Decision.SUPPORTED_ROW, result.decision(), label + " supported row");
        eq(target, result.row().targetUnits(), label + " target material amount");
        eq(mb, result.row().millibuckets(), label + " independent fluid golden");
        eq(melter, result.row().melter(), label + " target Melter permission");
        eq(true, result.row().smelter(), label + " Smelter permission");
        eq(16, result.row().power(), label + " source power");
    }
    private static void eq(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
    }
}
