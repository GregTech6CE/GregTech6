package com.gregtech.gregtech.client;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.content.tool.AutomaticToolRules;
import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

/** Original automatic-tool and data-switch casing colors on the actual registered blocks. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AutomaticToolClientColors {
    private AutomaticToolClientColors() {}
    private static int rgb(Block block, int layer) {
        if (layer != 0) return 0xffffff;
        if (block instanceof com.gregtech.gregtech.block.inventory.UsbSwitchBlock) return Materials.SteelGalvanized.getColor();
        return AutomaticToolRules.material(AutomaticToolRules.profile(BuiltInRegistries.BLOCK.getKey(block).getPath())).getColor();
    }
    private static java.util.List<Block> blocks() {
        var result = new java.util.ArrayList<Block>();
        var ids = new java.util.ArrayList<String>();
        AutomaticToolRules.ALL.forEach(spec -> ids.add(spec.id()));
        ids.add("usb_switch"); ids.add("hdd_switch");
        for (String id : ids) result.add(BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation("gregtech", id)));
        return result;
    }
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        for (var block : blocks()) event.register((state, level, pos, layer) -> rgb(block, layer), block);
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) {
        for (var block : blocks()) event.register((stack, layer) -> rgb(block, layer), block.asItem());
    }
}
