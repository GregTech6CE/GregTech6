package gregtech.platform.neoforge.logistics;
import gregtech.content.registry.GTBlockEntities;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class MetalChestCapabilities {@SubscribeEvent public static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,GTBlockEntities.METAL_CHEST.get(),(be,side)->be.isRemoved()?null:be.componentCapability(side));}}
