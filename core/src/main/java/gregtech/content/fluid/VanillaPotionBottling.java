package gregtech.content.fluid;

import java.util.Map;

/** Loader_Fluids' potion containers: 250 L per bottle, with strength, duration and form preserved. */
public final class VanillaPotionBottling {
    public static final int AMOUNT = 250;
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("awkward", "awkward"),
            Map.entry("thick", "thick"),
            Map.entry("mundane", "mundane"),
            Map.entry("damage", "harming"),
            Map.entry("health", "healing"),
            Map.entry("jump", "leaping"),
            Map.entry("speed", "swiftness"),
            Map.entry("strength", "strength"),
            Map.entry("regen", "regeneration"),
            Map.entry("poison", "poison"),
            Map.entry("fireresistance", "fire_resistance"),
            Map.entry("nightvision", "night_vision"),
            Map.entry("weakness", "weakness"),
            Map.entry("slowness", "slowness"),
            Map.entry("waterbreathing", "water_breathing"),
            Map.entry("invisibility", "invisibility"));

    private VanillaPotionBottling() {}

    public record Bottle(String potion, String item) {}

    /** Drinks such as potion.coffee and custom brews have no vanilla bottle equivalent. */
    public static Bottle bottle(String fluid) {
        if (fluid == null || !fluid.startsWith("potion.")) return null;
        String[] parts = fluid.substring(7).split("\\.");
        String type = TYPES.get(parts[0]);
        if (type == null) return null;
        String variant = "", item = "potion";
        for (int i = 1; i < parts.length; i++) {
            switch (parts[i]) {
                case "strong", "long" -> {
                    if (!variant.isEmpty()) return null;
                    variant = parts[i] + "_";
                }
                case "splash", "lingering" -> {
                    if (!item.equals("potion")) return null;
                    item = parts[i] + "_potion";
                }
                default -> {
                    return null;
                }
            }
        }
        return new Bottle(variant + type, item);
    }

    public static String fluid(String potion, String item) {
        String variant = "", type = potion;
        if (potion.startsWith("strong_") || potion.startsWith("long_")) {
            int separator = potion.indexOf('_');
            variant = "." + potion.substring(0, separator);
            type = potion.substring(separator + 1);
        }
        String form = switch (item) {
            case "potion" -> "";
            case "splash_potion" -> ".splash";
            case "lingering_potion" -> ".lingering";
            default -> null;
        };
        if (form == null) return null;
        for (var entry : TYPES.entrySet())
            if (entry.getValue().equals(type)) return "potion." + entry.getKey() + variant + form;
        return null;
    }
}
