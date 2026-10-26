package gregtech.client;
import gregtech.content.block.plant.BushBlock;
import gregtech.content.registry.GTBushes;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
@net.neoforged.fml.common.EventBusSubscriber(modid=gregtech.engine.mod.GregTechIdentity.MOD_ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class SurfaceLifeClientSetup {
 @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block e){e.register((state,level,pos,tint)->0xFF000000|((BushBlock)state.getBlock()).tintColour(tint,state.getValue(BushBlock.STAGE)),GTBushes.allBlocks());}
 @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item e){e.register(ItemColorARGB.opaque((stack,tint)->((BushBlock)((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock()).inventoryColour(tint)),java.util.Arrays.stream(GTBushes.allBlocks()).map(net.minecraft.world.level.block.Block::asItem).toArray(net.minecraft.world.item.Item[]::new));}
}
