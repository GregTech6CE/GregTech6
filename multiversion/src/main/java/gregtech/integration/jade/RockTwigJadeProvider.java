package gregtech.integration.jade;

import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialPresentation;
import gregtech.content.blockentity.RockBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Shows what a ground pebble / twig pile yields (rock material is client-synced). */
public enum RockTwigJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gt6ce", "rock_twig");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof RockBlockEntity rock) {
            for (var item : gregtech.content.block.RockBlock.yields(rock))
                tooltip.add(Component.translatable("jade.gt6ce.omni.rock_contents", item.getHoverName(), item.getCount()));
        } else if (accessor.getBlock() instanceof gregtech.content.block.TwigBlock) {
            tooltip.add(Component.translatable("gt6ce.jade.twig_content"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
