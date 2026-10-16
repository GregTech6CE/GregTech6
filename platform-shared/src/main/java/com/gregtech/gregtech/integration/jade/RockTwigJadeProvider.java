package com.gregtech.gregtech.integration.jade;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.blockentity.RockBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Shows what a ground pebble / twig pile yields (rock material is client-synced). */
public enum RockTwigJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("gregtech", "rock_twig");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof RockBlockEntity rock) {
            GTMaterial material = GTMaterialRegistry.get(rock.getMaterial());
            String display = material != null && material.resolve().isValid()
                    ? material.resolve().getLocalName() : rock.getMaterial();
            tooltip.add(Component.translatable("gregtech.jade.rock_content", display));
        } else if (accessor.getBlock() instanceof com.gregtech.gregtech.block.TwigBlock) {
            tooltip.add(Component.translatable("gregtech.jade.twig_content"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
