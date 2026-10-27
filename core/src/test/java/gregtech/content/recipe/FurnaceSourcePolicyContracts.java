package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.List;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceRecyclingFacts.*;
import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Source input permission and routing; no registry mutation or native output-form assumption. */
public final class FurnaceSourcePolicyContracts {
    private FurnaceSourcePolicyContracts() {}
    public static void main(String[] args) {
        gregtech.api.mod.ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        run();
        System.out.println("Furnace source policy contracts passed (no native registration claim).");
    }
    public static void run() {
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        closedDefaultContexts();
        inputPermissionNotThermalPermission();
        pureOverrideBoundaries();
        knownHiddenRouting();
        unknownCompatibility();
        amountsAndExperience();
    }
    private static GTMaterial material(int id, String name) {
        GTMaterial material = GTMaterialRegistry.get(id).resolve();
        equal(name, material.getName(), "exact test identity");
        return material;
    }
    private static void closedDefaultContexts() {
        long positives = 0;
        for (Facts facts : SourceRecyclingFacts.all()) {
            GTMaterial material = material(facts.identity().id(), facts.identity().name());
            equal(facts, OreSourcePolicy.reviewedFurnaceFacts(material), "default context resolves source facts");
            equal(facts.furnace(), OreSourcePolicy.furnaceDecision(material), "source input FURNACE is shared");
            boolean allowed = facts.furnace() == Decision.YES && !material.has(MaterialProperty.HIDDEN);
            equal(allowed, OreSourcePolicy.furnaceInput(material), "early producer cannot bypass a source NO");
            equal(allowed, OreSourcePolicy.usesReviewedFurnaceListener(material), "known listener ownership agrees");
            equal(allowed, FurnaceSmeltingRules.allows(material), "listener permission agrees");
            if (facts.furnace() == Decision.YES) positives++;
        }
        equal(186L, positives, "audited input-permission positives, not registered rows");
        for (int id : new int[]{8365, 8390}) {
            GTMaterial coke = GTMaterialRegistry.get(id);
            check(coke.has(MaterialProperty.MELTING), "built-in postInit coke MELTING is present");
            equal(Decision.NO, OreSourcePolicy.furnaceDecision(coke), "coke startup flag is not furnace permission");
        }
    }
    private static void inputPermissionNotThermalPermission() {
        for (int id : new int[]{280, 260, 220, 9175, 9114, 8008}) {
            GTMaterial input = GTMaterialRegistry.get(id);
            equal(Decision.NO, OreSourcePolicy.furnaceDecision(input), "known input NO " + input.getName());
            check(!OreSourcePolicy.furnaceInput(input), "no MELTING fallback for " + input.getName());
        }
        GTMaterial stone = material(8500, "Stone");
        check(!stone.has(MaterialProperty.MELTING), "current Stone lacks MELTING");
        equal(Decision.YES, OreSourcePolicy.furnaceDecision(stone), "stone factory FURNACE is independently positive");
        check(!FurnaceSmeltingRules.legacyAllows(stone), "unproven external callers retain their old positive-only policy");
        check(OreSourcePolicy.usesReviewedFurnaceListener(stone),
                "known positive remains listener-owned even if an actual ingot output is unavailable");
        GTMaterial dolomite = material(9163, "Dolomite");
        equal(Decision.YES, OreSourcePolicy.furnaceDecision(dolomite), "input Dolomite FURNACE positive");
        equal(Decision.NO, OreSourcePolicy.furnaceDecision(dolomite.getTargetSmeltingMaterial()),
                "its Calcite target permission is not the form gate");
        GTMaterial netherrack = material(8502, "Netherrack");
        check(find(8502, "Netherrack").blacklistedSmelter() == Decision.YES, "source thermal blacklist fixture");
        equal(Decision.YES, OreSourcePolicy.furnaceDecision(netherrack),
                "thermal BLACKLISTED_SMELTER is not ordinary furnace permission");
    }
    private static OreSourcePolicy.FurnaceContext context(Facts facts) {
        PortDefaults defaults = contextDefaults(facts.identity().id(), facts.identity().name());
        return new OreSourcePolicy.FurnaceContext(facts.identity(), true, false, defaults.melting(),
                defaults.target(), true, false, defaults.targetUnitsPerU());
    }
    private static void pureOverrideBoundaries() {
        Facts nickel = find(280, "Nickel");
        var base = context(nickel);
        equal(nickel, OreSourcePolicy.reviewedFurnaceContext(base), "pure default context");
        equal(null, OreSourcePolicy.reviewedFurnaceContext(new OreSourcePolicy.FurnaceContext(
                base.material(), false, false, base.melting(), base.target(), true, false, base.targetUnitsPerU())),
                "same source ID/name without core ownership is unsupported");
        equal(null, OreSourcePolicy.reviewedFurnaceContext(new OreSourcePolicy.FurnaceContext(
                base.material(), true, true, base.melting(), base.target(), true, false, base.targetUnitsPerU())),
                "addon ownership is unsupported");
        equal(null, OreSourcePolicy.reviewedFurnaceContext(new OreSourcePolicy.FurnaceContext(
                base.material(), true, false, !base.melting(), base.target(), true, false, base.targetUnitsPerU())),
                "MELTING mutation is context evidence, not guessed source permission");
        equal(null, OreSourcePolicy.reviewedFurnaceContext(new OreSourcePolicy.FurnaceContext(
                base.material(), true, false, base.melting(), new Identity(300, "Zinc"), true, false, U)),
                "changed smelting target is unsupported");
        equal(null, OreSourcePolicy.reviewedFurnaceContext(new OreSourcePolicy.FurnaceContext(
                base.material(), true, false, base.melting(), base.target(), true, false, U / 2)),
                "changed ratio is unsupported");
        equal(null, OreSourcePolicy.reviewedFurnaceContext(new OreSourcePolicy.FurnaceContext(
                base.material(), true, false, base.melting(), base.target(), false, false, U)),
                "replacement target object without core ownership is unsupported");
        equal(null, OreSourcePolicy.reviewedFurnaceContext(new OreSourcePolicy.FurnaceContext(
                new Identity(9516, "Superconductor"), true, false, true,
                new Identity(9516, "Superconductor"), true, false, U)), "unknown source identity stays unsupported");
    }
    private static void knownHiddenRouting() {
        // All three known-decision entrypoints share this guard. The raw source fact remains YES;
        // denying listener ownership must also deny the earlier dust-first producer, not expose fallback.
        equal(false, FurnaceSmeltingRules.reviewedAllows(Decision.YES, true), "known YES + HIDDEN is not routed early");
        equal(true, FurnaceSmeltingRules.reviewedAllows(Decision.YES, false), "visible known YES is listener-owned");
        equal(false, FurnaceSmeltingRules.reviewedAllows(Decision.NO, false), "known NO is final without legacy fallback");
        equal(false, FurnaceSmeltingRules.reviewedAllows(Decision.NO, true), "hidden known NO stays denied");
    }
    private static void unknownCompatibility() {
        GTMaterial unregistered = new GTMaterial(280, "Nickel", "Nickel", 0xffffff).put(MaterialProperty.MELTING);
        equal(Decision.UNKNOWN, OreSourcePolicy.furnaceDecision(unregistered), "unregistered matching name/ID is not core");
        check(OreSourcePolicy.furnaceInput(unregistered), "unknown early caller retains old MELTING fallback");
        check(!OreSourcePolicy.usesReviewedFurnaceListener(unregistered), "unknown is not newly reserved for listener");
        check(!FurnaceSmeltingRules.allows(unregistered), "unknown listener retains old workability fallback");
        GTMaterial hiddenLegacy = new GTMaterial(300, "Zinc", "Zinc", 0xffffff)
                .put(MaterialProperty.MELTING, MaterialProperty.HIDDEN);
        equal(Decision.UNKNOWN, OreSourcePolicy.furnaceDecision(hiddenLegacy), "unregistered hidden case stays unknown");
        check(OreSourcePolicy.furnaceInput(hiddenLegacy), "UNKNOWN early-caller behavior is deliberately unchanged");
        check(!FurnaceSmeltingRules.allows(hiddenLegacy), "existing listener hidden guard is retained");
        check(!OreSourcePolicy.usesReviewedFurnaceListener(hiddenLegacy), "unknown is not newly listener-owned");
        check(!FurnaceSmeltingRules.allows(null), "null input denied");
        check(!OreSourcePolicy.furnaceInput(null), "null early input denied");
    }
    private static void amountsAndExperience() {
        equal(U / 3, FurnaceSmeltingRules.amount(U, U / 3), "Galena U input conversion once");
        equal(U / 27, FurnaceSmeltingRules.amount(U / 9, U / 3), "tiny form scales the smelting ratio");
        equal(2 * U / 3, FurnaceSmeltingRules.amount(2 * U, U / 3), "external two-unit form");
        equal(1L, FurnaceSmeltingRules.experience(U / 27, 0), "XP rounds upward independently of output volume");
        equal(2L, FurnaceSmeltingRules.experience(U / 3, 3), "ceil one-third times input quality plus one");
        equal(0L, FurnaceSmeltingRules.experience(0, 3), "zero output has no XP");
        var noExperience = FurnaceSmeltingRules.EXTERNAL.stream().filter(row -> !row.experience())
                .map(FurnaceSmeltingRules.ExternalRow::input).toList();
        equal(List.of(MaterialPrefix.dustPure, MaterialPrefix.dustRefined, MaterialPrefix.dustImpure),
                noExperience, "dirty dust-family listeners preserve source no-XP metadata");
    }
}
