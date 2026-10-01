package com.gregtech.gregtech.content.food;
import java.util.List;
/** Original MultiItemBottles families, including separate lubricant rows. */
public final class BottleFillingRows {private BottleFillingRows(){}
    public record Family(String bottleId, String fluidKey, String source) {}

    /** GT6 {@code MultiItemBottles} order; {@code source} is the row block the four counts come from. */
    public static final List<Family> FAMILIES = List.of(
            new Family("seed_oil", "Oil_Seed", "MultiItemBottles:129-132"),
            new Family("milk", "Milk", "MultiItemBottles:143-146"),
            new Family("soy_milk", "MilkSoy", "MultiItemBottles:151-154"),
            new Family("honey", "Honey", "MultiItemBottles:164-167"),
            new Family("green_slime_bottle", "Slime_Green", "MultiItemBottles:211-214"),
            new Family("pink_slime_bottle", "Slime_Pink", "MultiItemBottles:216-219"),
            new Family("blue_slime_bottle", "Slime_Blue", "MultiItemBottles:221-224"),
            new Family("juice", "Juice", "MultiItemBottles:232-235"),
            new Family("maple_sap", "Sap_Maple", "MultiItemBottles:268-271"),
            new Family("rainbow_sap", "Sap_Rainbow", "MultiItemBottles:277-280"));

public static List<Family> all(){var rows=new java.util.ArrayList<>(FAMILIES);rows.add(new Family("lubricant_bottle","Lubricant","MultiItemBottles:390-396"));return List.copyOf(rows);}
}
