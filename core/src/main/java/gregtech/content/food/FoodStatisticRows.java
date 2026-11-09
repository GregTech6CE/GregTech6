package gregtech.content.food;

import java.util.List;

/** Original GT6 CS.FoodsGT, reduced to the medical items that lower radiation. */
public final class FoodStatisticRows {
    private FoodStatisticRows() {}

    public record Row(String itemId, int radiation) {}

    public static final List<Row> ROWS = List.of(new Row("gt6ce:radaway", -50), new Row("gt6ce:cure_all", -999));
}
