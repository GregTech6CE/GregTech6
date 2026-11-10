package gregtech.content.block;

import gregtech.api.material.GTMaterial;
import gregtech.client.MaterialTooltips;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.engine.material.MaterialPresentation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.level.Level;
//?}
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
//?} else {
import org.jetbrains.annotations.Nullable;
//?}

import java.util.List;

/** Block item for {@link MaterialBlock} and {@link GTStoneBlock}. */
public class MaterialBlockItem extends BlockItem {
//? if >=1.21.1 {
//?} else {
    @Nullable
//?}
    private final GTMaterial material;

    public MaterialBlockItem(Block block, Properties properties) {
        super(block, properties);
        material = block instanceof MaterialBlockLike b ? b.material()
                : block instanceof GTStoneBlock b ? b.stoneMaterial()
                : block instanceof GTStoneSlabBlock b ? b.stoneMaterial() : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getBlock() instanceof OreBlock ore && OreBlock.isBrokenStack(stack)) {
            return Component.translatable("block.gt6ce.ore_broken", MaterialPresentation.name(ore.material()));
        }
        if (material != null && getBlock() instanceof MaterialBlockLike form) {
            return MaterialPresentation.formName(form.prefix().getName(), material,
                    Component.translatable(getDescriptionId(), MaterialPresentation.name(material)));
        }
        if (getBlock() instanceof GTStoneBlock stone) {
            return Component.translatable(getDescriptionId(), MaterialPresentation.name(stone.stoneMaterial()),
                    stone.variant().displayName());
        }
        if (getBlock() instanceof GTStoneSlabBlock stone) {
            return Component.translatable(getDescriptionId(), MaterialPresentation.name(stone.stoneMaterial()),
                    stone.variant().displayName());
        }
        return super.getName(stack);
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
//?}
        if (material != null && getBlock() instanceof MaterialBlockLike materialBlock) {
            MaterialTooltips.appendBlock(stack, materialBlock.material(), materialBlock.prefix(), tooltip, flag);
        } else if (getBlock() instanceof GTStoneBlock stoneBlock) {
            MaterialTooltips.appendStoneBlock(stack, stoneBlock.stoneMaterial(), stoneBlock.variant(), false, tooltip, flag);
        } else if (getBlock() instanceof GTStoneSlabBlock stoneSlab) {
            MaterialTooltips.appendStoneBlock(stack, stoneSlab.stoneMaterial(), stoneSlab.variant(), true, tooltip, flag);
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
