package com.gregtech.gregtech.client;
import net.minecraft.world.level.block.Block;
import com.gregtech.gregtech.block.misc.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid=com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class LongDistanceClientColors {
    private LongDistanceClientColors(){}
    private static int rgb(Block block,int layer){
        int value=layer!=0?0xffffff:block instanceof LongDistEndpointBlock endpoint?endpoint.material().getColor():((LongDistanceTransformerBlock)block).material().getColor();
        return value;
    }
    private static java.util.List<Block> endpoints(){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream().filter(block->block instanceof LongDistEndpointBlock||block instanceof LongDistanceTransformerBlock).toList();}
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){for(var block:endpoints())event.register((state,level,pos,layer)->rgb(block,layer),block);}
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event){for(var block:endpoints())event.register((stack,layer)->rgb(block,layer),block.asItem());}
}
