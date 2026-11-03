package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** GT6's extra brewing ingredients, translated to vanilla potion ids. */
public final class PotionBrewingRoutes {
    public record Route(String basePotion, ItemStack ingredient, String resultPotion) {}

    private static final String WATER = "water";
    private static final String MUNDANE = "mundane";
    private static final String AWKWARD = "awkward";
    private static final String THICK = "thick";

    private PotionBrewingRoutes() {}

    public static List<Route> all() {
        var routes = new ArrayList<Route>();
        for (var prefix : new MaterialPrefix[] {MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny}) {
            add(routes, Materials.Arsenic, prefix, WATER, "harming");
            add(routes, Materials.Arsenic, prefix, MUNDANE, "harming");
            add(routes, Materials.Arsenic, prefix, AWKWARD, "poison");
            add(routes, Materials.Arsenic, prefix, THICK, "strong_harming");
        }
        for (var prefix : new MaterialPrefix[] {
            MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny, MaterialPrefix.gemChipped
        }) {
            add(routes, Materials.Sugar, prefix, WATER, MUNDANE);
            add(routes, Materials.Sugar, prefix, AWKWARD, "swiftness");
            add(routes, Materials.Sugar, prefix, THICK, "strong_swiftness");
        }
        add(routes, Materials.Blaze, MaterialPrefix.dustTiny, WATER, MUNDANE);
        add(routes, Materials.Blaze, MaterialPrefix.dustTiny, AWKWARD, "strength");
        add(routes, Materials.Blaze, MaterialPrefix.dustTiny, THICK, "strong_strength");
        for (var material : new GTMaterial[] {Materials.Breeze, Materials.Blitz}) {
            add(routes, material, MaterialPrefix.dustTiny, WATER, MUNDANE);
            add(routes, material, MaterialPrefix.dustTiny, AWKWARD, "water_breathing");
            add(routes, material, MaterialPrefix.dustTiny, THICK, "long_water_breathing");
        }
        add(routes, Materials.Blizz, MaterialPrefix.dustTiny, WATER, THICK);
        add(routes, Materials.Blizz, MaterialPrefix.dustTiny, AWKWARD, "fire_resistance");
        add(routes, Materials.Blizz, MaterialPrefix.dustTiny, MUNDANE, "long_fire_resistance");
        add(routes, Materials.Basalz, MaterialPrefix.dustTiny, WATER, THICK);
        add(routes, Materials.Basalz, MaterialPrefix.dustTiny, AWKWARD, "leaping");
        add(routes, Materials.Basalz, MaterialPrefix.dustTiny, MUNDANE, "strong_leaping");
        return List.copyOf(routes);
    }

    private static void add(
            List<Route> routes, GTMaterial material, MaterialPrefix prefix, String base, String result) {
        ItemStack ingredient = GTItems.getStack(prefix, material);
        if (!ingredient.isEmpty()) routes.add(new Route(base, ingredient, result));
    }
}
