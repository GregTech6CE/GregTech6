package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

/** GT_Tool_Axe:105-141: only the same block in the column above, with increasing wear. */
public final class AxeColumnHarvest {
    private static final ThreadLocal<Boolean> HARVESTING = ThreadLocal.withInitial(() -> false);
    private AxeColumnHarvest() {}
    public static boolean harvesting() { return HARVESTING.get(); }

    public static boolean eligible(Player player, ItemStack tool, BlockState state) {
        GTToolType type = GTToolHelper.getType(tool);
        return (type == GTToolType.AXE || type == GTToolType.DOUBLE_AXE)
                && GTToolHelper.isUsable(tool) && !player.isShiftKeyDown()
                && !ModList.get().isLoaded("treecapitator") && !ModList.get().isLoaded("treecap")
                && !state.getBlock().getClass().getName().startsWith("com.ferreusveritas.dynamictrees")
                && (state.is(BlockTags.LOGS) || state.getBlock() instanceof HugeMushroomBlock);
    }

    public static float speed(Player player, ItemStack tool, BlockState state, BlockPos pos, float speed) {
        if (!eligible(player, tool, state)) return speed;
        float weight = 1f, increment = 1f;
        for (BlockPos next = pos.above(); next.getY() < player.level().getMaxBuildHeight()
                && player.level().hasChunkAt(next)
                && player.level().getBlockState(next).getBlock() == state.getBlock(); next = next.above()) {
            increment += 0.1f;
            weight += increment;
        }
        return state.getBlock() instanceof HugeMushroomBlock && weight > 2f
                ? speed / (4f * weight) : 2f * speed / weight;
    }

    public static void harvest(ServerPlayer player, ItemStack tool, BlockState state, BlockPos pos) {
        if (harvesting() || !eligible(player, tool, state) || player.getAbilities().instabuild) return;
        int cost = (int) Math.ceil(Math.max(0, state.getDestroySpeed(player.level(), pos))
                * GTToolHelper.getType(tool).damagePerBlockBreak());
        HARVESTING.set(true);
        try {
            for (BlockPos next = pos.above(); next.getY() < player.level().getMaxBuildHeight()
                    && player.level().hasChunkAt(next)
                    && player.level().getBlockState(next).getBlock() == state.getBlock(); next = next.above()) {
                cost++;
                if (!GTToolHelper.isUsable(tool) || GTToolHelper.getMaxDurability(tool) - tool.getDamageValue() < cost) break;
                // Uses the player's normal harvest path: protection events, drops and enchantments.
                if (!player.gameMode.destroyBlock(next)) break;
                GTToolHelper.damageForUse(tool, cost, player);
            }
        } finally {
            HARVESTING.remove();
        }
    }
}
