package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.data.BlockHarvestPolicy;
import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** GT_Tool_Wrench_LV: original wrench targets, material quality, and twice material speed. */
public final class ElectricWrenchHarvest {
    private static final net.minecraft.tags.TagKey<Block> WRENCH=BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","mineable/wrench"));
    private static final net.minecraft.tags.TagKey<Block> MONKEY_WRENCH=BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","mineable/monkey_wrench"));
    private ElectricWrenchHarvest() {}
    public static boolean target(BlockState state) {
        Block block=state.getBlock();
        if (block instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock pipe) {
            var texture=pipe.spec().material().getTextureSet();
            if (texture==com.gregtech.gregtech.api.material.MaterialTextureSet.WOOD
                    || texture==com.gregtech.gregtech.api.material.MaterialTextureSet.RUBBER) return false;
        }
        if(block instanceof com.gregtech.gregtech.block.machine.ItemPipeBlock pipe){var texture=pipe.spec().material().getTextureSet();if(texture==com.gregtech.gregtech.api.material.MaterialTextureSet.WOOD||texture==com.gregtech.gregtech.api.material.MaterialTextureSet.RUBBER)return false;}
        return state.is(WRENCH) || state.is(MONKEY_WRENCH)
                || block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
                || block instanceof net.minecraft.world.level.block.piston.PistonHeadBlock
                || block instanceof RedstoneLampBlock || block instanceof HopperBlock || block instanceof DispenserBlock;
    }
    public static int requiredQuality(BlockState state) {
        int vanilla=state.is(BlockTags.NEEDS_DIAMOND_TOOL)?3:state.is(BlockTags.NEEDS_IRON_TOOL)?2:state.is(BlockTags.NEEDS_STONE_TOOL)?1:0;
        return Math.max(vanilla,BlockHarvestPolicy.level(state.getBlock()));
    }
    public static boolean canHarvest(ElectricToolItem tool, ItemStack stack, BlockState state) {
        return tool.toolName().equals("Wrench") && tool.canInteract(stack) && target(state)
                && (tool.headMaterial(stack).getToolQuality()+tool.definition().quality())>=requiredQuality(state);
    }
    public static float speed(ElectricToolItem tool, ItemStack stack) {
        return Math.max(Float.MIN_NORMAL,tool.definition().speed()*tool.headMaterial(stack).getToolSpeed());
    }
}
