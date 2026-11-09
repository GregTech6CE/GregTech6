package gregtech.content.food;

import java.util.List;

/** Original GT6 CS.FoodsGT, reduced to the radiation values the port still tracks. */
public final class FoodStatisticRows {
    private FoodStatisticRows() {}

    public record Row(String itemId, int radiation) {}

    public static final List<Row> ROWS = List.of(
            new Row("gt6ce:banana", 4),
            new Row("gt6ce:banana_slice", 1),
            new Row("gt6ce:banana_ice_cream", 2),
            new Row("gt6ce:radaway", -50),
            new Row("gt6ce:cure_all", -999));
}
