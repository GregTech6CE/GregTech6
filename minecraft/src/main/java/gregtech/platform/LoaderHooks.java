package gregtech.platform;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.EventHooks;
*///?} else {
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
//?}

/** Thin platform facade for loader event hooks and vanilla furnace burn time lookup. */
public final class LoaderHooks {
    private LoaderHooks() {}

    public static void firePlayerCraftingEvent(Player player, ItemStack crafted, Container craftMatrix) {
//? if >=1.21.1 {
/*        EventHooks.firePlayerCraftingEvent(player, crafted, craftMatrix);
*///?} else {
        ForgeEventFactory.firePlayerCraftingEvent(player, crafted, craftMatrix);
//?}
    }

    public static boolean onExplosionStart(Level level, Explosion explosion) {
//? if >=1.21.1 {
/*        return EventHooks.onExplosionStart(level, explosion);
*///?} else {
        return ForgeEventFactory.onExplosionStart(level, explosion);
//?}
    }

    public static void onExplosionDetonate(Level level, Explosion explosion, List<Entity> list, double diameter) {
//? if >=1.21.1 {
/*        EventHooks.onExplosionDetonate(level, explosion, list, diameter);
*///?} else {
        ForgeEventFactory.onExplosionDetonate(level, explosion, list, diameter);
//?}
    }

    public static void onPlayerDestroyItem(Player player, ItemStack original, @Nullable InteractionHand hand) {
//? if >=1.21.1 {
/*        EventHooks.onPlayerDestroyItem(player, original, hand);
*///?} else {
        ForgeEventFactory.onPlayerDestroyItem(player, original, hand);
//?}
    }

    public static int vanillaBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
//? if >=1.21.1 {
/*        return stack.getBurnTime(recipeType);
*///?} else {
        return ForgeHooks.getBurnTime(stack, recipeType);
//?}
    }
}
