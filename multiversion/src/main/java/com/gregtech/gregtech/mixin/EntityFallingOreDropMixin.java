package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.misc.SpikeBlock;
import com.gregtech.gregtech.item.SpikeBlockItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve broken-ore host and Spike material/mode when vanilla falling entities drop items. */
@Mixin(Entity.class)
public abstract class EntityFallingOreDropMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void gregtech$preserveFallingBrokenOre(ItemLike item,
            CallbackInfoReturnable<ItemEntity> result) {
        if (!((Object) this instanceof FallingBlockEntity falling)) return;
        BlockState state = falling.getBlockState();
        // Vanilla falling drops pass only ItemLike; ordinary BlockItem loses source meta7/15 identity.
        // Reuse the already registered block item and its native state writer, not a new entity type.
        if (state.getBlock() instanceof SpikeBlock spike && state.getBlock() == item) {
            result.setReturnValue(falling.spawnAtLocation(SpikeBlockItem.variant(spike,
                    state.getValue(SpikeBlock.MODE), state.getValue(SpikeBlock.SECONDARY))));
            return;
        }
        if (!(state.getBlock() instanceof OreBlock) || state.getBlock() != item || !OreBlock.isBroken(state)) return;
        result.setReturnValue(falling.spawnAtLocation(OreBlock.withStone(new ItemStack(item), state)));
    }
}
