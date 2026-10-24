package gregtech.client;
import gregtech.registry.GTSpikes;import gregtech.block.misc.SpikeBlock;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.api.distmarker.Dist;import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
@EventBusSubscriber(modid = gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SpikesClientSetup {
@SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
event.enqueueWork(() -> gregtech.registry.GTSpikes.all().forEach(entry ->
                net.minecraft.client.renderer.item.ItemProperties.register(entry.get().asItem(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "spike_omni"),
                        (stack, level, entity, seed) -> gregtech.block.misc.SpikeBlock.itemMode(stack)
                                == gregtech.block.misc.SpikeBlock.Mode.WALL ? 0 : 1)));
}
@SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block e){for(var b:GTSpikes.all())e.register((s,l,p,t)->t==0?b.get().materialTint(s.getValue(SpikeBlock.SECONDARY)):0xFFFFFF,b.get());}@SubscribeEvent public static void items(RegisterColorHandlersEvent.Item e){for(var b:GTSpikes.all())e.register(ItemColorARGB.opaque((s,t)->{var data=s.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);return t==0?b.get().materialTint(data!=null&&Boolean.parseBoolean(data.properties().getOrDefault("secondary","false"))):0xFFFFFF;}),b.get().asItem());}}
