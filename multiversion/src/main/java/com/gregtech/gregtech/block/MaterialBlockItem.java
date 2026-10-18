package com.gregtech.gregtech.block;
//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.api.material.MaterialPresentation;
//?}

import com.gregtech.gregtech.api.material.GTMaterial;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.api.material.MaterialPresentation;
*///?} else {
import com.gregtech.gregtech.block.stone.StoneMaterialWeights;
import com.gregtech.gregtech.block.stone.StoneVariant;
//?}
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import com.gregtech.gregtech.block.stone.GTStoneSlabBlock;
//? if >=1.21.1 {
//?} else {
import com.gregtech.gregtech.client.MaterialTooltips;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
//?}
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
//?} else {
import org.jetbrains.annotations.Nullable;
//?}

/** Original material/stone block item identity at the Neo item boundary. */
//? if >=1.21.1 {
//?} else {
import java.util.List;

/** Block item for {@link MaterialBlock} and {@link GTStoneBlock}. */
//?}
public class MaterialBlockItem extends BlockItem {
//? if >=1.21.1 {
//?} else {
    @Nullable
//?}
    private final GTMaterial material;

    public MaterialBlockItem(Block block, Properties properties) {
        super(block, properties);
//? if >=1.21.1 {
        /*material = block instanceof MaterialBlockLike b ? b.material()
                : block instanceof GTStoneBlock b ? b.stoneMaterial()
                : block instanceof GTStoneSlabBlock b ? b.stoneMaterial() : null;
    }

    private static Component name(GTMaterial material) {
        return MaterialPresentation.name(material);
*///?} else {
        if (block instanceof MaterialBlockLike materialBlock) {
            this.material = materialBlock.material();
        } else if (block instanceof GTStoneBlock stoneBlock) {
            this.material = stoneBlock.stoneMaterial();
        } else if (block instanceof GTStoneSlabBlock stoneSlab) {
            this.material = stoneSlab.stoneMaterial();
        } else {
            this.material = null;
        }
//?}
    }

    @Override
    public Component getName(ItemStack stack) {
//? if >=1.21.1 {
        /*if (getBlock() instanceof OreBlock ore && OreBlock.isBrokenStack(stack))
            return Component.translatable("block.gregtech.ore_broken", name(ore.material()));
        if (material != null && getBlock() instanceof MaterialBlockLike form)
*///?} else {
        if (getBlock() instanceof OreBlock ore && OreBlock.isBrokenStack(stack)) {
            return Component.translatable("block.gregtech.ore_broken", MaterialPresentation.name(ore.material()));
        }
        if (material != null && getBlock() instanceof MaterialBlockLike form) {
//?}
            return MaterialPresentation.formName(form.prefix().getName(), material,
//? if >=1.21.1 {
                    /*Component.translatable(getDescriptionId(), name(material)));
        if (getBlock() instanceof GTStoneBlock stone)
            return Component.translatable(getDescriptionId(), name(stone.stoneMaterial()), stone.variant().displayName());
        if (getBlock() instanceof GTStoneSlabBlock stone)
            return Component.translatable(getDescriptionId(), name(stone.stoneMaterial()), stone.variant().displayName());
*///?} else {
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
//?}
        return super.getName(stack);
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
//?}
        if (material != null && getBlock() instanceof MaterialBlockLike materialBlock) {
//? if >=1.21.1 {
            /*com.gregtech.gregtech.client.MaterialTooltips.appendBlock(stack, materialBlock.material(), materialBlock.prefix(), tooltip, flag);
*///?} else {
            MaterialTooltips.appendBlock(stack, materialBlock.material(), materialBlock.prefix(), tooltip, flag);
//?}
        } else if (getBlock() instanceof GTStoneBlock stoneBlock) {
//? if >=1.21.1 {
            /*com.gregtech.gregtech.client.MaterialTooltips.appendStoneBlock(stack, stoneBlock.stoneMaterial(), stoneBlock.variant(), false, tooltip, flag);
*///?} else {
            MaterialTooltips.appendStoneBlock(stack, stoneBlock.stoneMaterial(), stoneBlock.variant(), false, tooltip, flag);
//?}
        } else if (getBlock() instanceof GTStoneSlabBlock stoneSlab) {
//? if >=1.21.1 {
            /*com.gregtech.gregtech.client.MaterialTooltips.appendStoneBlock(stack, stoneSlab.stoneMaterial(), stoneSlab.variant(), true, tooltip, flag);
*///?} else {
            MaterialTooltips.appendStoneBlock(stack, stoneSlab.stoneMaterial(), stoneSlab.variant(), true, tooltip, flag);
//?}
        }
//? if >=1.21.1 {
        /*super.appendHoverText(stack, context, tooltip, flag);
*///?} else {
        super.appendHoverText(stack, level, tooltip, flag);
//?}
    }

    public int getTintColor() {
        return material == null ? 0xFFFFFF : 0xFF000000 | (material.getColor() & 0xFFFFFF);
    }

//? if >=1.21.1 {
    /*public GTMaterial material() { return material; }
*///?} else {
    @Nullable
    public GTMaterial material() {
        return material;
    }
//?}
}
