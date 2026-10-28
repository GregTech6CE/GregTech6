package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Original GT6 {@code Loader_Recipes_Alloys} (3703e403) fluid recipes.
 * Amounts are literal millibuckets, not material units; no 144 mB conversion applies.
 * The 52 source mix calls expand to 101 rows with the pinned Glowstone membership.
 */
public final class SourceAlloyRecipes {
    private SourceAlloyRecipes() {}

    public static final int POWER = 16;

    public record Amount(String fluidName, int millibuckets) {
        public Amount {
            Objects.requireNonNull(fluidName, "fluidName");
            if (fluidName.isBlank() || millibuckets <= 0) throw new IllegalArgumentException("Invalid fluid amount");
        }
    }

    /** Selector 0 means no selector (Smelter); Mixer selectors 2/3/4 are retained inputs. */
    public record Row(List<Amount> inputs, Amount output, long ticks, int selector) {
        public Row {
            inputs = List.copyOf(inputs);
            Objects.requireNonNull(output, "output");
            if (ticks <= 0
                    || (selector == 0 ? inputs.size() != 1 : selector < 2 || selector > 4 || inputs.size() != selector))
                throw new IllegalArgumentException("Invalid alloy row");
        }
    }

    private static final List<Row> SMELTER = List.of(
            new Row(List.of(amount("molten.copper", 1)), amount("molten.annealedcopper", 1), 16, 0),
            new Row(List.of(amount("molten.iron", 1)), amount("molten.wroughtiron", 1), 16, 0));
    private static final List<Row> MIXER = mixerRows();

    public static List<Row> smelter() {
        return SMELTER;
    }

    public static List<Row> mixer() {
        return MIXER;
    }

