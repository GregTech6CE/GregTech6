package gregtech.engine.material;

import gregtech.engine.prefix.PrefixRegistry;
import java.util.Set;

/** Source prefix and resolved material identity, including forms without a registered item prefix. */
public record ItemMaterialForm(String prefix, String material) {
    public ItemMaterialForm {
        prefix = PrefixRegistry.sourceName(prefix);
    }

    public static final Set<String> STANDARD_ORES = Set.of(
            "ore",
            "oreBlackgranite",
            "oreRedgranite",
            "oreVanillastone",
            "oreVanillagranite",
            "oreAndesite",
            "oreDiorite",
            "oreDeepslate",
            "oreBlackstone",
            "oreMoon",
            "oreMars",
            "oreSpace",
            "orePhobos",
            "oreDeimos",
            "oreVenus",
            "oreMercury",
            "oreCeres",
            "oreJupiter",
            "oreIo",
            "oreEuropa",
            "oreGanymede",
            "oreCallisto",
            "oreSaturn",
            "oreRhea",
            "oreTitan",
            "oreOberon",
            "oreIapetus",
            "oreUranus",
            "oreTitania",
            "oreNeptune",
            "oreTriton",
            "orePluto",
            "oreEris",
            "oreKepler22b",
            "oreHolystone",
            "oreLivingrock",
            "oreDeadrock",
            "oreBetweenstone",
            "orePitstone",
            "oreUmberstone",
            "oreKomatiite",
            "oreBasalt",
            "oreMarble",
            "oreLimestone",
            "oreSiltstone",
            "oreShale",
            "oreSlate",
            "oreGreenschist",
            "oreBlueschist",
            "orePinkschist",
            "oreGrayschist",
            "oreGneiss",
            "oreLightprismarine",
            "oreDarkprismarine",
            "oreKimberlite",
            "oreQuartzite",
            "oreNetherrack",
            "oreEndstone",
            "oreSandstone");
    public static final Set<String> DENSE_ORES = Set.of("oreNether", "oreDense", "oreEnd");
}
