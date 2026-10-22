package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.multiblock.StructureLayoutOverlayRuntime;
import com.gregtech.gregtech.network.PacketSyncStructureLayouts;
import net.minecraft.client.Minecraft;

/** Applies a synced layout overlay and asks recipe viewers to rebuild structure pages. */
public final class StructureLayoutClient {
    private static boolean jeiFailed;
    private static boolean emiFailed;

    private StructureLayoutClient() {}

    public static void accept(PacketSyncStructureLayouts packet) {
        StructureLayoutOverlayRuntime.applySynced(packet.layouts());
        refresh();
    }

    public static void refresh() {
        var minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        refreshJei(minecraft);
        refreshEmi();
    }

    private static void refreshJei(Minecraft minecraft) {
        if (jeiFailed || minecraft.getConnection() == null) return;
        try {
            //? if >=1.21.1 {
            /*if (!net.neoforged.fml.ModList.get().isLoaded("jei")) return;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RecipesUpdatedEvent(minecraft.getConnection().getRecipeManager()));
            *///?} else {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("jei")) return;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RecipesUpdatedEvent(minecraft.getConnection().getRecipeManager()));
            //?}
        } catch (RuntimeException | LinkageError failure) {
            jeiFailed = true;
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
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            emiFailed = true;
        } catch (RuntimeException | ReflectiveOperationException | LinkageError failure) {
            emiFailed = true;
        }
    }
}
