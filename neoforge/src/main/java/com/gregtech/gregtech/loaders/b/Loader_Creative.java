package com.gregtech.gregtech.loaders.b;
import com.gregtech.gregtech.api.material.*;import com.gregtech.gregtech.api.prefix.*;import com.gregtech.gregtech.data.MaterialPrefix;import com.gregtech.gregtech.registry.*;import net.minecraft.world.item.*;import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class Loader_Creative {private Loader_Creative(){}
public static void register(net.neoforged.bus.api.IEventBus bus){GTCreativeTabIcons.register();GTCreativeTabIcons.ICONS.register(bus);GTCreativeTabs.registerTabs();GTCreativeTabs.TABS.register(bus);}
@SubscribeEvent public static void populateTabs(BuildCreativeModeTabContentsEvent event){if(!event.getTabKey().location().getNamespace().equals("gregtech"))return;
 for(var prefix:PrefixRegistry.all()){var key=GTCreativeTabs.keyFor(prefix);if(key!=null&&key.equals(event.getTabKey())){populatePrefix(event,prefix);return;}}
 for(var prefix:BlockPrefixRegistry.all()){var key=GTCreativeTabs.keyFor(prefix);if(key!=null&&key.equals(event.getTabKey())){populateBlockPrefix(event,prefix);return;}}
 for(var stack:OriginCreativeContents.contents(event.getTabKey().location().getPath()))event.accept(stack.copyWithCount(1));
}
    private static void populatePrefix(BuildCreativeModeTabContentsEvent event, MaterialPrefix prefix) {
        for (var binding : GTItems.creativeEntries(prefix)) {
            GTMaterial material=binding.material();
            if (material.has(MaterialProperty.HIDDEN) || material.resolve()!=material || !prefix.isValidFor(material)) continue;
            if(binding.item().isBound()) event.accept(new ItemStack(binding.item().get(),1));
        }
    }
    private static void populateBlockPrefix(BuildCreativeModeTabContentsEvent event, BlockMaterialPrefix prefix) {
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if (material.has(MaterialProperty.HIDDEN)) continue;
            if (material.resolve() != material) continue;
            if (!prefix.isValidFor(material)) continue;
            ItemStack stack = GTBlocks.getCreativeStack(prefix, material);
            if (!stack.isEmpty()) event.accept(stack);
        }
    }
}
