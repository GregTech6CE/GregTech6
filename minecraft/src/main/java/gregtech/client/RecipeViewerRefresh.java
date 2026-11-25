package gregtech.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/** Rebuilds installed recipe viewers after client-side display data changes. */
public final class RecipeViewerRefresh {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean jeiFailed;
    private static boolean emiFailed;

    private RecipeViewerRefresh() {}

    static boolean ready() {
        if (emiFailed) return true;
        try {
            //? if >=1.21.1 {
            /*if (!net.neoforged.fml.ModList.get().isLoaded("emi")) return true;
            *///?} else {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("emi")) return true;
            //?}
            if (!(boolean) Class.forName("dev.emi.emi.runtime.EmiReloadManager").getMethod("isLoaded").invoke(null)) return false;
            // EMI builds its lookup index on a second worker after plugin reload has finished.
            return Class.forName("dev.emi.emi.registry.EmiRecipes").getField("activeWorker").get(null) == null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            emiFailed = true;
            LOGGER.warn("Cannot check EMI reload state; disabling explicit EMI display refresh", failure);
            return true;
        }
    }

    public static void refresh() {
        var minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getConnection() == null) return;
        if (!refreshJei(minecraft)) refreshEmi();
    }

    /** Returns whether the event sequence also requests EMI's reload. */
    private static boolean refreshJei(Minecraft minecraft) {
        if (jeiFailed || minecraft.getConnection() == null) return false;
        try {
            //? if >=1.21.1 {
            /*if (!net.neoforged.fml.ModList.get().isLoaded("jei")) return false;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RecipesUpdatedEvent(minecraft.getConnection().getRecipeManager()));
            return false;
            *///?} else {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("jei")) return false;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RecipesUpdatedEvent(minecraft.getConnection().getRecipeManager()));
            // JEI 15 waits for both notifications, including when it is already running.
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TagsUpdatedEvent(
                    minecraft.getConnection().registryAccess(), true, minecraft.isLocalServer()));
            return true;
            //?}
        } catch (RuntimeException | LinkageError failure) {
            jeiFailed = true;
            LOGGER.warn("Cannot refresh JEI display data", failure);
            return false;
        }
    }

    private static void refreshEmi() {
        if (emiFailed) return;
        try {
            //? if >=1.21.1 {
            /*if (!net.neoforged.fml.ModList.get().isLoaded("emi")) return;
            *///?} else {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("emi")) return;
            //?}
            Class.forName("dev.emi.emi.runtime.EmiReloadManager").getMethod("reload").invoke(null);
        } catch (RuntimeException | ReflectiveOperationException | LinkageError failure) {
            emiFailed = true;
            LOGGER.warn("Cannot refresh EMI display data", failure);
        }
    }
}
