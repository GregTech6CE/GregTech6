package com.gregtech.gregtech.block;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.stone.StoneMaterialWeights;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
import com.gregtech.gregtech.client.MaterialTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Block item for {@link MaterialBlock} and {@link GTStoneBlock}. */
public class MaterialBlockItem extends BlockItem {
    @Nullable
    private final GTMaterial material;

    public MaterialBlockItem(Block block, Properties properties) {
        super(block, properties);
        if (block instanceof MaterialBlockLike materialBlock) {
            this.material = materialBlock.material();
        } else if (block instanceof GTStoneBlock stoneBlock) {
            this.material = stoneBlock.stoneMaterial();
        } else if (block instanceof GTStoneSlabBlock stoneSlab) {
            this.material = stoneSlab.stoneMaterial();
        } else {
            this.material = null;
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getBlock() instanceof OreBlock ore && OreBlock.isBrokenStack(stack)) {
            return Component.translatable("block.gregtech.ore_broken", MaterialPresentation.name(ore.material()));
        }
        if (material != null && getBlock() instanceof MaterialBlockLike form) {
            return Component.translatableWithFallback("oredict." + form.prefix().getName() + material.getName(), "%s",
                    Component.translatable(getDescriptionId(), MaterialPresentation.name(material)));
        }
        if (getBlock() instanceof GTStoneBlock stoneBlock) {
            return Component.translatable(getDescriptionId(),
                    MaterialPresentation.name(stoneBlock.stoneMaterial()),
                    stoneBlock.variant().displayName());
        }
        if (getBlock() instanceof GTStoneSlabBlock stoneSlab) {
            return Component.translatable(getDescriptionId(),
                    MaterialPresentation.name(stoneSlab.stoneMaterial()),
                    stoneSlab.variant().displayName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (material != null && getBlock() instanceof MaterialBlockLike materialBlock) {
            MaterialTooltips.appendBlock(stack, materialBlock.material(), materialBlock.prefix(), tooltip, flag);
        } else if (getBlock() instanceof GTStoneBlock stoneBlock) {
            MaterialTooltips.appendStoneBlock(stack, stoneBlock.stoneMaterial(), stoneBlock.variant(), false, tooltip, flag);
        } else if (getBlock() instanceof GTStoneSlabBlock stoneSlab) {
            MaterialTooltips.appendStoneBlock(stack, stoneSlab.stoneMaterial(), stoneSlab.variant(), true, tooltip, flag);
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public int getTintColor() {
        return material == null ? 0xFFFFFF : 0xFF000000 | (material.getColor() & 0xFFFFFF);
    }

    @Nullable
    public GTMaterial material() {
        return material;
    }
}
