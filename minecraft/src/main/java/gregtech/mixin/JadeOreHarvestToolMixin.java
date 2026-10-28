package gregtech.mixin;

import com.google.common.collect.ImmutableList;
import gregtech.content.block.OreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shared ore blocks have state-dependent hosts that Jade's block tags cannot represent. */
@Pseudo
@Mixin(targets = "snownee.jade.addon.harvest.HarvestToolProvider", remap = false)
public abstract class JadeOreHarvestToolMixin {
    @Inject(method = "getTool", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void gt6ce$hostHarvestTool(
            BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<ImmutableList<ItemStack>> result) {
        if (!(state.getBlock() instanceof OreBlock) || !OreBlock.stoneOf(state).isSediment()) return;
        // This is the recommended tool category, not a tier/harvest permission override.
        // Keep Jade's normal cache, display settings and actual held-tool check.
        int tier =
                state.requiresCorrectToolForDrops() ? gregtech.engine.tool.GTToolHelper.requiredHarvestLevel(state) : 0;
        var shovel = tier <= 0
                ? Items.WOODEN_SHOVEL
                : tier == 1
                        ? Items.STONE_SHOVEL
                        : tier == 2 ? Items.IRON_SHOVEL : tier == 3 ? Items.DIAMOND_SHOVEL : Items.NETHERITE_SHOVEL;
        result.setReturnValue(ImmutableList.of(new ItemStack(shovel)));
    }
}
