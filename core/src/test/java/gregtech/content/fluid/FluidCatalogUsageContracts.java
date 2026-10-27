package gregtech.content.fluid;

import gregtech.content.data.FluidCatalog;
import gregtech.content.food.BottleFluidIdentity;
import gregtech.content.recipe.FermentationRecipeDefinitions;

import static gregtech.content.recipe.generated.CentrifugeAssertions.*;

/** Removing orphan compatibility entries must preserve the GT food and fuel chains. */
public final class FluidCatalogUsageContracts {
    private FluidCatalogUsageContracts() {}

    public static void run() {
        for (String[] row : FermentationRecipeDefinitions.RECIPES) {
            check(FluidCatalog.get(row[0]) != null, "fermentation input " + row[0]);
            check(FluidCatalog.get(row[1]) != null, "fermentation output " + row[1]);
        }
        for (String key : new String[]{"Juice_Apple", "Juice_Orange", "Juice_Lemon", "Vodka", "Oil_Olive"}) {
            var fluid = BottleFluidIdentity.resolve(key);
            check(fluid != null, "used drink/fuel survives compatibility cleanup: " + key);
            check(fluid.hasFlag(FluidCatalog.FluidFlags.FOOD), "drink flags survive: " + key);
        }
        // These have no named recipe references: runtime recipes discover them by category flags.
        for (String key : new String[]{"Wine_Agave", "Wine_Sparkling", "Whiskey", "Whiskey_Rye", "Whiskey_Corn"}) {
            var fluid = FluidCatalog.get(key);
            long category = key.startsWith("Wine_") ? FluidCatalog.FluidFlags.WINE : FluidCatalog.FluidFlags.WHISKEY;
            check(fluid != null && fluid.hasFlag(category), "category-driven fermentation/fuel survives: " + key);
        }
        for (String path : new String[]{"binnie.liqueuralmond", "binnie.beerstout", "binnie.tequila",
                "ic2steam", "ic2superheatedsteam", "myst.ink.black", "endergoo", "purelava", "ooze"})
            check(FluidCatalog.all().values().stream().noneMatch(e -> e.registryName().equals(path)),
                    "no unused placeholder fluid: " + path);
    }
}
