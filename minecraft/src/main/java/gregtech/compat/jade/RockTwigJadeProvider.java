package gregtech.compat.jade;

import gregtech.content.block.RockBlock;
import gregtech.content.block.TwigBlock;
import gregtech.content.blockentity.RockBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

/** Shows what a ground pebble / twig pile yields (rock material is client-synced). */
public enum RockTwigJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "rock_twig");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof RockBlockEntity rock) {
            for (var item : RockBlock.yields(rock))
                content(tooltip, item, "jade.gt6ce.omni.rock_contents");
        } else if (accessor.getBlock() instanceof TwigBlock) {
            var items = TwigBlock.possibleSticks(accessor.getLevel(), accessor.getPosition());
            boolean random = items.size() > 1;
            if (random) tooltip.add(Component.translatable("gt6ce.jade.twig_possible_contents"));
            for (var item : items)
                content(tooltip, item, random ? "jade.gt6ce.omni.item_count" : "jade.gt6ce.omni.rock_contents");
        }
    }

    private static void content(ITooltip tooltip, ItemStack item, String key) {
        if (item.isEmpty()) return;
        JadeText.add(tooltip, IElementHelper.get().item(item),
                Component.translatable(key, item.getHoverName(), item.getCount()));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
