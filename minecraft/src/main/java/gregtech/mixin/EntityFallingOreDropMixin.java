package gregtech.mixin;

import gregtech.content.block.OreBlock;
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

/** Preserve the falling-ore host when vanilla falling entities drop items. */
@Mixin(Entity.class)
public abstract class EntityFallingOreDropMixin {
    @Inject(
            method =
                    "spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void gregtech$preserveFallingOre(ItemLike item, CallbackInfoReturnable<ItemEntity> result) {
        if (!((Object) this instanceof FallingBlockEntity falling)) return;
        BlockState state = falling.getBlockState();
        if (!(state.getBlock() instanceof OreBlock) || state.getBlock() != item || !OreBlock.falls(state)) return;
        result.setReturnValue(falling.spawnAtLocation(OreBlock.withStone(new ItemStack(item), state)));
    }
}
