package gregtech.item;

import net.minecraft.world.food.FoodProperties;

/**
 * Original MultiItemFood 31001: edible even when full, zero nutrition, -50 radiation.
 * FoodStatEvents applies the table's dose once after consumption, including the last pill.
 */
public final class RadawayItem extends TechItem {

    public RadawayItem() {
        super("Radaway", new Properties().food(new FoodProperties.Builder()
//? if >=1.21.1 {
        /*.nutrition(0).saturationModifier(0).alwaysEdible().build()));
*///?} else {
        .nutrition(0).saturationMod(0).alwaysEat().build()));
//?}
    }
}
