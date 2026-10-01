package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Reports one aggregate per rebuild, rather than spamming one line per material tab. */
@Mod.EventBusSubscriber(modid=GregTech.MOD_ID,value=Dist.CLIENT)
public final class CreativeBuildTimings {
    private static long nanos;
    private static int tabs;
    private CreativeBuildTimings() {}
    public static void record(long elapsed) { nanos+=elapsed; tabs++; }
    @SubscribeEvent public static void endTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || tabs==0) return;
        GregTech.LOGGER.info("Creative population: {} GT tabs, {} ms (excludes vanilla search indexing and rendering)",
                tabs,nanos/1_000_000);
        nanos=0;tabs=0;
    }
}
