package gregtech.bootstrap.registration;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.CatalogVisibility;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTCreativeTabs;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import net.minecraft.world.item.ItemStack;

//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class CreativeTabBootstrap {
    private CreativeTabBootstrap() {}

    @SubscribeEvent
    public static void populateTabs(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().getNamespace().equals(REGISTRY_NAMESPACE)) return;
        for (var prefix : PrefixRegistry.all()) {
            var key = GTCreativeTabs.keyFor(prefix);
            if (key != null && key.equals(event.getTabKey())) {
                populatePrefix(event, prefix);
                return;
            }
        }
        for (var prefix : BlockPrefixRegistry.all()) {
            var key = GTCreativeTabs.keyFor(prefix);
            if (key != null && key.equals(event.getTabKey())) {
                populateBlockPrefix(event, prefix);
                return;
            }
        }
        for (var stack : OriginCreativeContents.contents(event.getTabKey().location().getPath()))
            event.accept(stack.copyWithCount(1));
    }

    private static void populatePrefix(BuildCreativeModeTabContentsEvent event, MaterialPrefix prefix) {
        if (prefix.isHiddenFromCreative() && !gregtech.GregTechConfig.Rule.HIDDEN_PREFIXES.enabled()) return;
        for (var binding : GTItems.creativeEntries(prefix)) {
            GTMaterial material = binding.material();
            if ((material.has(MaterialProperty.HIDDEN)
                            && !gregtech.GregTechConfig.Rule.HIDDEN_MATERIALS.enabled())
                    || material.resolve() != material
                    || (!prefix.isValidFor(material)
                            && !gregtech.engine.addon.AddonMaterials.explicitPlateOrFoil(prefix, material))) continue;
            if (binding.item().isBound()) {
                var stack = new ItemStack(binding.item().get(), 1);
                if (CatalogVisibility.visible(stack)) event.accept(stack);
            }
        }
    }

    private static void populateBlockPrefix(BuildCreativeModeTabContentsEvent event, BlockMaterialPrefix prefix) {
        for (GTMaterial material : GTMaterialRegistry.sortedMaterials()) {
            if ((material.has(MaterialProperty.HIDDEN)
                    && !gregtech.GregTechConfig.Rule.HIDDEN_MATERIALS.enabled())) continue;
            if (material.resolve() != material) continue;
            if (!prefix.isValidFor(material)) continue;
            ItemStack stack = GTBlocks.getCreativeStack(prefix, material);
            if (!stack.isEmpty() && CatalogVisibility.visible(stack)) event.accept(stack);
        }
    }
}
