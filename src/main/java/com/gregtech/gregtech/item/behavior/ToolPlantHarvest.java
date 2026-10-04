package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** GT_Tool_Sword/Knife/Sense drop conversion; original seed drops are retained. */
public final class ToolPlantHarvest {
    private ToolPlantHarvest() {}
    public static void harvest(GTToolType type, ItemStack tool, Level level, BlockState state, BlockPos pos) {
        if (level.isClientSide || type != GTToolType.SWORD && type != GTToolType.KNIFE && type != GTToolType.SENSE) return;
        int fortune = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, tool);
        int count;
        if (state.is(Blocks.GRASS) || state.is(Blocks.FERN))
            count = 1 + level.random.nextInt(1 + fortune);
        else if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN))
            count = 2 + level.random.nextInt(1 + fortune) + level.random.nextInt(1 + fortune);
        else if (state.is(Blocks.DEAD_BUSH)) {
            var dead = com.gregtech.gregtech.api.material.GTMaterialRegistry.get("WoodDead");
            Block.popResource(level, pos, com.gregtech.gregtech.registry.GTItems.getStack(
                    com.gregtech.gregtech.data.MaterialPrefix.stick, dead, 1 + level.random.nextInt(2 + fortune)));
            return;
        } else if (state.is(Blocks.VINE)) {
            Block.popResource(level, pos, new ItemStack(Blocks.VINE));
            return;
        } else return;
        var grass = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", "grass_2"));
        if (grass == net.minecraft.world.item.Items.AIR) throw new IllegalStateException("Missing source cut grass item");
        Block.popResource(level, pos, new ItemStack(grass, count));
    }
}
