package com.gregtech.gregtech.recipe;

/** Invalidates both successful and empty worktable previews after a server datapack reload. */
//? if >=1.21.1 {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
*///?} else {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID)
//?}
public final class AdvancedCraftingRecipeReload {
    private static volatile long generation;
    private AdvancedCraftingRecipeReload() {}
    public static long generation(){return generation;}

//? if >=1.21.1 {
    /*@net.neoforged.bus.api.SubscribeEvent
    public static void reloaded(net.neoforged.neoforge.event.OnDatapackSyncEvent event){
*///?} else {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void reloaded(net.minecraftforge.event.OnDatapackSyncEvent event){
//?}
        if(event.getPlayer()==null)invalidate();
    }
    public static void invalidate(){generation++;}
}
