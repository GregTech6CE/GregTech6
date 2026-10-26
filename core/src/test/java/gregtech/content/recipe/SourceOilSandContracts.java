package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMapDefinitions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOilSandRules.*;
import static gregtech.content.recipe.SourceOreCrushingRules.Form;
import static gregtech.content.recipe.SourceOreCrushingRules.Resolver;
import static gregtech.content.recipe.SourceOreCrushingRules.Slot;
import static gregtech.content.recipe.SourceOreCrushingRules.Stack;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SourceRecyclingFacts.Decision;
import static gregtech.content.recipe.SourceRecyclingFacts.Identity;

/** Independent OilSand event literals; native holders, oil binding and matching are tested separately. */
public final class SourceOilSandContracts {
    private SourceOilSandContracts() {}
    private static final Identity STONE = new Identity(8500, "Stone");
    private static final Identity NETHERRACK = new Identity(8502, "Netherrack");
    private static final Identity ENDSTONE = new Identity(8504, "Endstone");
    private static final Resolver ALL = (form, material, count) -> Availability.PRESENT;

    public static void main(String[] args) {
        run();
        System.out.println("Source OilSand contracts passed (no native execution).");
    }
    public static void run() {
        eq(6, MachineRecipeMapDefinitions.Centrifuge.mOutputItemsCount, "actual native solid capacity");
        var raw = plan(new Request(2 * U, List.of()), ALL);
        eq(Status.ROW, raw.status(), "raw source row");
        eq(128L, raw.ticks(), "raw literal duration");
        eq(500L, raw.oilMb(), "raw literal oil amount");
        eq(new Stack(Form.DUST, SAND, 4), raw.slots().get(0).stack(), "raw Sand4");
        eq(6, raw.slots().size(), "six source output positions");
        eq(16, raw.power(), "source16EU/t");
        eq(true, raw.optimize(), "source addRecipe1 optimize true");
        for (int i = 1; i < 6; i++) eq(new Slot(null, 10000), raw.slots().get(i), "raw absent tail");

        var block = plan(new Request(18 * U, List.of()), ALL);
        eq(1152L, block.ticks(), "blockRaw source weight18U, not mAmount=-1");
        eq(4500L, block.oilMb(), "blockRaw oil4500");
        eq(new Stack(Form.DUST, SAND, 36), block.slots().get(0).stack(), "blockRaw Sand36");
        var dense = plan(new Request(4 * U, List.of()), ALL);
        eq(256L, dense.ticks(), "dense weight4U");
        eq(1000L, dense.oilMb(), "dense oil1000");
        eq(8, dense.slots().get(0).stack().count(), "dense Sand8, no inferred host");

        var hosted = plan(new Request(2 * U, List.of(new Byproduct(STONE, 4 * U),
                new Byproduct(NETHERRACK, 2 * U), new Byproduct(ENDSTONE, U))), ALL);
        eq(128L, hosted.ticks(), "host quantities do not add Crusher costs");
        eq(500L, hosted.oilMb(), "host quantities do not increase oil");
        eq(new Stack(Form.DUST, STONE, 4), hosted.slots().get(1).stack(), "direct Stone4");
        eq(new Stack(Form.DUST, NETHERRACK, 2), hosted.slots().get(2).stack(), "direct Netherrack, not target");
        eq(new Stack(Form.DUST, ENDSTONE, 1), hosted.slots().get(3).stack(), "ordered direct Endstone");
        for (var slot : hosted.slots()) eq(10000L, slot.chance(), "all outputs guaranteed");

        var absent = plan(new Request(2 * U, List.of(new Byproduct(STONE, U),
                new Byproduct(NETHERRACK, U))), (form, material, count) ->
                material.equals(STONE) || material.equals(SAND) ? Availability.ABSENT : Availability.PRESENT);
        eq(Status.ROW, absent.status(), "oil and later tail permit absent Sand/host forms");
        eq(null, absent.slots().get(0).stack(), "absent Sand does not become fallback material");
        eq(null, absent.slots().get(1).stack(), "interior missing tail retained");
        eq(NETHERRACK, absent.slots().get(2).stack().material(), "tail not compacted into missing host");
        var fluidOnly = plan(new Request(2 * U, List.of(new Byproduct(STONE, U))),
                (form, material, count) -> Availability.ABSENT);
        eq(Status.ROW, fluidOnly.status(), "fluid-only source row remains valid");
        eq(true, fluidOnly.slots().stream().allMatch(slot -> slot.stack() == null), "no invented solids");

        var ordered = new ArrayList<Byproduct>();
        for (int i = 1; i <= 5; i++) ordered.add(new Byproduct(STONE, i * U));
        ordered.add(new Byproduct(null, -1)); // source never requests byproduct(5)
        var limited = plan(new Request(2 * U, ordered), ALL);
        eq(Status.ROW, limited.status(), "sixth byproduct not inspected");
        for (int i = 1; i <= 5; i++) eq(i, limited.slots().get(i).stack().count(), "first five exact order");

        var fraction = plan(new Request(U / 3, List.of()), ALL);
        eq(22L, fraction.ticks(), "duration rounds up independently");
        eq(84L, fraction.oilMb(), "oil rounds up independently");
        eq(new Stack(Form.DUST_TINY, SAND, 6), fraction.slots().get(0).stack(), "fractional direct dust amount");
        var overWhole = plan(new Request(2 * U + 1, List.of()), ALL);
        eq(129L, overWhole.ticks(), "non-divisible weight ceil64");
        eq(501L, overWhole.oilMb(), "non-divisible weight ceil250");
        var noWhole = plan(new Request(2 * U, List.of()), (form, material, count) ->
                form == Form.DUST ? Availability.ABSENT : Availability.PRESENT);
        eq(new Stack(Form.DUST_SMALL, SAND, 16), noWhole.slots().get(0).stack(), "OM.dust availability fallback");
        var physical = plan(new Request(36 * U, List.of()), ALL);
        eq(new Stack(Form.BLOCK_DUST, SAND, 8), physical.slots().get(0).stack(), "72U physical blockDust first");
        var physicalAbsent = plan(new Request(36 * U, List.of()), (form, material, count) ->
                form == Form.BLOCK_DUST ? Availability.ABSENT : Availability.PRESENT);
        eq(new Stack(Form.DUST, SAND, 64), physicalAbsent.slots().get(0).stack(), "whole fallback capped64");
        var belowDust = plan(new Request(1, List.of(new Byproduct(STONE, U / 72 - 1))),
                (form, material, count) -> { throw new AssertionError("subminimum dust must not resolve"); });
        eq(Status.ROW, belowDust.status(), "positive fluid-only tiny weight");
        eq(1L, belowDust.ticks(), "minimum positive ceil tick");
        eq(1L, belowDust.oilMb(), "minimum positive ceil fluid");
        eq(Status.UNSUPPORTED, plan(new Request(2 * U, List.of()), (form, material, count) ->
                form == Form.DUST ? Availability.UNKNOWN : Availability.PRESENT).status(), "unknown preferred form cannot fall through");
        eq(Status.UNSUPPORTED, plan(new Request(2 * U, List.of(new Byproduct(STONE, U))),
                (form, material, count) -> material.equals(STONE) ? Availability.UNKNOWN : Availability.PRESENT).status(), "unknown host defers whole row");
        eq(Status.UNSUPPORTED, plan(new Request(0, List.of()), ALL).status(), "zero weight unsupported");
        eq(Status.UNSUPPORTED, plan(new Request(-U, List.of()), ALL).status(), "negative weight unsupported");
        eq(Status.UNSUPPORTED, plan(new Request(Long.MAX_VALUE, List.of()), ALL).status(), "overflow unsupported");
        eq(Status.UNSUPPORTED, plan(new Request(U * (long) Integer.MAX_VALUE, List.of()), ALL).status(), "native range unsupported");
        eq(Status.UNSUPPORTED, plan(new Request(U, List.of(new Byproduct(null, U))), ALL).status(), "unresolved identity unsupported");
        eq(Status.UNSUPPORTED, plan(new Request(U, List.of(new Byproduct(STONE, -1))), ALL).status(), "negative byproduct unsupported");
        eq(Status.UNSUPPORTED, plan(new Request(U, List.of(new Byproduct(STONE, Long.MAX_VALUE))), ALL).status(), "extreme dust fallback arithmetic unsupported");
        eq(Status.UNSUPPORTED, plan(null, ALL).status(), "missing context unsupported");
        sourceBoundaries();
    }
    private static void sourceBoundaries() {
        eq(true, isOilSand(9851, "OilSand"), "exact source canonical identity");
        eq(false, isOilSand(9851, "Oilsands"), "symbol/alias is not canonical identity");
        eq(false, isOilSand(9852, "OilSand"), "same name wrong id");
        eq(false, isOilSand(9851, null), "unresolved name");
        for (String prefix : List.of("oreBedrock", "orePoor", "oreSmall", "oreRich", "oreNormal"))
            eq(false, eligible(true, false, prefix), "source excluded prefix " + prefix);
        for (String prefix : List.of("oreRaw", "blockRaw", "oreDense", "oreVanillastone", "oreSand", "oreRedSand", "oreGravel"))
            eq(true, eligible(true, false, prefix), "source admitted prefix " + prefix);
        eq(false, eligible(false, false, "dust"), "ORE permission needed");
        eq(false, eligible(true, true, "oreVanillastone"), "container excluded");
        eq(false, eligible(true, false, null), "unresolved prefix");
        var aliases = List.of("stone", "granite", "stone_granite", "diorite", "stone_diorite", "andesite", "stone_andesite",
                "deepslate", "netherrack", "end_stone", "stone_granite_black", "stone_granite_red", "stone_komatiite",
                "stone_basalt", "stone_marble", "stone_limestone", "stone_shale", "stone_slate", "stone_greenschist",
                "stone_blueschist", "stone_gneiss", "stone_prismarine_light", "stone_prismarine_dark", "stone_kimberlite", "stone_quartzite");
        for (String alias : aliases) {
            var profile = hostProfile(alias);
            eq(Decision.YES, hostDecision(alias), "known ordinary host " + alias);
            eq(2 * U, profile.prefixWeight(), "all reviewed standard hosts2U");
            eq(1, profile.byproducts().size(), "one source host byproduct");
            eq(4 * U, profile.byproducts().get(0).amount(), "source host direct4U");
            var row = plan(new Request(profile.prefixWeight(), profile.byproducts()), ALL);
            eq(128L, row.ticks(), "all standard hosts same event time");
            eq(profile.byproducts().get(0).material(), row.slots().get(1).stack().material(), "source host not transformed");
        }
        for (String alias : List.of("sand", "red_sand", "gravel")) {
            var profile = hostProfile(alias);
            eq(Decision.YES, hostDecision(alias), "DUST_ORE is not an event ban");
            eq(List.of(), profile.byproducts(), "source sediment has no extra byproducts");
            eq(2 * U, profile.prefixWeight(), "source sediment2U");
            eq(false, hasBrokenSourcePair(alias), "no inferred broken sediment");
        }
        eq("oreRedSand", hostProfile("red_sand").sourcePrefix(), "exact source prefix casing");
        eq(Decision.NO, hostDecision("bedrock"), "source bedrock exclusion");
        eq(null, hostProfile("bedrock"), "no fabricated bedrock profile");
        for (String alias : List.of("tuff", "stone_chalk", "stone_dolomite", "stone_gabbro", "stone_gypsum",
                "stone_oilshale", "stone_rhyolite", "stone_salt", "stone_sylvite", "stone_talc"))
            eq(Decision.UNKNOWN, hostDecision(alias), "unproven host remains unknown");
        eq(Decision.UNKNOWN, hostDecision(null), "absent semantic proof not default Stone");
        eq(true, hasBrokenSourcePair("stone"), "reviewed stone broken pair");
        eq(false, hasBrokenSourcePair("deepslate"), "broken deepslate unproved not NO");
        eq(false, hasBrokenSourcePair("stone_gneiss"), "broken gneiss unproved not NO");
    }
    private static void eq(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
    }
}
