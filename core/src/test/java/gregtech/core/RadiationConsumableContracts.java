package gregtech.core;

import gregtech.content.food.FoodStatisticRows;
import gregtech.content.food.NutritionRules;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone data and source-boundary regression checks, not a Minecraft event/GameTest. */
public final class RadiationConsumableContracts {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path root = args.length == 0 ? Path.of("") : Path.of(args[0]);
        var rows = FoodStatisticRows.ROWS.stream()
                .filter(row -> row.itemId().equals("gt6ce:radaway")).toList();
        require(rows.size() == 1, "one canonical Radaway nutrition row");
        int delta = rows.get(0).radiation();
        require(delta == -50, "original Radaway dose");
        for (int dose = 0; dose <= 127; dose++) {
            require(NutritionRules.change(dose, delta) == Math.max(0, dose - 50),
                    "single consumption dose " + dose);
        }
        require(NutritionRules.change(120, delta) == 70, "120 must become 70, not double-dose 20");
        require(NutritionRules.change(NutritionRules.change(120, delta), delta) == 20,
                "two separate consumptions still apply twice");

        Path source = root.resolve("multiversion/src/main/java/gregtech");
        String item = Files.readString(source.resolve("content/item/RadawayItem.java"));
        require(!item.contains("finishUsingItem("), "Radaway must not duplicate the finish-event dose");
        require(!item.contains("PlayerRadiation.change("), "no item-local dose mutation");
        require(item.contains(".alwaysEdible()") && item.contains(".alwaysEat()"), "usable while full on both loaders");

        String events = Files.readString(source.resolve("content/food/FoodStatEvents.java"));
        require(events.contains("ItemStack stack = event.getItem();"), "uses original consumed stack, not empty remainder");
        require(events.contains("PlayerRadiation.change(player, stats[GTFoodStats.RADIATION]);"), "table event retains dose ownership");
        require(events.contains("event.getEntity().level().isClientSide"), "client excluded from dose updates");
        System.out.println("RadiationConsumableContracts: " + checks + " checks passed (data/source contracts only)");
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
