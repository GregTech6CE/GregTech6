package gregtech.content.fluid;

import gregtech.content.data.FluidCatalog;
import java.util.HashSet;

public final class PotionBottlingContracts {
    public static void main(String[] args) {
        var bottles = new HashSet<VanillaPotionBottling.Bottle>();
        for (var entry : FluidCatalog.all().values()) {
            var bottle = VanillaPotionBottling.bottle(entry.registryName());
            if (bottle == null) continue;
            require(entry.registryName().equals(VanillaPotionBottling.fluid(bottle.potion(), bottle.item())),
                    "Lost strength, duration or form: " + entry.registryName());
            require(bottles.add(bottle), "Duplicate potion: " + bottle);
        }
        require(bottles.size() == 93, "Expected all 93 registered vanilla potion variants: " + bottles.size());
        require(VanillaPotionBottling.AMOUNT == 250, "Source bottle volume");
        require(VanillaPotionBottling.bottle("potion.speed.long.splash").equals(
                new VanillaPotionBottling.Bottle("long_swiftness", "splash_potion")), "Extended splash speed");
        require(VanillaPotionBottling.bottle("potion.health.strong.lingering").equals(
                new VanillaPotionBottling.Bottle("strong_healing", "lingering_potion")), "Strong lingering healing");
        for (String name : new String[]{"potion.tainted", "potion.coffee", "potion.poison.unknown", "potion.speed.long.strong"})
            require(VanillaPotionBottling.bottle(name) == null, "Invented vanilla potion for " + name);
        require(VanillaPotionBottling.fluid("luck", "potion") == null, "Unregistered GT fluid");
        require(VanillaPotionBottling.fluid("healing", "glass_bottle") == null, "Invalid bottle form");
        System.out.println("Potion bottling contracts passed: 93 variants, exact round trips and unsupported brew rejection.");
    }

    private static void require(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }
}
