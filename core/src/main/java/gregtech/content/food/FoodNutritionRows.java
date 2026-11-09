package gregtech.content.food;
/** Shared original GT6 food rows; Minecraft bindings stay in platform code. */
public final class FoodNutritionRows {
    private FoodNutritionRows() {}

    public record Food(
            int level, float saturation, int hydration, int temperature, boolean alwaysEdible, boolean rotten) {}

    public record Entry(
            String id,
            String display,
            int meta,
            Food food,
            String container,
            int containerCount,
            boolean hasFoodStat,
            int line) {}
}