    private static List<Row> mixerRows() {
        List<Row> rows = new ArrayList<>();
        for (String copper : List.of("molten.copper", "molten.annealedcopper")) {
            mix(rows, amount("molten.constantan", 2), amount(copper, 1), amount("molten.nickel", 1));
            mix(rows, amount("molten.arseniccopper", 4), amount(copper, 3), amount("molten.arsenic", 1));
            mix(rows, amount("molten.bronze", 4), amount(copper, 3), amount("molten.tin", 1));
            mix(rows, amount("molten.brass", 4), amount(copper, 3), amount("molten.zinc", 1));
            mix(rows, amount("molten.aluminiumbrass", 4), amount(copper, 1), amount("molten.aluminium", 3));
            mix(rows, amount("molten.redalloy", 1), amount(copper, 1), amount("molten.redstone", 4));
            mix(rows, amount("molten.sterlingsilver", 5), amount(copper, 1), amount("molten.silver", 4));
            mix(
                    rows,
                    amount("molten.purplealloy", 2),
                    amount(copper, 1),
                    amount("molten.silver", 1),
                    amount("molten.redstone", 4),
                    amount("molten.nikolite", 4));
            mix(
                    rows,
                    amount("molten.signalum", 4),
                    amount(copper, 3),
                    amount("molten.silver", 1),
                    amount("molten.redstone", 10));
            mix(
                    rows,
                    amount("molten.signalum", 8),
                    amount(copper, 1),
                    amount("molten.silver", 2),
                    amount("molten.redalloy", 5));
            mix(
                    rows,
                    amount("molten.blackbronze", 5),
                    amount(copper, 3),
                    amount("molten.silver", 1),
                    amount("molten.gold", 1));
            mix(
                    rows,
                    amount("molten.hepatizon", 8),
                    amount(copper, 3),
                    amount("molten.tin", 1),
                    amount("molten.gold", 4));
            mix(rows, amount("molten.blackbronze", 5), amount(copper, 3), amount("molten.electrum", 2));
            mix(rows, amount("molten.rosegold", 5), amount(copper, 1), amount("molten.gold", 4));
            mix(
                    rows,
                    amount("molten.arsenicbronze", 5),
                    amount(copper, 3),
                    amount("molten.tin", 1),
                    amount("molten.arsenic", 1));
            mix(
                    rows,
                    amount("molten.bismuthbronze", 5),
                    amount(copper, 3),
                    amount("molten.zinc", 1),
                    amount("molten.bismuth", 1));
        }
        for (String iron : List.of("molten.iron", "molten.wroughtiron", "molten.pigiron", "molten.meteoriciron")) {
            mix(rows, amount("molten.invar", 3), amount(iron, 2), amount("molten.nickel", 1));
            mix(
                    rows,
                    amount("molten.kanthal", 3),
                    amount(iron, 1),
                    amount("molten.aluminium", 1),
                    amount("molten.chromium", 1));
            mix(rows, amount("molten.tinalloy", 2), amount(iron, 1), amount("molten.tin", 1));
            mix(rows, amount("molten.angmallen", 2), amount(iron, 1), amount("molten.gold", 1));
            mix(
                    rows,
                    amount("molten.stainlesssteel", 9),
                    amount(iron, 6),
                    amount("molten.nickel", 1),
                    amount("molten.chromium", 1),
                    amount("molten.manganese", 1));
            mix(
                    rows,
                    amount("molten.stainlesssteel", 9),
                    amount(iron, 4),
                    amount("molten.invar", 3),
                    amount("molten.chromium", 1),
                    amount("molten.manganese", 1));
            mix(
                    rows,
                    amount("molten.stainlesssteel", 36),
                    amount(iron, 24),
                    amount("molten.nichrome", 5),
                    amount("molten.chromium", 3),
                    amount("molten.manganese", 4));
            mix(rows, amount("molten.electrotinealloy", 1), amount(iron, 1), amount("molten.nikolite", 8));
        }
        for (String steel : List.of("molten.steel", "molten.meteoricsteel", "molten hsla", "molten.knightmetal")) {
            mix(rows, amount("molten.tungstensteel", 2), amount(steel, 1), amount("molten.tungsten", 1));
        }
        // Pinned ANY.Glowstone.mToThis membership, including the six non-base variants.
        for (String glowstone : List.of(
                "molten.glowstone",
                "molten.glowstoneceres",
                "molten.glowstoneio",
                "molten.glowstoneenceladus",
                "molten.glowstoneproteus",
                "molten.glowstonepluto",
                "molten.gloomstone")) {
            mix(
                    rows,
                    amount("molten.lumium", 4),
                    amount("molten.tin", 3),
                    amount("molten.silver", 1),
                    amount(glowstone, 4));
        }
        mix(rows, amount("molten.bluealloy", 1), amount("molten.nikolite", 4), amount("molten.silver", 1));
        mix(rows, amount("molten.purplealloy", 1), amount("molten.redalloy", 1), amount("molten.bluealloy", 1));
        mix(rows, amount("molten.redstonealloy", 1), amount("molten.silicon", 1), amount("molten.redstone", 1));
        mix(rows, amount("molten.nikolinealloy", 1), amount("molten.silicon", 1), amount("molten.nikolite", 1));
        mix(rows, amount("molten.nichrome", 5), amount("molten.chromium", 1), amount("molten.nickel", 4));
        mix(rows, amount("molten.netherite", 1), amount("molten.gold", 4), amount("molten.ancientdebris", 4));
        mix(rows, amount("molten.titaniumgold", 4), amount("molten.gold", 1), amount("molten.titanium", 3));
        mix(rows, amount("molten.hepatizon", 2), amount("molten.gold", 1), amount("molten.bronze", 1));
        mix(rows, amount("molten.electrum", 2), amount("molten.gold", 1), amount("molten.silver", 1));
        mix(rows, amount("molten.goldinductive", 2), amount("molten.gold", 1), amount("molten.redstone", 1));
        mix(rows, amount("molten.solderingalloy", 10), amount("molten.tin", 9), amount("molten.antimony", 1));
        mix(rows, amount("molten.batteryalloy", 5), amount("molten.lead", 4), amount("molten.antimony", 1));
        mix(rows, amount("molten.celenegil", 2), amount("molten.orichalcum", 1), amount("molten.platinum", 1));
        mix(rows, amount("molten.manyullyn", 2), amount("molten.ardite", 1), amount("molten.cobalt", 1));
        mix(rows, amount("molten.magnalium", 3), amount("molten.magnesium", 1), amount("molten.aluminium", 2));
        mix(
                rows,
                amount("molten.cobaltbrass", 9),
                amount("molten.brass", 7),
                amount("molten.aluminium", 1),
                amount("molten.cobalt", 1));
        mix(rows, amount("molten.bismuthbronze", 5), amount("molten.brass", 4), amount("molten.bismuth", 1));
        mix(rows, amount("molten.arsenicbronze", 5), amount("molten.bronze", 4), amount("molten.arsenic", 1));
        mix(rows, amount("molten.arsenicbronze", 5), amount("molten.arseniccopper", 4), amount("molten.tin", 1));
        mix(
                rows,
                amount("molten.ultimet", 9),
                amount("molten.cobalt", 5),
                amount("molten.chromium", 2),
                amount("molten.nickel", 1),
                amount("molten.molybdenum", 1));
        mix(
                rows,
                amount("molten.ultimet", 36),
                amount("molten.cobalt", 20),
                amount("molten.chromium", 7),
                amount("molten.nichrome", 5),
                amount("molten.molybdenum", 4));
        mix(rows, amount("molten.osmiridium", 2), amount("molten.osmiumelemental", 1), amount("molten.iridium", 1));
        mix(
                rows,
                amount("molten.hssg", 9),
                amount("molten.tungstensteel", 5),
                amount("molten.chromium", 1),
                amount("molten.molybdenum", 2),
                amount("molten.vanadium", 1));
        mix(
                rows,
                amount("molten.hsse", 9),
                amount("molten.hssg", 6),
                amount("molten.cobalt", 1),
                amount("molten.manganese", 1),
                amount("molten.silicon", 1));
        mix(
                rows,
                amount("molten.hsss", 9),
                amount("molten.hssg", 6),
                amount("molten.osmiridium", 2),
                amount("molten.iridium", 1));
        mix(
                rows,
                amount("molten.hsss", 9),
                amount("molten.hssg", 6),
                amount("molten.osmiumelemental", 1),
                amount("molten.iridium", 2));
        return List.copyOf(rows);
    }

    private static Amount amount(String fluidName, int millibuckets) {
        return new Amount(fluidName, millibuckets);
    }

    private static void mix(List<Row> rows, Amount output, Amount... inputs) {
        long total = 0;
        for (Amount input : inputs) total += input.millibuckets();
        rows.add(new Row(List.of(inputs), output, Math.max(total, output.millibuckets()), inputs.length));
    }
}
