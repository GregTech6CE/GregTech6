package gregtech.integration.jade;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.material.MaterialPresentation;
import gregtech.blockentity.RockBlockEntity;
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
            GTMaterial material = GTMaterialRegistry.get(rock.getMaterial());
            Component display = material != null && material.resolve().isValid()
                    ? MaterialPresentation.name(material.resolve()) : Component.literal(rock.getMaterial());
            tooltip.add(Component.translatable("gt6ce.jade.rock_content", display));
        } else if (accessor.getBlock() instanceof gregtech.block.TwigBlock) {
            tooltip.add(Component.translatable("gt6ce.jade.twig_content"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
