package gregtech.compat.jade;

import gregtech.content.block.misc.ConcreteBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
//? if >=1.21.1 {
/*import static snownee.jade.api.JadeIds.CORE_OBJECT_NAME;
*///?} else {
import static snownee.jade.api.Identifiers.CORE_OBJECT_NAME;
//?}
import snownee.jade.api.config.IPluginConfig;

/** The world colour is a block state; Jade's default item name only sees the default colour. */
public enum ConstructionNameJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var state = accessor.getBlockState();
        if (!state.hasProperty(ConcreteBlock.COLOR)) return;
        var name = Component.translatable(accessor.getBlock().getDescriptionId() + "."
                + state.getValue(ConcreteBlock.COLOR).getName()).withStyle(net.minecraft.ChatFormatting.WHITE);
        tooltip.remove(CORE_OBJECT_NAME);
        tooltip.add(0, name, CORE_OBJECT_NAME);
    }

    @Override public int getDefaultPriority() { return 1000; }
    @Override public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "construction_name");
    }
}
