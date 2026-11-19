package gregtech.content.worldgen.dungeon;

import gregtech.content.item.BottleItem;
import gregtech.content.registry.GTFluids;
import gregtech.platform.Mc;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Resolves optional dungeon drinks after the game registries have loaded. */
final class DungeonDrinks {
    private static final String FOOD_ADDON = "gt6food";

    private DungeonDrinks() {}

    @Nullable
    static String drinkPath(String field) {
        return switch (field) {
            case "Mead" -> "mead";
            case "Rum_White" -> "rum";
            case "Rum_Dark" -> "pirate_brew";
            case "Wine_Lemon" -> "limoncello";
            case "Grenade_Juice" -> "cave_johnsons_grenade_juice";
            case "Lemonade" -> "lemonade";
            case "Soda" -> "soda";
            case "Purple_Drink" -> "purple_drink";
            case "Holywater" -> "holy_water";
            case "Vodka" -> "vodka";
            case "Wine_Grape_Purple" -> "ricardo_sanchez";
            default -> null;
        };
    }

    @Nullable
    static FluidStack fluid(String field, int amount) {
        return fluid(field, field, amount);
    }

    @Nullable
    static FluidStack fluid(String field, String fallbackField, int amount) {
        String path = drinkPath(field);
        if (path != null) {
            var addonFluid = Mc.fluid(ResourceLocation.fromNamespaceAndPath(FOOD_ADDON, path));
            if (addonFluid != null && addonFluid != Fluids.EMPTY) {
                return new FluidStack(addonFluid, amount);
            }
        }
        FluidStack fallback = GTFluids.stack(fallbackField, amount);
        if (path != null && (fallback == null || fallback.isEmpty())) {
            return new FluidStack(Fluids.WATER, amount);
        }
        return fallback;
    }

    static ItemStack bottle(String path, boolean drink) {
        if (drink) {
            var addonItem = Mc.item(ResourceLocation.fromNamespaceAndPath(FOOD_ADDON, path));
            if (addonItem != null && addonItem != Items.AIR) {
                return new ItemStack(addonItem);
            }
        }
        var coreItem = Mc.item(Mc.gregtechId(path));
        if (coreItem != null && coreItem != Items.AIR) {
            return new ItemStack(coreItem);
        }
        return BottleItem.emptyBottle();
    }
}
