package com.gregtech.gregtech.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 magnet block — used in motor/generator crafting and magnetic field effects. */
public class MagnetBlock extends Block {
    private final String materialName;
    private final com.gregtech.gregtech.api.material.GTMaterial material;

    public MagnetBlock(String materialName, com.gregtech.gregtech.api.material.GTMaterial material, Properties properties) {
        super(properties);
        this.materialName = materialName;
        this.material = material;
    }

    public String materialName() { return materialName; }
    public int tintRgb() { return material.getColor(); }

    @Override
    public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<Component> tooltip, TooltipFlag flag) {
        // Material names/properties are supplied by the shared material tooltip handler.
        // There is no original gt.tooltip.magnet declaration.
    }
}
