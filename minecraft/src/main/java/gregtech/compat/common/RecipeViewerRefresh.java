package gregtech.compat.common;

import gregtech.platform.LoaderHooks;
import gregtech.platform.Mc;
import net.minecraft.client.Minecraft;
//? if >=1.21.1 {
/*import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
*///?} else {
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;
import net.minecraftforge.event.TagsUpdatedEvent;
//?}

/** Reloads the viewer that owns the native atlas pages after server data changes. */
public final class RecipeViewerRefresh {
    private static boolean emiFailed;
    private static boolean jeiFailed;

    private RecipeViewerRefresh() {}

    static boolean ready() {
        if (emiFailed || !LoaderHooks.isModLoaded("emi")) return true;
        try {
            var manager = Class.forName("dev.emi.emi.runtime.EmiReloadManager");
            if (!(boolean) manager.getMethod("isLoaded").invoke(null)) return false;
            return Class.forName("dev.emi.emi.registry.EmiRecipes").getField("activeWorker").get(null) == null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            emiFailed = true;
            Mc.logger().warn("Cannot check EMI reload state", failure);
            return true;
        }
    }

    public static void refresh() {
        var minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        if (connection == null) return;
        if (LoaderHooks.isModLoaded("emi")) {
            if (emiFailed) return;
            try {
                Class.forName("dev.emi.emi.runtime.EmiReloadManager").getMethod("reload").invoke(null);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                emiFailed = true;
                Mc.logger().warn("Cannot refresh EMI display data", failure);
            }
        } else if (!jeiFailed && LoaderHooks.isModLoaded("jei")) {
            try {
                EVENT_BUS.post(new RecipesUpdatedEvent(connection.getRecipeManager()));
                //? if <1.21.1 {
                // JEI 15 waits for both recipes and tags before rebuilding its runtime.
                EVENT_BUS.post(new TagsUpdatedEvent(
                        connection.registryAccess(), true, minecraft.isLocalServer()));
                //?}
            } catch (RuntimeException | LinkageError failure) {
                jeiFailed = true;
                Mc.logger().warn("Cannot refresh JEI display data", failure);
            }
        }
    }
}
