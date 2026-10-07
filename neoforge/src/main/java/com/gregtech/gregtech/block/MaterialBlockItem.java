package com.gregtech.gregtech.block;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Original material/stone block item identity at the Neo item boundary. */
public class MaterialBlockItem extends BlockItem {
    private final GTMaterial material;

    public MaterialBlockItem(Block block, Properties properties) {
        super(block, properties);
        material = block instanceof MaterialBlockLike b ? b.material()
                : block instanceof GTStoneBlock b ? b.stoneMaterial()
                : block instanceof GTStoneSlabBlock b ? b.stoneMaterial() : null;
    }

    private static Component name(GTMaterial material) {
        return Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback());
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getBlock() instanceof OreBlock ore && OreBlock.isBrokenStack(stack))
            return Component.translatable("block.gregtech.ore_broken", name(ore.material()));
        if (material != null && getBlock() instanceof MaterialBlockLike form)
            return Component.translatableWithFallback(com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceTranslationKey(form.prefix().getName(), material.getName()), "%s",
                    Component.translatable(getDescriptionId(), name(material)));
        if (getBlock() instanceof GTStoneBlock stone)
            return Component.translatable(getDescriptionId(), name(stone.stoneMaterial()), stone.variant().displayName());
        if (getBlock() instanceof GTStoneSlabBlock stone)
            return Component.translatable(getDescriptionId(), name(stone.stoneMaterial()), stone.variant().displayName());
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (material != null && getBlock() instanceof MaterialBlockLike materialBlock) {
            com.gregtech.gregtech.client.MaterialTooltips.appendBlock(stack, materialBlock.material(), materialBlock.prefix(), tooltip, flag);
        } else if (getBlock() instanceof GTStoneBlock stoneBlock) {
            com.gregtech.gregtech.client.MaterialTooltips.appendStoneBlock(stack, stoneBlock.stoneMaterial(), stoneBlock.variant(), false, tooltip, flag);
        } else if (getBlock() instanceof GTStoneSlabBlock stoneSlab) {
            com.gregtech.gregtech.client.MaterialTooltips.appendStoneBlock(stack, stoneSlab.stoneMaterial(), stoneSlab.variant(), true, tooltip, flag);
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public int getTintColor() {
        return material == null ? 0xFFFFFF : 0xFF000000 | (material.getColor() & 0xFFFFFF);
    }

    public GTMaterial material() { return material; }
}
