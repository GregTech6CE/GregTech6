package gregtech.content.food;
/** Shared original GT6 nutrition rows; Minecraft bindings stay in platform code. */
public final class DrinkNutritionRows {
 public record Effect(String sourceId, int duration, int amplifier, int chance) {}
 private DrinkNutritionRows() {}
 public record Row(String field,int foodLevel,float saturation,int alcohol,int caffeine,int dehydration,int sugar,int fat,int radiation, boolean alwaysEdible, boolean invisibleParticles, String useAction, boolean milk, boolean extinguish, boolean explosive, java.util.List<Effect> effects) {}
}
