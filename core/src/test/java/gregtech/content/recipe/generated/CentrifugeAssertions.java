package gregtech.content.recipe.generated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Exact descriptor values: preserve order, raw chances, and the source optimize flag. */
public final class CentrifugeAssertions {
    private CentrifugeAssertions() {}

    public record Row(boolean optimize, String map, long power, long ticks, List<Long> chances,
                      List<String> inputs, List<String> fluidInputs, List<String> fluidOutputs,
                      List<String> outputs) {}

    public static List<Row> collect(Runnable loader) {
        List<Row> rows = new ArrayList<>();
        GeneratedRecipeSink.emitSource((opt, map, power, ticks, chances, in, fi, fo, out) ->
                rows.add(new Row(opt, map, power, ticks,
                        chances == null ? null : Arrays.stream(chances).boxed().toList(),
                        List.of(in), List.of(fi), List.of(fo), List.of(out))), loader);
        return rows;
    }

    public static Row centrifuge(String input, long power, long ticks, List<Long> chances,
                                 List<String> fluids, String... outputs) {
        return new Row(true, "Centrifuge", power, ticks, chances, List.of(input), List.of(),
                fluids, List.of(outputs));
    }

    public static Row only(List<Row> rows, Predicate<Row> predicate, String reason) {
        List<Row> matches = rows.stream().filter(predicate).toList();
        equal(1, matches.size(), reason + " row count");
        return matches.get(0);
    }

    public static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    public static void equal(Object expected, Object actual, String reason) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(reason + "\nExpected: " + expected + "\nActual: " + actual);
        }
    }
}
