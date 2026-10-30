package gregtech.content.food;

import gregtech.api.material.GTValues;
import java.util.List;

/** Meat and bone payloads from {@code MultiItemFood.java:529-578}; quantities are material units. */
public final class MeatCutCatalog {
    private MeatCutCatalog() {}

    public record Cut(String raw, String cooked, long meat, long bone, String cannedName) {}

    private static final long U = GTValues.U;
    public static final List<Cut> CUTS = List.of(
            new Cut("raw_ham", "cooked_ham", 2 * U, U / 4, "Canned Ham"),
            new Cut("raw_ham_slice", "cooked_ham_slice", U / 2, 0, ""),
            new Cut("raw_bacon", "grilled_bacon", U / 2, 0, "Canned Bacon"),
            new Cut("raw_ribs", "grilled_ribs", 2 * U, U, "Canned Ribs"),
            new Cut("raw_rib_eye_steak", "grilled_rib_eye_steak", 3 * U, U / 4, "Canned Beef"),
            new Cut("dogmeat", "grilled_dogmeat", 2 * U, U / 9, "Canned Dog"),
            new Cut("minecraft:mutton", "minecraft:cooked_mutton", 2 * U, U / 9, "Canned Mutton"),
            new Cut("minecraft:rabbit", "minecraft:cooked_rabbit", 2 * U, U / 9, ""),
            new Cut("horse_meat", "grilled_horse_meat", 2 * U, U / 4, "Canned Horse"),
            new Cut("mule_meat", "grilled_mule_meat", 5 * U / 2, U / 4, "Canned Horse"),
            new Cut("donkey_meat", "grilled_donkey_meat", 5 * U / 2, U / 9, "Canned Horse"));
}
