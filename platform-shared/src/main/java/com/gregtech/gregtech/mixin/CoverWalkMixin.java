package com.gregtech.gregtech.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Block.class)
public abstract class CoverWalkMixin {
    @Inject(method="stepOn",at=@At("HEAD"),require=1)
    private void gregtech$coverWalk(Level level,BlockPos pos,BlockState state,Entity entity,CallbackInfo ci){com.gregtech.gregtech.content.cover.CoverWorldInteraction.walk(level,pos,entity);}
}
